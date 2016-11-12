(ns httpj.file-server
  (:gen-class))

(defn get-file
  [file-path]
  (println file-path)
  (slurp file-path))
