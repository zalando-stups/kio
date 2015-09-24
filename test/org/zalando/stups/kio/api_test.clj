(ns org.zalando.stups.kio.api-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.friboo.user :as fuser]))

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

(deftest test-update-application-criticality1
  "If the application does not exist, it should not call update and return 404"
  (let [calls (atom 0)
        criticality nil
        request nil
        db nil]
    (with-redefs [api/load-application (constantly nil)
                  sql/update-application-criticality! (fn [] (swap! calls inc))]
      (let [response (api/update-application-criticality! criticality request db)]
        (is (= @calls 0))
        (is (= 404 (:status response)))))))

(deftest test-update-application-criticality2
  "If the application does exist, it should check for special uids"
  (let [request {:tokeninfo {"uid" "npiccolotto"}
                 :configuration {:allowed-uids "npiccolotto"}}
        db nil
        criticality nil]
    (with-redefs [api/load-application (constantly {:id "kio"})
                  sql/update-application-criticality! (constantly 1)]
      (is (-> (api/update-application-criticality! criticality request db)
              (get :status)
              (= 200))))))

(deftest test-update-application-criticality3
  "If the application does exist it should return 403 if special uids do not match"
  (let [request {:tokeninfo {"uid" "npiccolotto"}
                 :configuration {:allowed-uids "foo,bar"}}
        db nil
        criticality nil]
    (with-redefs [api/load-application (constantly {:id "kio"})
                  sql/update-application-criticality! (constantly 1)]
      (try (do (api/update-application-criticality! criticality request db)
               (is false))
           (catch Exception e (is (-> (ex-data e)
                                      (get :http-code)
                                      (= 403))))))))

(deftest test-write-auth-service-no-realm-not-ok
  "If the realm is missing it should not get access."
  (let [request {:tokeninfo {"uid" "stups_robot"
                             "scope" ["uid" "application.write_all"]}
                 :configuration {:allowed-uids "stups_robot,foo"}}]
    (try (do
           (api/require-write-authorization request "stups")
           (is false))
      (catch Exception e))))

(deftest test-write-auth-service-no-uid-not-ok
  "If the uid is missing it should not get access."
  (let [request {:tokeninfo {"realm" "/services"
                             "scope" ["uid" "application.write_all"]}
                 :configuration {:allowed-uids "stups_robot,foo"}}]
    (try (do
           (api/require-write-authorization request "stups")
           (is false))
      (catch Exception e))))

(deftest test-write-auth-service-ok
  "If a service has necessary scope and uid it should get access."
  (let [request {:tokeninfo {"uid" "stups_robot"
                             "realm" "/services"
                             "scope" ["uid" "application.write_all"]}
                 :configuration {:allowed-uids "stups_robot,foo"}}]
    (api/require-write-authorization request "stups")))

(deftest test-write-auth-service-not-ok1
  "If a service doesn't have the scope it should not get access."
  (let [request {:tokeninfo {"uid" "stups_robot"
                             "realm" "/services"
                             "scope" ["uid"]}
                 :configuration {:allowed-uids "stups_robot,foo"}}]
    (try (do
           (api/require-write-authorization request "stups")
           (is false))
      (catch Exception e))))

(deftest test-write-auth-service-not-ok2
  "If a service doesn't have the correct uid it should not get access."
  (let [request {:tokeninfo {"uid" "stups_robot"
                             "realm" "/services"
                             "scope" ["uid" "application.write_all"]}
                 :configuration {:allowed-uids "bar,foo"}}]
    (try (do
           (api/require-write-authorization request "stups")
           (is false))
      (catch Exception e))))

(deftest test-write-auth-employee-specific-team-ok
  "A user has to be in the correct team"
  (let [request {:tokeninfo {"uid" "npiccolotto"
                             "realm" "/employees"
                             "scope" ["uid"]}
                 :configuration {:team-service-url "http://example.com"}}]
    (with-redefs [fuser/get-teams (constantly [{:name "stups"} {:name "asa"}])
                  fuser/require-internal-user (constantly nil)]
      (api/require-write-authorization request "stups"))))

(deftest test-write-auth-employee-specific-team-not-ok
  "A user has to be in the correct team"
  (let [request {:tokeninfo {"uid" "npiccolotto"
                             "realm" "/employees"
                             "scope" ["uid"]}
                 :configuration {:team-service-url "http://example.com"}}]
    (with-redefs [fuser/get-teams (constantly [{:name "test"} {:name "asa"}])
                  fuser/require-internal-user (constantly nil)]
      (try (do
             (api/require-write-authorization request "stups")
             (is false))
        (catch Exception e)))))