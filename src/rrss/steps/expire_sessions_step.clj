(ns rrss.steps.expire-sessions-step
  (:import java.util.Date)
  (:use [rrss.steps :only (create-step)]))

(defn- worker-loop
  "Do worker job every resolution seconds until the atom stop-signal fires"
  [resolution worker stop-signal]
  (when-not @stop-signal
    (Thread/sleep (* 1000 resolution))
    (worker)
    (recur resolution worker stop-signal)))

(defn- expire
  "Delete sessions older than duration seconds. All session keys are saved in a
  sorted map according to rrss.steps.sessions-set-step.sessions-set-step"
  [connection duration all-sessions-key]
  (let [now (-> (Date.) .getTime)
        duration-ms (* 1000 duration)
        max-score (double (- now duration-ms))
        old-keys (.zrangeByScore connection all-sessions-key 0.0 max-score)]
    (doseq [key old-keys]
      (.zrem connection all-sessions-key (into-array String [key]))
      (.del connection (into-array String [key])))))


(def ^{:private true} default-options
  {:duration (* 7 24 60 60)
   :resolution (* 10 60)
   :all-sessions-key "sessions:all"})


(defn expire-sessions-step
  "Create a Step that deletes sessions that have not been written lately
  See documentation for rrss.expiring-redis-store"
  ([] (expire-sessions-step {}))
  ([options]
   (let [{:keys [duration resolution all-sessions-key with-stop]} (merge default-options options)
         thread (atom nil)
         stop-signal (atom false)
         create-thread (fn [connection]
                         (Thread. (fn []
                                    (worker-loop
                                      resolution
                                      (partial expire connection duration all-sessions-key)
                                      stop-signal))))
         start-thread (fn [connection]
                        (swap! thread
                               #(or %
                                    (doto (create-thread connection)
                                      (.setDaemon true)
                                      (.start)))))
         step-fun (fn [opdata next-step]
                    (let [res (next-step opdata)]
                      (start-thread (:connection opdata))
                      res))
         step (create-step {:write step-fun :read step-fun :delete step-fun})]

     (if with-stop
       [step (fn [] (swap! stop-signal not))]
       step))))

