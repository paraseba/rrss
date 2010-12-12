(ns rrss.hooks.sessions-set-hook
  (:require rrss.hooks)
  (:import rrss.hooks.Hook))

(defn sessions-set-hook
  ([] (sessions-set-hook "sessions:all"))
  ([set-key]
   (Hook. nil
          (fn [{:keys (keys connection base-function) :as data}]
            (base-function)
            (.sadd connection set-key (:redis keys))
            data)
          (fn [{:keys (keys connection base-function) :as data}]
            (base-function)
            (.srem connection set-key (:redis keys))
            data))))

