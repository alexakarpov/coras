(ns ^{:doc
     "Core namespace, you'll land here with `lein repl`. Contains functions for working with the events processing machine:
--- (submit-event <MachineID>)
--- (machine-start)
--- (machine-toggle-on-off)"}
  eplk.core
  (:require [clojure.core.async :as a :refer [go chan >!! <!!  timeout]])
  (:require (eplk [driver :as driver]
                   [events :as events]
                   [utils :as utils]))
  (:gen-class))

;; this is where the events channel is maitained during the interactive session
(def in-ch (delay (chan 10)))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})

(defn close-channel []
  (a/close! @in-ch))

(defn machine-start [& {:keys [channel]
                        :or {channel @in-ch}}]
  "Start the machine consuming the events in the channel (on a thread-pool)"
  (driver/run-with-chan channel))

(defn machine-toggle-on-off []
  "Pause/wake up the machine processing event"
  (driver/toggle))

(defn submit-event [machine-id]
  "Performs a blocking put of the event onto the interactive events channel"
  (utils/submit-event @in-ch machine-id))

;; running with events from STDIN
(def stdin-reader
  (java.io.BufferedReader. *in*))

(defn -main
  "Runs with an input channel attached to STDIN"
  [& args]
  (let [input-chan (chan 10)]
    (driver/run-with-chan input-chan) ; sets up the consumer
    (doseq [line (line-seq stdin-reader)] ; produce the line from STDIN
      (>!! input-chan (events/event-from-line line)))
    (while (false? (= 0 (.count (.buf input-chan))))
      (println ".")
      (Thread/sleep 100))))

(comment
  (submit-event "M1")
  (submit-event "M2")
  (submit-event "M3")
  (utils/report-on-chan @in-ch)
  @driver/switch
  @driver/late
  (def in-ch (delay (chan 10)))
  (driver/toggle)
  (machine-start :channel @in-ch)
  (close-channel)
)
