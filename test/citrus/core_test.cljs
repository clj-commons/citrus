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

(defmethod test-controller :init []
          {:state :some-state})

(defmethod test-controller :nil []
          {:state nil})

(deftest bug-20-nil-valid-state
  (let [r (citrus/reconciler {:state (atom {}) :controllers {:test test-controller}})
        sub (citrus/subscription r [:test])]
    (citrus/dispatch! r :test :init)
    (println "1" (js/Date.now))
    (async done1 (js/setTimeout (fn async1 []
                                  (println "2" (js/Date.now))
                                  (is (= :some-state @sub))
                                  #_(citrus/dispatch! r :test :nil)
                                  #_(async done2 (js/setTimeout (fn async2 []
                                                                  (is (= nil @sub))
                                                                  (done2))
                                                                30))
                                  (done1))
                                3000))))
