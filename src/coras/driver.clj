(ns coras.driver
  ^{:doc
    "This is where the Event-processing/writing machine lives. Behold, the core.async magic! Even though I prefer Actor model, this is kinda cool."
    }
  (:require [clojure.core.async :as a])
  (:require [clojure.data.json :as json])
  (:require (coras [utils :as utils]))
  (:require (coras [events :as e]))
  (:require (coras [kafka :as k]))
  (:gen-class))

;; "pause" switch for the machine
(def switch (atom true))
;; the only bit of state we're tracking
(def late (atom false))

(defn toggle []
  (swap! switch not))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file (utils/read-config :journal-file)}}]
  (spit out-file (str event "\n") :append true))


(defn run-with-chan [in-channel]
  "the meat and potatoes"
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
           k/send-event
           append-to-journal)
          (recur (a/alts! [in-channel
                           (long-timeout)])))))))
