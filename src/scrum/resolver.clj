(ns scrum.resolver)

(deftype Resolver [state resolvers path reducer]

  clojure.lang.IDeref
  (deref [_]
    (let [resolve (get resolvers path)
          data (resolve)]
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

(defn make-resolver [state resolvers path reducer]
   (Resolver. state resolvers path reducer))

