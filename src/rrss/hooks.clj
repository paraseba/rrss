(ns rrss.hooks)

(defrecord Key [original redis])
(defrecord Hook [on-read on-write on-delete])
(defrecord OperationData [keys connection base-function result])

(defn- identity-hook-function [hook-data]
  (assoc hook-data :result ((:base-function hook-data))))

(defn- compose-hook-functions [funs]
  (reduce
    (fn [res f] (comp f res))
    identity-hook-function
    (keep identity funs)))

(defn compose-hooks [hooks]
  (Hook.
    (compose-hook-functions (map :on-read hooks))
    (compose-hook-functions (map :on-write hooks))
    (compose-hook-functions (map :on-delete hooks))))

