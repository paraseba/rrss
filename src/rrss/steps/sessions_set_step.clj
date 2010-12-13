(ns rrss.steps.sessions-set-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)]))

(def ^{:private true} default-options
  {:all-sessions-key "sessions:all"})

(defn sessions-set-step
  ([] (sessions-set-step {}))
  ([options]
   (let [{set-key :all-sessions-key} (merge default-options options)]
     (create-step
       {:write (fn [opdata next-step]
                 (let [res (next-step opdata)
                       score (-> (Date.) .getTime double)]
                   (.zadd (:connection res) set-key score (:redis-key res))
                   res))
        :delete (fn [opdata next-step]
                  (let [res (next-step opdata)]
                    (when-let [key (:redis-key res)]
                      (.zrem (:connection res) set-key key))
                    res))}))))

