(ns user
  (:require
   [clojure.spec.alpha :as s]
   [clojure.tools.namespace.repl]
   [com.stuartsierra.component.user-helpers]
   [expound.alpha]))

(s/check-asserts true)
(alter-var-root #'s/*explain-out* (constantly expound.alpha/printer))

(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test")
(com.stuartsierra.component.user-helpers/set-dev-ns 'web.dev)

(comment
  (clojure.tools.namespace.repl/refresh))
