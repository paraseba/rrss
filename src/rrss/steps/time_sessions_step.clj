(ns rrss.steps.time-sessions-step
  (:import java.util.Date)
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn time-sessions-step
  ([] (time-sessions-step "%s:written-at"))
  ([time-key-format]
   (reify Step
     (on-read [_ opdata next-step] (next-step opdata))
     (on-write [_ opdata next-step]
       (let [res (next-step opdata)]
         (.set (:connection res)
               (format time-key-format (:redis-key res))
               (str (.getTime (Date.))))
         res))
     (on-delete [_ opdata next-step]
       (let [res (next-step opdata)]
         (.del (:connection res)
               (into-array String (format time-key-format (:redis-key res))))
         res)))))
