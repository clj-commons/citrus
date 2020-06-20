(ns citrus.core
  (:require-macros [citrus.macros :as m])
  (:require [citrus.reconciler :as r]
            [citrus.cursor :as c]))

(defn- -get-default-batched-updates
  []
  {:schedule-fn js/requestAnimationFrame
   :release-fn  js/cancelAnimationFrame})

(defn reconciler
  "Creates an instance of Reconciler

    (citrus/reconciler {:state (atom {})
                        :controllers {:counter counter/control}
                        :effect-handlers {:http effects/http}
                        :batched-updates {:schedule-fn f :release-fn f'}
                        :chunked-updates f})

  Arguments

    config              - a map of
      state             - app state atom
      controllers       - a map of state controllers
      citrus/handler    - a function to handle incoming events (see doc/custom-handler.md)
      effect-handlers   - a map of effects handlers
      batched-updates   - a map of two functions used to batch reconciler updates, defaults to
                          `{:schedule-fn js/requestAnimationFrame :release-fn js/cancelAnimationFrame}`
      chunked-updates   - a function used to divide reconciler update into chunks, doesn't used by default

  Returned value supports deref, watches and metadata.
  The only supported option is `:meta`"
  [{:keys [state controllers effect-handlers co-effects batched-updates chunked-updates]
    :citrus/keys [handler]}
   & {:as options}]
  (binding []
    (let [watch-fns (volatile! {})
          rec (r/->Reconciler
                controllers
                (or handler r/citrus-default-handler)
                effect-handlers
                co-effects
                state
                (volatile! [])
                (volatile! nil)
                (or batched-updates (-get-default-batched-updates))
                chunked-updates
                (:meta options)
                watch-fns)]
      (add-watch state (list rec :watch-fns)
                 (fn [_ _ oldv newv]
                   (when (not= oldv newv)
                     (m/doseq [w @watch-fns]
                       (let [[k watch-fn] w]
                         (watch-fn k rec oldv newv))))))
      rec)))

(defn dispatch!
  "Invoke an event on particular controller asynchronously

    (citrus/dispatch! reconciler :users :load \"id\")

  Arguments

    reconciler - an instance of Reconciler
    controller - name of a controller
    event      - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler controller event & args]
  {:pre [(instance? r/Reconciler reconciler)]}
  (r/dispatch! reconciler controller event args))

(defn dispatch-sync!
  "Invoke an event on particular controller synchronously

    (citrus/dispatch! reconciler :users :load \"id\")

  Arguments

    reconciler - an instance of Reconciler
    controller - name of a controller
    event      - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler controller event & args]
  {:pre [(instance? r/Reconciler reconciler)]}
  (r/dispatch-sync! reconciler controller event args))

(defn broadcast!
  "Invoke an event on all controllers asynchronously

    (citrus/broadcast! reconciler :init)

  Arguments

    reconciler - an instance of Reconciler
    event      - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler event & args]
  {:pre [(instance? r/Reconciler reconciler)]}
  (r/broadcast! reconciler event args))

(defn broadcast-sync!
  "Invoke an event on all controllers synchronously

    (citrus/broadcast! reconciler :init)

  Arguments

    reconciler - an instance of Reconciler
    event      - a dispatch value of a method defined in the controller
    args       - arguments to be passed into the controller"
  [reconciler event & args]
  {:pre [(instance? r/Reconciler reconciler)]}
  (r/broadcast-sync! reconciler event args))


(defn subscription
  "Create a subscription to state updates

    (citrus/subscription reconciler [:users 0] (juxt [:fname :lname]))

  Arguments

    reconciler - an instance of Reconciler
    path       - a vector which describes a path into reconciler's atom value
    reducer    - an aggregate function which computes a materialized view of data behind the path"
  ([reconciler path]
   (subscription reconciler path identity))
  ([reconciler path reducer]
   {:pre [(instance? r/Reconciler reconciler)]}
   (c/reduce-cursor-in reconciler path reducer)))
