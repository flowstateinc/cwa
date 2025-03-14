(ns web.config
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]))

;; One could just (System/getenv "DATABASE_URL").
(defmacro ^:private env
  [k]
  `(System/getenv ~(str/upper-case (csk/->snake_case_string k))))

(defn read-config
  []
  (let [database-url (env :database-url)
        http-port    (or (some-> (env :port) parse-long) 3000)]
    {:buster    {:resources #{"public/android-chrome-192x192.png"
                              "public/android-chrome-512x512.png"
                              "public/app.css"
                              "public/app.js"
                              "public/apple-touch-icon.png"
                              "public/favicon-16x16.png"
                              "public/favicon-32x32.png"
                              "public/favicon.ico"
                              "public/js/htmx-ext-preload@2.1.0.js"
                              "public/js/htmx@2.0.3.min.js"}}
     :concierge {}
     :migrator  {:database-url      database-url
                 :throw-exceptions? false}
     :postgres  {:database-url database-url}
     :service   {:http-host "0.0.0.0"
                 :http-port http-port
                 :join?     false}}))
