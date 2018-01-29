(ns coras.core
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require [coras.driver :as driver])
  (:gen-class))

(def stdin-reader
  (java.io.BufferedReader. *in*))

(defn -main
  [& args]
  (println "Creating an input channel from STDIN")
  (let [in-chan (a/chan 10)]
    (driver/run in-chan)
    (doseq [line (line-seq stdin-reader)]
      (println "read: " line)
      (a/>!! in-chan line))))
