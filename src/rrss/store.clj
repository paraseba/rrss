(ns rrss.store
  (:use [ring.middleware.session.store :only (SessionStore)]))


(defn- with-connection [pool f]
  (let [connection (.getResource pool)]
    (try
      (f connection)
      (finally (when connection
                 (.returnResource pool connection))))))

(defn- make-operation-map
  ([connection key] {:original-key key :connection connection})
  ([connection key data] {:original-key key :data data :connection connection}))

(deftype RedisStore [pool step-chain-map]
  SessionStore
  (read-session [_ key]
    (with-connection pool
      (fn [connection]
        (:result ((:read step-chain-map) (make-operation-map connection key))))))

  (write-session [_ key data]
    (with-connection pool
      (fn [connection]
        (:result ((:write step-chain-map) (make-operation-map connection key data))))))

  (delete-session [_ key]
    (with-connection pool
      (fn [connection]
        (:result ((:delete step-chain-map) (make-operation-map connection key)))))))
