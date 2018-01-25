(ns coras.driver
  (:require [clojure.core.async :as a])
;  (:require [clojure.data.json :as json])
;  (:require (coras [events :as e]))
  (:gen-class))

(defn report-on-chan [msg chan]
  (println (format "%s: channel size: %d" msg (.count (.buf chan)))))

(defn log-it [event]
  (spit "/tmp/journal.log" (str event "\n") :append true))

(defn run [events-channel]
  (loop [event (a/<!! events-channel)]
    (report-on-chan :driver events-channel)
    (spit "/tmp/journal.log" (str event "\n") :append true)
    (recur (a/<!! events-channel))))
