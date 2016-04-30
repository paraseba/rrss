(ns steps_test
  (:import redis.clients.jedis.Jedis)
  (:import java.util.Date)
  (:use rrss
        [rrss.steps]
        (rrss.steps sessions-set-step time-sessions-step expire-sessions-step)
        [ring.middleware.session.store :only (read-session write-session delete-session)]
        [clojure.test :only (deftest testing is use-fixtures)]))

(def jedis (Jedis. "localhost"))

(defn- flush-fixture [f]
  (try
    (.flushDB jedis)
    (finally (f))))

(use-fixtures :each flush-fixture)
(use-fixtures :once #(do (.flushDB jedis) (%)))

(deftest step-chain-is-called
  (let [counter (atom 0)
        step1 (fn [opdata next-step]
                (swap! counter inc)
                (next-step opdata))
        step2 (fn [opdata next-step]
                (swap! counter inc)
                (next-step opdata))
        step3 (fn [opdata next-step]
                (swap! counter inc)
                (next-step opdata))
        store (redis-store {:steps [(create-write-step step1)
                                    (create-read-step step2)
                                    (create-delete-step step3)]})]
    (is (= 0 @counter))
    (write-session store "foo" {:hi :bye})
    (is (= 1 @counter))
    (read-session store "foo")
    (is (= 2 @counter))
    (delete-session store "foo")
    (is (= 3 @counter))))

(deftest step-chain-order
  (let [step1 (fn [opdata next-step]
                (is (nil? (:step2-pre opdata)))
                (is (nil? (:step2-pos opdata)))
                (let [res (next-step (assoc opdata :step1-pre true))]
                  (is (:step2-pre res))
                  (is (:step2-pos res))
                  res))
        step2 (fn [opdata next-step]
                (is (:step1-pre opdata))
                (is (nil? (:step1-pos opdata)))
                (assoc
                  (next-step (assoc opdata :step2-pre true))
                  :step2-pos
                  true))
        store (redis-store {:steps [(create-read-step step2)
                                    (create-read-step step1)]})]

    (write-session store "foo" {:hi :bye})
    (is (= {"hi" "bye"} (read-session store "foo")))
    (delete-session store "foo")
    (is (= {} (read-session store "foo")))))

(deftest test-sessions-set-step
  (let [store (redis-store {:steps [(sessions-set-step)]})]
    (write-session store "foo" {:hi :bye})
    (write-session store "bar" {:hi :bye})
    (is (= #{"sessions:foo" "sessions:bar"} (into #{} (.zrange jedis "sessions:all" (long 0) (long -1)))))
    (delete-session store "bar")
    (is (= #{"sessions:foo"} (into #{} (.zrange jedis "sessions:all" (long 0) (long -1)))))))

(defn millis-ago [time-str]
  (- (.getTime (Date.))
     (Long/parseLong time-str)))

(deftest test-time-sessions-step
  (let [store (redis-store {:steps [(time-sessions-step)]})]
    (write-session store "foo" {:hi :bye})
    (is (> 1000 (millis-ago (.get jedis "sessions:foo:written-at"))))))

(deftest set-and-time-steps
  (let [store (redis-store {:steps [(time-sessions-step) (sessions-set-step)]})]
    (write-session store "foo" {:hi :bye})
    (is (= #{"sessions:foo"} (into #{} (.zrange jedis "sessions:all" (long 0) (long -1)))))
    (is (> 1000 (millis-ago (.get jedis "sessions:foo:written-at"))))))

(deftest test-expire-sessions-step
  (let [[step stop] (expire-sessions-step {:resolution 1 :duration 1 :with-stop true})
        store (redis-store {:steps [(sessions-set-step) step]})]
    (Thread/sleep 1000)
    (write-session store "old" {"old" "foo"})
    (is (= {"old" "foo"} (read-session store "old")))
    (Thread/sleep 2200)
    (write-session store "new" {:new :foo})
    (is (= {"new" "foo"} (read-session store "new")))
    (is (= {} (read-session store "old")))
    (Thread/sleep 2200)
    (is (= {} (read-session store "old")))
    (is (= {} (read-session store "new")))
    (stop)))
