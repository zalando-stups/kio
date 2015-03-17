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

(ns org.zalando.kio.sql
  (:require [yesql.core :refer [defquery]]))

(def default-db-spec {:db-classname   "org.postgresql.Driver"
                      :db-subprotocol "postgresql"
                      :db-subname     "//localhost:5432/local_kio_db"
                      :db-user        "postgres"
                      :db-password    "postgres"})

; TODO maybe configure as map and then iterate? does yesql not provide such a thing?
(defquery read-applications "sql/application-read-all.sql")
(defquery read-application "sql/application-read.sql")
(defquery save-application! "sql/application-save.sql")
(defquery delete-application! "sql/application-delete.sql")
(defquery save-application-secret! "sql/application-save-secret.sql")
