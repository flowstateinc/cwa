(ns web.concierge
  (:require
   [clojure.spec.alpha :as s]))

(defrecord Concierge [])

(s/fdef make-concierge
  :args (s/cat :config ::config)
  :ret  ::config)

(defn make-concierge
  [config]
  (map->Concierge config))
