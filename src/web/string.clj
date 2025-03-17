(ns web.string
  (:import
   (org.apache.commons.lang3 StringUtils)))

(set! *warn-on-reflection* true)

(defn remove-prefix
  [^String s ^String prefix]
  {:pre [(string? s) (string? prefix)]}
  (StringUtils/removeStart s prefix))
