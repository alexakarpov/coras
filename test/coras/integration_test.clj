(ns coras.integration-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [coras.core :refer :all]
            [coras.kafka :as k]
            [coras.events :as e]
            [coras.driver :as d]
            [coras.utils :as u]
            [clojure.core.async :as a]))

(deftest test-end-to-end
  (testing "event is processed and logged to Kafka"
    (let [kafka-mock (a/chan 10)]
      ;; mocking Kafka interface, so instead of submittin to Kafka, we push the value into this channel
      (with-redefs [k/send-event #(a/>!! kafka-mock %)]
        (let [ch (a/chan 3)
              event (e/make-event "Machine1")
              _ (a/>!! ch event)]
          ;; we start with no messages in Kafka
          (is (= 0 (u/count-in-channel kafka-mock)))
          ;; drumroll!!
          (d/run-with-chan ch)
          (let [[val ach] ;; read from our mocked Kafka
                (a/alts!! [kafka-mock (a/timeout 1000)])]
            ;; assert what we've logged to journal (Kafka) is the processed JSON-encoded event
            (is (= val
                   (json/write-str (e/process-event event)))))
          (d/dokill)
          )))))
