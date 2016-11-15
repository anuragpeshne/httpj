(ns httpj.file-server
  (:gen-class)
  (:require [clojure.core.cache :as cache]
            [clojure.java.io/file]
            [clojure.java.io/reader])
  (:import [java.io.FileNotFoundException]))

(def file-cache (cache/lu-cache-factory))

(defn get-file
  [file-path]
  (if (cache/has? file-cache file-path)
    (let [f (cache/hit file-cache file-path)
          rdr (clojure.java.io/reader f)]
      rdr)
    (let [f (clojure.java.io/file file-path)]
      (if (and (.exists f) (not (.isDirectory f)))
        (cache/miss file-cache file-path f)
        nil))))
