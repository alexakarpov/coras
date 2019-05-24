(ns ^{:doc
      "Core namespace, stuff that is picked up by 'lein ring server"}
    eplk.core
  (:require [eplk.driver :as driver]
            [eplk.events :as events]
            [eplk.utils :as utils])
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [clojure.core.async :as a :refer [go chan >!! <!!  timeout]])
  (:gen-class))

(s/defschema MachineCycled
  {:machine_id s/Str
   :timestamp Long
   })

(s/defschema MachineCycledResponse
  {:machine_id s/Str
   :timestamp s/Str
   :type s/Str
   })

;; this is where the events channel is maintained during the interactive session
(defonce in-ch (delay (chan 10)))

(defn machine-start [& {:keys [channel]
                        :or {channel @in-ch}}]
  "Start the machine consuming the events in the channel (on a thread-pool)"
  (driver/run-with-chan channel))

(machine-start :channel @in-ch)

(defn submit-event [machine-id]
  "Performs a blocking put of the event onto the interactive events channel"
  (println "entering eplk.core/submit-event")
  (utils/submit-event @in-ch machine-id))

(def app
  (api ;; macro that builds the whole REST API, which is what you see on port 300, fully equipped with a web-client to make the api requests.
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :data {:info {:title "Simple"
                   :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}]}}}

   (context "/api" []
            :tags ["api"]
            (POST "/event" []
                  :return MachineCycledResponse
                  :body [event MachineCycled]
                  :summary "echoes a MachineCycled event"
                  (let [mid (:machine_id event)]
                    (println "Submitting event for" mid "through the REST API endpoint")
                    (ok (submit-event mid)))))))
