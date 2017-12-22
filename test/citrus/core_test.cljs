(ns citrus.core-test
  (:require [clojure.test :refer [deftest testing is]]
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
