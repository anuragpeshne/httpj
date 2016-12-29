(ns httpj.file-server
  (:gen-class)
  (:require [clojure.core.cache :as cache])
  (:import [java.io.FileNotFoundException]
           [java.nio.file Files Path Paths]))

(def file-cache (atom (cache/lu-cache-factory {})))

(defn get-file
  [file-path]
  (if (cache/has? @file-cache file-path)
    ;;(cache/hit @file-cache file-path)
    (get @file-cache file-path)
    (let [file (Paths/get "" (into-array [file-path]))]
      (if (Files/isReadable file)
        (let [file-bytes (Files/readAllBytes file)
              file-metaful (let [file-name (-> file .getFileName .toString)
                                 extension (if (= (.lastIndexOf file-name ".") -1)
                                             ;; the case when there is no extension
                                             ""
                                             (subs file-name
                                                   (+ (.lastIndexOf file-name ".") 1)))
                                 last-modified (Files/getLastModifiedTime
                                                file
                                                (into-array [java.nio.file.LinkOption/NOFOLLOW_LINKS]))]
                             (with-meta {:bytes file-bytes}
                               {:length (count file-bytes)
                                :name file-name
                                :ext extension
                                :last-modified last-modified}))]
          (println (str "File miss" file-path))
          (swap! file-cache #(cache/miss % file-path file-metaful))
          file-metaful) ;;return file-bytes
        ;; else: if file not found or is unreadable
        nil))))
