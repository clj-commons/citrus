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

(deftype Reconciler [controllers handler effect-handlers co-effects state queue scheduled? batched-updates chunked-updates meta watch-fns]

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

    (when-not (fn? handler)
      (assert (contains? controllers cname) (str "Controller " cname " is not found"))
      (assert (some? event) (str "Controller " cname " was called without event name"))
      (assert (-> (get controllers cname) methods (contains? event))
              (str "Controller " cname " doesn't declare " event " method")))

    (queue-effects!
      queue
      (if (fn? handler)
        [cname event #(handler cname event args %1 %2)]
        [cname event #((get controllers cname) event args (get %1 cname) %2)]))

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
                               (fn [cofx [key & args]]
                                 (assoc cofx key (apply (co-effects key) args)))
                               {}
                               cofx)
                        effects (ctrl st cofx)]
                    (m/doseq [effect (dissoc effects :state)]
                      (let [[id effect] effect]
                        (when (s/check-asserts?)
                          (when-let [spec (s/get-spec id)]
                            (s/assert spec effect)))
                        (when-let [effect-handler (get effect-handlers id)]
                          (effect-handler this cname effect))))
                    (cond
                      (and (contains? effects :state) (fn? handler))
                      (recur (:state effects) events)

                      (contains? effects :state)
                      (recur (assoc st cname (:state effects)) events)

                      :else
                      (recur st events)))
                  st))]
          (reset! state next-state)))))

  (dispatch-sync! [this cname event args]

    (when-not (fn? handler)
      (assert (contains? controllers cname) (str "Controller " cname " is not found"))
      (assert (some? event) (str "Controller " cname " was called without event name"))
      (assert (-> (get controllers cname) methods (contains? event))
              (str "Controller " cname " doesn't declare " event " method")))

    (let [ctrl (if (fn? handler)
                 handler
                 (get controllers cname))
          cofx (get-in (.-meta ctrl) [:citrus event :cofx])
          cofx (reduce
                 (fn [cofx [key & args]]
                   (assoc cofx key (apply (co-effects key) args)))
                 {}
                 cofx)
          effects (if (fn? handler)
                    (ctrl cname event args @state cofx)
                    (ctrl event args (get @state cname) cofx))]
      (m/doseq [effect effects]
        (let [[id effect] effect
              effect-handler (get effect-handlers id)]
          (when (s/check-asserts?)
            (when-let [spec (s/get-spec id)]
              (s/assert spec effect)))
          (cond
            (and (fn? handler) (= id :state))
            (reset! state effect)

            (= id :state)
            (swap! state assoc cname effect)

            effect-handler
            (effect-handler this cname effect)

            :else nil)))))

  (broadcast! [this event args]
    (if (fn? handler)
      (js/console.error "broadcast! doesn't work with :citrus/handler, do it manually on your side")
      (m/doseq [controller (keys controllers)]
        (dispatch! this controller event args))))

  (broadcast-sync! [this event args]
    (if (fn? handler)
      (js/console.error "broadcast-sync! doesn't work with :citrus/handler, do it manually on your side")
      (m/doseq [controller (keys controllers)]
        (dispatch-sync! this controller event args)))))
