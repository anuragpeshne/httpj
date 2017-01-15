(ns httpj.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.pprint]
            [httpj.file-server])
  (:import [java.net.ServerSocket]
           [java.net.Socket]
           [java.io.File]
           [java.io.BufferedReader]
           [java.io.InputStreamReader]))

(def port 8080)
(def debug false)
(def mimes
  {"css" "text/css"
   "csv" "text/csv"
   "doc" "application/msword"
   "epub" "application/epub+zip"
   "gif" "image/gif"
   "htm" "text/html"
   "html" "text/html"
   "ico" "image/x-icon"
   "jpeg" "image/jpeg"
   "jpg" "image/jpeg"
   "js" "application/js"
   "json" "application/json"
   "md" "text/plain"
   "pdf" "application/pdf"
   "svg" "image/svg+xml"
   "xml" "application/xml"
   "zip" "application/zip"})

(defrecord ReqHeader [method path version])

(defn parse-head-line
  [line]
  (let [tokens (str/split line #" ")
        method (cond (= (str/upper-case (get tokens 0)) "GET") :GET
                     (= (str/upper-case (get tokens 0)) "POST") :POST
                     :else "unknown") ;;probably throw exception
        path (get tokens 1)
        version (condp = (get tokens 2)
                  "HTTP/1.0" :HTTP-1
                  "HTTP/1.1" :HTTP-1.1
                  "HTTP/2.0" :HTTP-2
                  "Unknown Version")]
    (ReqHeader. method path version)))

(defn generate-header
  [code file & args]
  (let [status (condp = code
                 :404 "404 Not Found"
                 :200 "200 OK"
                 "501 Not Implemented")
        file-meta (meta file)
        mime-type (if (or (nil? file) (= (:ext file-meta) ""))
                    "text/plain"
                    (get mimes (:ext file-meta)))
        content-len (if (nil? file)
                      (first args)
                      (:length file-meta))]
    (str (reduce #(str %1 "\r\n" %2) [(str "HTTP/1.1 " status)
                                      "Server: httpj/x.x"
                                      (str "Content-Type: " mime-type)
                                      (str "Content-Length: " content-len)])
         "\r\n\r\n")))

(defn send-response
  [parsed-req out out-bin]
  (let [file (httpj.file-server/get-file
              (str "." (-> parsed-req :head-line :path)))
        file-meta (meta file)]
    (if (nil? file)
      (let [msg "404: Not Found!\r\n"]
        (doto out
          (.print (generate-header :404 nil (count msg)))
          (.print msg)
          .flush ))
      (do
        (doto out (.print (generate-header :200 file)) .flush)
        (clojure.java.io/copy (:bytes file) out-bin)
        (.flush out-bin)
        (doto out (.print "\r\n") .flush)))))

(defn parse-reqest
  "parses and returns request obj"
  [in]
  (let [inp (line-seq in)
        head-line (parse-head-line (first inp))
        headers (loop [cur-inp (rest inp) list []]
                  (if (or (= (first cur-inp) "") (nil? (first cur-inp))) list
                      (recur (rest cur-inp)
                             (conj list (apply hash-map
                                               (str/split (first cur-inp) #": "))))))]
    {:head-line head-line, :headers headers}))

(defn handle-client
  "This function is executed after accepting socket"
  [socket]
  (let [in (java.io.BufferedReader.
                (java.io.InputStreamReader. (.getInputStream socket)))
        out (java.io.PrintWriter. (.getOutputStream socket))
        out-bin (java.io.BufferedOutputStream. (.getOutputStream socket))
        clientHandler (future
                        (if debug
                          (println "got a connection@" (.getRemoteSocketAddress socket)))
                        (let [parsed-req (parse-reqest in)]
                          (send-response parsed-req out out-bin)
                          (if (or (= :HTTP-1 (-> parsed-req :head-line :version))
                                  (= "close" (get (:headers parsed-req) "Connection")))
                            (.close socket))))]))

(defn -main
  "Starting point for httpj"
  [& args]
  (println "Server ready.")
  (let [listener-socket (java.net.ServerSocket. port)]
    (while true
      (let [client-socket (.accept listener-socket)]
        (handle-client client-socket)))))
