(ns eplk.integration-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [eplk.core :refer :all]
            [eplk.kafka :as k]
            [eplk.events :as e]
            [eplk.driver :as d]
            [eplk.utils :as u]
            [clojure.core.async :as a]))

(deftest test-end-to-end
  (testing "event is processed and logged to Kafka"
    (let [kafka-mock (a/chan 10)]
      ;; mocking Kafka interface, so instead of submittin to Kafka, we push the value into this channel
      (with-redefs [k/send-message #(a/>!! kafka-mock [%1 %2])]
        (let [ch (a/chan 1)
              event (e/make-event "Machine1")]
          ;; drumroll!!
          (a/>!! ch event)
          (d/run-with-chan ch)
          (let [val (a/<!! kafka-mock)]
            ;; assert what we've logged to journal (Kafka) is the processed JSON-encoded event
            (is (= val
                   [42 (json/write-str (e/process-event event))]))
            (is (= (count @d/heartbeats) 1))
            (is (= (count @d/alarms) 0)))
;          (d/clear-state-stop-machine ch)
          )))))

(run-tests)
