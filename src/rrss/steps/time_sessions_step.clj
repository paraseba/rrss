(ns rrss.steps.time-sessions-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)]))

(defn time-sessions-step
  "Create a step that adds an extra key-value for each session containing the last
  write time. Time is written as the number of seconds since January 1, 1970, 00:00:00 GMT.
  You can pass a string to change the default time key name, the string is used as input
  to a format call"
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
