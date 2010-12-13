(ns rrss.steps.sessions-set-step
  (:use [rrss.steps :only (create-step)])
  (:import rrss.steps.Step))

(defn sessions-set-step
  ([] (sessions-set-step "sessions:all"))
  ([set-key]
   (create-step
     {:write (fn [opdata next-step]
               (let [res (next-step opdata)]
                 (.sadd (:connection res) set-key (:redis-key res))
                 res))
      :delete (fn [opdata next-step]
                (let [res (next-step opdata)]
                  (.srem (:connection res) set-key (:redis-key res))
                  res))})))

