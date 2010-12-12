(ns rrss_test
  (:import redis.clients.jedis.Jedis)
  (:import java.util.Date)
  (:use rrss
        (rrss.hooks sessions-set-hook time-sessions-hook)
        [ring.middleware.session.store :only (read-session write-session delete-session)]
        [clojure.test :only (deftest testing is use-fixtures)]))

(def jedis (Jedis. "localhost"))

(defn- flush-fixture [f]
  (try
    (.flushDB jedis)
    (finally (f))))

(use-fixtures :each flush-fixture)
(use-fixtures :once #(do (.flushDB jedis) (%)))

(deftest test-construction
  (is (redis-store))
  (is (redis-store {:port 6378 :host "example.com"})))

(def store (redis-store))

(deftest test-write-session
  (write-session store "empty" {})
  (is (= {} (read-session store "empty")))

  (write-session store "simple" {:hi :bye})
  (is (= {"hi" "bye"} (read-session store "simple")))

  (write-session store "key-str" {:hi "bye" "hi" :bye})
  (is (= {"hi" "bye"} (read-session store "key-str")))

  (write-session store "almost-empty" {})
  (write-session store "almost-empty" {:not :so-much-empty})
  (is (= {"not" "so-much-empty"} (read-session store "almost-empty"))))

(deftest create-random-keys
  (let [new-key (write-session store nil {:foo :bar})]
    (is (string? new-key))
    (is (< 10 (count new-key)))
    (is (= {"foo" "bar"} (read-session store new-key)))))

(deftest test-delete-session
  (write-session store "deadman" {1 2})
  (delete-session store "deadman")
  (is (= {} (read-session store "deadman")))
  (is (nil? (delete-session store nil))))

(deftest test-read-session
  (is (= {} (read-session store "unknown")))
  (is (= {} (read-session store nil))))

(deftest test-return-types
  (is (= "foo" (write-session store "foo" {:hi :bye})))
  (is (nil? (delete-session store "foo"))))

(deftest test-redis-keys
  (write-session store "foo" {:hi :bye})
  (is (= "hash" (.type jedis "sessions:foo")))

  (write-session (redis-store {:map-key #(str % ":suffix")}) "bar" {:hi :bye})
  (is (= "hash" (.type jedis "bar:suffix")))
  (is (= "none" (.type jedis "sessions:bar"))))

(deftest test-sessions-set-hook
  (let [store (redis-store {:hooks [(sessions-set-hook)]})]
    (write-session store "foo" {:hi :bye})
    (write-session store "bar" {:hi :bye})
    (is (= #{"sessions:foo" "sessions:bar"} (.smembers jedis "sessions:all")))
    (delete-session store "bar")
    (is (= #{"sessions:foo"} (.smembers jedis "sessions:all")))))

(defn millis-ago [time-str]
  (- (.getTime (Date.))
     (Long/parseLong time-str)))

(deftest test-time-sessions-hook
  (let [store (redis-store {:hooks [(time-sessions-hook)]})]
    (write-session store "foo" {:hi :bye})
    (is (> 1000 (millis-ago (.get jedis "sessions:foo:written-at"))))))
