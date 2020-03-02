(ns citrus.custom-handler-test
  (:require [clojure.test :refer [deftest testing is async]]
            [citrus.core :as citrus]
            [citrus.reconciler :as rec]
            [citrus.cursor :as cur]
            [goog.object :as gobj]
            [rum.core :as rum]))

(defn mk-citrus-map-dispatch-handler
  "An example of a different default "
  [handlers]
  (fn [reconciler events]
    (let [state-atom (gobj/get reconciler "state")]
      ;; (add-watch state-atom :logger
      ;;            (fn [_key _atom old-state new-state]
      ;;              (js/console.log (pr-str "old:" old-state))
      ;;              (js/console.log (pr-str "new:" new-state))))
      (reset!
        state-atom
        (loop [state @reconciler
               [[ctrl event-key event-args :as event] & events] events]
          (if (nil? event)
            state
            (if-let [handler (get handlers (keyword ctrl event-key))]
              (recur (handler state event-args) events)
              (let [e (ex-info (str "No handler for " (keyword ctrl event-key)) {})]
                (js/console.error e)
                (throw e))))) ))))

(def handlers
  {:user/log-in (fn [state [user-name]]
                  (assoc state :current-user user-name))
   :user/log-out (fn [state _]
                   (dissoc state :current-user))
   :counters/reset (fn [state [counter-key]]
                     (assoc-in state [:counters counter-key] 0))
   :counters/inc (fn [state [counter-key]]
                   (update-in state [:counters counter-key] (fnil inc 0)))
   :counters/dec (fn [state [counter-key]]
                   (update-in state [:counters counter-key] (fnil dec 0)))})

(def r (citrus/reconciler {:state           (atom {})
                           :default-handler (mk-citrus-map-dispatch-handler handlers)}))

(def current-user (citrus/subscription r [:current-user]))
(def counters (citrus/subscription r [:counters]))

(deftest initial-state
  (testing "Checking initial state in atom"
    (is (nil? @current-user))
    (is (nil? @counters))))

(deftest dispatch-sync-test
  (testing "One dispatch-sync! works"
    (citrus/dispatch-sync! r :counters :inc :a)
    (is (= {:a 1} @counters)))

  (testing "dispatch-sync! in a series keeps the the last call"
    (dotimes [_ 10]
      (citrus/dispatch-sync! r :counters :inc :x))
    (is (= {:a 1 :x 10} @counters)))

  (testing "dispatch-sync! a non-existing event fails"
    (is (thrown-with-msg? js/Error
                          #"No handler for :test/non-existing-event"
                          (citrus/dispatch-sync! r :test :non-existing-event)))))

(deftest dispatch-test
  (testing "dispatch! in a series preservers order"
    (dotimes [_ 10]
      (citrus/dispatch! r :counters :inc :a))
    (citrus/dispatch! r :counters :reset :a)
    (dotimes [_ 5]
      (citrus/dispatch! r :counters :inc :a))
    (citrus/dispatch! r :user :log-in "roman")
    (async done (js/requestAnimationFrame
                  (fn []
                    (is (= {:a 5 :x 10} @counters))
                    (is (= "roman" @current-user))
                    (done))))) )
