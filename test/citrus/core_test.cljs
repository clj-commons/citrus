(ns citrus.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [citrus.core :as citrus]
            [citrus.reconciler :as rec]
            [citrus.cursor :as cur]
            [goog.object :as obj]))

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

  (testing "broadcast-sync! in series keeps the last value"
    (doseq [i (range 10)]
      (citrus/broadcast-sync! r :set-state i))
    (is (= 9 @sub))
    (is (= 9 @dummy)))

  (testing "broadcast-sync! a non-existing event fails"
    (is (thrown-with-msg? js/Error
                          #"No method .* for dispatch value: :non-existing-event"
                          (citrus/broadcast-sync! r :non-existing-event)))))


(deftest dispatch!

  (testing "dispatch! works asynchronously"
    (citrus/dispatch-sync! r :test :set-state "sync")
    (citrus/dispatch! r :test :set-state "async")
    (is (= "sync" @sub))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= "async" @sub))
                                            (done)))))

  (testing "dispatch! in series keeps the last value"
    (doseq [i (range 10)]
      (citrus/dispatch! r :test :set-state i))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= 9 @sub))
                                            (done)))))

  #_(testing "dispatch! an non-existing event fails"
    (let [err-handler (fn [err]
                        (is (re-find #"No method .* for dispatch value: :non-existing-dispatch" (.toString err))))]
      (obj/set js/window "onerror" err-handler)
      (citrus/dispatch! r :test :non-existing-dispatch)
      (async done (js/requestAnimationFrame (fn []
                                                (obj/set js/window "onerror" nil)
                                                (done)))))))


(deftest broadcast!

  ;; Look at the assertions in the async block... False positives, don't understand why
  #_(testing "broadcast! works asynchronously"
    (citrus/broadcast-sync! r :set-state "sync")
    (citrus/broadcast! r :set-state "async")
    (is (= "sync" @sub))
    (is (= "sync" @dummy))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= 1 2 "async" @sub))
                                            (is (= 1 2 "async" @dummy))
                                            (done)))))

  (testing "broadcast! in series keeps the last value"
    (doseq [i (range 10)]
      (citrus/broadcast! r :set-state i))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= 9 @sub))
                                            (is (= 9 @dummy))
                                            (done)))))

  #_(testing "broadcast! an non-existing event fails"
    (let [err-handler (fn [err]
                        (is (re-find #"No method .* for dispatch value: :non-existing-broadcast" (.toString err))))]
      (obj/set js/window "onerror" err-handler)
      (citrus/broadcast! r :non-existing-broadcast)
      (async done (js/requestAnimationFrame (fn []
                                              (obj/set js/window "onerror" nil)
                                              (done)))))))
