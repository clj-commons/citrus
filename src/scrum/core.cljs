(ns scrum.core
  (:require [rum.core :as rum]
            [scrum.reconciler :as r]
            [scrum.rum.cursor :refer [reduce-cursor-in]]))

(defn reconciler
  "Creates an instance of Reconciler

    (scrum/reconciler (atom {}) {:counter counter})

  Arguments

    state       - an atom
    controllers - a hash of state controllers

  Returned value supports deref, swap!, reset!, watches and metadata.
  The only supported option is `:meta`"
  [state controllers & {:as options}]
  (r/Reconciler. state controllers (:meta options)))

(defn dispatch!
  "Invoke an action on particular controller

    (scrum/dispatch! reconciler :users :load \"id\")

  Arguments

    reconciler - an instance of Reconciler
    controller - name of a controller
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler controller action & args]
  (r/dispatch! reconciler controller action args))

(defn broadcast!
  "Invoke an action on all controllers

    (scrum/broadcast! reconciler :init)

  Arguments

    reconciler - an instance of Reconciler
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler action & args]
  (r/broadcast! reconciler action args))


(defn subscription
  "Create a subscription to state updates

    (scrum/subscription reconciler [:users 0] (juxt [:fname :lname]))

  Arguments

    reconciler - an instance of Reconciler
    path       - a vector which describes a path into reconciler's atom value
    reducer    - an aggregate function which computes a materialized view of data behind the path"
  ([reconciler path]
   (rum/cursor-in reconciler path))
  ([reconciler path reducer]
   (reduce-cursor-in reconciler path reducer)))
