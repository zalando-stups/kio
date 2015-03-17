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

(ns org.zalando.kio.api
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer :all]
            [com.stuartsierra.component :as component]
            [io.sarnowski.swagger1st.core :as s1st]
            [org.zalando.friboo.system.http :as http]
            [org.zalando.kio.sql :as sql]))

; create a dependency injecting function mapper component
(defrecord API [mapper-fn db]
  component/Lifecycle
  (start [this]
    (assoc this :mapper-fn (fn [operationId]
                             (fn [request]
                               (if-let [cljfn (s1st/map-function-name operationId)]
                                 ; -> fn [parameters request db]
                                 (cljfn (http/flattened-parameter-mapper request) request db))))))

  (stop [this]
    (assoc this :mapper-fn nil))

  http/API
  (get-mapper-fn [_] mapper-fn))

(defn new-api
  "Default constructor for API component."
  []
  (map->API {}))


;;; API functions

(defn- json-content-type [response]
  (content-type response "application/json"))

(defn- compute-status [result-set]
  (if (empty? result-set)
    (not-found {})
    (response (first result-set))))

(defn read-applications [_ _ db]
  (-> (sql/read-applications db)
      (response)
      (json-content-type)))

(defn read-application [{:keys [application_id]} _ db]
  (-> (sql/read-application db {:id application_id})
      (compute-status)
      (json-content-type)))

(defn save-application [{:keys [application application_id]} _ db]
  (sql/save-application! db (merge application {:id application_id}))
  (response nil))

(defn delete-application [{:keys [application_id]} _ db]
  (if
    (> (sql/delete-application! db {:id application_id}) 0)
    (response nil)
    (not-found nil)))

(defn save-application-secret [{:keys [application_id secret]} _ db]
  (if
    (> (sql/save-application-secret! db {:id     application_id
                                         :secret secret}) 0)
    (response nil)
    (not-found nil)))
