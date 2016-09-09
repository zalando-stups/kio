(ns org.zalando.stups.kio.audit
  (:require [clj-time.format :as tf]
            [clj-time.core :as t]))

(def date-formatter
  (tf/formatters :date-time-no-ms))

(defn get-date
  []
  (tf/unparse date-formatter (t/now)))

(defn drop-nil-values
  [record]
  (into {} (remove (comp nil? second) record)))

(defn app-modified
  [tokeninfo app]
  {:event_type   {:namespace "internal"
                  :name      "application-modified"
                  :version   "2"}
   :triggered_at (get-date)
   :triggered_by {:type       "USER"
                  :id         (:uid tokeninfo)
                  :additional {:realm (:realm tokeninfo)}}
   :payload      {:application (drop-nil-values (merge
                                                  {:id       (:id app)
                                                   :repo_url (:scm_url app)
                                                   :app_url  (:service_url app)
                                                   :owner    (:team_id app)}
                                                  (dissoc app
                                                    :id
                                                    :scm_url
                                                    :service_url
                                                    :team_id)))}})
