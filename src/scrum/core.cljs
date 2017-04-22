(ns scrum.core
  (:require [rum.core :as rum]
            [scrum.reconciler :as r]
            [scrum.rum.cursor :refer [reduce-cursor-in]]))

(defn reconciler
  "Creates an instance of Reconciler

    (scrum/reconciler {:state (atom {})
                       :controllers {:counter counter}
                       :batched-updates f
                       :chunked-updates f})

  Arguments

    config              - a map of
      state             - app state atom
      controllers       - a map of state controllers
      batched-updates   - a function used to batch reconciler updates, defaults to `js/requestAnimationFrame`
      chunked-updates   - a function used to divide reconciler update into chunks, doesn't used by default

  Returned value supports deref, watches and metadata.
  The only supported option is `:meta`"
  [{:keys [state controllers batched-updates chunked-updates]} & {:as options}]
  (r/Reconciler.
    controllers
    state
    (volatile! [])
    (volatile! nil)
    (or batched-updates js/requestAnimationFrame)
    chunked-updates
    (:meta options)))

(defn dispatch!
  "Invoke an action on particular controller asynchronously

    (scrum/dispatch! reconciler :users :load \"id\")

  Arguments

    reconciler - an instance of Reconciler
    controller - name of a controller
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler controller action & args]
  (r/dispatch! reconciler controller action args))

(defn dispatch-sync!
  "Invoke an action on particular controller synchronously

    (scrum/dispatch! reconciler :users :load \"id\")

  Arguments

    reconciler - an instance of Reconciler
    controller - name of a controller
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler controller action & args]
  (r/dispatch-sync! reconciler controller action args))

(defn broadcast!
  "Invoke an action on all controllers asynchronously

    (scrum/broadcast! reconciler :init)

  Arguments

    reconciler - an instance of Reconciler
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler action & args]
  (r/broadcast! reconciler action args))

(defn broadcast-sync!
  "Invoke an action on all controllers synchronously

    (scrum/broadcast! reconciler :init)

  Arguments

    reconciler - an instance of Reconciler
    action     - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler action & args]
  (r/broadcast-sync! reconciler action args))


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
