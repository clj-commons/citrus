(ns citrus.handler-test
  (:require [clojure.test :refer [deftest testing is async]]
            [citrus.core :as citrus]
            [rum.core :as rum]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Stateful tests
;;
;; Only one reconciler is defined for all tests for convenience
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; a spy, instead of `window.onerror`
(def on-error (atom nil))

(defmulti dummy-controller (fn [event] event))

(defmethod dummy-controller :set-state [_ [new-state] state]
  {:state (assoc state :dummy new-state)})

(defmulti test-controller (fn [event] event))

(defmethod test-controller :default [event _ state]
  (when-let [on-error @on-error]
    (on-error event))
  {:state state})

(defmethod test-controller :set-state [_ [new-state] state]
  {:state (assoc state :test new-state)})

(defmethod test-controller :set-substate-a [_ [new-substate] current-state]
  {:state (assoc-in current-state [:test :a] new-substate)})

(defmethod test-controller :set-substate-b [_ [new-substate] current-state]
  {:state (assoc-in current-state [:test :b] new-substate)})

(def side-effect-atom (atom 0))

(defn side-effect [_ _ _]
  (swap! side-effect-atom inc))

(defmethod test-controller :side-effect [_ _ _]
  {:side-effect true})

(def controllers
  {:test  test-controller
   :dummy dummy-controller})

(defn handler [ctrl event args state cofx]
  (let [ctrl (get controllers ctrl)]
    (ctrl event args state cofx)))

(def r (citrus/reconciler {:state           (atom {:test  :initial-state
                                                   :dummy nil})
                           :citrus/handler  handler
                           :effect-handlers {:side-effect side-effect}}))

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
    (reset! on-error #(is (= :non-existing-event %)))
    (citrus/dispatch-sync! r :test :non-existing-event)
    (reset! on-error nil)))


(deftest broadcast-sync!

  (testing "broadcast-sync! doesn't work with :citrus/handler"
    (is (thrown-with-msg? js/Error
                          #"broadcast-sync! doesn't work with :citrus/handler, do it manually on your side"
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

  (testing "dispatch! an non-existing event fails"
    (async done
      (reset! on-error
              #(do (is (= % :non-existing-dispatch))
                   (done))))
    (citrus/dispatch! r :test :non-existing-dispatch)
    (reset! on-error nil)))



(deftest broadcast!

  (testing "broadcast! doesn't work with :citrus/handler"
    (is (thrown-with-msg? js/Error
                          #"broadcast! doesn't work with :citrus/handler, do it manually on your side"
                          (citrus/broadcast! r :non-existing-broadcast)))))

(deftest dispatch-nil-state-issue-20
  ;https://github.com/roman01la/citrus/issues/20

  (testing "synchronously setting state as `nil` works"
    (citrus/dispatch-sync! r :test :set-state nil)
    (is (nil? @sub)))

  (testing "asynchronously setting state as `nil` works"
    (citrus/dispatch-sync! r :test :set-state "foo")
    (citrus/dispatch! r :test :set-state nil)
    (is (= "foo" @sub))
    (async done (js/requestAnimationFrame (fn []
                                            (is (nil? @sub))
                                            (done))))))

(deftest double-dispatch-issue-23
  ;https://github.com/roman01la/citrus/issues/23

  (testing "asynchronously dispatching 2 events that change different parts of the same controller"
    (citrus/dispatch! r :test :set-state {:initial :state})
    (citrus/dispatch! r :test :set-substate-a "a")
    (citrus/dispatch! r :test :set-substate-b "b")
    (async done (js/requestAnimationFrame (fn []
                                            (is (= {:initial :state :a "a" :b "b"} @sub))
                                            (done)))))

  (testing "synchronously dispatching 2 events that change different parts of the same controller"
    (citrus/dispatch-sync! r :test :set-state {:initial :state})
    (citrus/dispatch-sync! r :test :set-substate-a "a")
    (citrus/dispatch-sync! r :test :set-substate-b "b")
    (is (= {:initial :state :a "a" :b "b"} @sub)))

  (testing "mixed dispatch of 2 events that change different part of the same controller"
    (citrus/dispatch-sync! r :test :set-state {:initial :state})
    (citrus/dispatch! r :test :set-substate-a "a")
    (citrus/dispatch-sync! r :test :set-substate-b "b")
    (async done (js/requestAnimationFrame (fn []
                                            (is (= {:initial :state :a "a" :b "b"} @sub))
                                            (done))))))

(deftest side-effects

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


(deftest subscription

  (testing "basic cases already tested above")

  (testing "reducer function"
    (let [reducer-sub (citrus/subscription r [:test] #(str %))]
      (citrus/dispatch-sync! r :test :set-state 1)
      (is (= "1" @reducer-sub))))

  (testing "deep path"
    (let [deep-sub (citrus/subscription r [:test :a 0])]
      (citrus/dispatch-sync! r :test :set-state {:a [42]})
      (is (= 42 @deep-sub))))

  (testing "with rum's derived-atom"
    (let [derived-sub (rum/derived-atom [sub dummy] ::key
                                        (fn [sub-value dummy-value]
                                          (/ (+ sub-value dummy-value) 2)))]
      (citrus/dispatch-sync! r :test :set-state 10)
      (citrus/dispatch-sync! r :dummy :set-state 20)
      (is (= 15 @derived-sub)))))


(deftest custom-scheduler

  (testing "a synchronous scheduler updates state synchronously"
    (let [r (citrus/reconciler {:state           (atom {:test :initial-state})
                                :citrus/handler  handler
                                :batched-updates {:schedule-fn (fn [f] (f)) :release-fn (fn [_])}})
          sub (citrus/subscription r [:test])]
      (is (= :initial-state @sub))
      (citrus/dispatch! r :test :set-state nil)
      (is (nil? @sub))))

  (testing "an asynchronous scheduler updates state asynchronously"
    (let [async-delay 50                                    ;; in ms
          r (citrus/reconciler {:state           (atom {:test :initial-state})
                                :citrus/handler  handler
                                :batched-updates {:schedule-fn (fn [f] (js/setTimeout f async-delay)) :release-fn (fn [id] (js/clearTimeout id))}})
          sub (citrus/subscription r [:test])]
      (is (= :initial-state @sub))
      (citrus/dispatch! r :test :set-state nil)
      (is (= :initial-state @sub))
      (async done (js/setTimeout (fn []
                                   (is (nil? @sub))
                                   (done))
                                 async-delay)))))
