(ns httpj.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.pprint])
  (:import [java.net.ServerSocket]
           [java.net.Socket]
           [java.io.BufferedReader]
           [java.io.InputStreamReader]))

(def PORT 8080)

(defrecord ReqHeader [method path version])

(defn parseHeadLine
  [line]
  (let [tokens (str/split line #" ")
        method (cond (= (str/upper-case (get tokens 0)) "GET") :GET
                     (= (str/upper-case (get tokens 0)) "POST") :POST
                     :else "unknown") ;;probably throw exception
        path (get tokens 1)
        version (get tokens 2)]
    (ReqHeader. method path version)))

(defn generateOutput
  [parsedReq]
  (println "in generate output")
  (str "HTTP/1.0 200 OK\r\n"
       "Server: httpj/x.x\r\n"
       "\r\n"
       "Hello World!\r\n"))

(defn parseReqest
  "parses and returns request obj"
  [in]
  (let [inp (line-seq in)
        headLine (parseHeadLine (first inp))
        headers (loop [curInp (rest inp) list []]
                  (if (or (= (first curInp) "") (nil? (first curInp))) list
                      (recur (rest curInp)
                             (conj list (apply hash-map
                                               (str/split (first curInp) #": "))))))]
    (println "printing headers")
    (clojure.pprint/pprint headers)
    {:headLine headLine, :headers headers}))

(defn handleClient
  "This function is executed after accepting socket"
  [socket]
  (let [in (new java.io.BufferedReader
                (new java.io.InputStreamReader (.getInputStream socket)))
        out (new java.io.PrintWriter (.getOutputStream socket))
        clientHandler (future
                        (println "got a connection@" (.getRemoteSocketAddress socket))
                        (.print out (generateOutput (parseReqest in)))
                        (.flush out)
                        (.close socket))]))

(defn -main
  "Starting point for httpj"
  [& args]
  (println "Server ready.")
  (let [listenerSocket (new java.net.ServerSocket PORT)]
    (while true
      (let [clientSocket (.accept listenerSocket)]
        (handleClient clientSocket)))))
