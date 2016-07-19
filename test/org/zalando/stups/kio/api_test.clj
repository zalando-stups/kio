(ns org.zalando.stups.kio.api-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.kio.core :refer [run]]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.friboo.user :as fuser]
            [org.zalando.stups.friboo.auth :as auth]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [org.zalando.stups.kio.test-utils :as util]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]))

(deftest test-the-tester
  "I succeed"
  (is true))

(def base-url "http://localhost:8080")

(defn api-url [& path]
  (let [url (apply str base-url path)]
    (println (str "[request] " url))
    url))

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

#_(deftest test-create-versions-using-api
  (with-redefs [api/require-write-authorization (constantly true)
                api/from-token (constantly "barfoo")]
    (let [system (run {})
          request-options {:headers {:content-type "application/json"} :throw-exceptions false}
          version {:notes "My new version" :artifact "docker://stups/foo1:1.0-master"}
          version-data (assoc request-options :body (json/encode version))
          application {:team_id "bar" :active true :name "FooBar"}
          application-data (assoc request-options :body (json/encode application))
          create-app-resp (client/put (api-url "/apps/foo1") application-data)]

      (is (= (:status create-app-resp)
             200)
          (str "response of wrong status: " create-app-resp))

      (testing "create a valid new version 1.0-master"
        (let [response (client/put (api-url "/apps/foo1/versions/1.0-master") version-data)]
          (is (= (:status response)
                 200)
              (str "response of wrong status: " response))))

      (testing "create a valid new version 1.0"
        (let [response (client/put (api-url "/apps/foo1/versions/1.0") version-data)]
          (is (= (:status response)
                 200)
              (str "response of wrong status: " response))))

      (testing "create a valid new version v1.0"
        (let [response (client/put (api-url "/apps/foo1/versions/v1.0") version-data)]
          (is (= (:status response)
                 200)
              (str "response of wrong status: " response))))

      (testing "create a invalid new version"
        (let [response (client/put (api-url "/apps/foo1/versions/1..-..1") version-data)]
          (is (= (:status response)
                 400)
              (str "response of wrong status: " response))))

      (component/stop system))))

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

(deftest test-write-application
  (testing "when updating application, the team in db should be compared"
    (let [calls (atom {})]
      (with-redefs [api/require-write-authorization (comp (constantly true)
                                                          (util/track calls :auth))
                    sql/cmd-create-or-update-application! (constantly nil)
                    api/load-application (constantly {:team_id "db-team"})]
          (api/create-or-update-application! {:application_id "test"
                                              :application {:team_id "api-team"
                                                            :active true
                                                            :name "test"}}
                                             {:tokeninfo {"uid" "nikolaus"
                                                          "realm" "/employees"}}
                                             nil)
          (is (= "db-team" (-> @calls :auth first second))))))

  (testing "when creating application, the team in body should be compared"
    (let [calls (atom {})]
      (with-redefs [api/require-write-authorization (comp (constantly true)
                                                          (util/track calls :auth))
                    sql/cmd-create-or-update-application! (constantly nil)
                    api/load-application (constantly nil)]
        (api/create-or-update-application! {:application_id "test"
                                            :application {:team_id "api-team"
                                                          :active true
                                                          :name "test"}}
                                           {:tokeninfo {"uid" "nikolaus"
                                                        "realm" "/employees"}}
                                           nil)
        (is (= "api-team" (-> @calls :auth first second)))))))

(deftest test-require-write-access
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
      (with-redefs [auth/get-auth (constantly true)]
        (api/require-write-authorization request "stups"))))

  (testing "a human should get access if it is in the required team"
    (let [request {:tokeninfo {"uid" "npiccolotto"
                               "realm" "/employees"
                               "scope" ["uid"]}}]
      (with-redefs [auth/get-auth (constantly true)]
        (api/require-write-authorization request "stups"))))

  (testing "a human should not get access if it is not in the required team"
    (let [request {:tokeninfo {"uid" "npiccolotto"
                               "realm" "/employees"
                               "scope" ["uid"]}}]
      (with-redefs [auth/get-auth (constantly false)]
        (try (do
               (api/require-write-authorization request "stups")
               (is false))
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should not write without write scope"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid"]}}]
      (with-redefs [auth/get-auth (constantly true)]
        (try
          (api/require-write-authorization request "stups")
          (is false)
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should write with write scope and correct team"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}}]
      (with-redefs [auth/get-auth (constantly true)]
        (api/require-write-authorization request "stups"))))

  (testing "a robot should not write with write scope to another team"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid"]}}]
      (with-redefs [auth/get-auth (constantly false)]
        (try
          (api/require-write-authorization request "team-britney")
          (is false)
          (catch Exception e
            (is (= 403 (:http-code (ex-data e)))))))))

  (testing "a robot should write applications"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}}
          application {:team_id "stups"
                       :id "foo"}]
      (with-redefs [auth/get-auth (constantly true)
                    api/load-application (constantly application)
                    sql/cmd-create-or-update-application! (constantly nil)]
        (api/create-or-update-application! {:application application
                                            :application_id "foo"}
                                           request
                                           nil))))

  (testing "a robot should write versions"
    (let [request {:tokeninfo {"uid" "robobro"
                               "realm" "/services"
                               "scope" ["uid" "application.write"]}}
          application {:team_id "stups"
                       :id "foo"}
          version {:id "bar"}]
      (with-redefs [auth/get-auth (constantly true)
                    api/load-application (constantly application)
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
                   :configuration {:service-user-url "http://robot.com"
                                   :magnificent-url "magnificent-url"}}
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
                   :configuration {:team-service-url "http://employee.com"
                                   :magnificent-url "magnificent-url"}}
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
