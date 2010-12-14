(ns rrss.store
  "Define a type for the Ring store"
  (:use [ring.middleware.session.store :only (SessionStore)]))


(defn- with-connection
  "Get a connection from the given pool and run the function f passing the connection.
  Finally, return the connection resource to the pool"
  [pool f]
  (let [connection (.getResource pool)]
    (try
      (f connection)
      (finally (when connection
                 (.returnResource pool connection))))))

(defn- make-operation-map
  ([connection id] {:session-id id :connection connection})
  ([connection id data] {:session-id id :data data :connection connection}))

(deftype RedisStore [pool step-chain-map]
  SessionStore
  (read-session [_ id]
    (with-connection pool
      (fn [connection]
        (:result ((:read step-chain-map) (make-operation-map connection id))))))

  (write-session [_ id data]
    (with-connection pool
      (fn [connection]
        (:result ((:write step-chain-map) (make-operation-map connection id data))))))

  (delete-session [_ id]
    (with-connection pool
      (fn [connection]
        (:result ((:delete step-chain-map) (make-operation-map connection id)))))))
