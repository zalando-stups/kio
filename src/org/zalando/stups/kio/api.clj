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
            [org.zalando.stups.friboo.config :refer [require-config]]
            [org.zalando.stups.friboo.ring :refer :all]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.user :as u]
            [org.zalando.stups.kio.sql :as sql]
            [io.sarnowski.swagger1st.util.api :as api]
            [ring.util.response :refer :all]
            [clojure.string :as str]
            [clojure.java.jdbc :refer [with-db-transaction]]))

; define the API component and its dependencies
(def-http-component API "api/kio-api.yaml" [db])

(def default-http-configuration
  {:http-port 8080})

; shameless copy from essentials
(defn require-special-uid
  "Checks wether a given user is configured to be allowed to access this endpoint. Workaround for now."
  [{:keys [configuration tokeninfo]}]
  (let [allowed-uids (or (:allowed-uids configuration) "")
        uids (into #{} (str/split allowed-uids #","))]
    (when (and (not (contains? uids (get tokeninfo "uid")))
               (not (empty? allowed-uids)))
      (log/warn "ACCESS DENIED (unauthorized) because not a special user.")
      (api/throw-error 403 "Unauthorized"))))

;; applications

(defn read-applications [{:keys [search]} request db]
  (u/require-internal-user request)
  (if (nil? search)
    (do
      (log/debug "Read all applications.")
      (-> (sql/cmd-read-applications {} {:connection db})
          (sql/strip-prefixes)
          (response)
          (content-type-json)))
    (do
      (log/debug "Search in applications with term %s." search)
      (-> (sql/cmd-search-applications {:searchquery search} {:connection db})
          (sql/strip-prefixes)
          (response)
          (content-type-json)))))

(defn load-application
  "Loads a single application by ID, used for team checks."
  [application_id db]
  (-> (sql/cmd-read-application {:id application_id}
                                {:connection db})
      (sql/strip-prefixes)
      (first)))

(defn application-exists?
  "Checks whether an application with this id exists"
  [application_id db]
  (-> (load-application application_id db)
      (nil?)
      (not)))

(defn enrich-application
  "Adds calculated field(s) to an application"
  [application]
  (assoc application :required_approvers (if (= 1 (:criticality_level application))
                                              1
                                              2)))

(defn read-application [{:keys [application_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read application %s." application_id)
  (-> (sql/cmd-read-application
        {:id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (enrich-application)
      (single-response)
      (content-type-json)))

(defn create-or-update-application! [{:keys [application application_id]} request db]
  (let [uid (get-in request [:tokeninfo "uid"])
        defaults {:specification_url  nil
                  :documentation_url  nil
                  :subtitle           nil
                  :scm_url            nil
                  :service_url        nil
                  :description        nil
                  :specification_type nil}]
    (u/require-internal-team (:team_id application) request)
    (sql/cmd-create-or-update-application!
      (merge defaults application {:id                application_id
                                   :last_modified_by  uid
                                   :created_by        uid})
      {:connection db})
    (log/audit "Created/updated application %s using data %s." application_id application)
    (response nil)))

(defn update-application-criticality! [{:keys [application_id criticality]} request db]
  (let [uid (get-in request [:tokeninfo "uid"])]
    (if (application-exists? application_id db)
        (do (require-special-uid request)
            (sql/update-application-criticality! (merge criticality {:last_modified_by uid
                                                                     :id application_id})
                                                 {:connection db})
            (log/audit "Updated criticality of application %s using data %s." application_id criticality)
            (response nil))
        (not-found nil))))

(defn read-application-approvals [{:keys [application_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read all approvals for application %s." application_id)
  (->> (sql/cmd-read-application-approvals
         {:application_id application_id}
         {:connection db})
       (sql/strip-prefixes)
       (map #(:approval_type %))
       (response)
       (content-type-json)))

;; versions

(defn read-versions-by-application [{:keys [application_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read all versions for application %s." application_id)
  (-> (sql/cmd-read-versions-by-application
        {:application_id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (response)
      (content-type-json)))

(defn read-version-by-application [{:keys [application_id version_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read version %s of application %s." version_id application_id)
  (-> (sql/cmd-read-version-by-application
        {:id             version_id
         :application_id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (single-response)
      (content-type-json)))

(defn create-or-update-version! [{:keys [application_id version_id version]} request db]
  (if-let [application (load-application application_id db)]
    (do
      (u/require-internal-team (:team_id application) request)
      (with-db-transaction
        [connection db]
        (let [uid (get-in request [:tokeninfo "uid"])
              defaults {:notes nil}]
          (sql/cmd-create-or-update-version!
            (merge defaults version {:id               version_id
                                     :application_id   application_id
                                     :created_by       uid
                                     :last_modified_by uid})
            {:connection connection}))
        (sql/cmd-delete-approvals! {:application_id application_id
                                    :version_id version_id}
                                   {:connection connection}))
      (log/audit "Created/updated version %s for application %s using data %s." version_id application_id version)
      (response nil))
    (api/error 404 "application not found")))

;; approvals

(defn read-approvals-by-version [{:keys [application_id version_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read approvals for version %s of application %s." version_id application_id)
  (-> (sql/cmd-read-approvals-by-version
        {:version_id     version_id
         :application_id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (response)
      (content-type-json)))

(defn approve-version! [{:keys [application_id version_id approval]} request db]
  (if-let [application (load-application application_id db)]
    (do
      (u/require-internal-team (:team_id application) request)
      (let [defaults {:notes nil}
            uid (get-in request [:tokeninfo "uid"])]
        (sql/cmd-approve-version!
          (merge defaults approval {:version_id     version_id
                                    :application_id application_id
                                    :user_id        uid})
          {:connection db}))
      (log/audit "Approved version %s for application %s." version_id application_id)
      (response nil))
    (api/error 404 "application not found")))
