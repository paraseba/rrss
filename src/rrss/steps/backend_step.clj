(ns rrss.steps.backend-step
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn- read-session [connection id rkey]
  "Read session identified by id, saved under key rkey. Return empty map if id is nil"
  (if (nil? id)
    {}
    (let [m (.hgetAll connection rkey)]
      (into {} m))))

(defn- delete-session [connection rkey]
  "Delete the session identified by rkey"
  (when-not (nil? rkey)
    (.del connection (into-array String [rkey])))
  nil)

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defn- hmset [connection key data]
  (let [string-map (into {} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- write-session
  "Write session data using the given session id and redis key. Convert keyword session
  members to strings"
  [connection id rkey data]
  (when-not (empty? data) ;don't persist empty sessions, read will return {} anyway
    (hmset connection rkey data))
  id)

(def ^{:doc "Create a Step that handles Redis storage. It will save each session in a
            Redis hash"}
  backend-step
  (reify Step
    (on-read [_ opdata next-step]
      (let [{:keys (connection session-id redis-key) :as m} (next-step opdata)]
        (assoc m :result (read-session connection session-id redis-key))))
    (on-write [_ opdata next-step]
      (let [{:keys (connection session-id redis-key data) :as m} (next-step opdata)]
        (assoc m :result (write-session connection session-id redis-key data))))
    (on-delete [_ opdata next-step]
      (let [{:keys (connection redis-key) :as m} (next-step opdata)]
        (assoc m :result (delete-session connection redis-key))))))

