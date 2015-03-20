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

(ns org.zalando.stups.kio.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.kio.sql :as sql]
            [ring.util.response :refer :all]
            [org.zalando.stups.friboo.ring :refer :all]
            [clojure.tools.logging :as log]))

; define the API component and its dependencies
(def-http-component API "api/kio-api.yaml" [db])

(def default-http-configuration
  {:http-port 8080})

(defn read-applications [_ _ db]
  (log/debug "Read all applications")
  (-> (sql/read-applications {} {:connection db})
      (response)
      (content-type-json)))

(defn read-application [{:keys [application_id]} _ db]
  (log/debug "Read application by id" application_id)
  (-> (sql/read-application
        {:id application_id}
        {:connection db})
      (single-response)
      (content-type-json)))

(defn create-or-update-application [{:keys [application application_id]} _ db]
  (log/info "Create/update application" application_id "with data:" application)
  (sql/create-or-update-application!
    (merge application {:id application_id})
    {:connection db})
  (response nil))

(defn update-application-secret [{:keys [application_id secret]} _ db]
  (log/info "Updating the secret of application" application_id)
  (let [stored (pos? (sql/update-application-secret!
                    {:id     application_id
                     :secret secret}
                    {:connection db}))]
    (if stored
      (response nil)
      (not-found nil))))

; TODO this should not be supported?! only 'inactive' flag maybe?
(defn delete-application [{:keys [application_id]} _ db]
  (log/info "Delete application" application_id)
  (let [deleted (pos? (sql/delete-application!
                     {:id application_id}
                     {:connection db}))]
    (if deleted
      (response nil)
      (not-found nil))))
