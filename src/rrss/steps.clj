(ns rrss.steps)

(defprotocol Step
  (on-read [step operation-data next-step])
  (on-write [step operation-data next-step])
  (on-delete [step operation-data next-step]))

(defn- chain-funs [funs]
  (reduce
    (fn [chain f]
      (fn [opdata]
        (f opdata chain)))
    identity
    funs))

(defn- step-binder [f]
  #(partial f %))

(defn create-step-chain [steps]
  (let [read   (chain-funs (map (step-binder on-read) steps))
        write  (chain-funs (map (step-binder on-write) steps))
        delete (chain-funs (map (step-binder on-delete) steps))]
    {:read read :write write :delete delete}))

(defn create-step [{:keys (read write delete)}]
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

(defn create-read-step [fun]
  (create-step {:read fun}))

(defn create-write-step [fun]
  (create-step {:write fun}))

(defn create-delete-step [fun]
  (create-step {:delete fun}))
