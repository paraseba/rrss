(ns rrss
  (:import org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:require rrss.store)
  (:import rrss.store.RedisStore)
  (:use [rrss.hooks :only (compose-hooks)]))

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(defn redis-store
  ([] (redis-store {}))
  ([{:keys (host port map-key hooks)
     :or {host "localhost" port 6379 map-key (add-prefix "sessions:") :hooks []}}]
   (let [pool (JedisPool.  (org.apache.commons.pool.impl.GenericObjectPool$Config.)
                          host
                          port)]
     (RedisStore. pool {:map-key map-key :hook (compose-hooks hooks)}))))
