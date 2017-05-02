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
        data))))

(defn make-resolver [state resolvers path reducer]
   (Resolver. state resolvers path reducer))

