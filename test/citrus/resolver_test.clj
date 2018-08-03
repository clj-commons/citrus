(ns citrus.resolver-test
  (:require [clojure.test :refer :all]
            [citrus.resolver :as resolver]))

(deftest make-resolver
  (testing "Should return Resolver instance"
    (is (instance? citrus.resolver.Resolver
                   (resolver/make-resolver (atom {}) {} [:path] identity)))))

(deftest Resolver
  (testing "Should return resolved data when dereferenced"
    (let [r (resolver/make-resolver (atom {}) {:path (constantly 1)} [:path] nil)]
      (is (= @r 1))))
  (testing "Should return nested resolved data when dereferenced"
    (let [r (resolver/make-resolver (atom {}) {:path (constantly {:value 1})} [:path :value] nil)]
      (is (= @r 1))))
  (testing "Should apply reducer to resolved data when dereferenced"
    (let [r (resolver/make-resolver (atom {}) {:path (constantly 1)} [:path] inc)]
      (is (= @r 2))))
  (testing "Should populate state with resolved data after dereferencing"
    (let [state (atom {})]
      @(resolver/make-resolver state {:path (constantly 1)} [:path] nil)
      (is (= @state {:path 1}))))
  (testing "Should populate state with resolved data after dereferencing given nested path"
    (let [state (atom {})]
      @(resolver/make-resolver state {:path (constantly {:value 1})} [:path :value] nil)
      (is (= @state {:path {:value 1}})))))
