(ns eplk.utils
  (:require (clj-time [core :as t]
                      [format :as f])
            [clojure.data.json :as json]
            [eplk.events :as e]
            [config.core :refer [env]]
            [clojure.core.async :as a])
  (:gen-class))

(defn read-config [key] (env key))

(defn trep []
  (.getName (Thread/currentThread)))

(defn generate-events [machine-id & {:keys [from]
                                     :or {from (t/now)}}]
  "Produces an infinite lazy sequence of events, starting at 'from', each 40 seconds from the previous one"
  (let [forty-seconds-later
        (fn [cnt]
          (t/plus from (t/seconds (* cnt 40))))]
    (for [i (iterate #(inc %) 0)]
      (e/make-event machine-id :timestamp (forty-seconds-later i)))))

(defn count-in-channel [channel]
  (cond
    (nil? channel) :closed
    (nil? (.buf channel)) :empty_unbuff
    :else (.count (.buf channel))))

(defn submit-event [channel machine-id]
  (let [buffer (.buf channel)
        event (e/make-event machine-id)]
    (if (>= (.count buffer) 10)
      [:error :channel_full] ; so we don't *actually* block the REPL's main thread
      (do
        (println "buffer has space, enq this event for" machine-id )
        (a/>!! channel event)
        event))))

(defn report-on-chan [chan]
  (cond
    (nil? chan) "nil channel, make sure to open one"
    (nil? (.buf chan)) "empty unbuffered channel"
    :else (format "channel size: %d, closed?: %s"
                  (.count (.buf chan))
                  (clojure.core.async.impl.protocols/closed? chan))))
