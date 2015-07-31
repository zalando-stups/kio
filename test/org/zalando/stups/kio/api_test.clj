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

(deftest test-update-application-criticality1
  "If the application does not exist, it should not call update and return 404"
  (let [calls (atom 0)]
    (with-redefs [api/application-exists? (constantly false)
                  sql/update-application-criticality! (fn [] (swap! calls inc))]
      (let [response (api/update-application-criticality! {:application_id "kio"} nil nil)]
        (is (= @calls 0))
        (is (= 404
               (:status response)))))))
