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

  (def x (go (let [c (a/chan)
                   [v p] (a/alts! [c] :default 42)]
               (println (format "v is %s p is %s" v p))))
    )
)

(defn track-machine [id]
  (println "tracking" id)
  (swap! heartbeats (fn [hs] (assoc hs (timeout 10000) id))))

(defn alarm-timeout [mid]
  (do
    (println "preparing alarm for " mid)
    (swap! alarms (fn [as] (assoc as (timeout 5000) mid)))))

(defn run-with-chan [core-chan]
  "the meat and potatoes"
  (go
    (println "go block - hello from" (u/trep))
    (loop [ch core-chan
           n 0]
      (do
        (println (u/trep) "- entered run-with-chan's go-loop, step" n (report-on-chan ch))
        (Thread/sleep 1000)
        (let
            [[msg ch] (alts! (concat
                              [ch]
                              (keys @heartbeats)
                              (keys @alarms))
                             :default 42)]
          (cond
            (nil? msg) (do
                         (println "WTF " ch ", why do I get nil from you here? at" (u/trep))
                         :wtf)
            (= ch :default) :ok
            (= ch core-chan) ; msg is from core channel
            (do ;; normal case
              (println (u/trep) "- driver received event:" msg " from " ch)
              (track-machine (:machine_id msg))
              (->
               msg
               process-event
               json/write-str
               append-to-journal)
              (println (format "%s recursing with %d heartbeats " (u/trep) (count (keys @heartbeats))))
              (recur core-chan (inc n)))
            :else ; message is from a timeout channel
            (let [late-machine (get @heartbeats ch)]
              (println (u/trep) "- timeout received for " late-machine)
              (assert (not (nil? late-machine)))
              (Thread/sleep 2000)
              (if (some #(= % late-machine) (vals @alarms))
                ;; machine is already 'late', fire an alarm
                (do
                  (swap! alarms (fn [as] (dissoc as ch)))
                  (->
                   late-machine
                   alert-event
                   json/write-str
                   append-to-journal)
                  (recur core-chan (inc n)))
                ;;+ this is the first breach
                (do (->
                     late-machine ; take the machine id
                     alarm-timeout
                     timeout-event ; build an event from it
                     json/write-str ; transform event to json
                     append-to-journal) ; log the json
                    (recur core-chan (inc n)))))))))))
