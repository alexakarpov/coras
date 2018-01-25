(ns coras.core
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require [coras.driver :as driver])
  (:require [coras.generators :as g])
  (:gen-class))

(defn -main
  [& args]
  (println "Initializing a channel..")
  (let [channel (a/chan 10)
        events (g/generate-events "M1" 10)]
    (doseq [e events]
      (a/>!! channel e))
    (driver/run channel)))

(defn map-to-output-line [amap]
  (json/write-str amap ))
