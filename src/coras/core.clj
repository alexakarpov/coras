(ns coras.core
  (:require [clojure.core.async :as a :refer [go chan >!! <!!  timeout]])
  (:require (coras [driver :as driver]
                   [events :as events]
                   [utils :as utils]))
  (:gen-class))

;; this is where the events channel is maitained during the interactive session
(def in-ch (delay (chan 10)))

;; user interface when running interactively in REPL
(defn start []
  (driver/run-with-chan @in-ch))

(defn stop []
  (a/close! @in-ch))

(defn submit-event [machine-id]
  "Performs a blocking put of the event onto the interactive events channel"
  (let [buffer (.buf @in-ch)]
    (if (>= (.count buffer) 10)
      [:error :channel_full] ; so we don't *actually* block the REPL's main thread
      (>!! @in-ch (events/make-event machine-id)))))

;; running witth events from STDIN
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
  (start)
  (stop)
)
