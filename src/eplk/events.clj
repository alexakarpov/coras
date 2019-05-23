(ns eplk.events
  "Functions for creating and transforming machine events."
  (:require [clojure.core.match :refer [match]])
  (:require (clj-time [core :as t]
                      [format :as f]))
  (:gen-class))

;; regular expression matching the incoming events schema.
(def event-re #"\{\"type\"\:\"(.+)\",\"machine_id\"\:\"(.+)\",\"timestamp\"\:\"(.+)\"\}")

(defn- format-timestamp [ts]
  (let [formatter (f/formatters :date-time-no-ms)]
    (f/unparse formatter ts)))

(defn make-event [machine-id & {:keys [type timestamp]
                                :or {type "CycleComplete"
                                     timestamp (format-timestamp (t/now))}}]
  {:type type
   :machine_id machine-id
   :timestamp timestamp})

(defn event-from-line [line]
  "creates a map of event data from it's string representation"
  (let [[_ type machine-id timestamp] (re-matches event-re line)
        event (make-event machine-id
                          :type type
                          :timestamp timestamp)]
    event))

(defn timeout-event [machine-id]
  "Creates a defined timeout-type event "
  (make-event machine-id
              :type "NonProductionLimitReached"))

(defn alert-event [machine-id]
  "Creates a defined alert-type event"
  (make-event machine-id
              :type "AlarmOpened"))

(defn process-event [event]
  (println (.getName (Thread/currentThread)) "- entering events/process-event:" event)
  (match event
         {:type "CycleComplete"
          :timestamp ts
          :machine_id mid}
         (do
           {:type "MachineCycled"
            :recorded_at (format-timestamp (t/now))
            :machine_id mid
            :timestamp ts
            })
         _ (throw (Exception. (format "Unprocessable event: %s " event)))))
