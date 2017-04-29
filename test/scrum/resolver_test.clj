(ns scrum.resolver-test
  (:require [clojure.test :refer :all]
            [scrum.resolver :as resolver]))

(deftest make-resolver
  (testing "Should return Resolver instance"
    (is (instance? scrum.resolver.Resolver
                   (resolver/make-resolver (atom {}) {} [:path] identity)))))

(deftest Resolver
  (testing "Should return resolved data when dereferenced"
    (let [r (resolver/make-resolver (atom {}) {[:path] (constantly 1)} [:path] nil)]
      (is (= @r 1))))
  (testing "Should apply reducer to resolved data when dereferenced"
    (let [r (resolver/make-resolver (atom {}) {[:path] (constantly 1)} [:path] inc)]
      (is (= @r 2))))
  (testing "Should populate state with resolved data after dereferencing"
    (let [state (atom {})]
      @(resolver/make-resolver state {[:path] (constantly 1)} [:path] nil)
      (is (= (:path @state) 1)))))
