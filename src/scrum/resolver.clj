(ns scrum.resolver)

(deftype Resolver [state resolver path reducer]

  clojure.lang.IDeref
  (deref [_]
    (let [data (resolver path)]
      (when state
        (swap! state assoc-in path data))
      (if reducer
        (reducer data)
        data))))

(defn make-resolver [state resolver path reducer]
   (Resolver. state resolver path reducer))

