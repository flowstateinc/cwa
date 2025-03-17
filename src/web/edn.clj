(ns web.edn
  (:refer-clojure :exclude [read])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp])
  (:import
   (java.io PushbackReader)))

(defn read
  [readable]
  (with-open [rdr (io/reader readable)]
    (edn/read {:readers *data-readers*} (PushbackReader. rdr))))

(defn write-str
  ^String [x]
  (binding [*print-length* nil
            *print-level* nil]
    (with-out-str (pp/pprint x))))
