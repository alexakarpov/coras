(ns coras.kafka
  (:require [clj-kafka.zk :as zk]
            [clj-kafka.producer :refer :all]
            [config.core :refer [env]]))

(def p (producer (:kafka-broker env)))

(def topic (:journal-topic env))

(defn send-event [event]
  (send-message p (message topic (.getBytes event)))
  event)
