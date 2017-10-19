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

(ns org.zalando.stups.kio.sql
  (:require [yesql.core :refer [defqueries]]
            [org.zalando.stups.friboo.system.db :refer [def-db-component generate-hystrix-commands]]))

(def-db-component DB :auto-migration? true)

(def default-db-configuration
  {:db-classname   "org.postgresql.Driver"
   :db-subprotocol "postgresql"
   :db-subname     "//localhost:5432/kio"
   :db-user        "postgres"
   :db-password    "postgres"
   :db-init-sql    "SET search_path TO zk_data, public"})

(defqueries "db/applications.sql")
(generate-hystrix-commands)

(def column-prefix-pattern #"[a-z]+_(.+)")

(defn remove-prefix [m]
  (->> m
       name
       (re-find column-prefix-pattern)
       second
       keyword))

(defn strip-prefixes
  "Removes the database field prefix."
  [results]
  (map (fn [result]
          (into {} (map (fn [[k v]] [(remove-prefix k) v]) result)))
       results))
