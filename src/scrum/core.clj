(ns scrum.core
  (:require [scrum.resolver :as r]))

(defn reconciler
  "Accepts an atom & a hash of subscription resolvers where keys are subscription path vectors and values are data resolving functions

    {[:counter] fetch-counter}

  Returns a hash of `resolvers` and `state` atom which will be populated with resolved subscriptions data during rendering"
  [resolvers state]
  {:state state
   :resolvers resolvers})

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
  ([reconciler path]
   (subscription reconciler path nil))
  ([{:keys [state resolvers]} path reducer]
   (r/make-resolver state resolvers path reducer)))

