(ns rrss.steps.backend-step
  (:require rrss.steps)
  (:import rrss.steps.Step))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defn- hmset [connection key data]
  (let [string-map (into {} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- read-session [connection okey rkey]
  (if (nil? okey)
    {}
    (let [m (.hgetAll connection rkey)]
      (into {} m))))

(defn- delete-session [connection rkey]
  (when-not (nil? rkey)
    (.del connection (into-array String [rkey])))
  nil)

(defn- write-session [connection okey rkey data]
  (let [okey (or rkey )])
  (when-not (empty? data) ;don't persist empty sessions, read will return {} anyway
    (hmset connection rkey data))
  okey)

(def backend-step
  (reify Step
    (on-read [_ opdata next-step]
      (let [{:keys (connection original-key redis-key) :as m} (next-step opdata)]
        (assoc m :result (read-session connection original-key redis-key))))
    (on-write [_ opdata next-step]
      (let [{:keys (connection original-key redis-key data) :as m} (next-step opdata)]
        (assoc m :result (write-session connection original-key redis-key data))))
    (on-delete [_ opdata next-step]
      (let [{:keys (connection redis-key) :as m} (next-step opdata)]
        (assoc m :result (delete-session connection redis-key))))))

