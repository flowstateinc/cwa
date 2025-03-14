(ns web.service
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [web.interceptors :as i]
   [web.router :as router]
   [web.service.manifest :as service.manifest]
   [web.service.root :as service.root])
  (:import
   (org.eclipse.jetty.server AbstractNetworkConnector Server)))

;;; ----------------------------------------------------------------------------
;;; Routes

(def routes
  (router/combine
   service.root/routes
   service.manifest/routes))

;;; ----------------------------------------------------------------------------
;;; CSP

(defn- qs [s] (format "'%s'" s))

(defn csp
  []
  {:default-src    (qs "self")
   :img-src        (qs "self")
   :object-src     (qs "none")
   :script-src     (qs "self")
   :style-src      (qs "self")
   :style-src-attr (qs "none")})

;;; ----------------------------------------------------------------------------
;;; Component

(defn make-definition
  [service]
  (let [{:keys [env http-host http-port join?]
         :or   {env :prod}} service]
    (-> {:env                         env
         ::http/allowed-origins       nil
         ::http/join?                 join?
         ::http/container-options     {:h2?  false
                                       :h2c? true
                                       :ssl? false}
         ::http/enable-csrf           nil
         ::http/enable-session        nil
         ::http/host                  http-host
         ::http/not-found-interceptor i/not-found-interceptor
         ::http/port                  http-port
         ::http/resource-path         "/public"
         ::http/routes                routes
         ::http/secure-headers        {:content-security-policy-settings (csp)}
         ::http/type                  :jetty}
        http/default-interceptors
        (update ::http/interceptors into (cond-> [i/asset-interceptor]
                                           (= :dev env)
                                           (i/prepend i/reload-assets-interceptor)))
        (update ::http/interceptors i/prepend (i/make-state-interceptor service))
        (update ::http/interceptors conj (i/make-referrer-policy-interceptor))
        (cond-> (= :dev env) http/dev-interceptors))))

(defrecord Service [buster
                    definition
                    http-host
                    http-port
                    join?
                    postgres]
  component/Lifecycle
  (start [this]
    (let [definition (http/create-server (make-definition this))]
      (http/start definition)
      (assoc this :definition definition)))
  (stop [this]
    (some-> this :definition http/stop)
    (assoc this :definition nil)))

(defmethod print-method Service
  [service ^java.io.Writer w]
  (.write w (format "#<Service port=%s>" (:http-port service))))

(s/fdef make-service
  :args (s/cat :config ::config)
  :ret ::config)

(defn make-service
  [config]
  (map->Service config))

;;; ----------------------------------------------------------------------------
;;; Jetty

(defn- jetty-connectors
  [^Server server]
  (map (fn connector->map
         [^AbstractNetworkConnector connector]
         (let [host (.getHost connector)]
           (cond-> {:local-port (.getLocalPort connector)
                    :port (.getPort connector)}
             (some? host) (assoc :host host))))
       (.getConnectors server)))

(defn- service-connectors
  [service]
  (-> service :definition ::http/server jetty-connectors))

;;; ----------------------------------------------------------------------------
;;; URL

(def ^{:arglists '([route-name & options])}
  url-for
  (route/url-for-routes (route/expand-routes routes)))

(defn service-port
  [service]
  (-> service service-connectors first :local-port))

(s/fdef service-url
  :args (s/cat :service ::service :path string?)
  :ret  string?)

(defn service-url
  [service path]
  (let [{:keys [host] :as connector} (first (service-connectors service))
        host                         (if (or (nil? host) (= "0.0.0.0" host))
                                       "localhost"
                                       host)]
    (str "http://"
         host
         (when-let [port (:local-port connector)]
           (str ":" port))
         (str/replace-first path #"^/?" "/"))))
