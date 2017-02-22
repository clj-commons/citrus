(ns scrum.reconciler)

(def queue (volatile! []))
(def schedule (volatile! []))

(defn- queue-update! [f]
  (vswap! queue conj f))

(defn- schedule-update! [f]
  (vswap! schedule conj f)
  (js/requestAnimationFrame #(when-not (zero? (count @schedule)) (f))))


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
    (queue-update! #(let [ctrl (get controllers cname)
                          cstate (get % cname)]
                      (->> cstate
                        (ctrl action args)
                        (assoc % cname))))
    (schedule-update!
     #(swap! state (fn [state]
                     (let [new-state (reduce (fn [state f] (f state)) state @queue)]
                       (vreset! queue [])
                       (vreset! schedule [])
                       new-state)))))

  (dispatch-sync! [this cname action args]
    (swap! state #(let [ctrl (get controllers cname)
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
