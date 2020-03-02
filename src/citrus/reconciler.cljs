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

(defprotocol IReconciler
  (dispatch! [this controller event args])
  (dispatch-sync! [this controller event args])
  (broadcast! [this event args])
  (broadcast-sync! [this event args]))

(defn citrus-default-handler [reconciler ctrl event-key event-args]
  (let [ctrl-fn (get (.-controllers reconciler) ctrl)
        cofx (get-in (.-meta ctrl) [:citrus event-key :cofx])
        cofx (reduce
               (fn [cofx [key & args]]
                 (assoc cofx key (apply ((.-co_effects reconciler) key) args)))
               {}
               cofx)
        state @reconciler
        effects (ctrl-fn event-key event-args (get state ctrl) cofx)]
    (js/console.log "running-citrus-default-handler" ctrl event-key)
    (m/doseq [effect (dissoc effects :state)]
      (let [[eff-type effect] effect]
        (when (s/check-asserts?)
          (when-let [spec (s/get-spec eff-type)]
            (s/assert spec effect)))
        (when-let [handler (get (.-effect_handlers reconciler) eff-type)]
          (handler reconciler ctrl effect))))
    (if (contains? effects :state)
      (assoc state ctrl (:state effects))
      state)))

(deftype Reconciler [#_default-handler controllers effect-handlers co-effects state queue scheduled? batched-updates chunked-updates meta watch-fns]

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
    (assert (contains? controllers cname) (str "Controller " cname " is not found"))
    (assert (some? event) (str "Controller " cname " was called without event name"))

    (queue-effects! queue [cname event args])

    (schedule-update!
      batched-updates
      scheduled?
      (fn []
        (let [events @queue
              _ (clear-queue! queue)]
          (reset! state
                  (loop [st @state
                         [event & events] events]
                    (if (seq event)
                      (let [[ctrl event args] event]
                        (recur (citrus-default-handler this ctrl event args) events))
                      st)))))))

  (dispatch-sync! [this cname event args]
    (assert (contains? controllers cname) (str "Controller " cname " is not found"))
    (assert (some? event) (str "Controller " cname " was called without event name"))

    (let [ret (citrus-default-handler this cname event args)]
      (when-let [new-ctrl-state (:state ret)]
        (swap! state assoc cname new-ctrl-state))))

  (broadcast! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch! this controller event args)))

  (broadcast-sync! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch-sync! this controller event args))))
