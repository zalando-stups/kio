(defproject org.zalando.stups/kio "0.25.0-SNAPSHOT"
  :description "The application registry."
  :url "https://github.com/zalando-stups/kio"

  :license {:name "The Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.zalando.stups/friboo "1.13.0"]
                 [clj-time "0.13.0"]
                 [org.zalando.stups/tokens "0.11.0-beta-2"]
                 [yesql "0.5.3"]
                 [org.clojure/core.memoize "0.7.1"]]

  :managed-dependencies [[org.flatland/ordered "1.5.7"]
                         [marick/suchwow "6.0.2"]]

  :main ^:skip-aot org.zalando.stups.kio.core
  :uberjar-name "kio.jar"

  :plugins [[io.sarnowski/lein-docker "1.1.0"]
            [org.zalando.stups/lein-scm-source "0.3.0"]
            [lein-cloverage "1.0.7-SNAPSHOT"]]

  :docker {:image-name #=(eval (str (some-> (System/getenv "DEFAULT_DOCKER_REGISTRY")
                                      (str "/"))
                                 "stups/kio"))}

  :release-tasks [["vcs" "assert-committed"]
                  ["clean"]
                  ["test"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["uberjar"]
                  ["scm-source"]
                  ["docker" "build"]
                  ["docker" "push"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :pom-addition [:developers
                 [:developer {:id "sarnowski"}
                  [:name "Tobias Sarnowski"]
                  [:email "tobias.sarnowski@zalando.de"]
                  [:role "Maintainer"]]]

  :test-selectors {:default     :unit
                   :unit        :unit
                   :integration :integration}

  :profiles {:uberjar {:aot :all}
             :dev     {:repl-options {:init-ns user}
                       :source-paths ["dev"]
                       :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                      [midje "1.9.8"]
                                      [org.clojure/java.classpath "0.2.3"]]}})
