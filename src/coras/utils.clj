(ns coras.utils
  (:gen-class))

(defn generate-events [machine-id & {:keys [from]
                                     :or {from (t/now)}}]
  "Produces an infinite lazy sequence of events, starting at 'from', each 40 seconds from the previous one"
  (let [forty-seconds-later
        (fn [cnt]
          (t/plus from (t/seconds (* cnt 40))))
        ]
    (for [i (iterate #(inc %) 0)]
      (e/make-event machine-id :timestamp (forty-seconds-later i)))))
