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

(ns kio.core
  (:require [io.sarnowski.swagger1st.core :as s1st]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer :all]
            [clojure.pprint :refer [pprint]]
            [yesql.core :refer [defquery]])
  (:gen-class))

(def db-spec {:classname   "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname     "//localhost:5432/local_kio_db"
              :user        "postgres"
              :password    "postgres"})

(defquery query-read-applications "sql/application-read-all.sql" {:connection db-spec})
(defquery query-read-application "sql/application-read.sql" {:connection db-spec})
(defquery query-save-application! "sql/application-save.sql" {:connection db-spec})
(defquery query-delete-application! "sql/application-delete.sql" {:connection db-spec})
(defquery query-save-application-secret! "sql/application-save-secret.sql" {:connection db-spec})

(defn json-content-type [response]
  (content-type response "application/json"))

(defn compute-status [result-set] (if (empty? result-set) (not-found {}) (response (first result-set))))

(defn read-applications [request]
  (-> (query-read-applications)
      (response)
      (json-content-type)))

(defn read-application [request]
  (-> (query-read-application {:id (get-in request [:parameters :path :application_id])})
      (compute-status)
      (json-content-type)))

(defn save-application [request]
  (let [app (get-in request [:parameters :body :application])
        app-id (get-in request [:parameters :path :application_id])]
    (query-save-application! (merge app {:id app-id})))
  (response nil))

(defn delete-application [request]
  (if
    (> (query-delete-application! {:id (get-in request [:parameters :path :application_id])}) 0)
    (response nil)
    (not-found nil)))

(defn save-application-secret [request]
  (if
    (> (query-save-application-secret! {:id     (get-in request [:parameters :path :application_id])
                                        :secret (get-in request [:parameters :body :secret])}) 0)
    (response nil)
    (not-found nil)))


(def app
  (-> (s1st/swagger-executor)
      (s1st/swagger-security)
      (s1st/swagger-validator)
      (s1st/swagger-parser)
      (s1st/swagger-discovery)
      (s1st/swagger-mapper ::s1st/yaml-cp "api.yaml")
      (wrap-params)))
