(ns coras.driver
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require (coras [utils :as utils]))
  (:require (coras [events :as e]))
  (:gen-class))


(def switch (atom true))
(def late (atom false))

(defn toggle []
  (swap! switch not))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file "/tmp/journal.out"}}]
  (spit out-file (str event "\n") :append true))

(defn run-with-chan [in-channel]
  (let [long-timeout #(a/timeout 45000)
        short-timeout #(a/timeout 15000)]
    (a/go-loop [[msg ch] (a/alts! [in-channel
                                   (long-timeout)])]
      (while (not @switch) ; is the switch ON?
        (Thread/sleep 1000))
      (if (nil? msg) ; timeout occurred
        (do
          (if @late
            (do
              (append-to-journal (e/alert-event (:machine_id msg)))
              (recur (a/alts! [in-channel])))
            (do
              (reset! late true)
              (append-to-journal (e/timeout-event (:machine_id msg)))
              (recur (a/alts! [in-channel
                               (short-timeout)])))))
        (do
          (reset! late false)
          (->
           msg
           e/process-event
           json/write-str
           append-to-journal)
          (recur (a/alts! [in-channel
                           (long-timeout)])))))))
