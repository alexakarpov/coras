(ns coras.core
  (:require [clojure.core.async :refer [chan >!!]])
  (:require [clojure.data.json :as json])
  (:require [coras.driver :as driver])
  (:gen-class))

(def in-ch (atom nil))

(defn open-interactive-channel []
  (println "Initializing a channel (size 10) for an interactive session")
  (reset! in-ch (chan 10)))

(defn add-to-in-ch [event]
  (let [size (.count (.buf @in-ch))]
    (if (>= size 10 )
      [:error :channel_size]
      (>!! @in-ch event))))

(def stdin-reader
  (java.io.BufferedReader. *in*))

(defn -main
  [& args]
  (println "Creating an input channel from STDIN")
  (let [in-chan (chan 10)]
    (driver/run in-chan)
    (doseq [line (line-seq stdin-reader)]
      (println "read: " line)
      (>!! in-chan line))))
