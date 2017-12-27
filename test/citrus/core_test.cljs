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



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Stateful tests
;;
;; Only one reconciler is defined for all tests for convenience
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti dummy-controller (fn [event] event))

(defmethod dummy-controller :set-state [_ [new-state] _]
  {:state new-state})

(defmulti test-controller (fn [event] event))

(defmethod test-controller :set-state [_ [new-state] _]
  {:state new-state})


(def r (citrus/reconciler {:state       (atom {:test  :initial-state
                                               :dummy nil})
                           :controllers {:test  test-controller
                                         :dummy dummy-controller}}))
(def sub (citrus/subscription r [:test]))
(def dummy (citrus/subscription r [:dummy]))

(deftest initial-state
  (testing "Checking initial state in atom"
    (is (= :initial-state @sub))
    (is (nil? @dummy))))

(deftest dispatch-sync!
  (testing "One dispatch-sync! works"
    (citrus/dispatch-sync! r :test :set-state 1)
    (is (= 1 @sub)))

  (testing "dispatch-sync! in a series keeps the the last call"
    (doseq [i (range 10)]
      (citrus/dispatch-sync! r :test :set-state i))
    (is (= 9 @sub)))

  (testing "dispatch-sync! a non-existing event fails"
    (is (thrown-with-msg? js/Error
                          #"No method .* for dispatch value: :non-existing-event"
                          (citrus/dispatch-sync! r :test :non-existing-event)))))

(deftest broadcast-sync!
  (testing "One broadcast-sync! works"
    (citrus/broadcast-sync! r :set-state 1)
    (is (= 1 @sub))
    (is (= 1 @dummy)))

  (testing "broadcast-sync! in series keeps the last call"
    (doseq [i (range 10)]
      (citrus/broadcast-sync! r :set-state i))
    (is (= 9 @sub))
    (is (= 9 @dummy))))

(deftest bug-20-nil-valid-state
  (let [r   (citrus/reconciler {:state (atom {:test :initial-state}) :controllers {:test test-controller}})
        sub (citrus/subscription r [:test])]
    (citrus/dispatch! r :test :set-state 1)
    (citrus/dispatch! r :test :set-state 2)
    (citrus/dispatch! r :test :set-state nil)
    (is (= :initial-state @sub))
    (async done (js/requestAnimationFrame (fn async []
                                            (is (= nil @sub))
                                            (done))))))
