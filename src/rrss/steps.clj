(ns rrss.steps)

(defprotocol Step
  (on-read [step operation-data])
  (on-write [step operation-data])
  (on-delete [step operation-data]))

(defrecord Key [original redis])
(defrecord OperationData [keys connection base-function result])

(defn- identity-step-function [step-data]
  (assoc step-data :result ((:base-function step-data))))

(defn compose-steps [steps]
  (let [;steps (reverse steps) ;fixme decide correct order
        read   (apply comp identity-step-function (map #(partial on-read %) steps))
        write  (apply comp identity-step-function (map #(partial on-write %) steps))
        delete (apply comp identity-step-function (map #(partial on-delete %) steps))]

  (reify Step
    (on-read   [_ data] (read data))
    (on-write  [_ data] (write data))
    (on-delete [_ data] (delete data)))))

