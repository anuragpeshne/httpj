(ns httpj.file-server
  (:gen-class)
  (:import [java.io.FileNotFoundException]))

(defn get-file
  [file-path]
  (try
    (slurp file-path)
    (catch java.io.FileNotFoundException e nil)))
