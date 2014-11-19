(ns blizzard.core-test
  (:require [clojure.test :refer [deftest is]]
            [blizzard.core :as blizzard]))

(deftest test-string->integer
  (is (= (blizzard/string->integer "1") 1)))

;; Not really testing anything here for lack of a constant environment
;; variable.
(deftest test-get-env
  (is (= (blizzard/get-env "") nil)))

(deftest test-n-or-1
  (is (= (blizzard/n-or-1 -1) 1))
  (is (= (blizzard/n-or-1 42) 42)))

(deftest test-n-or-max-ids
  (is (= (blizzard/n-or-max-ids 42 10) 10))
  (is (= (blizzard/n-or-max-ids 42 100) 42)))

(deftest test-take-ids
  (let [ids (blizzard/take-ids 10)]
    (is (every? #(instance? clojure.lang.BigInt %) ids))))

(deftest test-transit-write
  (let [json (blizzard/transit-write "foo" :json)]
    (is (= (str json) "[\"~#'\",\"foo\"]")))
  (let [json (blizzard/transit-write 42N :json)]
    (is (= (str json) "[\"~#'\",\"~n42\"]"))))
