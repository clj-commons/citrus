(ns routing.core
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [goog.dom :as dom]
            [routing.controllers.router :as router-ctrl]
            [routing.router :as router]
            [routing.components :refer [Root]]))

(def routes
  ["/" [["" :home]
        ["settings" :settings]
        [["user/" :id] :user]]])

(def r
  (citrus/reconciler
    {:state (atom {})
     :controllers {:router router-ctrl/control}}))

(citrus/broadcast-sync! r :init)

(router/start! #(citrus/dispatch! r :router :push %) routes)

(rum/mount (Root r) (dom/getElement "app"))
