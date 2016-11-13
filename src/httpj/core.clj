(ns httpj.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.pprint]
            [httpj.file-server])
  (:import [java.net.ServerSocket]
           [java.net.Socket]
           [java.io.BufferedReader]
           [java.io.InputStreamReader]))

(def PORT 8080)

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

(defn generate-output
  [parsed-req]
  (let [msg (httpj.file-server/get-file (str "." (-> parsed-req :headLine :path)))]
    (if (nil? msg) (let [msg "404: Not Found"]
                     (str "HTTP/1.1 404 Not Found\r\n"
                          "Server: httpj/x.x\r\n"
                          "Content-Length: " (count msg) "\r\n"
                          "\r\n"
                          msg
                          "\r\n"))
        (str "HTTP/1.1 200 OK\r\n"
             "Server: httpj/x.x\r\n"
             "Content-Length: " (count msg) "\r\n"
             "\r\n"
             msg
             "\r\n"))))

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
                        (.print out (generate-output (parse-reqest in)))
                        (.flush out))]))

(defn -main
  "Starting point for httpj"
  [& args]
  (println "Server ready.")
  (let [listener-socket (new java.net.ServerSocket PORT)]
    (while true
      (let [client-socket (.accept listener-socket)]
        (handle-client client-socket)))))
