(ns eplk.driver
  ^{:doc
    "This is where the Event-processing/writing machine lives. Behold, the core.async magic! Even though I prefer Actor model, this is kinda cool."
    }
  (:require [clojure.core.async :as a :refer [>!
                                              >!!
                                              <!
                                              <!!
                                              alts!
                                              alts!!
                                              timeout
                                              go
                                              ]])
  (:require [clojure.data.json :as json])
  (:require (clj-time [core :as t]
                      [format :as f]))
  (:require [eplk.utils :as u :refer [read-config report-on-chan]])
  (:require [eplk.events :refer [process-event timeout-event alert-event]])
  (:require [eplk.kafka :as k])
  (:gen-class))

(def heartbeats (atom {}))
(def alarms (atom {}))

;; "pause" switch for the machine
(def pause (atom true))

;; kill switch to force machine to terminate
(def kill (atom false))
(defn dokill []
  (swap! kill not ))

(defn clear-state-stop-machine [ch] (do
                                      (reset! kill true)
                                      (swap! heartbeats {})
                                      (swap! alarms {})
                                      (a/close! ch)))

(defn toggle []
  (swap! pause not))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file (read-config :journal-file)}}]
  (k/send-message 42 event)
  (spit out-file (str event "\n") :append true))

;; this map will store timeout channels mapped to machine ids
(comment
  @heartbeats
  @alarms
)

(defn track-machine [id]
  (println "tracking" id)
  (swap! heartbeats (fn [hs] (assoc hs (timeout 10000) id)))
  id)

(defn alarm-timeout [id]
  (println "preparing alarm for" id)
  (swap! alarms (fn [as] (assoc as (timeout 5000) id)))
  id)

(comment
  (alts!! (concat
           [@eplk.core/in-ch]
           (keys @heartbeats)
           (keys @alarms)))
  (vals @heartbeats)
  (vals @alarms)
  )

(defn run-with-chan [core-chan]
  "the meat and potatoes"
  (go
    (loop [ch core-chan
           n 0]
      (do
        (println (u/trep) "- entered run-with-chan's go-loop, step" n (report-on-chan ch))
        (let
            [_ (println (u/trep) "- calling alts!")
             [msg ch] (alts! (concat
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
                (recur core-chan (inc n))))))))))
