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
                   (citrus/subscription (citrus/reconciler {}) [:path])))))
