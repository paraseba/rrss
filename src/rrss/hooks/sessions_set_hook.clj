(ns rrss.hooks.sessions-set-hook
  (:require rrss.hooks)
  (:import rrss.hooks.Hook))

(defn sessions-set-hook
  ([] (sessions-set-hook "sessions:all"))
  ([set-key]
   (reify Hook
     (on-read [_ {f :base-function :as data}] (f) data)
     (on-write [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.sadd connection set-key (:redis keys))
       data)
     (on-delete [_ {:keys (keys connection base-function) :as data}]
       (base-function)
       (.srem connection set-key (:redis keys))
       data))))

