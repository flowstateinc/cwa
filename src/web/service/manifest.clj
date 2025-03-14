(ns web.service.manifest
  (:require
   [web.interceptors :as i]))

(defn get-manifest
  [_request]
  {:status 200
   :body   {"name"             "Clojure Web Application"
            "short_name"       "cwa"
            "background_color" "#171717"
            "display"          "standalone"
            "icons"            [{"src"   "/android-chrome-192x192.png"
                                 "sizes" "192x192"
                                 "type"  "image/png"}
                                {"src"   "/android-chrome-512x512.png"
                                 "sizes" "512x512"
                                 "type"  "image/png"}]}})

(def routes
  #{["/manifest.json"
     :get (conj i/common-interceptors `get-manifest)
     :route-name :web.route/get-manifest]})
