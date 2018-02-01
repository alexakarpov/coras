(ns coras.kafka
  (:require [clj-kafka.zk :as zk]
            [clj-kafka.producer :refer :all]
            [coras.utils :refer [read-config]]))

(def p (producer (read-config :kafka-broker)))

(def topic (read-config :journal-topic))
(defn send-event [event]
  (send-message p (message topic (.getBytes event)))
  event)
