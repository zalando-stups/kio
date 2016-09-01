(ns org.zalando.stups.kio.unit-test.api-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer :all]
            [org.zalando.stups.kio.core :refer [run]]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.friboo.user :as u]
            [org.zalando.stups.friboo.auth :as auth]
            [clojure.java.jdbc :as jdbc]))

(defn with-status?
  "Checks if exception has status-code in ex-data"
  [status]
  (fn [e]
    (let [data (ex-data e)]
      (= status (:http-code data)))))

(deftest enrich-application
  (facts "enrich-application"
    (fact "it properly sets required_approvers"
      (api/enrich-application {:criticality_level 1}) => (contains {:required_approvers 1})
      (api/enrich-application {:criticality_level 2}) => (contains {:required_approvers 2})
      (api/enrich-application {:criticality_level 3}) => (contains {:required_approvers 2}))))

(deftest test-read-access
  (facts "people without a team can read stuff"
    (fact "applications without search"
      (api/read-applications {} .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-applications anything {:connection .db.}) => []))
    (fact "applications with search"
      (api/read-applications .params. .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        .params. =contains=> {:search "foo bar"}
        (sql/cmd-search-applications anything {:connection .db.}) => []))
    (fact "single app"
      (api/read-application nil .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-application anything {:connection .db.}) => {}))

    (fact "versions"
      (api/read-versions-by-application nil .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-versions-by-application anything {:connection .db.}) => {}))
    (fact "single version"
      (api/read-version-by-application nil .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-version-by-application anything {:connection .db.}) => {}))
    (fact "approvals"
      (api/read-approvals-by-version nil .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "employees"}}
        (sql/cmd-read-approvals-by-version anything {:connection .db.}) => {}))))

(deftest test-write-application
  (facts "writing applications"
    (fact "when updating application, the team in db is compared"
      (api/create-or-update-application! .params. .request. .db.) =not=> (throws Exception)
      (provided
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id .api-team-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => {:team_id .db-team-id.}
        (sql/cmd-create-or-update-application! anything {:connection .db.}) => nil
        (api/require-write-authorization .request. .db-team-id.) => nil))

    (fact "when creating application, the team in body is compared"
      (api/create-or-update-application! .params. .request. .db.) =not=> (throws Exception)
      (provided
        .params. =contains=> {:application_id .app-id.
                              :application    {:team_id .api-team-id.
                                               :active  true
                                               :name    "test"}}
        .request. =contains=> {:tokeninfo {"uid"   "nikolaus"
                                           "realm" "/employees"}}
        (api/load-application .app-id. .db.) => nil
        (sql/cmd-create-or-update-application! anything {:connection .db.}) => nil
        (api/require-write-authorization .request. .api-team-id.) => nil))))

(deftest test-require-write-access
  (facts "write access"
    (fact "a robot does not get access if the uid is missing"
      (api/require-write-authorization .request. .team.) => (throws Exception anything (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/services"
                                               "scope" ["uid" "application.write_all"]}
                               :configuration {:allowed-uids "stups_robot,foo"}}))

    (fact "a robot does get access if it has write_all scope and required uid"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo     {"realm" "/services"
                                               "uid"   "stups_robot"
                                               "scope" ["uid" "application.write_all"]}
                               :configuration {:allowed-uids "stups_robot,foo"}}
        (auth/get-auth .request. .team.) => true))

    (fact "a human does get access if it is in the required team"
      (api/require-write-authorization .request. .team.) => nil
      (provided
        .request. =contains=> {:tokeninfo {"realm" "/employees"
                                           "uid"   "npiccolotto"
                                           "scope" ["uid"]}}
        (auth/get-auth .request. .team.) => true))

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
      (api/create-or-update-application! .params. .request. .db.) => (contains {:status 200})
      (provided
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
      (api/create-or-update-version! .params. .request. .db.) =not=> (throws Exception)
      (provided
        .params. =contains=> {:version        .version.
                              :version_id     .version-id.
                              :application_id .application-id.}
        .request. =contains=> {:tokeninfo {"uid"   "robobro"
                                           "realm" "/services"
                                           "scope" ["uid" "application.write"]}}
        .version. =contains=> {:id .version-id.}
        (auth/get-auth .request. .team-id.) => true
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.}
        ; this is because of the with-transaction macro...
        (jdbc/db-transaction* anything anything) => irrelevant))

    (fact "a robot can not write approvals"
      (api/approve-version! .params. .request. .db.) => (throws Exception (with-status? 403))
      (provided
        .request. =contains=> {:tokeninfo     {"uid"   "robobro"
                                               "realm" "/services"
                                               "scope" ["uid" "application.write"]}
                               :configuration {:service-user-url "http://robot.com"
                                               :magnificent-url  "magnificent-url"}}
        .params. =contains=> {:version_id     .version-id.
                              :application_id .application-id.
                              :notes          .notes.}
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.}
        (sql/cmd-approve-version! anything anything) => irrelevant :times 0))

    (fact "a human can write approvals"
      (api/approve-version! .params. .request. .db.) =not=> (throws Exception)
      (provided
        .request. =contains=> {:tokeninfo     {"uid"   .uid.
                                               "realm" "/employees"
                                               "scope" ["uid"]}
                               :configuration {:team-service-url "http://employee.com"
                                               :magnificent-url  "magnificent-url"}}
        .params. =contains=> {:version_id     .version-id.
                              :application_id .application-id.
                              :approval       {:notes .notes.}
                              }
        (u/require-internal-team .team-id. .request.) => nil
        (api/load-application .application-id. .db.) => {:team_id .team-id.
                                                         :id      .application-id.}

        (sql/cmd-approve-version! {:version_id     .version-id.
                                   :application_id .application-id.
                                   :user_id        .uid.
                                   :notes          .notes.} {:connection .db.}) => nil))))
