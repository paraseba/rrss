(ns rrss.store
  (:import (java.util UUID Date))
  (:use [rrss.hooks :only (on-read on-write on-delete)])
  (:import (rrss.hooks Key OperationData))
  (:use [ring.middleware.session.store :only (SessionStore)]))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defn- hmset [connection key data]
  (let [string-map (into {} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- read-session* [connection {okey :original rkey :redis}]
  (if (nil? okey)
    {}
    (let [m (.hgetAll connection rkey)]
      (into {} m))))

(defn- delete-session* [connection {okey :original rkey :redis}]
  (when-not (nil? okey)
    (.del connection (into-array String [rkey])))
  nil)

(defn- write-session* [connection {okey :original rkey :redis :as kk} data]
  (when-not (empty? data) ;don't persist empty sessions, read will return {} anyway
    (hmset connection rkey data))
  okey)

(defn- random-key [] (str (UUID/randomUUID)))

(defn- with-connection [pool f]
  (let [connection (.getResource pool)]
    (try
      (f connection)
      (finally (when connection
                 (.returnResource pool connection))))))

(defn- make-key [original config]
  (Key. original
        (if (nil? original)
          nil
          ((:map-key config) original))))

(deftype RedisStore [pool config]
  SessionStore
  (read-session [_ key]
    (with-connection pool
      (fn [connection]
        (let [key (make-key key config)
              fun (partial read-session* connection key)
              op-data (OperationData. key connection fun nil)]
          (:result (on-read (:hook config) op-data))))))

  (write-session [_ key data]
    (with-connection pool
      (fn [connection]
        (let [key (make-key (or key (random-key)) config)
              fun (partial write-session* connection key data)
              op-data (OperationData. key connection fun nil)]
          (:result (on-write (:hook config) op-data))))))

  (delete-session [_ key]
    (with-connection pool
      (fn [connection]
        (let [key (make-key key config)
              fun (partial delete-session* connection key)
              op-data (OperationData. key connection fun nil)]
          (:result (on-delete (:hook config) op-data)))))))
