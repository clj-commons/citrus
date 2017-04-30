(ns scrum.cursor
  (:require [goog.object :as gobj]))

(deftype ReduceCursor [ref path reducer meta]
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
    (-> (-deref ref)
        (get-in path)
        (reducer)))

  IWatchable
  (-add-watch [this key callback]
    (add-watch ref (list this key)
      (fn [_ _ oldv newv]
        (let [old (reducer (get-in oldv path))
              new (reducer (get-in newv path))]
          (when (not= old new)
            (callback key this old new)))))
    this)

  (-remove-watch [this key]
    (remove-watch ref (list this key))
    this)

  IHash
  (-hash [this] (goog/getUid this))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer "#object [scrum.cursor.ReduceCursor ")
    (pr-writer {:val (-deref this)} writer opts)
    (-write writer "]")))

(defn reduce-cursor-in
  "Given atom with deep nested value, path inside it and reducing function, creates an atom-like structure
   that can be used separately from main atom, but only for reading value:

     (def state (atom {:users {\"Ivan\" {:children [1 2 3]}}}))
     (def ivan (scrum.cursor/reduce-cursor-in state [:users \"Ivan\" :children] last))
     (deref ivan) ;; => 3

  Returned value supports deref, watches and metadata.
  The only supported option is `:meta`"
  [ref path reducer & {:as options}]
  (ReduceCursor. ref path reducer (:meta options)))
