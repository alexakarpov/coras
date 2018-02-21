(ns coras.kafka
  (:require [clj-kafka.zk :as zk]
            [clj-kafka.producer :refer :all]
            [clj-kafka.consumer.zk :refer :all]
            [clj-kafka.admin :as admin]
            [config.core :refer [env]]))

(def p (producer (:kafka-broker env)))

(def topic (:journal-topic env))

(def config {"zookeeper.connect" "localhost:2181"
             "group.id" "foo"
             "auto.offset.reset" "smallest"
             "auto.commit.enable" "false"})

(comment
  (defmacro with-resources
    [bindings close-fn & body]
    (let [[x v & more] bindings]
      `(let [~x ~v]
         (try
           ~(if-let [more (seq more)]
              `(with-resources ~more ~close-fn ~@body)
              `(do ~@body))
           (finally
             (~close-fn ~x)))))))

(defn send-event [event]
  (send-message p (message topic (.getBytes event)))
  event)

(defn ak-create-topic [tname]
  (with-open [zk (admin/zk-client "127.0.0.1:2181")]
    (if-not (admin/topic-exists? zk "test-topic")
      (admin/create-topic zk tname
                          {:partitions 3
                           :replication-factor 1
                           :config {"cleanup.policy" "compact"}}))))

(defn ak-delete-topic [tname]
  (with-open [zk (admin/zk-client "127.0.0.1:2181")]
    (admin/delete-topic zk tname)))

