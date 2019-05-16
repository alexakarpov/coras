(ns eplk.kafka
  (:require
   [dvlopt.kafka       :as K]
   [dvlopt.kafka.admin :as K.admin]
   [dvlopt.kafka.in    :as K.in]
   [dvlopt.kafka.out   :as K.out]
   [config.core :refer [env]]))

(comment
  (config.core/reload-env)

  (with-open [consumer (K.in/consumer {::K/nodes              [[(:kafka-host env) 9092]]
                                       ::K/deserializer.key   :long
                                       ::K/deserializer.value :string
                                       ::K.in/configuration   {"auto.offset.reset" "earliest"
                                                               "enable.auto.commit" false
                                                               "group.id"           "clj-group"}})]
    (K.in/register-for consumer
                       [(:journal-topic env)])
    (doseq [record (K.in/poll consumer
                              {::K/timeout [5 :seconds]})]
      (println (format "Record %d @%d - Key = %d, Value = %s"
                       (::K/offset record)
                       (::K/timestamp record)
                       (::K/key record)
                       (::K/value record))))
    (K.in/commit-offsets consumer))


  )

(defn producer-conf []
  {::K/nodes [[(:kafka-host env) 9092]]
   ::K/serializer.key (K/serializers :long)
   ::K/serializer.value :string
   ::K.out/configuration {"client.id" "my-producer"}})

(defn send-message [i body]
  (println (format "%d received with %s" i body))
  (with-open [producer (K.out/producer (producer-conf))]
    (K.out/send producer
                {::K/topic "ak_test_events"
                 ::K/key   i
                 ::K/value (format "%s with id of %d" body i)}
                (fn callback [exception metadata]
                  (println (format "Event %d : %s"
                                   i
                                   (if exception
                                     "FAILURE"
                                     "SUCCESS")))))))

(defn list-topics []
  (with-open [admin (K.admin/admin {:dvlopt.kafka/nodes [[(:kafka-host env) 9092]]})]
    (keys @(K.admin/topics admin))))


(list-topics)
