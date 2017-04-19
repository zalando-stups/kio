(ns org.zalando.stups.kio.integration-test.api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest is testing]]
            [midje.sweet :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [org.zalando.stups.kio.core :refer [run]]
            [org.zalando.stups.kio.api :as api]
            [org.zalando.stups.friboo.system.oauth2 :as oauth2]))

(defn api-url [& path]
  (let [url (apply str "http://localhost:8080" path)]
    (println (str "[request] " url))
    url))

(defrecord NoTokenRefresher
  [configuration]
  com.stuartsierra.component/Lifecycle
  (start [this] this)
  (stop [this] this))

(deftest ^:integration integration-test
  (with-redefs [api/require-write-authorization (constantly nil)
                oauth2/map->OAuth2TokenRefresher map->NoTokenRefresher
                oauth2/access-token (constantly "token")
                api/from-token (constantly "barfoo")]

    (let [system          (run {})
          request-options {:throw-exceptions false
                           :content-type     :json}
          application     {:team_id "bar"
                           :active  true
                           :name    "FooBar"}
          version         {:notes    "My new version"
                           :artifact "docker://stups/foo1:1.0-master"}
          version-req     (->
                            request-options
                            (assoc :body (json/encode version))
                            (assoc :as :json))]
      (against-background [(before :contents (http/put
                                               (api-url "/apps/foo1")
                                               (assoc request-options :body (json/encode application))))
                           (after :contents (component/stop system))]
        (facts "API"
          (facts "it validates version numbers correctly"
            (fact "1.0-master works"
              (http/put
                (api-url "/apps/foo1/versions/1.0-master")
                version-req) => (contains {:status 200}))

            (fact "1.0 works"
              (http/put
                (api-url "/apps/foo1/versions/1.0")
                version-req) => (contains {:status 200}))

            (fact "1..-..1 does not work"
              (http/put
                (api-url "/apps/foo1/versions/1..-..1")
                version-req) => (contains {:status 400}))))))))
