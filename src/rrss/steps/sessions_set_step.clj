(ns rrss.steps.sessions-set-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)]))

(def ^{:private true} default-options
  {:all-sessions-key "sessions:all"})

(defn sessions-set-step
  "Create a Step that saves all sessions keys in a Redis sorted set. The set score
  is the number of seconds between January 1, 1970, 00:00:00 GMT and the last write
  to the that session. The key name for the sorted set can be changed passing an
  :all-sessions-key option"
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
                      (.zrem (:connection res) set-key (into-array String [key])))
                    res))}))))

