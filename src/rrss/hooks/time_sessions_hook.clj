(ns rrss.hooks.time-sessions-hook
  (:require rrss.hooks)
  (:import java.util.Date)
  (:import rrss.hooks.Hook))

(defn time-sessions-hook
  ([] (time-sessions-hook "%s:written-at"))
  ([time-key-format]
   (reify Hook
     (on-read [_ {f :base-function :as data}] (f) data)
     (on-write [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.set connection (format time-key-format (:redis keys)) (str (.getTime (Date.))))
       data)
     (on-delete [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.del connection (into-array String (format time-key-format (:redis keys))))
       data))))
