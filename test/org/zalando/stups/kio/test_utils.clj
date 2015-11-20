; shameless copy from mint-worker and mint-storage
(ns org.zalando.stups.kio.test-utils
  (:require [clojure.test :refer :all]))

(defn track
  "Returns a function that conjs its arguments into the atom"
  ([a action]
   (fn [& args]
     (let [prev (or (get @a action)
                    [])]
     (swap! a assoc action (conj prev args))))))

(defn throwing
  "Returns a function that throws with the provided arguments when executed"
  [& [msg data]]
  (fn [& args]
    (throw (ex-info (or msg "any exception")
                    (or data {})))))

(defmacro third
  "Just as first, second"
  [coll]
  `(nth ~coll 2))

(defn sequentially
  "Returns a function that returns provided arguments sequentially on every call"
  [& args]
  (let [calls (atom -1)
        limit (dec (count args))]
    (fn [& inner]
      (if (neg? limit)
          nil
          (do
            (swap! calls inc)
            (nth args (if (> @calls limit)
                        limit
                        @calls)))))))

(defmacro same!
  [x y]
  `(is (= ~x ~y)))

(defmacro true!
  [x]
  `(same! true ~x))
