(ns web.dev
  (:require
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl :refer [set-init start stop]]
   [io.pedestal.log :as log]
   [web.config :as config]
   [web.system :as system]))

(defn- system-ex-data
  [cause]
  (ex-data (last (take-while some? (iterate ex-cause cause)))))

(defn before-refresh
  []
  (try
    (log/trace :msg "Stopping development system...")
    (stop)
    (catch Exception exception
      (log/warn :in        ::before-refresh
                :ex-data   (system-ex-data exception)
                :exception exception))))

(defn after-refresh
  []
  (try
    (log/trace :msg "Starting development system...")
    (start)
    (catch Exception exception
      (log/warn :in        ::after-refresh
                :ex-data   (system-ex-data exception)
                :exception exception)
      (when-let [system (-> exception ex-data :system)]
        (log/debug :in  ::after-refresh
                   :msg "Stopping broken system...")
        (component/stop-system system)))))

(set-init
 (fn [_system] (system/system (config/read-config))))
