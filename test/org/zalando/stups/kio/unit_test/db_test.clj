(ns org.zalando.stups.kio.unit-test.db-test
  (:require [midje.sweet :refer :all]
            [clojure.test :refer [deftest]]
            [org.zalando.stups.kio.sql :as db]
            [com.stuartsierra.component :as component]
            [org.zalando.stups.kio.sql :as sql])
  (:import (org.flywaydb.core Flyway)))

(def config {:classname   "org.postgresql.Driver"
             :subprotocol "postgresql"
             :subname     "//localhost:5432/postgres"
             :user        "postgres"
             :password    "postgres"
             :init-sql    "SET search_path TO zk_data, public"})

(deftest apply-migrations

  (fact "run migrations against db so that docs can be generated"
    (component/stop (org.zalando.stups.friboo.system.db/start-component (sql/map->DB {:configuration config}) true))))
