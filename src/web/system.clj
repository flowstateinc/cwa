(ns web.system
  (:require
   [clojure.spec.alpha :as s]
   [com.stuartsierra.component :as component]
   [io.pedestal.log :as log]
   [medley.core :as medley]
   [web.assets :as assets]
   [web.concierge :as concierge]
   [web.postgres :as postgres]
   [web.service :as service]
   [web.spec])
  (:import
   (java.security Security)))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Bootstrapper

(defrecord Bootstrapper []
  component/Lifecycle
  (start [this]
    (Security/setProperty "networkaddress.cache.ttl" (str 60 #_seconds))
    (Thread/setDefaultUncaughtExceptionHandler
     (reify Thread$UncaughtExceptionHandler
       (uncaughtException [_ thread exception]
         (log/error :msg       "Uncaught exception!?"
                    :exception exception
                    :thread    (.getName thread)))))
    this)
  (stop [this]
    this))

(defn make-bootstrapper
  [config]
  (map->Bootstrapper config))

(defmethod print-method Bootstrapper
  [_ ^java.io.Writer w]
  (.write w "#<Bootstrapper>"))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Components

(defn components
  [config]
  (component/system-map
   :bootstrapper (make-bootstrapper        (:bootstrapper config))
   :buster       (assets/make-buster       (:buster config))
   :concierge    (concierge/make-concierge (:concierge config))
   :migrator     (postgres/make-migrator   (:migrator config))
   :postgres     (postgres/make-pool       (:postgres config))
   :service      (service/make-service     (:service config))))

(def dependencies
  {:concierge [:postgres]
   :service   [:bootstrapper :buster :concierge :migrator :postgres]})

(defn system
  [config]
  {:pre [(s/assert ::config config)]}
  (component/system-using
   (medley/mapply component/system-map (components config))
   dependencies))
