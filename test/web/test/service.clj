(ns web.test.service
  (:require
   [hato.client :as http]
   [web.service :as service]))

(def ^:private http-client
  (http/build-http-client {:connect-timeout 100}))

(defn- cleanup-hato-response
  [response]
  (-> response
      (select-keys #{:body :headers :status})
      (update :headers #(into (sorted-map) (dissoc % ":status")))))

(defn request
  [service request-options]
  (-> (merge {:throw-exceptions? false} request-options)
      (assoc :http-client http-client)
      (update :url #(service/service-url service %))
      http/request
      cleanup-hato-response))
