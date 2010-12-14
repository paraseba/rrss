(ns rrss.steps)

(defprotocol Step
  "Perform store operations.
  Information flows through the Steps chain in a map (operation-data) passed as
  argument to operations. Each Step must call the following Step (next-step)
  passing a possibly modified operation-data
  Default keys in operation-data are:
    * session-id: session id
    * redis-key: session id as stored in redis
    * data (for write operations)
    * connection: instance of Jedis to contact Redis db"
  (on-read [step operation-data next-step] "Perform read operation")
  (on-write [step operation-data next-step] "Perform write operation")
  (on-delete [step operation-data next-step] "Perform delete operation"))

(defn- chain-funs [funs]
  (reduce
    (fn [chain f]
      (fn [opdata]
        (f opdata chain)))
    identity
    funs))

(defn- step-binder [f]
  #(partial f %))

(defn create-step-chain
  "Chain a sequence of steps so they can be called with a single function call.
  Last step in the sequence will be called first, it will perform its operation and
  will pass control down the chain until the first Step in the given seq is reached"
  [steps]
  (let [read   (chain-funs (map (step-binder on-read) steps))
        write  (chain-funs (map (step-binder on-write) steps))
        delete (chain-funs (map (step-binder on-delete) steps))]
    {:read read :write write :delete delete}))

(defn create-step
  "Creates a Step using the given functions as implementations. Valid keys in the
  argument map are read, write and delete. All functions must take two arguments:
  the operation data map, and the next-step in step chain. next-step is a function
  that takes only one argument: an operation data map"
  [{:keys (read write delete)}]
  (reify Step
    (on-read [_ operation-data next-step]
      (if read
        (read operation-data next-step)
        (next-step operation-data)))
    (on-write [_ operation-data next-step]
      (if write
        (write operation-data next-step)
        (next-step operation-data)))
    (on-delete [_ operation-data next-step]
      (if delete
        (delete operation-data next-step)
        (next-step operation-data)))))

(defn create-read-step
  "Create a Step using a given read function, write and delete will just pass forward
  to the next Step in the chain"
  [fun]
  (create-step {:read fun}))

(defn create-write-step
  "Create a Step using a given write function, read and delete will just pass forward
  to the next Step in the chain"
  [fun]
  (create-step {:write fun}))

(defn create-delete-step
  "Create a Step using a given delete function, read and write will just pass forward
  to the next Step in the chain"
  [fun]
  (create-step {:delete fun}))
