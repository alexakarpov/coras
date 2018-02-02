(ns coras.utils
  (:require (clj-time [core :as t]
                      [format :as f])
            [clojure.data.json :as json]
            [coras [events :as e]]
            [config.core :refer [env]]
            [clojure.core.async :as a])
  (:gen-class))

(defn read-config [key] (env key))

(defn generate-events [machine-id & {:keys [from]
                                     :or {from (t/now)}}]
  "Produces an infinite lazy sequence of events, starting at 'from', each 40 seconds from the previous one"
  (let [forty-seconds-later
        (fn [cnt]
          (t/plus from (t/seconds (* cnt 40))))
        ]
    (for [i (iterate #(inc %) 0)]
      (e/make-event machine-id :timestamp (forty-seconds-later i)))))

(defn report-on-chan [chan]
  (cond
    (nil? chan) "nil channel, make sure to open one"
    (nil? (.buf chan)) "empty unbuffered channel"
    :else (format "channel size: %d, closed?: %s"
                  (.count (.buf chan))
                  (clojure.core.async.impl.protocols/closed? chan))))
