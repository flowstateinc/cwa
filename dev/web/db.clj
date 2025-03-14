(ns web.db
  (:require
   [next.jdbc :as jdbc]
   [ragtime.next-jdbc]
   [ragtime.repl]
   [ragtime.strategy]))

(defn rollback
  []
  (let [migrations (ragtime.next-jdbc/load-resources "migrations")]
    (ragtime.repl/rollback
     {:datastore  (ragtime.next-jdbc/sql-database
                   (jdbc/get-datasource {:dbtype "postgres"
                                         :dbname "web_dev"})
                   {:migrations-table "migrations"})
      :migrations migrations})))

(comment
  (do
    (require '[com.stuartsierra.component.repl :refer [system]]
             '[web.postgres :as postgres])
    (postgres/tables (:postgres system)))

  (rollback))
