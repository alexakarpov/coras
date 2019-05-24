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

(defn submit-event [channel machine-id]
  (let [buffer (.buf channel)
        event (e/make-event machine-id)]
    (if (>= (.count buffer) 10)
      [:error :channel_full] ; so we don't *actually* block the REPL's main thread
      (do
        ; buffer has space, safe to call blocking put
        (a/>!! channel event)
        event))))
