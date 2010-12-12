(ns rrss.steps.sessions-set-step
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn sessions-set-step
  ([] (sessions-set-step "sessions:all"))
  ([set-key]
   (reify Step
     (on-read [_ {f :base-function :as data}] (f) data)
     (on-write [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.sadd connection set-key (:redis keys))
       data)
     (on-delete [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.srem connection set-key (:redis keys))
       data))))

