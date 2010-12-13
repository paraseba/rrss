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
  (let [;steps (reverse steps) ;fixme decide correct order
        read   (chain-funs (map (step-binder on-read) steps))
        write  (chain-funs (map (step-binder on-write) steps))
        delete (chain-funs (map (step-binder on-delete) steps))]
    {:read read :write write :delete delete}))

(defn create-read-step [fun]
  (reify Step
    (on-read [_ operation-data next-step] (fun operation-data next-step))
    (on-write [_ operation-data next-step] (next-step operation-data))
    (on-delete [_ operation-data next-step] (next-step operation-data))))

(defn create-write-step [fun]
  (reify Step
    (on-read [_ operation-data next-step] (next-step operation-data))
    (on-write [_ operation-data next-step] (fun operation-data next-step))
    (on-delete [_ operation-data next-step] (next-step operation-data))))

(defn create-delete-step [fun]
  (reify Step
    (on-read [_ operation-data next-step] (next-step operation-data))
    (on-write [_ operation-data next-step] (next-step operation-data))
    (on-delete [_ operation-data next-step] (fun operation-data next-step))))
