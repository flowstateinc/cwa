(ns web.ui
  (:require
   [clojure.string :as str]))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Classes

(defn- normalize-class
  [x]
  (cond
    (vector? x) x
    (list? x)   (vec x)
    (string? x) (when-not (str/blank? x)
                  (str/split x #"\s+"))))

(defn merge-classes
  [& xs]
  (into []
        (comp (mapcat normalize-class) (remove nil?) (distinct))
        xs))

;;; --------------------------------------------------------------------------------------------------------------------
;;; Tags

(defn make-tag
  [tag-name attrs]
  (fn tag
    [& args]
    (let [[args-attrs content]
          (if (map? (first args))
            [(first args) (rest args)]
            [{} args])

          as    (:as args-attrs tag-name)
          class (merge-classes (:class attrs) (:class args-attrs))]
      (into [as (cond-> (merge (dissoc attrs :class)
                               (dissoc args-attrs :as :class))
                  (seq class) (assoc :class class))]
            content))))

;;; ----------------------------------------------------------------------------
;;; Header

(defn header
  [links]
  [:header {:class "bg-white"}
   [:nav {:class      ["mx-auto"
                       "flex"
                       "max-w-7xl"
                       "items-center"
                       "justify-between"
                       "p-6"
                       "lg:px-8"]
          :aria-label "Global"}
    [:div {:class "flex items-center gap-x-12"}
     [:div
      {:class "hidden lg:flex lg:gap-x-12"}
      (for [{:keys [href text]} links]
        [:a {:href href :class "text-sm/6 font-semibold text-gray-900"} text])]]
    [:div {:class "hidden lg:flex"}
     [:span {:class "cursor-default text-sm/6 font-semibold text-gray-400"}
      "Sign out"]]]])

;;; ----------------------------------------------------------------------------
;;; Container

(def container
  (make-tag :div {:class ["mx-auto" "max-w-7xl" "sm:px-6" "lg:px-8"]}))

;;; ----------------------------------------------------------------------------
;;; Title

(def title
  (make-tag :h1 {:class ["mt-2"
                         "text-4xl"
                         "font-semibold"
                         "tracking-tight"
                         "text-gray-900"
                         "sm:text-6xl"]}))
