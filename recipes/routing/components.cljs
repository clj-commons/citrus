(ns routing.components
  (:require [rum.core :as rum]
            [citrus.core :as citrus]))

(rum/defc Root < rum/reactive [r]
  (let [{route :handler params :route-params} (rum/react (citrus/subscription r [:router]))]
    (case route
          :home "Home view"
          :settings "Settings view"
          :user (str "User #" (:id params) " view")
          nil)))
