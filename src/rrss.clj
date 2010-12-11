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

(defn- hmset [connection key data]
  (let [string-map (into {"__" ""} (map #(vec (map as-str %)) data))]
    (.hmset connection key string-map)))

(defn- read-session* [key]
  (fn [connection]
    (if (nil? key)
      {}
      (let [m (.hgetAll connection key)]
        (dissoc (into {} m) "__")))))

(defn- delete-session* [key]
  (fn [connection]
    (when-not (nil? key)
      (.del connection (into-array String [key])))
    nil))

(defn- write-session* [key data]
  (let [key (or key (str (UUID/randomUUID)))]
    (fn [connection]
      (hmset connection key data)
      key)))

(deftype RedisStore [pool]
  SessionStore
  (read-session [_ key]
    (with-connection pool (read-session* key)))

  (write-session [_ key data]
    (with-connection pool (write-session* key data)))

  (delete-session [_ key]
    (with-connection pool (delete-session* key))))

(defn redis-store
  ([] (redis-store {}))
  ([{:keys (host port) :or {host "localhost" port 6379}}]
   (let [pool (JedisPool.  (org.apache.commons.pool.impl.GenericObjectPool$Config.)
                          host
                          port)]
     (RedisStore. pool))))
