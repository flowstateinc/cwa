(ns web.spec
  (:require
   [clojure.spec.alpha :as s]
   [next.jdbc.specs]))

(comment
  (s/describe (s/spec :next.jdbc.specs/jdbcUrl)))

(def retain
  "Prevent naive tooling from removing side-effectful requires with this one
  simple require."
  ::retain)

;;; --------------------------------------------------------------------------------------------------------------------
;;; Buster

(s/def :web.assets/resources (s/coll-of string? :kind set?))

(s/def :web.assets/config
  (s/keys :req-un [:web.assets/resources]))

;;; ----------------------------------------------------------------------------
;;; Concierge

(s/def :web.concierge/config
  (s/keys))

;;; ----------------------------------------------------------------------------
;;; Migrator

(s/def :web.migrator/database-url :next.jdbc.specs/jdbcUrl)
(s/def :web.migrator/throw-exceptions? boolean?)

(s/def :web.migrator/config
  (s/keys :req-un [:web.migrator/database-url
                   :web.migrator/throw-exceptions?]))

;;; ----------------------------------------------------------------------------
;;; PostgreSQL

;; Anywhere we want to refer to a bigserial primary key, use `::postgres/id`.
(s/def :web.postgres/id pos-int?)

(s/def :web.postgres/database-url :next.jdbc.specs/jdbcUrl)

(s/def :web.postgres/config
  (s/keys :req-un [:web.postgres/database-url]))

;;; ----------------------------------------------------------------------------
;;; Service

(s/def :web.service/http-host string?)
(s/def :web.service/http-port (s/or :zero zero? :pos-int pos-int?))
(s/def :web.service/join? boolean?)

(s/def :web.service/config
  (s/keys :req-un [:web.service/http-host
                   :web.service/http-port
                   :web.service/join?]))

;;; ----------------------------------------------------------------------------
;;; System

(s/def :web.system/buster :web.assets/config)
(s/def :web.system/concierge :web.concierge/config)
(s/def :web.system/migrator :web.migrator/config)
(s/def :web.system/postgres :web.postgres/config)
(s/def :web.system/service :web.service/config)

(s/def :web.system/config
  (s/keys :req-un [:web.system/buster
                   :web.system/concierge
                   :web.system/migrator
                   :web.system/postgres
                   :web.system/service]))
