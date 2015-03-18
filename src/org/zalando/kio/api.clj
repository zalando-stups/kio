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
            [org.zalando.friboo.system.http :refer [def-http-component]]
            [org.zalando.kio.sql :as sql]))

; define the API component and its dependencies
(def-http-component API "kio-api.yaml" [db])

(def default-http-configuration
  {:http-port 8080})

(defn- json-content-type [response]
  (content-type response "application/json"))

(defn- compute-status [result-set]
  (if (empty? result-set)
    (not-found {})
    (response (first result-set))))

(defn read-applications [_ _ db]
  (-> (sql/read-applications {} {:connection db})
      (response)
      (json-content-type)))

(defn read-application [{:keys [application_id]} _ db]
  (-> (sql/read-application {:id application_id} {:connection db})
      (compute-status)
      (json-content-type)))

(defn save-application [{:keys [application application_id]} _ db]
  (sql/save-application! (merge application {:id application_id}) {:connection db})
  (response nil))

(defn delete-application [{:keys [application_id]} _ db]
  (if
    (> (sql/delete-application! {:id application_id} {:connection db}) 0)
    (response nil)
    (not-found nil)))

(defn save-application-secret [{:keys [application_id secret]} _ db]
  (if
    (> (sql/save-application-secret! {:id     application_id
                                      :secret secret} {:connection db}) 0)
    (response nil)
    (not-found nil)))
