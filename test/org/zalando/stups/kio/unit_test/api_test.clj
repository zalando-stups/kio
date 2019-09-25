(ns org.zalando.stups.kio.unit-test.api-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [org.zalando.stups.kio.core :refer [run]]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.kio.metrics :as metrics]
            [org.zalando.stups.friboo.user :as u]
            [org.zalando.stups.friboo.auth :as auth]))

(defn with-status?
  "Checks if exception has status-code in ex-data"
  [status]
  (fn [e]
    (let [data (ex-data e)]
      (= status (:http-code data)))))

(t/deftest ^:unit enrich-application
  (facts "enrich-application"
    (fact "it properly sets required_approvers"
      (api/enrich-application {:criticality_level 1}) => (contains {:required_approvers 1})
      (api/enrich-application {:criticality_level 2}) => (contains {:required_approvers 2})
      (api/enrich-application {:criticality_level 3}) => (contains {:required_approvers 2}))))

(t/deftest ^:unit test-read-access
  (facts "read stuff"
    (fact "applications without search - cached empty response [will fail on rerun in the same REPL => db call is memoized]"
      (api/read-applications {} .request. {:db .db.}) =not=> (contains {:body []})
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-applications anything {:connection .db.}) => []))
    (fact "applications without search - service request"
      (api/read-applications {:team_id "foo"} .request. {:db .db.}) => (contains {:body [{:test 1}]})
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "services"}}
        (sql/cmd-read-applications anything {:connection .db.}) => [{:test 1}]))
    (fact "applications with search"
      (api/read-applications .params. .request. {:db .db.}) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        .params. =contains=> {:search "foo bar"}
        (sql/cmd-search-applications anything {:connection .db.}) => []))
    (fact "applications with search"
      (api/read-applications .params. .request. {:db .db.}) => (contains {:body []})
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "services"}}
        .params. =contains=> {:search "foo bar"}
        (sql/cmd-search-applications anything {:connection .db.}) => []))
    (fact "single app - service request [will fail on rerun in the same REPL => db call is memoized]"
      (api/read-application {:application_id "foo"} .request. {:db .db.}) => (contains {:body {:id "foo" :required_approvers 2}})
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "services"}}
        (sql/cmd-read-application anything {:connection .db.}) => [{:id "foo"}]))
    (fact "single app"
      (api/read-application {:application_id "foo1"} .request. {:db .db.}) => (contains {:body {} :status 404})
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "services"}}
        (sql/cmd-read-application anything {:connection .db.}) => []))))

(t/deftest ^:unit test-write-application
  (facts "writing applications"
    (fact "when updating application, the team in db is compared"
      (api/create-or-update-application! .params. .request. {:db .db. :http-audit-logger .logger.}) =not=> (throws Exception)
      (provided
        .logger. =contains=> {:log-fn identity}
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id .api-team-id.
                                               :id .app-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => {:team_id .db-team-id. :id .app-id.}
        (sql/cmd-create-or-update-application! anything {:connection .db.}) => nil
        (api/team-exists? .request. .api-team-id.) => true
        (api/require-write-authorization .request. .db-team-id.) => nil))

    (fact "when creating application, the team in body is compared"
      (api/create-or-update-application! .params. .request. {:db .db. :http-audit-logger .logger.}) =not=> (throws Exception)
      (provided
        .logger. =contains=> {:log-fn identity}
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id .api-team-id.
                                               :id .app-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => nil
        (sql/cmd-create-or-update-application! anything {:connection .db.}) => nil
        (api/team-exists? .request. .api-team-id.) => true
        (api/require-write-authorization .request. .api-team-id.) => nil))

    (fact "when creating/updating application, the team is checked"
      (api/create-or-update-application! .params. .request. {:db .db. :http-audit-logger .logger.}) => (throws Exception)
      (provided
        .logger. =contains=> {:log-fn identity}
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id nil
                                               :id .app-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => nil))

    (fact "when creating/updating application, the team is checked"
      (api/create-or-update-application! .params. .request. {:db .db. :http-audit-logger .logger.}) => (throws Exception)
      (provided
        .logger. =contains=> {:log-fn identity}
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id " "
                                               :id .app-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:configuration {:magnificent-url .magnificent-url.}
                               :tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => nil))))

(t/deftest ^:unit test-require-write-access
  (facts "write access"
    (fact "access is denied if the uid is missing"
      (api/require-write-authorization .request. .team.) => (throws Exception anything (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/services"
                                               "scope" ["uid" "application.write"]}
                               :configuration {:admin-users "/services/stups_robot,foo"}}))

    (fact "access is denied if the realm is neither services nor employees"
      (api/require-write-authorization .request. .team.) => (throws Exception anything (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/somerealm"
                                               "scope" ["uid" "application.write"]}
                               :configuration {:admin-users "/services/stups_robot,foo"}}))

    (fact "a robot does get access if it has write scope and required uid"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/services"
                                               "uid"   "stups_robot"
                                               "scope" ["uid" "application.write"]}}
        (auth/get-auth .request. .team.) => true))

    (fact "a robot does get access if it is configured as admin user"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/services"
                                                "uid"   "stups_robot"
                                                "scope" ["uid"]}
                               :configuration {:admin-users "/services/stups_robot,foo"}}))

    (fact "a human does get access if it is configured as admin user"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/employees"
                                               "uid"   "stups_robot"
                                               "scope" ["uid"]}
                               :configuration {:admin-users "/employees/stups_robot,foo"}}))

   (fact "a human does not get access if it is configured as admin user in wrong realm"
     (api/require-write-authorization .request. .team.) => (throws Exception anything (with-status? 403))
     (provided
      .request. =contains=> {:tokeninfo     {"realm" "/employees"
                                             "uid"   "stups_robot"
                                             "scope" ["uid"]}
                             :configuration {:admin-users "/wrongrealm/stups_robot,foo"}}
       (auth/get-auth .request. .team.) => false))

   (fact "a human does get access if it is in the required team"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/employees"
                                           "uid"   "npiccolotto"
                                           "scope" ["uid"]}}
        (auth/get-auth .request. .team.) => true)

    (fact "a human does not get access if it is not in the required team"
      (api/require-write-authorization .request. .team.) => (throws Exception (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/employees"
                                           "uid"   "npiccolotto"
                                           "scope" ["uid"]}}
        (auth/get-auth .request. .team.) => false))

    (fact "a robot can not write without write scope"
      (api/require-write-authorization .request. .team.) => (throws Exception (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/services"
                                           "uid"   "robobro"
                                           "scope" ["uid"]}}
        (auth/get-auth .request. .team.) => true))

    (fact "a robot can write with write scope and correct team"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/services"
                                           "uid"   "robobro"
                                           "scope" ["uid" "application.write"]}}
        (auth/get-auth .request. .team.) => true))

    (fact "a robot can not write with write scope to another team"
      (api/require-write-authorization .request. .team.) => (throws Exception (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/services"
                                           "uid"   "robobro"
                                           "scope" ["uid"]}}
        (auth/get-auth .request. .team.) => false))

    (fact "a robot can write applications"
      (api/create-or-update-application! .params. .request. {:db .db. :http-audit-logger .logger.}) => (contains {:status 200})
      (provided
        .logger. =contains=> {:log-fn identity}
        .request. =contains=> {:tokeninfo {"uid"   "robobro"
                                           "realm" "/services"
                                           "scope" ["uid" "application.write"]}}
        .params. =contains=> {:application    {:team_id .team-id.
                                               :id      .app-id.}
                              :application_id .app-id.}
        (auth/get-auth .request. .team-id.) => true
        (api/load-application .app-id. .db.) => {:team_id .team-id.
                                                 :id      .app-id.}
        (sql/cmd-create-or-update-application! anything {:connection .db.}) => nil))

    (fact "a robot can write versions"
      (api/create-or-update-version! .params. .request. {:db .db. :app-metrics .app-metrics.}) =not=> (throws Exception)
      (provided
        .params. =contains=> {:version        .version.
                              :version_id     .version-id.
                              :application_id .application-id.}
        .request. =contains=> {:tokeninfo {"uid"   "robobro"
                                           "realm" "/services"
                                           "scope" ["uid" "application.write"]}}
        .version. =contains=> {:id .version-id.}
        (auth/get-auth .request. .team-id.) => true
        (metrics/mark-deprecation .app-metrics. :deprecation-version-put) => nil
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.}))

    (fact "a robot can not write approvals"
      (api/approve-version! .params. .request. {:db .db. :app-metrics .app-metrics.}) => (throws Exception (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo     {"uid"   "robobro"
                                               "realm" "/services"
                                               "scope" ["uid" "application.write"]}
                               :configuration {:service-user-url "http://robot.com"
                                               :magnificent-url  "magnificent-url"}}
        .params. =contains=> {:version_id     .version-id.
                              :application_id .application-id.
                              :notes          .notes.}
        (metrics/mark-deprecation .app-metrics. :deprecation-version-approvals-put) => nil
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.}))

    (fact "a human can write approvals"
      (api/approve-version! .params. .request. {:db .db. :app-metrics .app-metrics.}) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo     {"uid"   .uid.
                                               "realm" "/employees"
                                               "scope" ["uid"]}
                               :configuration {:team-service-url "http://employee.com"
                                               :magnificent-url  "magnificent-url"}}
        .params. =contains=> {:version_id     .version-id.
                              :application_id .application-id.
                              :approval       {:notes .notes.}}

        (u/require-internal-team .team-id. .request.) => nil
        (metrics/mark-deprecation .app-metrics. :deprecation-version-approvals-put) => nil
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.})))))

(t/deftest ^:unit merge-app-fields
  (t/testing "can toggle the active status"
    (t/is (= {:active false}
             (api/merge-app-fields {:active true} {:active false})))
    (t/is (= {:active true}
             (api/merge-app-fields {:active false} {:active true})))))