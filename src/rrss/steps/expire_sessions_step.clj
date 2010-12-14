(ns rrss.steps.expire-sessions-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)]))

(defn- worker-loop [resolution worker]
  (Thread/sleep (* 1000 resolution))
  (worker)
  (recur resolution worker))

(defn- expire [connection duration all-sessions-key]
  (let [now (-> (Date.) .getTime)
        duration-ms (* 1000 duration)
        max-score (double (- now duration-ms))
        old-keys (.zrangeByScore connection all-sessions-key 0.0 max-score)]
    (doseq [key old-keys]
      (.zrem connection all-sessions-key key)
      (.del connection (into-array String [key])))))

(defn- create-worker-thread [connection duration resolution all-sessions-key]
  (Thread. (fn []
             (worker-loop resolution
                          (partial expire connection duration all-sessions-key)))))

(def ^{:private true} default-options
  {:duration (* 7 24 60 60)
   :resolution (* 10 60)
   :all-sessions-key "sessions:all"})

(defn expire-sessions-step
  ([] (expire-sessions-step {}))
  ([options]
   (let [{:keys [duration resolution all-sessions-key]} (merge default-options options)
         thread (atom nil)
         start-thread (fn [connection]
                        (swap! thread #(or %
                                           (create-worker-thread
                                             connection
                                             duration
                                             resolution
                                             all-sessions-key)))
                        (or (.isAlive @thread)
                            (.start @thread)))
         step-fun (fn [opdata next-step]
                    (let [res (next-step opdata)]
                      (start-thread (:connection opdata))
                      res))]

     (create-step {:write step-fun :read step-fun :delete step-fun}))))

