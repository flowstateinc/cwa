(ns web.postgres.employment
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::created-at inst?)
(s/def ::id pos-int?)
(s/def ::updated-at (s/nilable inst?))
(s/def ::public-id uuid?)

(s/def ::organization-id pos-int?)
(s/def ::title (s/nilable string?))
(s/def ::user-id pos-int?)
