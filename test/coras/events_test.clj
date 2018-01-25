(ns coras.events-test
  (:require
   (clj-time [core :as t]
             [format :as f])
   [clojure.test :refer :all]
   [coras.events :refer :all]))

(defonce time-regex #"(\d{4})-(\d{2})-(\d{2})T(\d{2})\:(\d{2})\:(\d{2})Z")

(deftest event-generation-with-type-wo-ts
  (testing "event created with passed type, without a timestamp"
    (let [event (make-event "M2" :type "Broke")]
      (is (and (= (:type event) "Broke")
               (= (:machine_id event) "M2")
               (re-matches time-regex (:timestamp event)))))))

(deftest event-generation-test-ts
  (testing "event created without a passed type, with a timestamp"
    (let [ts (t/now)
          ft (f/unparse (f/formatters :date-time-no-ms) ts)
          event (make-event "M1" :timestamp ts)]
      (is (and (= (:type event) "CycleComplete")
               (= (:machine_id event) "M1")
               (= (:timestamp event) ft))))))

(deftest event-from-line-test
  (testing "event is created from a line"
    (let [event_line "Machine2 CycleComplete"
          event (event-from-line event_line)]
      (is (and (= (:type event) "CycleComplete")
               (= (:machine_id event) "Machine2")
               (re-matches time-regex (:timestamp event)))))))
