(ns httpj.file-server
  (:gen-class)
  (:require [clojure.core.cache :as cache])
  (:import [java.io.FileNotFoundException]
           [java.nio.file Files Path Paths]))

(def file-cache (atom (cache/lu-cache-factory {})))

(defn get-file-extension
  [file-name]
  (if (= (.lastIndexOf file-name ".") -1)
    ;; the case when there is no extension
    ""
    (subs file-name
          (+ (.lastIndexOf file-name ".") 1))))

(defn extract-metadata
  [file]
  (let [size (Files/size file)
        file-name (-> file .getFileName .toString)
        extension (get-file-extension file-name)
        last-modified (Files/getLastModifiedTime
                       file
                       (into-array [java.nio.file.LinkOption/NOFOLLOW_LINKS]))]
    {:length size
     :name file-name
     :ext extension
     :last-modified last-modified}))

(defn get-file
  [file-path]
  (if (cache/has? @file-cache file-path)
    ;;(cache/hit @file-cache file-path)
    (deref (get @file-cache file-path))
    (let [file (Paths/get "" (into-array [file-path]))]
      (if (Files/isReadable file)
        (let [file-future
              (future
                ;; we are having a {:bytes obj} map because we cannot
                ;; add metadata to native java obj (java.bytearray)
                (with-meta
                  {:bytes (Files/readAllBytes file)}
                  (extract-metadata file)))]
          (println (str "File miss" file-path))
          (swap! file-cache #(cache/miss % file-path file-future))
          (deref file-future)) ;;return future containing file bytes
        ;; else: if file not found or is unreadable
        nil))))
