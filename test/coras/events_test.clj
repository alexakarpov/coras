(ns coras.events-test
  (:require
   (clj-time [core :as t]
             [format :as f])
   [clojure.test :refer :all]
   [clojure.core.match :refer [match]]
   [coras.events :refer :all]))

(def time-regex #"(\d{4})-(\d{2})-(\d{2})T(\d{2})\:(\d{2})\:(\d{2})Z")

(deftest event-generation-with-type-no-ts
  (testing "event created with passed type, without a timestamp"
    (let [event (make-event "M2" :type "Broke")]
      (is (and (= (:type event) "Broke")
               (= (:machine_id event) "M2")
               (re-matches time-regex (:timestamp event)))))))

(deftest event-generation-no-type-with-ts-test
  (testing "event created without a passed type, with a timestamp"
    (let [ts (t/now)
          ft (f/unparse (f/formatters :date-time-no-ms) ts)
          event (make-event "M1" :timestamp ts)
          _ (println event)]
      (is (match event
                 {:type "CycleComplete"
                  :timestamp ts
                  :machine_id "M1"} true
                 _ false)))))

(deftest process-cc-test
  (testing "CycleComplete event processed"
    (let [src-event (make-event "M2")
          ts (:timestamp src-event)
          dest-event (process-event src-event)]
      (is (match dest-event
                 {:machine_id "M2"
                  :timestamp ts
                  :recorded_at _
                  :type "MachineCycled"} true
                 _ false)))))

(deftest bad-event-test
  (testing "bad evebt throws"
    (is (thrown? Exception (process-event {:foo "bar"})))))
