(ns web.postgres
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [honey.sql :as sql]
   [inflections.core :as infl]
   [io.pedestal.log :as log]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc.connection]
   [next.jdbc.protocols :as jdbc.protocols]
   [next.jdbc.result-set :as jdbc.result-set]
   [ragtime.next-jdbc]
   [ragtime.repl]
   [ragtime.strategy]
   [web.spec])
  (:import
   (com.zaxxer.hikari HikariDataSource)
   (java.sql ResultSet ResultSetMetaData)))

;;; ----------------------------------------------------------------------------
;;; Specs

(comment web.spec/retain)

;;; ----------------------------------------------------------------------------
;;; Result sets

(defn- singularize-segmented
  [s]
  (if-let [n (str/last-index-of s "_")]
    (str (subs s 0 n) "_" (-> s (subs (inc n)) infl/singular))
    (infl/singular s)))

;; No enums, for now.
(def ^:private enum->namespace
  {})

(defn- qualify
  [x]
  (let [entity-name (-> x singularize-segmented csk/->kebab-case-string)]
    (cond->> entity-name
      (not (str/blank? entity-name))
      (str "web.postgres."))))

(def qualify-memo (memoize qualify))

;; No special case column names, for now.
(def ^:private labels
  {})

(defn- label
  [x]
  (or (get labels x)
      (csk/->kebab-case-string x)))

(def label-memo (memoize label))

(defn- resultset-builder
  [^ResultSet result-set options]
  (jdbc.result-set/as-modified-maps result-set
                                    (assoc options
                                           :qualifier-fn qualify-memo
                                           :label-fn     label-memo)))

(defn- column-reader
  [^ResultSet rs ^ResultSetMetaData rsmeta ^Integer integer]
  (let [i         (int integer)
        object    (.getObject rs i)
        type-name (.getColumnTypeName rsmeta i)
        s         (enum->namespace type-name)]
    (try
      (if (and (some? object) (some? s))
        (keyword (str "web.postgres." s) object)
        object)
      (catch Exception cause
        (throw (ex-info "Failed to read column?!"
                        {:object object :type-name type-name :s s}
                        cause))))))

(def qualified-builder-fn
  (jdbc.result-set/as-maps-adapter resultset-builder column-reader))

(def execute-defaults
  (assoc jdbc/snake-kebab-opts :builder-fn qualified-builder-fn))

;;; ----------------------------------------------------------------------------
;;; Execute!

(defn- ->conn
  [connectable]
  (or (::conn connectable) (:conn connectable) connectable))

(defn execute!
  ([connectable query]
   (execute! connectable query nil))
  ([connectable query options]
   (jdbc/execute! (->conn connectable)
                  (sql/format query options)
                  (merge execute-defaults options))))

(defn execute-one!
  ([connectable query]
   (execute-one! connectable query nil))
  ([connectable query options]
   (jdbc/execute-one! (->conn connectable)
                      (sql/format query options)
                      (merge execute-defaults options))))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Tables

(def ^:private tables-never-to-truncate
  "Tables one must never truncate!"
  #{"migrations"})

(defn tables
  [postgres]
  {:post [(every? string? %)]}
  (into (sorted-set)
        (map :web.postgres.pg-table/tablename)
        (execute! postgres {:select [:tablename]
                            :from   [:pg-catalog.pg-tables]
                            :where (into [:and
                                          [:not= "information_schema" :schemaname]
                                          [:not= "pg_catalog" :schemaname]]
                                         (map #(vector :not= % :tablename))
                                         (sort tables-never-to-truncate))})))

;;; ----------------------------------------------------------------------------
;;; Migrations

(defn migrate
  [migrator]
  (let [database-url (:database-url migrator)
        ds           (jdbc/get-datasource {:dbtype  "postgres"
                                           :jdbcUrl database-url})
        migrations   (ragtime.next-jdbc/load-resources "migrations")
        ops          (atom [])
        reporter     (fn [_ op id]
                       (log/info :msg "Applying migration..." :op op :id id)
                       (swap! ops conj {:op op :id id}))]
    (try
      (ragtime.repl/migrate {:datastore  (ragtime.next-jdbc/sql-database
                                          ds {:migrations-table "migrations"})
                             :migrations migrations
                             :reporter   reporter
                             :strategy   ragtime.strategy/apply-new})
      @ops
      (catch Exception exception
        (if (:throw-exceptions? migrator)
          (throw (ex-info "Migration error?!"
                          {:migrations migrations}
                          exception))
          (log/warn :msg             "Migration error!?!!"
                    :migration-names (mapv :id migrations)
                    :exception       exception))))))

(defrecord Migrator [database-url throw-exceptions?]
  component/Lifecycle
  (start [this]
    (migrate this)
    this)
  (stop [this]
    this))

(s/fdef make-migrator
  :args (s/cat :config :web.migrator/config)
  :ret  :web.migrator/config)

(defn make-migrator
  [config]
  (map->Migrator config))

;;; ----------------------------------------------------------------------------
;;; Pool

(defrecord Pool [crypto database-url datasource]
  component/Lifecycle
  (start [this]
    (let [ds (jdbc.connection/->pool HikariDataSource {:jdbcUrl database-url})]
      (with-open [conn (jdbc/get-connection ds)]
        (log/trace :msg        "Connection established! Closing."
                   :datasource ds)
        (.close ^java.sql.Connection conn))
      (assoc this :datasource ds)))
  (stop [this]
    (when-let [ds (:datasource this)]
      (log/trace :msg          "Shutting down connection pool..."
                 :database-url database-url)
      (.close ^com.zaxxer.hikari.HikariDataSource ds)))

  next.jdbc.protocols/Connectable
  (get-connection [this opts]
    (jdbc/get-connection (:datasource this) opts)))

(defn make-pool
  [config]
  {:pre [(s/valid? ::config config)]}
  (map->Pool config))
