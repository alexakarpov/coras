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
                       [topic])
    (doseq [record (K.in/poll consumer
                              {::K/timeout [5 :seconds]})]
      (println (format "Record %d @%d - Key = %d, Value = %s"
                       (::K/offset record)
                       (::K/timestamp record)
                       (::K/key record)
                       (::K/value record))))
    (K.in/commit-offsets consumer))


  )

(defn make-producer []
  (K.out/producer {::K/nodes [[(:kafka-host env) 9002]]
                   ::K/serializer.key (K/serializers :long)
                   ::K/serializer.value :string
                   ::K.out/configuration {"client.id" "clj-producer"}}))

(defn send-message [i body]
  (println (format "%d received with %s" i body))
  (with-open [producer (make-producer) ]
    (K.out/send producer
                {::K/topic (:journal-topic env)
                 ::K/key 42
                 ::K/value (str "bbbody")}
                (fn callback [exception metadata]
                  (println (format "Event %d : %s"
                                   i
                                   (if exception
                                     "FAILURE"
                                     "SUCCESS")))))))

;; ########## working ###############

(with-open [producer (K.out/producer {::K/nodes [[(:kafka-host env) 9092]]
                                      ::K/serializer.key (K/serializers :long)
                                      ::K/serializer.value :string
                                      ::K.out/configuration {"client.id" "my-producer"}})]
  (doseq [i (range 5)]
    (K.out/send producer
                {::K/topic "ak_test_events"
                 ::K/key   i
                 ::K/value (str "message " i)}
                (fn callback [exception metadata]
                  (println (format "Event %d : %s"
                                   i
                                   (if exception
                                     "FAILURE"
                                     "SUCCESS")))))))
;; ###########$$$$$$$$$$$$$$$$$######


(send-message 12 "hello")


(try
  (throw
   (ex-info "The ice cream has melted!"
            {:causes             #{:fridge-door-open :dangerously-high-temperature}
             :current-temperature {:value 25 :unit :celsius}}))
  (catch Exception e (ex-data e)))



(defn list-topics []
  (with-open [admin (K.admin/admin {:dvlopt.kafka/nodes [["kafka.alexakarpov.xyz" 9092]]})]
    (keys @(K.admin/topics admin))))


(list-topics)
