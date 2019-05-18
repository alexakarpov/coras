(ns eplk.driver
  ^{:doc
    "This is where the Event-processing/writing machine lives. Behold, the core.async magic! Even though I prefer Actor model, this is kinda cool."
    }
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require (eplk [utils :as utils]))
  (:require (eplk [events :as e]))
  (:require (eplk [kafka :as k]))
  (:gen-class))

;; "pause" switch for the machine
(def switch (atom true))

;; kill switch to force machine to terminate
(def kill (atom false))
(defn dokill [] (reset! kill true))


;; this needs to be a growing collection of atoms - a map from machine_id to its state (late status)

(def late (atom false))

(defn toggle []
  (swap! switch not))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file (utils/read-config :journal-file)}}]
  (k/send-message 42 event)
  (spit out-file (str event "\n") :append true))

(defn remember-machine [event]
  (print "event: " event)
  event)

(defn late? [mid]
  ())

(defn run-with-chan [in-channel]
  "the meat and potatoes"
  (let [long-timeout #(a/timeout 45000)
        short-timeout #(a/timeout 15000)]
    (a/go-loop [[msg ch] (a/alts! [in-channel
                                   (long-timeout)])]
      (if (not @kill)
        (do
          (while (not @switch) ; is the switch ON?
            (Thread/sleep 1000))
          (if (not (= ch in-channel)) ; msg in alts! is from a timeout channel
            (do
              (if @late
                ;; we're already in a 'late' state, fire an alarm
                (do
                  (append-to-journal (json/write-str (e/alert-event (:machine_id msg))))
                  (recur (a/alts! [in-channel])))
                (do
                  (reset! late true)
                  (append-to-journal (json/write-str (e/timeout-event (:machine_id msg))))
                  (recur (a/alts! [in-channel
                                   (short-timeout)])))))
            (do
              ;(reset! late false)
              (->
               msg
               e/process-event
               remember-machine
               json/write-str
               append-to-journal)
              (recur (a/alts! [in-channel
                               (long-timeout)])))))))))

(comment
  (a/go-loop [seconds (atom 0)
              add-seconds! #(swap! seconds + %)]
    (println "Waiting 1 second")
    (a/<! (a/timeout 1000))
    (add-seconds! 1)
    (println "Waiting 2 seconds")
    (a/<! (a/timeout 2000))
    (add-seconds! 2)
    (println
     (format "Waited %s seconds"
             @seconds)))
  )
