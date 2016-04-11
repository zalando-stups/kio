(ns org.zalando.stups.kio.api-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.friboo.user :as fuser]
            [org.zalando.stups.kio.test-utils :as util]
            [clojure.java.jdbc :as jdbc]))

(deftest test-the-tester
  "I succeed"
  (is true))

(deftest test-load-app
  "Test db mocking"
  (with-redefs [sql/cmd-read-application (constantly [{:a_id "kio"}])]
    (is (:id (api/load-application "kio" nil)
        "kio"))))

(deftest test-enrich-application1
  (let [app {:criticality_level 1}
        approvers (:required_approvers (api/enrich-application app))]
    (is (= 1 approvers))))

(deftest test-enrich-application2
  (let [app {:criticality_level 2}
        approvers (:required_approvers (api/enrich-application app))]
    (is (= 2 approvers))))

(deftest test-enrich-application3
  (let [app {:criticality_level 3}
        approvers (:required_approvers (api/enrich-application app))]
    (is (= 2 approvers))))

(deftest test-read-access

  (testing "people without a team should read applications"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "employees"}}]
      (with-redefs [sql/cmd-read-applications (constantly [])
                    sql/cmd-search-applications (constantly [])]
        (api/read-applications nil request nil)
        (api/read-applications {:search "foo bar"} request nil))))

  (testing "people without a team should read a single app"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "employees"}}]
      (with-redefs [sql/cmd-read-application (constantly {})]
        (api/read-application nil request nil))))

  (testing "people without a team should read versions"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "employees"}}]
      (with-redefs [sql/cmd-read-versions-by-application (constantly [])]
        (api/read-versions-by-application nil request nil))))

  (testing "people without a team should read a single version"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "employees"}}]
      (with-redefs [sql/cmd-read-version-by-application (constantly {})]
        (api/read-version-by-application nil request nil))))

  (testing "people without a team should read a single version"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "employees"}}]
      (with-redefs [sql/read-approvals-by-version (constantly {})]
        (api/read-approvals-by-version nil request nil)))))

(deftest test-require-write-access

  (testing "a robot should not get access if realm is missing"
    (let [request {:tokeninfo {"uid" "stups_robot"
                               "scope" ["uid" "application.write_all"]}
                   :configuration {:allowed-uids "stups_robot,foo"}}]
      (try (do
             (api/require-write-authorization request "stups")
             (is false))
        (catch Exception e
          (is (= 403 (:http-code (ex-data e))))))))

  (testing "a robot should not get access if the uid is missing"
    (let [request {:tokeninfo {"realm" "/services"
                               "scope" ["uid" "application.write_all"]}
                   :configuration {:allowed-uids "stups_robot,foo"}}]
      (try (do
             (api/require-write-authorization request "stups")
             (is false))
        (catch Exception e
          (is (= 403 (:http-code (ex-data e))))))))

  (testing "a robot should get access if it has write_all scope and required uid"
    (let [request {:tokeninfo {"uid" "stups_robot"
                               "realm" "/services"
                               "scope" ["uid" "application.write_all"]}
                   :configuration {:allowed-uids "stups_robot,foo"}}]
      (api/require-write-authorization request "stups")))

  (testing "a robot should not get access if it doesn't have required uid but write_all scope"
    (let [request {:tokeninfo {"uid" "stups_robot"
                               "realm" "/services"
                               "scope" ["uid" "application.write_all"]}
                   :configuration {:allowed-uids "bar,foo"}}]
      (try (do
             (api/require-write-authorization request "stups")
             (is false))
        (catch Exception e
          (is (= 403 (:http-code (ex-data e))))))))

  (testing "a human should get access if it is in the required team"
    (let [request {:tokeninfo {"uid" "npiccolotto"
                               "realm" "/employees"
                               "scope" ["uid"]}
                   :configuration {:team-service-url "http://example.com"}}]
      (with-redefs [fuser/get-teams (constantly [{:name "stups"} {:name "asa"}])
                    fuser/require-internal-user (constantly nil)]
        (api/require-write-authorization request "stups"))))

  (testing "a human should not get access if it is not in the required team"
    (let [request {:tokeninfo {"uid" "npiccolotto"
                               "realm" "/employees"
                               "scope" ["uid"]}
                   :configuration {:team-service-url "http://example.com"}}]
      (with-redefs [fuser/get-teams (constantly [{:name "test"} {:name "asa"}])
                    fuser/require-internal-user (constantly nil)]
        (try (do
               (api/require-write-authorization request "stups")
               (is false))
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should not write without write scope"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid"]}
                   :configuration {:service-user-url "http://robot.com"}}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)]
        (try
          (api/require-write-authorization request "stups")
          (is false)
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should write with write scope and correct team"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}
                   :configuration {:service-user-url "http://robot.com"}}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)]
        (api/require-write-authorization request "stups"))))

  (testing "a robot should not write with write scope to another team"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid"]}
                   :configuration {:service-user-url "http://robot.com"}}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)]
        (try
          (api/require-write-authorization request "team-britney")
          (is false)
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should write applications"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}
                   :configuration {:service-user-url "http://robot.com"}}
          application {:team_id "stups"
                       :id "foo"}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)
                    sql/cmd-create-or-update-application! (constantly nil)]
        (api/create-or-update-application! {:application application
                                            :application_id "foo"}
                                           request
                                           nil))))

  (testing "a robot should write versions"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}
                   :configuration {:service-user-url "http://robot.com"}}
          application {:team_id "stups"
                       :id "foo"}
          version {:id "bar"}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)
                    ; does not matter, is used by with-db-transaction macro
                    jdbc/db-transaction* #(list %1 %2)
                    sql/cmd-create-or-update-version! (constantly nil)
                    sql/cmd-delete-approvals! (constantly nil)
                    api/load-application (constantly application)]
        (api/create-or-update-version! {:version version
                                        :version_id "bar"
                                        :application_id "foo"}
                                       request
                                       nil))))

  (testing "a robot should not write approvals"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}
                   :configuration {:service-user-url "http://robot.com"}}
          application {:team_id "stups"
                       :id "foo"}]
      (with-redefs [fuser/require-service-team (constantly "stups")
                    fuser/require-internal-user (constantly nil)
                    api/load-application (constantly application)
                    sql/cmd-approve-version! (constantly nil)]
        (try
          (api/approve-version! {:version_id "bar"
                                 :application_id "foo"
                                 :notes "test"}
                                request
                                nil)
          (is false)
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a human should write approvals"
    (let [request {:tokeninfo {"uid" "nikolaus"
                               "realm" "/employees"
                               "scope" ["uid"]}
                   :configuration {:team-service-url "http://employee.com"}}
          application {:team_id "stups"
                       :id "foo"}]
      (with-redefs [fuser/get-teams (constantly [{:name "stups"} {:name "asa"}])
                    fuser/require-internal-user (constantly nil)
                    api/load-application (constantly application)
                    sql/cmd-approve-version! (constantly nil)]
        (api/approve-version! {:version_id "bar"
                               :application_id "foo"
                               :notes "test"}
                              request
                              nil)))))
