(ns org.zalando.stups.kio.api-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.kio.sql :as sql]
            [org.zalando.stups.kio.api :as api]))

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
