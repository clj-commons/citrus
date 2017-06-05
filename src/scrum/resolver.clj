(ns scrum.resolver)

(deftype Resolver [state resolver path reducer]

  clojure.lang.IDeref
  (deref [_]
    (let [data (resolver path)]
      (when state
        (swap! state assoc-in path data))
      (if reducer
        (reducer data)
        data)))

  clojure.lang.IRef
  (setValidator [this vf]
    (throw (UnsupportedOperationException. "scrum.resolver.Resolver/setValidator")))

  (getValidator [this]
    (throw (UnsupportedOperationException. "scrum.resolver.Resolver/getValidator")))

  (getWatches [this]
    (throw (UnsupportedOperationException. "scrum.resolver.Resolver/getWatches")))

  (addWatch [this key callback]
    this)

  (removeWatch [this key]
    this))

(defn make-resolver [state resolver path reducer]
   (Resolver. state resolver path reducer))
