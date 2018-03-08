(ns citrus.core-test
  (:require [clojure.test :refer [deftest testing is async]]
            [citrus.core :as citrus]
            [citrus.reconciler :as rec]
            [citrus.cursor :as cur]
            [goog.object :as obj]
            [rum.core :as rum]))

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

(def side-effect-atom (atom 0))

(defn side-effect [reconciler ctrl-name effect]
  (swap! side-effect-atom inc))

(defmethod test-controller :side-effect [_ _ _]
  {:side-effect true})


(def r (citrus/reconciler {:state           (atom {:test  :initial-state
                                                   :dummy nil})
                           :controllers     {:test  test-controller
                                             :dummy dummy-controller}
                           :effect-handlers {:side-effect side-effect}}))

(def sub (citrus/subscription r [:test]))
(def dummy (citrus/subscription r [:dummy]))


#_(deftest initial-state

  (testing "Checking initial state in atom"
    (is (= :initial-state @sub))
    (is (nil? @dummy))))


#_(deftest dispatch-sync!

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


#_(deftest broadcast-sync!

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


#_(deftest dispatch!

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

  (testing "dispatch! an non-existing event fails"
    (let [err-handler (fn [err]
                        (is (re-find #"No method .* for dispatch value: :non-existing-dispatch" (.toString err))))]
      (obj/set js/window "onerror" err-handler)
      (citrus/dispatch! r :test :non-existing-dispatch)
      (async done (js/requestAnimationFrame (fn []
                                                (obj/set js/window "onerror" nil)
                                                (done)))))))


(deftest broadcast!

  ;; Look at the assertions in the async block... False positives, don't understand why
  (testing "broadcast! works asynchronously"
    (citrus/broadcast-sync! r :set-state "sync")
    (citrus/broadcast! r :set-state "async")
    (is (= "sync" @sub))
    (is (= "sync" @dummy))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= 1 "async" @sub))
                                            (is (= 1 "async" @dummy))
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


#_(deftest side-effects

  (testing "Works synchronously"
    (is (zero? @side-effect-atom))
    (citrus/dispatch-sync! r :test :side-effect)
    (is (= 1 @side-effect-atom)))

  (testing "Works asynchronously"
    (is (= 1 @side-effect-atom))
    (citrus/dispatch! r :test :side-effect)
    (is (= 1 @side-effect-atom))
    (async done (js/requestAnimationFrame (fn []
                                            (is (= 2 @side-effect-atom))
                                            (done))))))


#_(deftest subscription

  (testing "basic cases already tested above")

  (testing "reducer function"
    (let [reducer-sub (citrus/subscription r [:test] #(str %))]
      (citrus/dispatch-sync! r :test :set-state 1)
      (is (= "1" @reducer-sub))))

  (testing "deep path"
    (let [deep-sub (citrus/subscription r [:test :a])]
      (citrus/dispatch-sync! r :test :set-state {:a 42})
      (is (= 42 @deep-sub))))

  (testing "with rum's derived-atom"
    (let [derived-sub (rum/derived-atom [sub dummy] ::key
                                        (fn [sub-value dummy-value]
                                          (/ (+ sub-value dummy-value) 2)))]
      (citrus/dispatch-sync! r :test :set-state 10)
      (citrus/dispatch-sync! r :dummy :set-state 20)
      (is (= 15 @derived-sub)))))


#_(deftest custom-scheduler

  (testing "a synchronous scheduler updates state synchronously"
    (let [r (citrus/reconciler {:state           (atom {:test :initial-state})
                                :controllers     {:test test-controller}
                                :batched-updates {:schedule-fn (fn [f] (f)) :release-fn (fn [_])}})
          sub (citrus/subscription r [:test])]
      (is (= :initial-state @sub))
      (citrus/dispatch! r :test :set-state nil)
      (is (nil? @sub))))

  (testing "an asynchronous scheduler updates state asynchronously"
    (let [async-delay 50 ;; in ms
          r (citrus/reconciler {:state           (atom {:test :initial-state})
                                :controllers     {:test test-controller}
                                :batched-updates {:schedule-fn (fn [f] (js/setTimeout f async-delay)) :release-fn (fn [id] (js/clearTimeout id))}})
          sub (citrus/subscription r [:test])]
      (is (= :initial-state @sub))
      (citrus/dispatch! r :test :set-state nil)
      (is (= :initial-state @sub))
      (async done (js/setTimeout (fn []
                                   (is (nil? @sub))
                                   (done))
                                 async-delay)))))
