(ns rrss
  "Ring Redis Session Store
  Ring session store implemented on top of Redis key/value store"
  (:import org.apache.commons.pool.impl.GenericObjectPool$Config)
  (:import redis.clients.jedis.JedisPool)
  (:use [rrss.store :only (make-redis-store)])
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

(defn- pool-config
  "Create a default pool"
  [options]
  (org.apache.commons.pool.impl.GenericObjectPool$Config.))

(defn- all-steps
  "Join extra steps with the native steps needed for all stores"
  [key-mapper steps]
  (concat [(create-mapkey-step key-mapper) backend-step] steps))

(defn redis-store
  "Create the default session store.
  Valid options are:
    :host redis server URL string
    :port redis server port
    :key-mapper a function that takes a key string and returns the key that
    will be saved to redis. Defaults to a transformation foo -> sessions:foo
    :steps vector of extra steps that will be used. See rrss.steps"

  ([] (redis-store {}))
  ([options]
   (let [{:keys (host port)} (merge default-options options)]
     (redis-store (JedisPool. (pool-config options) host port) options)))
  ([pool options]
   (let [{:keys (key-mapper steps)} (merge default-options options)]
     (make-redis-store pool (create-step-chain (all-steps key-mapper steps))))))

(defn expiring-redis-store
  "Create a session store with session expiration.
  Sessions are checked in a background thread an deleted when the last write
  is older than a certain value.
  Valid options are:
    :host redis server URL string
    :port redis server port
    :duration number of seconds after which a session can be deleted. Defaults
              to 1 week
    :resolution interval in seconds between successive session checks. Defaults
                to 10 minutes
    :key-mapper a function that takes a key string and returns the key that
    will be saved to redis. Defaults to a transformation foo -> sessions:foo
    :steps vector of extra steps that will be used. See rrss.steps"

  ([] (expiring-redis-store {}))
  ([options]
   (let [{:keys (host port)} (merge default-options options)
         pool (JedisPool. (pool-config options) host port)]
     (expiring-redis-store pool options)))
  ([pool options]
   (let [{:keys (key-mapper steps)} (merge default-options options)
         steps (concat [(sessions-set-step options) (expire-sessions-step options)] steps)]
     (make-redis-store pool (create-step-chain (all-steps key-mapper steps))))))

