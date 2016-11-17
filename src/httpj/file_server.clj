(ns httpj.file-server
  (:gen-class)
  (:require [clojure.core.cache :as cache])
  (:import [java.io.FileNotFoundException]
           [java.io.File]))

(def file-cache (atom (cache/lu-cache-factory {})))

(defn get-file
  [file-path]
  (if (cache/has? @file-cache file-path)
    ;;(cache/hit @file-cache file-path)
    (get @file-cache file-path)
    (let [f (new java.io.File file-path)]
      (if (and (.exists f) (not (.isDirectory f)))
        (do
          (println (str "File miss" file-path))
          (swap! file-cache #(cache/miss % file-path f))
          f)
        nil))))
