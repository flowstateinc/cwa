(ns web.postgres.user
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(s/def ::created-at inst?)
(s/def ::id pos-int?)
(s/def ::public-id uuid?)
(s/def ::updated-at (s/nilable inst?))

(s/def ::email (s/and string? #(str/includes? % "@")))
