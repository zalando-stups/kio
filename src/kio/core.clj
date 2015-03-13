;
; Copyright 2015 Zalando SE
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns kio.core
  (:require [io.sarnowski.swagger1st.core :as s1st]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

(defn readApplications [request]
  (pprint request)
  {:status  200
   :body    "[{\"id\": \"kio\", \"name\": \"Kio\", \"teamId\": \"stups\"}, {\"id\": \"pierone\", \"name\": \"Pier One\", \"teamId\": \"stups\"}]"
   :headers {"Content-Type" "application/json"}})

(defn readApplication [request]
  (pprint request)
  {:status  200
   :body    "{\"id\": \"kio\", \"name\": \"Kio\", \"teamId\": \"stups\", \"description\": \"Registry for applications\", \"url\": \"https://kio.stups.example.org\", \"scmUrl\": \"git@github.com:zalando-stups/kio.git\", \"documentationUrl\": \"http://zalando-stups.github.io\"}"
   :headers {"Content-Type" "application/json"}})

(defn saveApplication [request]
  (pprint request)
  {:status  201
   :headers {"Content-Type" "application/json"}})

(defn deleteApplication [request]
  (pprint request)
  {:status  200
   :headers {"Content-Type" "application/json"}})

(defn saveApplicationSecret [request]
  (pprint request)
  {:status  201
   :headers {"Content-Type" "application/json"}})


(def app
  (-> (s1st/swagger-executor)
      (s1st/swagger-validator)
      (s1st/swagger-parser)
      (s1st/swagger-mapper ::s1st/yaml-cp "api.yaml")
      (wrap-params)))
