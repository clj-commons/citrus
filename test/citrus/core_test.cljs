(ns citrus.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [citrus.core :as citrus]
            [citrus.reconciler :as rec]
            [citrus.cursor :as cur]))

(deftest reconciler
  (testing "Should return a Reconciler instance"
           (let [r (citrus/reconciler {:state (atom {}) :controllers {}})]
             (is (instance? rec/Reconciler r)))))

(deftest subscription
  (testing "Should return a Resolver instance"
    (let [r (citrus/reconciler {:state (atom {}) :controllers {}})]
      (is (instance? cur/ReduceCursor
                    (citrus/subscription r nil))))))

(defmulti test-controller (fn [event] event))

(defmethod test-controller :set-state [_ [new-state] _]
          {:state new-state})

(deftest bug-20-nil-valid-state
  (let [r (citrus/reconciler {:state (atom {:test :some-state}) :controllers {:test test-controller}})
        sub (citrus/subscription r [:test])]
    (citrus/dispatch! r :test :set-state 1)
    (citrus/dispatch! r :test :set-state 2)
    (citrus/dispatch! r :test :set-state nil)
    (async done (js/requestAnimationFrame (fn async []
                                             (is (= nil @sub))
                                             (done))))))
