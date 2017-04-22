(ns scrum.resolver)

(deftype Resolver [resolve reducer]

  clojure.lang.IDeref

  (deref [_]
    (let [data (resolve)]
      (if reducer
        (reducer data)
        data))))

(defn make-resolver
  ([resolve]
   (make-resolver resolve nil))
  ([resolve reducer]
   (Resolver. resolve reducer)))

