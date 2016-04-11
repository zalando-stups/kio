
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
            [clj-time.coerce :as tcoerce]
            [io.sarnowski.swagger1st.util.api :as api]
            [ring.util.response :refer :all]
            [clojure.string :as str]
            [clojure.java.jdbc :refer [with-db-transaction]]))

; define the API component and its dependencies
(def-http-component API "api/kio-api.yaml" [db])

(def default-http-configuration
  {:http-port 8080})

(defn from-token
  [request field & return-default]
  (get-in request
          [:tokeninfo field]
          (when return-default return-default)))

; shameless copy from essentials
(defn require-special-uid
  "Checks wether a given user is configured to be allowed to access this endpoint. Workaround for now."
  [{:keys [configuration tokeninfo]}]
  (let [allowed-uids (or (:allowed-uids configuration) "")
        allowed (set (str/split allowed-uids #","))
        uid (get tokeninfo "uid")]
    (when (and (seq allowed-uids)
               (not (allowed uid)))
      (log/warn "ACCESS DENIED (unauthorized) because not a special user.")
      (api/throw-error 403 "Unauthorized"))))

(defn- require-service-authorization
  "Checks whether service user has correct scope and team membership"
  [team request]
  (let [has-scope? (set (from-token request "scope"))]
    (when-not (or (has-scope? "application.write_all")
                  (has-scope? "application.write"))
      (log/warn "ACCESS DENIED (unauthorized) because insufficient scopes.")
      (api/throw-error 403 "Unauthorized"))
    (when (has-scope? "application.write_all")
      ; we have to limit which robots access can do that for now
      (require-special-uid request))
    (when (has-scope? "application.write")
      (u/require-service-team team request))))

(defn require-uid
  "Checks whether uid is present on token, throws 403 otherwise"
  [request]
  (when-not (from-token request "uid")
    (log/warn "ACCESS DENIED (unauthorized) because no uid in tokeninfo.")
    (api/throw-error 403 "Unauthorized")))

(defn require-write-authorization
  "If user is employee, check that is in correct team.
   If user is service, check that it has application_write.all scope OR has application.write and is correct team"
  [request team]
  (require-uid request)
  (u/require-internal-user request)
  (let [service-realm? #{"services" "/services"}
        employee-realm? #{"employees" "/employees"}
        realm (from-token request "realm")]
    (when (service-realm? realm)
      (require-service-authorization team request))
    (when (employee-realm? realm)
      (u/require-internal-team team request))))

;; applications

(defn read-applications
  [{:keys [search modified_before modified_after team_id active]} request db]
  (u/require-realms #{"employees" "services"} request)
  (let [conn {:connection db}
        params {:searchquery    (when search
                                  (-> search
                                      str/trim
                                      (str/replace #" " "|")
                                      (str/replace #"\|+" " | ")))
                :team_id team_id
                :active active
                :modified_before (tcoerce/to-sql-time modified_before)
                :modified_after  (tcoerce/to-sql-time modified_after)}]
    (if (nil? search)
      (do
        (log/debug "Read all applications.")
        (-> (sql/cmd-read-applications params conn)
            (sql/strip-prefixes)
            (response)
            (content-type-json)))
      (do
        (log/debug "Search in applications with term %s." search)
        (-> (sql/cmd-search-applications params conn)
            (sql/strip-prefixes)
            (response)
            (content-type-json))))))

(defn load-application
  "Loads a single application by ID, used for team checks."
  [application_id db]
  (-> (sql/cmd-read-application {:id application_id}
                                {:connection db})
      (sql/strip-prefixes)
      (first)))

(defn enrich-application
  "Adds calculated field(s) to an application"
  [application]
  (assoc application :required_approvers (if (= 1 (:criticality_level application))
                                           1
                                           2)))

(defn enrich-applications
  [applications]
  (map enrich-application applications))

(defn read-application [{:keys [application_id]} request db]
  (u/require-realms #{"employees" "services"} request)
  (log/debug "Read application %s." application_id)
  (-> (sql/cmd-read-application
        {:id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (enrich-applications)
      (single-response)
      (content-type-json)))

(defn create-or-update-application! [{:keys [application application_id]} request db]
  (let [uid (from-token request "uid")
        defaults {:specification_url   nil
                  :documentation_url   nil
                  :subtitle            nil
                  :scm_url             nil
                  :service_url         nil
                  :description         nil
                  :specification_type  nil
                  :publicly_accessible false}]
    (require-write-authorization request (:team_id application))
    (sql/cmd-create-or-update-application!
      (merge defaults application {:id               application_id
                                   :last_modified_by uid
                                   :created_by       uid})
      {:connection db})
    (log/audit "Created/updated application %s using data %s." application_id application)
    (response nil)))

(defn read-application-approvals [{:keys [application_id]} request db]
  (u/require-internal-user request)
  (log/debug "Read all approvals for application %s." application_id)
  (->> (sql/cmd-read-application-approvals
         {:application_id application_id}
         {:connection db})
       (sql/strip-prefixes)
       (map :approval_type)
       (response)
       (content-type-json)))

;; versions

(defn read-versions-by-application [{:keys [application_id]} request db]
  (u/require-realms #{"employees" "services"} request)
  (log/debug "Read all versions for application %s." application_id)
  (-> (sql/cmd-read-versions-by-application
        {:application_id application_id}
        {:connection db})
      (sql/strip-prefixes)
      (response)
      (content-type-json)))

(defn read-version-by-application [{:keys [application_id version_id]} request db]
  (u/require-realms #{"employees" "services"} request)
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
      (require-write-authorization request (:team_id application))
      (with-db-transaction
        [connection db]
        (let [uid (from-token request "uid")
              defaults {:notes nil}]
          (sql/cmd-create-or-update-version!
            (merge defaults version {:id               version_id
                                     :application_id   application_id
                                     :created_by       uid
                                     :last_modified_by uid})
            {:connection connection}))
        (sql/cmd-delete-approvals! {:application_id application_id
                                    :version_id     version_id}
                                   {:connection connection}))
      (log/audit "Created/updated version %s for application %s using data %s." version_id application_id version)
      (response nil))
    (api/error 404 "application not found")))

;; approvals

(defn read-approvals-by-version [{:keys [application_id version_id]} request db]
  (u/require-realms #{"employees" "services"} request)
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
            uid (from-token request "uid")]
        (sql/cmd-approve-version!
          (merge defaults approval {:version_id     version_id
                                    :application_id application_id
                                    :user_id        uid})
          {:connection db}))
      (log/audit "Approved version %s for application %s." version_id application_id)
      (response nil))
    (api/error 404 "application not found")))
