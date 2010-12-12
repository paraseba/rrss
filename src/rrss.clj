(ns rrss
  "Ring Redis Session Store
  Create a Ring session store, implemented on top of Redis key/value store"
  (:import org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:require rrss.store)
  (:import rrss.store.RedisStore)
  (:use [rrss.steps :only (compose-steps)]))

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(def ^{:private true} default-options
  {:host "localhost"
   :port 6379
   :map-key (add-prefix "sessions:")
   :steps []})

(defn- pool-config [options]
  (org.apache.commons.pool.impl.GenericObjectPool$Config.))

(defn redis-store
  "Create the session store.
  Valid options are:
    :host redis server URL string
    :port redis server port
    :map-key a function that takes a key string and returns the key that
    will be saved to redis. Defaults to  foo -> sessions:foo
    :steps vector of steps that will be used. See rrss.steps"

  ([] (redis-store {}))
  ([options]
   (let [{:keys (host port)} (merge default-options options)]
     (redis-store (JedisPool. (pool-config options) host port) options)))
  ([pool options]
   (let [{:keys (map-key steps)} (merge default-options options)]
     (RedisStore. pool {:map-key map-key :step (compose-steps steps)}))))
