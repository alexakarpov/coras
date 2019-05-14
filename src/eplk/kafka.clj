(ns eplk.kafka
  (:require
   [dvlopt.kafka       :as K]
   [dvlopt.kafka.admin :as K.admin]
   [dvlopt.kafka.in    :as K.in]
   [dvlopt.kafka.out   :as K.out]
   [config.core :refer [env]]))

(def topic (:journal-topic env))

;; (defn create-topic [topic-name]
;;   (with-open [admin (K.admin/admin)]
;;     (K.admin/create-topics
;;      admin
;;      {topic-name {::K.admin/number-of-partitions 2
;;                   ::K.admin/replication-factor   1
;;                   ::K.admin/configuration        {"cleanup.policy" "compact"}}})
;;     (println "Existing topics : " (keys @(K.admin/topics admin {::K/internal? false})))))

(defn delete-topics [topic & topic-names]
  (with-open [admin (K.admin/admin)]
    (K.admin/delete-topics
     admin
     (cons topic topic-names))
    (println "Remaining topics : " (keys @(K.admin/topics admin {::K/internal? false})))))

(defn list-topics []
  (with-open [admin (K.admin/admin {:dvlopt.kafka/nodes [["kafka.alexakarpov.xyz" 9092]]})]
    (keys @(K.admin/topics admin))))

(list-topics)
