
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
            [org.zalando.stups.friboo.auth :as auth]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.audit :as audit]
            [org.zalando.stups.kio.metrics :as metrics]
            [clj-time.coerce :as tcoerce]
            [io.sarnowski.swagger1st.util.api :as api]
            [ring.util.response :refer :all]
            [clojure.string :as str]
            [clojure.java.jdbc :refer [with-db-transaction]]
            [clojure.core.memoize :as memo]))

; define the API component and its dependencies
(def-http-component API "api/kio-api.yaml" [db http-audit-logger app-metrics] :dependencies-as-map true)

(def default-http-configuration
  {:http-port 8080})

; TODO should be replaced with tokeninfo, but requires test changes
(defn from-token
  [request field & return-default]
  (get-in request
          [:tokeninfo field]
          (when return-default return-default)))

(defn tokeninfo
  [request]
  (clojure.walk/keywordize-keys (:tokeninfo request)))

(defn require-uid
  "Checks whether uid is present on token, throws 403 otherwise"
  [request]
  (when-not (from-token request "uid")
    (log/warn "ACCESS DENIED (unauthorized) because no uid in tokeninfo.")
    (api/throw-error 403 "Unauthorized")))

(defn is-admin-in-realm?
  [uid realm {:keys [configuration]}]
  (when (and uid realm)
    (let [uid-with-realm (str realm "/" uid)
          allowed-uids-with-realm (or (:admin-users configuration) "")
          allowed (set (str/split allowed-uids-with-realm #","))]
      (allowed uid-with-realm))))

(defn require-write-authorization
  "If user is employee, check that is in correct team.
   If user is service, check that it has application.write scope and is correct team.
   If user is listed as admin grant access to user"
  [request team]
  (require-uid request)

  (let [realm (str "/" (u/require-realms #{"employees" "services"} request))
        uid (from-token request "uid")
        is-admin? (is-admin-in-realm? uid realm request)]
    (when-not is-admin?
      (let [has-auth? (auth/get-auth request team)
            is-robot? (= "/services" realm)
            has-scope? (set (from-token request "scope"))]
        (when-not has-auth?
          (api/throw-error 403 "Unauthorized"))
        (when (and
                is-robot?
                (not (has-scope? "application.write")))
          (api/throw-error 403 "Unauthorized"))))))

;; applications

;;https://github.com/clojure/core.memoize/blob/master/docs/Using.md#overriding-the-cache-keys
(defn ^{:clojure.core.memoize/args-fn #(list (first %) (-> (second %) type str))}
  run-db-query
  [params function-name db-spec]
  (function-name params {:connection db-spec}))

(def run-db-query-memo
  (memo/ttl #'run-db-query :ttl/threshold 60000))

(defn read-applications
  [{:keys [search modified_before modified_after team_id incident_contact active]} request {:keys [db]}]
  (let [realm (u/require-realms #{"employees" "services"} request)
        conn {:connection db}
        params {:searchquery search
                :team_id team_id
                :incident_contact incident_contact
                :active active
                :modified_before (tcoerce/to-sql-time modified_before)
                :modified_after  (tcoerce/to-sql-time modified_after)}]
    (if (nil? search)
      (do
        (log/debug "Read all applications.")
        (-> (if (#{"employees"} realm)
              (sql/cmd-read-applications params conn)
              (run-db-query-memo params sql/cmd-read-applications db))
            (response)
            (content-type-json)))
      (do
        (log/debug "Search in applications with term %s." search)
        (-> (if (#{"employees"} realm)
              (sql/cmd-search-applications params conn)
              (run-db-query-memo params sql/cmd-search-applications db))
            (response)
            (content-type-json))))))

(defn load-application
  "Loads a single application by ID, used for team checks."
  [application_id db]
  (-> (sql/cmd-read-application {:id application_id}
                                {:connection db})
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

(defn read-application [{:keys [application_id]} request {:keys [db]}]
  (log/debug "Read application %s." application_id)
  (-> (if (#{"employees"} (u/require-realms #{"employees" "services"} request))
        (sql/cmd-read-application {:id application_id} {:connection db})
        (run-db-query-memo {:id application_id} sql/cmd-read-application db))
      (enrich-applications)
      (single-response)
      (content-type-json)))

(defn create-or-update-application! [{:keys [application application_id]} request {:keys [db http-audit-logger]}]
  (let [uid (from-token request "uid")
        defaults {:incident_contact    nil
                  :specification_url   nil
                  :documentation_url   nil
                  :subtitle            nil
                  :scm_url             nil
                  :service_url         nil
                  :description         nil
                  :specification_type  nil
                  :publicly_accessible false
                  :criticality_level   2}
        existing_application (load-application application_id db)]

    (if (nil? existing_application)
      (require-write-authorization request (:team_id application))
      (require-write-authorization request (:team_id existing_application)))

    (let [app-to-save (merge-with #(or %2 %1) defaults application {:id               application_id
                                                                    :last_modified_by uid
                                                                    :created_by       uid})
          log-fn (:log-fn http-audit-logger)]
      (sql/cmd-create-or-update-application!
        app-to-save
        {:connection db})
      (log-fn (audit/app-modified
                (tokeninfo request)
                app-to-save)))
    (log/audit "Created/updated application %s using data %s." application_id application)
    (response nil)))

(defn read-application-approvals [_ request {:keys [app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-application-approvals-get)
  (u/require-internal-user request)
  (->> []
       (response)
       (content-type-json)))

;; versions

(defn read-versions-by-application [_ request {:keys [app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-versions-get)
  (u/require-realms #{"employees" "services"} request)
  (-> []
      (response)
      (content-type-json)))

(defn read-version-by-application [_ request {:keys [app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-version-get)
  (u/require-realms #{"employees" "services"} request)
  (-> (not-found {})
      (content-type-json)))

(defn create-or-update-version! [{:keys [application_id]} request {:keys [db app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-version-put)
  (if-let [application (load-application application_id db)]
    (do
      (require-write-authorization request (:team_id application))
      (response nil))
    (api/error 404 "application not found")))

;; approvals

(defn read-approvals-by-version [_ request {:keys [db app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-version-approvals-get)
  (u/require-realms #{"employees" "services"} request)
  (-> []
      (response)
      (content-type-json)))

(defn approve-version! [{:keys [application_id]} request {:keys [db app-metrics]}]
  (metrics/mark-deprecation app-metrics :deprecation-version-approvals-put)
  (if-let [application (load-application application_id db)]
    (do
      (u/require-internal-team (:team_id application) request)
      (response nil))
    (api/error 404 "application not found")))
