(ns scrum.reconciler)

(defn- queue-update! [f queue]
  (vswap! queue conj f))

(defn- clear-queue! [queue]
  (vreset! queue []))


(defn- schedule-update! [f scheduled?]
  (vreset! scheduled? true)
  (js/requestAnimationFrame #(when @scheduled? (f))))

(defn- unschedule! [scheduled?]
  (vreset! scheduled? nil))


(defprotocol IReconciler
  (dispatch! [this controller action args])
  (dispatch-sync! [this controller action args])
  (broadcast! [this action args])
  (broadcast-sync! [this action args]))

(deftype Reconciler [state controllers meta]

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
    (-deref (:scrum/state state)))

  IWatchable
  (-add-watch [this key callback]
    (add-watch (:scrum/state state) (list this key)
      (fn [_ _ oldv newv]
        (when (not= oldv newv)
          (callback key this oldv newv))))
    this)

  (-remove-watch [this key]
    (remove-watch (:scrum/state state) (list this key))
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
    (console.log "QUEUE")
    (queue-update!
     #(let [ctrl (get controllers cname)
            cstate (get % cname)]
        (->> cstate
          (ctrl action args)
          (assoc % cname)))
     (:scrum/queue state))
    (schedule-update!
     #(swap! (:scrum/state state)
             (fn [old-state]
               (console.log "UPDATE")
               (let [queue @(:scrum/queue state)]
                 (clear-queue! (:scrum/queue state))
                 (unschedule! (:scrum/scheduled? state))
                 (reduce (fn [interim-state f] (f interim-state))
                         old-state
                         queue))))
     (:scrum/scheduled? state)))

  (dispatch-sync! [this cname action args]
    (swap! (:scrum/state state)
           #(let [ctrl (get controllers cname)
                  cstate (get % cname)]
              (->> cstate
                (ctrl action args)
                (assoc % cname)))))

  (broadcast! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch! this controller action args)))

  (broadcast-sync! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch-sync! this controller action args))))
