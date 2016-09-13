(ns org.zalando.stups.kio.unit-test.audit-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer [deftest]]
            [org.zalando.stups.kio.audit :as audit]))

(def tokeninfo
  {:uid   "uid"
   :realm "realm"})

(def app
  {:id          "id"
   :scm_url     "scm_url"
   :service_url "service_url"
   :team_id     "team_id"
   :nil-value   nil
   :additional  "additional"})

(def app-modified
  (audit/app-modified tokeninfo app))

(deftest ^:unit test-audit
  (facts "app-modified"
    (fact "creates correct envelope"
      app-modified => (just {:triggered_at anything
                             :triggered_by anything
                             :event_type   anything
                             :payload      anything})
      (:triggered_at app-modified) => #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"
      (:event_type app-modified) => (just {:namespace "cloud.zalando.com"
                                           :name      "application-modified"
                                           :version   "2"})
      (:triggered_by app-modified) => (just {:type       "USER"
                                             :id         "uid"
                                             :additional {:realm "realm"}}))
    (fact "creates correct payload"
      (:payload app-modified) => (just {:application {:id         (:id app)
                                                      :repo_url   (:scm_url app)
                                                      :app_url    (:service_url app)
                                                      :owner      (:team_id app)
                                                      :additional (:additional app)}}))))
