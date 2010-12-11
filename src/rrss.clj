(ns rrss
  (:import  org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:import java.util.UUID)
  (:use [ring.middleware.session.store :only (SessionStore)]))

(defn- with-connection [pool f & args]
  (let [connection (.getResource pool)]
    (try
      (apply f connection args)
      (finally (when connection
                 (.returnResource pool connection))))))

(defn- as-str [o]
  (if (keyword? o)
    (name o)
    (str o)))

(defrecord Key [original redis])
(defrecord Hook [on-write on-read on-delete])
(defrecord HookData [keys fun connection])

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
  ((:fun hook-data)))

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

(deftype RedisStore [pool config]
  SessionStore
  (read-session [_ key]
    (with-connection pool read-session* (make-key key config)))

  (write-session [_ key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (with-connection pool write-session* (make-key key config) data)))

  (delete-session [_ key]
    (with-connection pool delete-session* (make-key key config))))

(defn redis-store
  ([] (redis-store {}))
  ([{:keys (host port map-key hooks)
     :or {host "localhost" port 6379 map-key (add-prefix "sessions:") :hooks []}}]
   (let [pool (JedisPool.  (org.apache.commons.pool.impl.GenericObjectPool$Config.)
                          host
                          port)]
     (RedisStore. pool {:map-key map-key :hook (compose-hooks hooks)}))))
