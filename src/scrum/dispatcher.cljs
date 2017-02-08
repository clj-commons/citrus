(ns scrum.dispatcher
  (:require [scrum.db :refer [db]]))

(defonce controllers (atom {}))

(defn- transact!
  "Apply controller to state value and update state with a returning value"
  [control action args]
  (reset! db (control action args @db)))

(defn broadcast!
  "Broadcast an event to all registered controllers"
  [action & args]
  (doseq [control (vals @controllers)]
    (transact! control action args)))

(defn dispatch!
  "Dispatch an event to a particular controller"
  [name action & args]
  (transact! (get @controllers name) action args))

(defn register!
  "Register a controller"
  [name control]
  (swap! controllers assoc name control))
