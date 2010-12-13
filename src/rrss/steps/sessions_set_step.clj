(ns rrss.steps.sessions-set-step
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn sessions-set-step
  ([] (sessions-set-step "sessions:all"))
  ([set-key]
   (reify Step
     (on-read [_ opdata next-step] (next-step opdata))
     (on-write [_ opdata next-step]
       (let [res (next-step opdata)]
         (.sadd (:connection res) set-key (:redis-key res))
         res))
     (on-delete [_ opdata next-step]
       (let [res (next-step opdata)]
         (.srem (:connection res) set-key (:redis-key res))
         res)))))

