(ns kinsky-poc
  (:require [kinsky.client      :as client]
            [kinsky.async       :as async]
            [clojure.core.async :as a :refer [go <! >!]]))

(comment
  (let [p (client/producer {:bootstrap.servers "localhost:9092"}
                           (client/keyword-serializer)
                           (client/edn-serializer))]
    (client/send! p "account" :account-a {:action :login})))
