(defproject kio "0.1.0-SNAPSHOT"
            :description "The application registry."
            :url "https://kio.stups.example.org"

            :license {:name "The Apache License, Version 2.0"
                      :url  "http://www.apache.org/licenses/LICENSE-2.0"}

            :scm {:url "git@github.com:zalando-stups/kio.git"}
            :min-lein-version "2.0.0"

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.zalando/friboo "0.1.0"]
                           [yesql "0.5.0-rc2"]
                           [org.postgresql/postgresql "9.3-1102-jdbc41"]]

            :main ^:skip-aot org.zalando.kio.core

            :profiles {:uberjar {:aot :all}

                       :dev {:repl-options {:init-ns user}
                             :source-paths ["dev"]
                             :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                            [org.clojure/java.classpath "0.2.2"]]}})
