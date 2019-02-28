(ns citrus.core-test
  (:require [clojure.test :refer :all]
            [citrus.core :as citrus]))

(deftest reconciler
  (testing "Should return Reconciler hash"
    (let [r (citrus/reconciler {:state (atom {}) :resolvers {}})]
      (is (contains? r :state))
      (is (contains? r :resolvers))
      (is (instance? clojure.lang.Atom (:state r)))
      (is (= (:resolvers r) {})))))

(deftest dispatch!
  (testing "Should return `nil`"
    (is (nil? (citrus/dispatch! nil nil nil)))))

(deftest dispatch-sync!
  (testing "Should return `nil`"
    (is (nil? (citrus/dispatch-sync! nil nil nil)))))

(deftest broadcast!
  (testing "Should return `nil`"
    (is (nil? (citrus/broadcast! nil nil)))))

(deftest broadcast-sync!
  (testing "Should return `nil`"
    (is (nil? (citrus/broadcast-sync! nil nil)))))

(deftest subscription
  (testing "Should return Resolver instance"
    (is (instance? citrus.resolver.Resolver
                   (citrus/subscription (citrus/reconciler {}) [:path]))))
  (testing "Should return nested data when a nested path is supplied"
    (let [sub (citrus/subscription (citrus/reconciler {:state (atom {})
                                                       :resolvers {:a (constantly {:b [1 2 3]})}}) [:a :b])]
      (is (= @sub [1 2 3]))))
  (testing "Should return derived data when a reducer function is supplied"
    (let [sub (citrus/subscription (citrus/reconciler {:state (atom {})
                                                       :resolvers {:a (constantly {:b [1 2 3]})}}) [:a] map?)]
      (is (= @sub true))))
  (testing "Should return derived data when a reducer function *and* nested data are supplied"
    (let [sub (citrus/subscription (citrus/reconciler {:state (atom {})
                                                       :resolvers {:a (constantly {:b [1 2 3]})}}) [:a :b] count)]
      (is (= @sub 3)))))
