(ns scrum.core
  (:require [rum.core :as rum]
            [scrum.db :refer [db]]
            [scrum.rum.cursor :refer [reduce-cursor-in]]))

(defn subscription
  "Create a subscription to state updates"
  ([path]
   (rum/cursor-in db path))
  ([path reducer]
   (reduce-cursor-in db path reducer)))
