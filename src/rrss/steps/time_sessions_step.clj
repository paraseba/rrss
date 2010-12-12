(ns rrss.steps.time-sessions-step
  (:require rrss.steps)
  (:import java.util.Date)
  (:import rrss.steps.Step))

(defn time-sessions-step
  ([] (time-sessions-step "%s:written-at"))
  ([time-key-format]
   (reify Step
     (on-read [_ {f :base-function :as data}] (f) data)
     (on-write [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.set connection (format time-key-format (:redis keys)) (str (.getTime (Date.))))
       data)
     (on-delete [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.del connection (into-array String (format time-key-format (:redis keys))))
       data))))
