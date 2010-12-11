(defproject rrss "0.1.0-SNAPSHOT"
  :description "Ring Redis Session Store"
  :url "https://github.com/paraseba/rrss"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [;[org.clojure/clojure "1.2.0"]
                 [ring/ring-core "0.3.5"]
                 [redis.clients/jedis "1.5.0"]])
