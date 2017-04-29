(ns scrum.core-test
  (:require [clojure.test :refer :all]
            [scrum.core :as scrum]))

(deftest reconciler
  (testing "Should return Reconciler hash"
    (let [r (scrum/reconciler {})]
      (is (contains? r :state))
      (is (contains? r :resolvers))
      (is (instance? clojure.lang.Atom (:state r)))
      (is (= (:resolvers r) {})))))

(deftest dispatch!
  (testing "Should return `nil`"
    (is (nil? (scrum/dispatch! nil nil nil)))))

(deftest dispatch-sync!
  (testing "Should return `nil`"
    (is (nil? (scrum/dispatch-sync! nil nil nil)))))

(deftest broadcast!
  (testing "Should return `nil`"
    (is (nil? (scrum/broadcast! nil nil)))))

(deftest broadcast-sync!
  (testing "Should return `nil`"
    (is (nil? (scrum/broadcast-sync! nil nil)))))

(deftest subscription
  (testing "Should return Resolver instance"
    (is (instance? scrum.resolver.Resolver
                   (scrum/subscription (scrum/reconciler {}) [:path])))))
