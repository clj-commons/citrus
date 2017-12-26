(ns citrus.reconciler
  (:require-macros [citrus.macros :as m]))

(defn- queue-effects! [queue f]
  (vswap! queue conj f))

(defn- clear-queue! [queue]
  (vreset! queue []))

(defn- schedule-update! [schedule! scheduled? f]
  (when-let [id @scheduled?]
    (vreset! scheduled? nil)
    (js/cancelAnimationFrame id))
  (vreset! scheduled? (schedule! f)))

(defprotocol IReconciler
  (dispatch! [this controller event args])
  (dispatch-sync! [this controller event args])
  (broadcast! [this event args])
  (broadcast-sync! [this event args]))

(deftype Reconciler [controllers effect-handlers state queue scheduled? batched-updates chunked-updates meta]

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
    (add-watch state (list this key)
      (fn [_ _ oldv newv]
        (when (not= oldv newv)
          (callback key this oldv newv))))
    this)

  (-remove-watch [this key]
    (remove-watch state (list this key))
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
    (queue-effects!
      queue
      [cname event #((get controllers cname) event args (get %1 cname) %2)])

    (schedule-update!
      batched-updates
      scheduled?
      (fn []
        (let [events @queue
              _ (clear-queue! queue)
              next-state
              (loop [st @state
                     [event & events] events]
                (if (seq event)
                  (let [[cname ename ctrl] event
                        cofx (get-in (.-meta ctrl) [:citrus ename :cofx])
                        cofx (reduce
                               (fn [cofx [key f]]
                                 (assoc cofx key (f)))
                               {}
                               cofx)
                        effects (ctrl st cofx)]
                    (m/doseq [[id effect] (dissoc effects :state)]
                             (when-let [handler (get effect-handlers id)]
                               (handler this cname effect)))
                    (if (contains? effects :state)
                      (recur (assoc st cname (:state effects)) events)
                      (recur st events)))
                  st))]
          (reset! state next-state)))))

  (dispatch-sync! [this cname event args]
    (let [ctrl (get controllers cname)
          cofx (get-in (.-meta ctrl) [:citrus event :cofx])
          cofx (reduce
                 (fn [cofx [key f]]
                   (assoc cofx key (f)))
                 {}
                 cofx)
          effects (ctrl event args (get @state cname) cofx)]
      (m/doseq [[id effect] effects]
        (let [handler (get effect-handlers id)]
          (cond
            (= id :state) (swap! state assoc cname effect)
            handler (handler this cname effect)
            :else nil)))))

  (broadcast! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch! this controller event args)))

  (broadcast-sync! [this event args]
    (m/doseq [controller (keys controllers)]
      (dispatch-sync! this controller event args))))
