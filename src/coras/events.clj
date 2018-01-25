(ns coras.events
  "Functions for creating and submitting machine events."
  (:require (clj-time [core :as t]
                      [format :as f]))
  (:gen-class))


(defn- format-timestamp [ts]
  (let [formatter (f/formatters :date-time-no-ms)] 
    (f/unparse formatter ts)))

(defn make-event
  ([machine-id & {:keys [type timestamp]
                  :or {type "CycleComplete"
                       timestamp (t/now)}}]
   {:type type
    :machine_id machine-id
    :timestamp (format-timestamp timestamp)}))

(defn event-from-line [line]
  (let [[machine type] (clojure.string/split line #"\s")
        event (make-event machine :type type)]
    event))
