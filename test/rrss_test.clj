(ns rrss_test
  (:import redis.clients.jedis.Jedis)
  (:use rrss
        [ring.middleware.session.store :only (read-session write-session delete-session)]
        [clojure.test :only (deftest testing is use-fixtures)]))

(defn- flush-fixture [f]
  (try
    (.flushDB (Jedis. "localhost"))
    (finally (f))))

(use-fixtures :each flush-fixture)
(use-fixtures :once #(do (.flushDB (Jedis. "localhost")) (%)))

(deftest test-construction
  (is (redis-store))
  (is (redis-store {:port 6378 :host "example.com"})))

(deftest test-write-session
  (let [store (redis-store)]
    (write-session store "empty" {})
    (is (= {} (read-session store "empty")))

    (write-session store "simple" {:hi :bye})
    (is (= {"hi" "bye"} (read-session store "simple")))

    (write-session store "key-str" {:hi "bye" "hi" :bye})
    (is (= {"hi" "bye"} (read-session store "key-str")))

    (write-session store "almost-empty" {})
    (write-session store "almost-empty" {:not :so-much-empty})
    (is (= {"not" "so-much-empty"} (read-session store "almost-empty")))))

(deftest test-delete-session
  (let [store (redis-store)]
    (write-session store "deadman" {1 2})
    (delete-session store "deadman")
    (is (= {} (read-session store "deadman")))))

(deftest test-read-session
  (is (= {} (read-session (redis-store) "unknown")))
  (is (= {} (read-session (redis-store) :unknown))))

