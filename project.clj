(defproject rrss "0.2.2"
  :description "Ring Redis Session Store"
  :url "https://github.com/paraseba/rrss"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [ring/ring-core "0.3.5"]
                 [redis.clients/jedis "1.5.0"]]
  :dev-dependencies [[org.clojure/clojure-contrib "1.2.0"]
                     [autodoc "0.7.1" :exclusions [org.clojure/clojure
                                                   org.clojure/clojure-contrib]]]
  :autodoc
    {:name "rrss"
     :description "A session store for Ring, backed by Redis key/value store"
     :copyright "Copyright 2010 Sebastian Galkin"
     :page-title "rrss API Documentation"
     ;:root "."
     ;:source-path ""
     :web-src-dir "http://github.com/paraseba/rrss/blob/"
     :web-home "http://paraseba.github.com/rrss/"
     :output-path "autodoc"
     :namespaces-to-document ["rrss" "rrss.steps" "rrss.steps.backend-step"
                              "rrss.steps.expire-sessions-step" "rrss.steps.mapkey-step"
                              "rrss.steps.sessions-set-step" "rrss.steps.time-sessions-step"
                              "rrss.store"]
     ;:trim-prefix "rrss."
     ;:buil-json-index true
     :load-except-list [#"/test/" #"project\.clj"]})

