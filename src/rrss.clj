(ns rrss
  "Ring Redis Session Store
  Create a Ring session store, implemented on top of Redis key/value store"
  (:import org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:require rrss.store)
  (:import rrss.store.RedisStore)
  (:use [rrss.hooks :only (compose-hooks)]))

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(def ^{:private true} default-options
  {:host "localhost"
   :port 6379
   :map-key (add-prefix "sessions:")
   :hooks []}) 

(defn- pool-config [options]
  (org.apache.commons.pool.impl.GenericObjectPool$Config.))

(defn redis-store
  "Create the session store.
  Valid options are:
    :host redis server URL string
    :port redis server port
    :map-key a function that takes a key string and returns the key that
    will be saved to redis. Defaults to  foo -> sessions:foo
    :hooks vector of hooks that will be used. See rrss.hooks"

  ([] (redis-store {}))
  ([options]
   (let [{:keys (host port map-key hooks)} (merge default-options options)
         pool (JedisPool. (pool-config options) host port)]
     (RedisStore. pool {:map-key map-key :hook (compose-hooks hooks)}))))
