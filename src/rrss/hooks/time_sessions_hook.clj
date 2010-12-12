(ns rrss.hooks.time-sessions-hook
  (:require rrss.hooks)
  (:import java.util.Date)
  (:import rrss.hooks.Hook))

(defn time-sessions-hook
  ([] (time-sessions-hook "%s:written-at"))
  ([time-key-format]
   (Hook. nil
          (fn [{:keys (keys connection base-function) :as data}]
            (base-function)
            (.set connection (format time-key-format (:redis keys)) (str (.getTime (Date.))))
            data)
          (fn [{:keys (keys connection base-function) :as data}]
            (base-function)
            (.del connection (into-array String (format time-key-format (:redis keys))))
            data))))


