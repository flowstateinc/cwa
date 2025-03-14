(ns web.test.html
  (:require
   [hickory.core :as hickory]
   [hickory.select :as sel]))

(defn parse
  [html]
  {:pre [(string? html)]}
  (-> html hickory/parse hickory/as-hickory))

(defn elements
  [doc tag-name]
  {:pre [(some? doc) (keyword? tag-name)]}
  (sel/select (sel/tag tag-name) doc))
