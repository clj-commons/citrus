(ns citrus.resolver)

(deftype Resolver [state resolver path reducer]

  clojure.lang.IDeref
  (deref [_]
    (let [[key & path] path
          resolve (get resolver key)
          data (resolve)]
      (when state
        (swap! state assoc key data))
      (if reducer
        (reducer (get-in data path))
        (get-in data path))))

  clojure.lang.IRef
  (setValidator [this vf]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/setValidator")))

  (getValidator [this]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/getValidator")))

  (getWatches [this]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/getWatches")))

  (addWatch [this key callback]
    this)

  (removeWatch [this key]
    this))

(defn make-resolver [state resolver path reducer]
   (Resolver. state resolver path reducer))
