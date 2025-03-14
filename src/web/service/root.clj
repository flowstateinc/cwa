(ns web.service.root
  (:require
   [web.interceptors :as i]
   [web.page :as page]
   [web.ui :as ui]))

(defn root-response
  [request]
  (let [{::page/keys [title]
         :as         page} (i/request->page-data request)]
    {:status 200
     :body   (page/html (page/layout page (ui/container (ui/title title))))}))

(def routes
  #{["/" :get (conj i/common-interceptors
                    (i/make-page-interceptor {::page/title "Coming soon!"})
                    `root-response)
     :route-name :web.route/get-root]})
