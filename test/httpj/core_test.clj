(ns httpj.core-test
  (:require [clojure.test :refer :all]
            [httpj.core :refer :all])
  (:import [java.net.Socket]
           [java.io.BufferedReader]
           [java.io.InputStreamReader]
           [java.io.PrintWriter]))

(def SERVER_ADDR "127.0.0.1")

(deftest serverSocket-test
  (testing "server socket connection"
    (let [sock (new java.net.Socket SERVER_ADDR PORT)
          streamReader (new java.io.InputStreamReader (.getInputStream sock))
          reader (new java.io.BufferedReader streamReader)
          out (new java.io.PrintWriter (.getOutputStream sock))
          testString "This is this"]
      (.println out testString)
      (.flush out)
      (is (= (.readLine reader) testString))
      (.close sock))))
