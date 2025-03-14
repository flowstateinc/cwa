(ns web.postgres.group
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::created-at inst?)
(s/def ::id pos-int?)
(s/def ::updated-at (s/nilable inst?))
(s/def ::public-id uuid?)

(s/def ::name (s/nilable string?))
