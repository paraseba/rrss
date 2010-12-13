(ns rrss.steps.time-sessions-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)])
  (:import rrss.steps.Step))

(defn time-sessions-step
  ([] (time-sessions-step "%s:written-at"))
  ([time-key-format]
   (create-step
     {:write (fn [opdata next-step]
               (let [res (next-step opdata)]
                 (.set (:connection res)
                       (format time-key-format (:redis-key res))
                       (str (.getTime (Date.))))
                 res))
      :delete (fn [opdata next-step]
                (let [res (next-step opdata)]
                  (.del (:connection res)
                        (into-array String (format time-key-format (:redis-key res))))
                  res))})))
