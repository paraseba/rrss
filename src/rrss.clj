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

(defn- hmset [connection key data]
  (let [string-map (into {"__" ""} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- read-session* [connection config key]
  (if (nil? key)
    {}
    (let [m (.hgetAll connection ((config :map-key) key))]
      (dissoc (into {} m) "__"))))

(defn- delete-session* [connection config key]
  (when-not (nil? key)
    (.del connection (into-array String [((config :map-key) key)])))
  nil)

(defn- write-session* [connection config key data]
  (let [key (or key (str (UUID/randomUUID)))]
    (hmset connection ((config :map-key) key) data)
    key))

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(deftype RedisStore [pool config]
  SessionStore
  (read-session [_ key]
    (with-connection pool read-session* config key))

  (write-session [_ key data]
    (with-connection pool write-session* config key data))

  (delete-session [_ key]
    (with-connection pool delete-session* config key)))

(defn redis-store
  ([] (redis-store {}))
  ([{:keys (host port map-key hooks)
     :or {host "localhost" port 6379 map-key (add-prefix "sessions:") :hooks {}}}]
   (let [pool (JedisPool.  (org.apache.commons.pool.impl.GenericObjectPool$Config.)
                          host
                          port)]
     (RedisStore. pool {:map-key map-key :hooks hooks}))))
