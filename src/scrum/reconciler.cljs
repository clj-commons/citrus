(ns scrum.reconciler)

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
  (dispatch! [this controller action args])
  (dispatch-sync! [this controller action args])
  (broadcast! [this action args])
  (broadcast-sync! [this action args]))

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
    (-write writer "#object [scrum.reconciler.Reconciler ")
    (pr-writer {:val (-deref this)} writer opts)
    (-write writer "]"))

  IReconciler
  (dispatch! [this cname action args]
    (queue-effects!
      queue
      [cname ((get controllers cname) action args (get @state cname))])

    (schedule-update!
      batched-updates
      scheduled?
      (fn []
        (let [effects @queue]
          (clear-queue! queue)
          (when-let [state-effects (filter (comp :state second) effects)]
            (swap! state
              #(reduce (fn [agg [cname {cstate :state}]]
                         (assoc agg cname cstate))
                       % state-effects)))
          (doseq [[cname effects] effects]
            (doseq [[id effect] effects]
              (when-let [handler (get effect-handlers id)]
                (handler this cname effect))))))))

  (dispatch-sync! [this cname action args]
    (let [effects ((get controllers cname) action args (get @state cname))]
      (doseq [[id effect] effects]
        (let [handler (get effect-handlers id)]
          (cond
            (= id :state) (swap! state assoc cname effect)
            handler (handler this cname effect)
            :else nil)))))

  (broadcast! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch! this controller action args)))

  (broadcast-sync! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch-sync! this controller action args))))
