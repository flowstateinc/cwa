(ns web.test.hooks
  (:require
   [io.pedestal.log :as log]
   [web.test.system :as test.system]))

(defn pre-suite
  [suite _test-plan]
  (log/info :msg "Warming system before test suite...")
  (test.system/must-start-system (test.system/system))
  suite)
