(ns rrss.steps.mapkey-step
  (:import (java.util UUID))
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn- make-key [original key-mapper]
  (if (nil? original)
    nil
    (key-mapper original)))

(defn- random-key [] (str (UUID/randomUUID)))

(defn- assoc-mapped-key [key-mapper data]
  (assoc data :redis-key (make-key (:original-key data) key-mapper)))

(defn create-mapkey-step [key-mapper]
  (reify Step
    (on-read [_ data next-step] (next-step (assoc-mapped-key key-mapper data)))
    (on-write [_ data next-step]
      (let [k (or (:original-key data) (random-key))]
        (next-step (assoc-mapped-key key-mapper (assoc data :original-key k)))))
    (on-delete [_ data next-step] (next-step (assoc-mapped-key key-mapper data)))))
