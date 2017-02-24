(ns scrum.reconciler)

(defn- queue-update! [f queue]
  (vswap! queue conj f))

(defn- clear-queue! [queue]
  (vreset! queue []))


(defn- schedule-update! [schedule! f scheduled?]
  (vreset! scheduled? true)
  (schedule! #(when @scheduled? (f))))

(defn- unschedule! [scheduled?]
  (vreset! scheduled? false))


(defprotocol IReconciler
  (dispatch! [this controller action args])
  (dispatch-sync! [this controller action args])
  (broadcast! [this action args])
  (broadcast-sync! [this action args]))

(deftype Reconciler [controllers state queue scheduled? batched-updates chunked-updates meta]

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
    (queue-update! #(let [ctrl (get controllers cname)]
                      (update % cname (partial ctrl action args)))
                   queue)
    (schedule-update! batched-updates
     #(swap! state
             (fn [old-state]
               (let [q @queue]
                 (clear-queue! queue)
                 (unschedule! scheduled?)
                 (reduce (fn [agg-state f] (f agg-state)) old-state q))))
     scheduled?))

  (dispatch-sync! [this cname action args]
    (swap! state #(let [ctrl (get controllers cname)]
                   (update % cname (partial ctrl action args)))))

  (broadcast! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch! this controller action args)))

  (broadcast-sync! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch-sync! this controller action args))))
