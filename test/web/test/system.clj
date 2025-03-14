(ns web.test.system
  (:require
   [com.stuartsierra.component :as component]
   [web.config :as config]
   [web.postgres :as postgres]
   [web.system :as system]))

(defn- system-ex-info
  [cause]
  (let [origin (last (take-while some? (iterate ex-cause cause)))]
    (ex-info (format "Error starting system: %s" (ex-message origin))
             (ex-data origin)
             cause)))

(defn must-start-system
  [system-map]
  (try
    (component/start-system system-map)
    (catch Exception cause
      (some-> cause ex-data :system component/stop-system)
      (throw (system-ex-info cause)))))

(defmacro with-system
  {:arglists     ['([system-binding system-map] body*)]
   :style/indent 1}
  [& more]
  (let [[[system-binding system-map] & body] more]
    `(let [running#        (must-start-system ~system-map)
           ~system-binding running#]
       (try
         ~@body
         (finally
           (component/stop-system running#))))))

(defn truncate
  [connectable]
  (doseq [table (postgres/tables connectable)]
    (postgres/execute! connectable {:truncate [table :restart :identity :cascade]})))

(defn system
  []
  ;; NOTE This database URL is not kept in sync with a provided DATABASE_URL.
  ;;
  ;; We might need to improve this for those not using `devenv`.
  (let [database-url "jdbc:postgresql://127.0.0.1:5432/web_test?user=web&password=please"]
    (-> (config/read-config)
        (assoc-in [:migrator :database-url] database-url)
        (assoc-in [:migrator :dump-structure?] false)
        (assoc-in [:postgres :database-url] database-url)
        (assoc-in [:service :http-port] 0)
        system/components
        (component/system-using system/dependencies)
        (component/subsystem #{:service}))))
