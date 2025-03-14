(ns web.page
  (:require
   [clojure.spec.alpha :as s]
   [hiccup2.core :as hiccup]
   [jsonista.core :as json]
   [web.assets :as assets]
   [web.spec]
   [web.ui :as ui]))

;;; ----------------------------------------------------------------------------
;;; Specs

(comment web.spec/retain)

(s/def ::asset-buster ::assets/config)
(s/def ::title string?)

(s/def ::data
  (s/keys :req [::asset-buster ::title]))

;;; ----------------------------------------------------------------------------
;;; Layout

(defn- htmx-config
  []
  (json/write-value-as-string {"allowEval"              false
                               "allowScriptTags"        false
                               "includeIndicatorStyles" false}))

(defn layout
  [page & content]
  {:pre [(s/assert ::data page)]}
  (let [{::keys [asset-buster title]} page
        asset-path                    #(assets/asset-path asset-buster %)
        links                         [{:href "/" :text "Home"}]]
    [:html {:class "antialiased min-h-full" :lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width"}]
      [:meta {:name "htmx-config" :content (htmx-config)}]
      [:title title]
      [:link {:rel "icon" :href "data:;base64,iVBORw0KGgo="}]
      [:link {:rel "apple-touch-icon" :sizes "180x180" :href (asset-path "/apple-touch-icon.png")}]
      [:link {:rel "icon" :type "image/png" :sizes "32x32" :href (asset-path "/favicon-32x32.png")}]
      [:link {:rel "icon" :type "image/png" :sizes "16x16" :href (asset-path "/favicon-16x16.png")}]
      [:link {:rel "manifest" :href "/manifest.json"}]
      [:link {:rel "stylesheet" :href (asset-path "/app.css")}]
      [:script {:src (asset-path "/js/htmx@2.0.3.min.js")}]
      [:script {:src (asset-path "/js/htmx-ext-preload@2.1.0.js")}]]
     [:body {:class "min-h-full" :hx-ext "preload"}
      (ui/header links)
      (into [:main] content)]]))

;;; ----------------------------------------------------------------------------
;;; Not found

(defn not-found
  [page]
  (layout page (ui/container
                [:h1 {:class "text-pretty text-2xl font-extrabold"}
                 (::title page)])))

;;; ----------------------------------------------------------------------------
;;; HTML

(defn htmx
  [content]
  (hiccup/html {:mode :html} content))

(defn html
  [content]
  (str "<!DOCTYPE html>" (htmx content)))
