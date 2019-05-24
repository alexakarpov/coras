(ns eplk.driver
  ^{:doc
    "This is where the Event-processing/writing machine lives. Behold, the core.async magic! Even though I prefer Actor model, this is kinda cool."
    }
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require (clj-time [core :as t]))
  (:require [eplk.utils :as u])
  (:require [eplk.events :refer [process-event timeout-event alert-event]])
  (:require [eplk.kafka :as k])
  (:gen-class))

(def heartbeats (atom {}))
(def alarms (atom {}))

(defn clear-state-stop-machine [ch] (do
                                      (swap! heartbeats {})
                                      (swap! alarms {})
                                      (a/close! ch)))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file (u/read-config :journal-file)}}]
  (k/send-message 42 event)
  (spit out-file (str event "\n") :append true))

(defn track-machine [id]
  "creates a timeout channel and store it in the heartbeats map"
  (println "tracking" id)
  (swap! heartbeats (fn [hs] (assoc hs (a/timeout 10000) id)))
  id)

(defn alarm-timeout [id]
  "create a timeout channel for id and store it in the heartbeats map"
  (println "preparing alarm for" id)
  (swap! alarms (fn [as] (assoc as (a/timeout 5000) id)))
  id)

(defn run-with-chan [core-chan]
  "the meat and potatoes"
  (a/go
    (loop [ch core-chan
           n 0]
      (let
          [[msg ch] (a/alts! (concat
                            [ch]
                            (keys @heartbeats)
                            (keys @alarms)))
           _ (println "processing" msg "from" ch "at" (t/now) "at" (u/trep))]
        (cond
          (= ch core-chan)
          (do ;; normal case
            (track-machine (:machine_id msg))
            (->
             msg
             process-event
             json/write-str
             append-to-journal)
            (recur core-chan (inc n)))
          (some #(= % ch) (keys @heartbeats))
          (do
            (let [late-machine (get @heartbeats ch)]
              (swap! heartbeats #(dissoc % ch))
              (println late-machine "is late")
              (->
               late-machine ; take the machine id
               alarm-timeout ; create an alarm for it
               timeout-event ; build an event from it
               json/write-str ; transform event to json
               append-to-journal)) ; log the json
            (recur core-chan (inc n)))
          :else
          (let [late-machine (get @alarms ch)]
            (assert (some #(= % ch) (keys @alarms)))
            (swap! alarms #(dissoc % ch))
            (a/close! ch)
            (Thread/sleep 1000)
            ;; machine is already 'late', fire an alarm
            (do
              (swap! alarms (fn [as] (dissoc as ch)))
              (->
               late-machine
               alert-event
               json/write-str
               append-to-journal)
              (recur core-chan (inc n)))))))))
