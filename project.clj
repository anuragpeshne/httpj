(defproject httpj "0.1.0-SNAPSHOT"
  :description "HTTP Server in Clojure"
  :url "https://github.com/anuragpeshne/httpj"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.cache "0.6.4"]]
  :main ^:skip-aot httpj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
