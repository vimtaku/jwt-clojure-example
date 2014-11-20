(defproject jwt-clojure-example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.2.0"]
                 [ring/ring-defaults "0.1.2"]
                 [buddy "0.2.0"]
                 [metosin/compojure-api "0.16.2"]
                 [clojurewerkz/scrypt "1.2.0"]
                 [org.clojure/data.json "0.2.5"]
                 [ring-middleware-format "0.4.0"]
                 [prismatic/schema "0.3.3"]
                 [org.clojars.runa/conjure "2.1.3"]
                 ]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler jwt-clojure-example.core.handler/app}
  :profiles
  {:dev {:dependencies [
                        [javax.servlet/servlet-api "2.5"]
                        [swiss-arrows "1.0.0"]
                        [metosin/ring-swagger-ui "2.0.17"]
                        [midje "1.6.3"]
                        [ring-mock "0.1.5"]
                        ]}})
