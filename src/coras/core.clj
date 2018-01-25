(ns coras.core
  (:require [clojure.core.async :as as])
  (:gen-class))

(defn run-timer [n]
  (as/go-loop [ticks 1]
    (do
      (println "tick..")
      (Thread/sleep 1000)
      (if (= ticks n) (println "bye!") (recur (+ 1 ticks))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello core.async")
)
