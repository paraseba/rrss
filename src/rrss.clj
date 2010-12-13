(ns rrss
  "Ring Redis Session Store
  Create a Ring session store, implemented on top of Redis key/value store"
  (:import org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:require rrss.store)
  (:import rrss.store.RedisStore)
  (:use [rrss.steps :only (create-step-chain)]
        (rrss.steps [backend-step :only (backend-step)]
                    [mapkey-step :only (create-mapkey-step)]
                    [sessions-set-step :only (sessions-set-step)]
                    [expire-sessions-step :only (expire-sessions-step)])))

(defn- add-prefix [prefix]
  (fn [key] (str prefix key)))

(def ^{:private true} default-options
  {:host "localhost"
   :port 6379
   :key-mapper (add-prefix "sessions:")
   :steps []})

(defn- pool-config [options]
  (org.apache.commons.pool.impl.GenericObjectPool$Config.))

(defn- all-steps [key-mapper steps]
  (concat [(create-mapkey-step key-mapper) backend-step] steps))

(defn redis-store
  "Create the session store.
  Valid options are:
    :host redis server URL string
    :port redis server port
    :key-mapper a function that takes a key string and returns the key that
    will be saved to redis. Defaults to  foo -> sessions:foo
    :steps vector of steps that will be used. See rrss.steps"

  ([] (redis-store {}))
  ([options]
   (let [{:keys (host port)} (merge default-options options)]
     (redis-store (JedisPool. (pool-config options) host port) options)))
  ([pool options]
   (let [{:keys (key-mapper steps)} (merge default-options options)]
     (RedisStore. pool (create-step-chain (all-steps key-mapper steps))))))

(defn expiring-redis-store
  ([] (expiring-redis-store {}))
  ([options]
   (let [{:keys (host port)} (merge default-options options)]
     (expiring-redis-store (JedisPool. (pool-config options) host port) options)))
  ([pool options]
   (let [{:keys (key-mapper steps)} (merge default-options options)
         steps (concat [(sessions-set-step options) (expire-sessions-step options)] steps)]
     (RedisStore. pool (create-step-chain (all-steps key-mapper steps))))))

