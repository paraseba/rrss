(ns rrss
  (:import  org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:import java.util.UUID)
  (:use [ring.middleware.session.store :only (SessionStore)]))

(defn- with-connection [pool f]
  (let [connection (.getResource pool)]
    (try
      (f connection)
      (finally (when connection
                 (.returnResource pool connection))))))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defrecord Key [original redis])
(defrecord Hook [on-write on-read on-delete])
(defrecord OperationData [keys connection base-function])

(defn- make-key [original config]
  (Key. original
        (if (nil? original)
          nil
          ((:map-key config) original))))

(defn- hmset [connection key data]
  (let [string-map (into {"__" ""} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- read-session* [connection {okey :original rkey :redis}]
  (if (nil? okey)
    {}
    (let [m (.hgetAll connection rkey)]
      (dissoc (into {} m) "__"))))

(defn- delete-session* [connection {okey :original rkey :redis}]
  (when-not (nil? okey)
    (.del connection (into-array String [rkey])))
  nil)

(defn- write-session* [connection {okey :original rkey :redis :as kk} data]
  (hmset connection rkey data)
  okey)

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(defn- identity-hook-function [hook-data]
  ((:base-function hook-data)))

(defn- compose-hook-functions [funs]
  (reduce
    (fn [res f] #(f res))
    identity-hook-function
    (keep identity funs)))

(defn- compose-hooks [hooks]
  (Hook.
    (compose-hook-functions (map :on-write hooks))
    (compose-hook-functions (map :on-read hooks))
    (compose-hook-functions (map :on-delete hooks))))

(defn- random-key [] (str (UUID/randomUUID)))

(deftype RedisStore [pool config]
  SessionStore
  (read-session [_ key]
    (with-connection pool
      (fn [connection]
        (let [key (make-key key config)
              fun (partial read-session* connection key)
              op-data (OperationData. key connection fun)]
          ((get-in config [:hook :on-read]) op-data)))))

  (write-session [_ key data]
    (with-connection pool
      (fn [connection]
        (let [key (make-key (or key (random-key)) config)
              fun (partial write-session* connection key data)
              op-data (OperationData. key connection fun)]
          ((get-in config [:hook :on-write]) op-data)))))


  (delete-session [_ key]
    (with-connection pool
      (fn [connection]
        (let [key (make-key key config)
              fun (partial delete-session* connection key)
              op-data (OperationData. key connection fun)]
          ((get-in config [:hook :on-delete]) op-data))))))

(defn redis-store
  ([] (redis-store {}))
  ([{:keys (host port map-key hooks)
     :or {host "localhost" port 6379 map-key (add-prefix "sessions:") :hooks []}}]
   (let [pool (JedisPool.  (org.apache.commons.pool.impl.GenericObjectPool$Config.)
                          host
                          port)]
     (RedisStore. pool {:map-key map-key :hook (compose-hooks hooks)}))))
