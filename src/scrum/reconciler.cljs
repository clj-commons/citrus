(ns scrum.reconciler)

(defn- queue-update! [f queue]
  (vswap! queue conj f))

(defn- clear-queue! [queue]
  (vreset! queue []))


(defn- schedule-update! [schedule! f scheduled?]
  (vreset! scheduled? true)
  (schedule! #(when @scheduled? (f))))

(defn- unschedule! [scheduled?]
  (vreset! scheduled? nil))


(defprotocol IReconciler
  (dispatch! [this controller action args])
  (dispatch-sync! [this controller action args])
  (broadcast! [this action args])
  (broadcast-sync! [this action args]))

(deftype Reconciler [state meta]

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
   (let [app-state (:scrum/state state)
         queue (:scrum/queue state)
         scheduled? (:scrum/scheduled? state)
         batched-updates (or (:scrum/batched-updates state) js/requestAnimationFrame)
         chunked-updates (:scrum/chunked-updates state)]
    (queue-update!
     #(let [ctrl (get-in state [:scrum/controllers cname])
            cstate (get % cname)]
        (->> cstate
          (ctrl action args)
          (assoc % cname)))
     queue)
    (schedule-update!
     batched-updates
     #(swap! app-state
             (fn [old-state]
               (let [queue @(:scrum/queue state)]
                 (clear-queue! (:scrum/queue state))
                 (unschedule! scheduled?)
                 (reduce (fn [interim-state f] (f interim-state))
                         old-state
                         queue))))
     scheduled?)))

  (dispatch-sync! [this cname action args]
    (swap! (:scrum/state state)
           #(let [ctrl (get-in state [:scrum/controllers cname])
                  cstate (get % cname)]
              (->> cstate
                (ctrl action args)
                (assoc % cname)))))

  (broadcast! [this action args]
    (doseq [controller (keys (:scrum/controllers state))]
      (dispatch! this controller action args)))

  (broadcast-sync! [this action args]
    (doseq [controller (keys (:scrum/controllers state))]
      (dispatch-sync! this controller action args))))
