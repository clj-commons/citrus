(ns scrum.reconciler)

(defprotocol IReconciler
  (dispatch! [this controller action args])
  (broadcast! [this action args]))

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

  IReset
  (-reset! [_ newv]
    (-swap! state (constantly newv)))

  ISwap
  (-swap! [this f]
    (reset! state (f (-deref state))))
  (-swap! [this f a]
    (reset! state (f (-deref state) a)))
  (-swap! [this f a b]
    (reset! state (f (-deref state) a b)))
  (-swap! [this f a b rest]
    (reset! state (apply f (-deref state) a b rest)))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer "#object [scrum.reconciler.Reconciler ")
    (pr-writer {:val (-deref this)} writer opts)
    (-write writer "]"))

  IReconciler
  (dispatch! [this cname action args]
    (-swap! this #(let [ctrl (get controllers cname)
                        cstate (get % cname)]
                    (->> cstate
                      (ctrl action args)
                      (assoc % cname)))))

  (broadcast! [this action args]
    (doseq [controller (keys controllers)]
      (dispatch! this controller action args))))
