(defproject kio "0.1.0-SNAPSHOT"
            :description "The application registry."
            :url "https://kio.stups.example.org"
            :license {:name "The Apache License, Version 2.0"
                      :url  "http://www.apache.org/licenses/LICENSE-2.0"}
            :scm {:url "git@github.com:zalando-stups/kio.git"}
            :min-lein-version "2.0.0"

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [io.sarnowski/swagger1st "0.3.0"]
                           [ring "1.3.2"]
                           [ring/ring-json "0.3.1"]
                           [yesql "0.5.0-rc2"]
                           [org.postgresql/postgresql "9.3-1102-jdbc41"]]

            :plugins [[lein-ring "0.9.2"]]

            :ring {:handler kio.core/app})
