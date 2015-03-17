;
; Copyright 2015 Zalando SE
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns org.zalando.kio.core
  (:require [com.stuartsierra.component :refer [using]]
            [org.zalando.friboo.system :as system]
            [org.zalando.friboo.system.db :refer [new-db]]
            [org.zalando.kio.api :refer [new-api]]
            [org.zalando.kio.sql :refer [default-db-spec]])
  (:gen-class))

(defn run
  "Initializes and starts the whole system."
  [default-configuration]
  (let [default-configuration (merge default-db-spec
                                     {:http-definition "api.yaml"
                                      :http-port 8080}
                                     default-configuration)
        configuration (system/load-configuration default-configuration)
        system (system/new-system configuration {:db  (new-db (:db configuration))
                                                 :api (using (new-api) [:db])})]
    (system/run configuration system)))

(defn -main
  "The actual main for our uberjar."
  [& args]
  (run {}))
