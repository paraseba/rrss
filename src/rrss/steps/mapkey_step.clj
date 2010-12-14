(ns rrss.steps.mapkey-step
  (:import (java.util UUID))
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn- make-key [original key-mapper]
  (if (nil? original)
    nil
    (key-mapper original)))

(defn- random-id
  "Generate a random id UUID string"
  [] (str (UUID/randomUUID)))

(defn- assoc-mapped-key [key-mapper data]
  (assoc data :redis-key (make-key (:session-id data) key-mapper)))

(defn create-mapkey-step
  "Native Step handling the conversion of the session id to a key. key-mapper is a
  function that takes a string, the session id, and returns other string, the Redis
  key for that session.
  For write operations with nil session id, it will create a random id"
  [key-mapper]
  (reify Step
    (on-read [_ data next-step] (next-step (assoc-mapped-key key-mapper data)))
    (on-write [_ data next-step]
      (let [k (or (:session-id data) (random-id))]
        (next-step (assoc-mapped-key key-mapper (assoc data :session-id k)))))
    (on-delete [_ data next-step] (next-step (assoc-mapped-key key-mapper data)))))
