(ns rrss.steps.sessions-set-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)])
  (:import rrss.steps.Step))

(defn sessions-set-step
  ([] (sessions-set-step "sessions:all"))
  ([set-key]
   (create-step
     {:write (fn [opdata next-step]
               (let [res (next-step opdata)
                     score (-> (Date.) .getTime double)]
                 (.zadd (:connection res) set-key score (:redis-key res))
                 res))
      :delete (fn [opdata next-step]
                (let [res (next-step opdata)]
                  (.zrem (:connection res) set-key (:redis-key res))
                  res))})))

