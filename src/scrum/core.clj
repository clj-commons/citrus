(ns scrum.core
  (:require [scrum.resolver :refer [make-resolver]]))

(defn dispatch!
  "dummy dispatch!"
  [_ _ _ & _])

(defn dispatch-sync!
  "dummy dispatch-sync!"
  [_ _ _ & _])

(defn broadcast!
  "dummy broadcast!"
  [_ _ & _])

(defn broadcast-sync!
  "dummy broadcast-sync!"
  [_ _ & _])

(defn subscription
  "Create a subscription to state updates

    (scrum/subscription resolvers [:users 0] (juxt [:fname :lname]))

  Arguments

    resolvers  - a map of resolvers
    path       - a vector which describes a path into resolver's result value
    reducer    - an aggregate function which computes a materialized view of data behind the path"
  ([resolvers path]
   (subscription resolvers path nil))
  ([resolvers path reducer]
   (-> resolvers
       (get path)
       (make-resolver reducer))))

