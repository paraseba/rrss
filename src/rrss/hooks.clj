(ns rrss.hooks)

(defprotocol Hook
  (on-read [hook operation-data])
  (on-write [hook operation-data])
  (on-delete [hook operation-data]))

(defrecord Key [original redis])
(defrecord OperationData [keys connection base-function result])

(defn- identity-hook-function [hook-data]
  (assoc hook-data :result ((:base-function hook-data))))

(defn compose-hooks [hooks]
  (let [;hooks (reverse hooks) ;fixme decide correct order
        read   (apply comp identity-hook-function (map #(partial on-read %) hooks))
        write  (apply comp identity-hook-function (map #(partial on-write %) hooks))
        delete (apply comp identity-hook-function (map #(partial on-delete %) hooks))]

  (reify Hook
    (on-read   [_ data] (read data))
    (on-write  [_ data] (write data))
    (on-delete [_ data] (delete data)))))

