(ns coras.driver
  (:require [clojure.core.async :as a])
  (:require (coras [utils :as utils]))
  (:require (coras [events :as e]))
  (:gen-class))

(defn append-to-journal [event & {:keys [out-file]
                                  :or {out-file "/tmp/journal.out"}}]
  (spit out-file (str event "\n") :append true))

(defn run [in-channel]
  (let []
    (a/go-loop [msg (a/<! in-channel)]
      (append-to-journal msg)
      (recur (a/<! in-channel)))))
