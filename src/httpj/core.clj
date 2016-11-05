(ns httpj.core
  (:gen-class)
  (:import [java.net.ServerSocket]
           [java.net.Socket]
           [java.io.BufferedReader]
           [java.io.InputStreamReader]))

(def PORT 8080)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (let [listenerSocket (new java.net.ServerSocket PORT)]
  (while true
    (let [clientSocket (.accept listenerSocket)
          in (new java.io.BufferedReader
                  (new java.io.InputStreamReader (.getInputStream clientSocket)))
          out (new java.io.PrintWriter (.getOutputStream clientSocket))
          clientHandler (future
                          (println "got a connection@" (.getRemoteSocketAddress clientSocket))
                          (doseq [line (line-seq in)]
                            (.println out line)
                            (.flush out)))]))))

(defn clientHandler
  "This function is executed after accepting socket"
  [socket]
  (println "hello"))
