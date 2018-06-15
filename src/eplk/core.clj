(ns ^{:doc
      "Core namespace, you'll land here with `lein repl`. Contains functions for working with the events processing machine:
--- (submit-event <MachineID>)
--- (machine-start)
--- (machine-toggle-on-off)"}
    eplk.core
  (:require (eplk [driver :as driver]
                  [events :as events]
                  [utils :as utils]))
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [clojure.core.async :as a :refer [go chan >!! <!!  timeout]])
  (:gen-class))

;; this is where the events channel is maitained during the interactive session
(defonce in-ch (delay (chan 10)))

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

(s/defschema MachineCycled
  {:machine_id s/Str
   :timestamp Long
   })

(def app
  (api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :data {:info {:title "Simple"
                   :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}]}}}

   (context "/api" []
            :tags ["api"]
            (POST "/event" []
                  :return MachineCycled
                  :body [event MachineCycled]
                  :summary "echoes a MachineCycled event"
                  (let [mid (:machine_id event)]
                    (println "Submitting event for" mid "through the REST API endpoint")
                    (submit-event mid)
                    (ok event))))))

(comment
  (submit-event "M1")
  (submit-event "M2")
  (submit-event "M3")
  (utils/report-on-chan @in-ch)
  @driver/switch
  @driver/late
  (driver/toggle)
  (machine-start :channel @in-ch)
  (close-channel)
)
