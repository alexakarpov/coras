(ns coras.kafka
  (:require [clj-kafka.zk :as zk]
            [clj-kafka.producer :refer :all]))

(def kafka-config (clojure.edn/read-string (slurp "resources/kafka-config.edn")))

(def p (producer kafka-config))

(defn send-event [event]
  (send-message p (message "ak_machine_events" (.getBytes event)))
  event)
