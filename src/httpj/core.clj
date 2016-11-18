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

(def PORT 8080)
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
        version (get tokens 2)]
    (ReqHeader. method path version)))

(defn generate-header
  [code file & args]
  (let [status (condp = code
                 :404 "404 Not Found"
                 :200 "200 OK"
                 "501 Not Implemented")
        mime-type (if (or (nil? file) (= (.lastIndexOf (.getName file) ".") -1))
                    "text/plain"
                    (get mimes (subs (.getName file)
                                     (+ (.lastIndexOf (.getName file) ".") 1))))
        content-len (if (nil? file)
                      (first args)
                      (.length file))]
    (str (reduce #(str %1 "\r\n" %2) [(str "HTTP/1.1 " status)
                                      "Server: httpj/x.x"
                                      (str "Content-Type: " mime-type)
                                      (str "Content-Length: " content-len)])
         "\r\n\r\n")))

(defn send-response
  [parsed-req out]
  (let [file (httpj.file-server/get-file (str "." (-> parsed-req :headLine :path)))]
    (if (nil? file)
      (let [msg "404: Not Found!\r\n"]
        (.print out (generate-header :404 nil (count msg)))
        (.print out msg)
        (.flush out))
      (do
        (.print out (generate-header :200 file))
        (with-open [rdr (clojure.java.io/reader file)]
          (doseq [line (line-seq rdr)]
            (.println out line)))
        (.print out "\r\n")
        (.flush out)))))

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
    {:headLine head-line, :headers headers}))

(defn handle-client
  "This function is executed after accepting socket"
  [socket]
  (let [in (new java.io.BufferedReader
                (new java.io.InputStreamReader (.getInputStream socket)))
        out (new java.io.PrintWriter (.getOutputStream socket))
        clientHandler (future
                        (println "got a connection@" (.getRemoteSocketAddress socket))
                        (send-response (parse-reqest in) out))]))

(defn -main
  "Starting point for httpj"
  [& args]
  (println "Server ready.")
  (let [listener-socket (new java.net.ServerSocket PORT)]
    (while true
      (let [client-socket (.accept listener-socket)]
        (handle-client client-socket)))))
