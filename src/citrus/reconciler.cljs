(ns citrus.reconciler
  (:require-macros [citrus.macros :as m])
  (:require [cljs.spec.alpha :as s]))

(defn- queue-effects! [queue f]
  (vswap! queue conj f))

(defn- clear-queue! [queue]
  (vreset! queue []))

(defn- schedule-update! [{:keys [schedule-fn release-fn]} scheduled? f]
  (when-let [id @scheduled?]
    (vreset! scheduled? nil)
    (release-fn id))
  (vreset! scheduled? (schedule-fn f)))

(defn citrus-default-handler
  "Implements Citrus' default event handling (as of 3.2.3).

  This function can be copied into your project and adapted to your needs.

  `events` is expected to be a list of events (tuples):

     [ctrl event-key event-args]"
  [reconciler events]
  (let [controllers (.-controllers reconciler)
        co-effects (.-co_effects reconciler)
        effect-handlers (.-effect_handlers reconciler)
        state-atom (.-state reconciler)]
    (reset!
      state-atom
      (loop [state @reconciler
             [[ctrl event-key event-args :as event] & events] events]
        (if (nil? event)
          state
          (do
            (assert (contains? controllers ctrl) (str "Controller " ctrl " is not found"))
            (let [ctrl-fn (get controllers ctrl)
                  cofx (get-in (.-meta ctrl) [:citrus event-key :cofx])
                  cofx (reduce
                         (fn [cofx [k & args]]
                           (assoc cofx k (apply (co-effects k) args)))
                         {}
                         cofx)
                  effects (ctrl-fn event-key event-args (get state ctrl) cofx)]
              (m/doseq [effect (dissoc effects :state)]
                (let [[eff-type effect] effect]
                  (when (s/check-asserts?)
                    (when-let [spec (s/get-spec eff-type)]
                      (s/assert spec effect)))
                  (when-let [handler (get effect-handlers eff-type)]
                    (handler reconciler ctrl effect))))
              (if (contains? effects :state)
                (recur (assoc state ctrl (:state effects)) events)
                (recur state events)))))))))

(defprotocol IReconciler
  (dispatch! [this controller event args])
  (dispatch-sync! [this controller event args])
  (broadcast! [this event args])
  (broadcast-sync! [this event args]))

(deftype Reconciler [controllers default-handler effect-handlers co-effects state queue scheduled? batched-updates chunked-updates meta watch-fns]

  Object
  (equiv [this other]
    (-equiv this other))

  IAtom

  IMeta
  (-meta [_] meta)

  IEquiv
  (-equiv [this other]
    (identical? this other))

  IDeref
  (-deref [_]
    (-deref state))

  IWatchable
  (-add-watch [this key callback]
    (vswap! watch-fns assoc key callback)
    this)

  (-remove-watch [this key]
    (vswap! watch-fns dissoc key)
    this)

  IHash
  (-hash [this] (goog/getUid this))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer "#object [citrus.reconciler.Reconciler ")
    (pr-writer {:val (-deref this)} writer opts)
    (-write writer "]"))

  IReconciler
  (dispatch! [this cname event args]
    (assert (some? event) (str "dispatch! was called without event name:" (pr-str [cname event args])))
    (queue-effects! queue [cname event args])
    (schedule-update!
      batched-updates
      scheduled?
      (fn batch-runner []
        (let [events @queue]
          (clear-queue! queue)
          (default-handler this events)))))

  (dispatch-sync! [this cname event args]
    (assert (some? event) (str "dispatch! was called without event name:" (pr-str [cname event args])))
    (default-handler this [[cname event args]]))

  (broadcast! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch! this controller event args)))

  (broadcast-sync! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch-sync! this controller event args))))
