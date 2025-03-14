(ns web.interceptors
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.interceptor :refer [interceptor]]
   [io.pedestal.interceptor.chain :as chain]
   [io.pedestal.interceptor.error :as error]
   [io.pedestal.log :as log]
   [ring.util.response :as response]
   [ring.util.time]
   [web.assets :as assets]
   [web.page :as page]))

;;; ----------------------------------------------------------------------------
;;; Chain

(defn prepend
  "Prepend an `interceptor` to the given interceptor `chain`. Note, Pedestal
  expects `chain` to be a vector so we both require and return a vector."
  [chain interceptor]
  {:pre [(vector? chain)]}
  (into [interceptor] chain))

(defn terminate
  [context response]
  (-> context (assoc :response response) chain/terminate))

;;; ----------------------------------------------------------------------------
;;; Request

(defn make-request-interceptor
  "Associate value `v` into the context's request under key `k`."
  [k v]
  (interceptor {:name ::make-request :enter #(assoc-in % [:request k] v)}))

;;; ----------------------------------------------------------------------------
;;; Referrer policy

(defn make-referrer-policy-interceptor
  "Adds a `Referrer-Policy` header with the given `policy` to any response
  without an existing `Referrer-Policy` header.

  Unlike Pedestal, header names are treated as case-insensitive. Defaults to
  `strict-origin`.

  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy"
  ([]
   (make-referrer-policy-interceptor "strict-origin"))
  ([policy]
   (interceptor
    {:name ::referrer-policy
     :leave
     (fn leave-referrer-policy
       [{:keys [response] :as context}]
       (cond-> context
         (http/response? response)
         (update :response response/update-header
                 "referrer-policy" #(or % policy))))})))

;;; ----------------------------------------------------------------------------
;;; State

(def ^:private state-key ::state)

(defn- must-getter
  [& ks]
  (fn must-get
    [m]
    {:post [(not (identical? % ::not-found))]}
    (get-in m ks ::not-found)))

(def context->buster (must-getter :request state-key :buster))
(def context->concierge (must-getter :request state-key :concierge))
(def context->postgres  (must-getter :request state-key :postgres))

(def request->buster (must-getter state-key :buster))
(def request->concierge (must-getter state-key :concierge))
(def request->postgres  (must-getter state-key :postgres))

(defn make-state-interceptor
  [state]
  (make-request-interceptor state-key state))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Assets

(def reload-assets-interceptor
  (interceptor
   {:name ::reload-assets
    :enter
    (fn enter-reload-assets
      [{:keys [request] :as context}]
      (log/trace :msg            "Regurgitating assets..."
                 :request-method (:request-method request)
                 :uri            (:uri request))
      (update-in context [:request state-key :buster] assets/regurgitate))}))

(def asset-interceptor
  (interceptor
   {:name ::asset
    :enter
    (fn enter-asset
      [{:keys [request] :as context}]
      (let [buster (context->buster context)]
        (if-let [{::assets/keys [content-type resource]} (assets/lookup buster request)]
          (do
            (log/trace :in ::asset :content-type content-type :resource resource)
            (let [{:keys [content
                          content-length
                          last-modified]} (response/resource-data resource)

                  last-modified (ring.util.time/format-date last-modified)
                  headers       {"Cache-Control"  "public, immutable, max-age=31536000"
                                 "Content-Type"   content-type
                                 "Content-Length" (str content-length)
                                 "Last-Modified"  last-modified}]
              (terminate context {:status  200
                                  :headers headers
                                  :body    content})))
          context)))}))

;;; ----------------------------------------------------------------------------
;;; Page

(defn- make-page-data
  [context]
  {::page/asset-buster (context->buster context)})

(defn make-page-interceptor
  [page]
  (interceptor
   {:name ::page
    :enter
    (fn enter-page
      [context]
      (assoc-in context [:request ::page/data]
                (merge page (make-page-data context))))}))

(def context->page-data (must-getter :request ::page/data))
(def request->page-data (must-getter ::page/data))

;;; ----------------------------------------------------------------------------
;;; Not found

(defn- htmx-request?
  [request]
  (= "true" (response/get-header request "hx-request")))

(defn- not-found-response
  [context]
  {:status  404
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (-> context
                make-page-data
                (assoc ::page/title "Not found")
                page/not-found
                page/html)})

(def not-found-interceptor
  (interceptor
   {:name ::not-found
    :leave
    (fn leave-not-found
      [{:keys [request response] :as context}]
      (if (http/response? response)
        context
        (let [response (if (htmx-request? request)
                         {:status  404
                          :headers {"Content-Type" "text/plain"}
                          :body    "Not found.\n"}
                         (not-found-response context))]
          (assoc context :response response))))}))

;;; ----------------------------------------------------------------------------
;;; Error

(def error-interceptor
  (error/error-dispatch
   [{:keys [request response] :as context} exception]
   :else
   (let [data            (ex-data exception)
         pedestal-keys   #{:exception
                           :exception-type
                           :execution-id
                           :interceptor
                           :stage}
         {:keys [execution-id
                 interceptor
                 stage]} data
         more-data       (apply dissoc (ex-data exception) pedestal-keys)]
     (log/error :msg            "Interceptor exception?!"
                :interceptor    interceptor
                :stage          stage
                :execution-id   execution-id
                :request-method (:request-method request)
                :uri            (:uri request)
                :ex-data        more-data
                :exception      exception)
     (assoc context
            ::chain/error nil
            :response     {:status  500
                           :headers {"Content-Type" "text/plain"}
                           :body    "Internal server error.\n"}))))

;;; ----------------------------------------------------------------------------
;;; Common

(def common-interceptors
  [error-interceptor
   (body-params/body-params)
   http/json-body
   http/html-body])
