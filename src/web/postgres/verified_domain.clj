(ns web.postgres.verified-domain
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::created-at inst?)
(s/def ::id pos-int?)
(s/def ::updated-at (s/nilable inst?))

(s/def ::domain string?)
(s/def ::organization-id pos-int?)
