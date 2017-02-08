(ns counter.core
  (:require [rum.core :as rum]
            [scrum.dispatcher :as d]
            [scrum.core :refer [subscription]]))

;;
;; define controller & event handlers
;;

(def initial-state 0)

(defmulti control (fn [action] action))

(defmethod control :init [_ _ db]
  (assoc db :counter initial-state))

(defmethod control :inc [_ _ db]
  (update db :counter inc))

(defmethod control :dec [_ _ db]
  (update db :counter dec))


;;
;; define subscription
;;

(def counter (subscription [:counter]))


;;
;; define UI component
;;

;; create dispatcher for particular controller
(def dispatch-counter! (partial d/dispatch! :counter))

(rum/defc Counter < rum/reactive []
  [:div
   [:button {:on-click #(dispatch-counter! :dec)} "-"]
   [:span (rum/react counter)]
   [:button {:on-click #(dispatch-counter! :inc)} "+"]])


;;
;; start up
;;

;; register controller
(d/register! :counter control)

;; initialize registered controllers
(defonce dispatched-init (d/broadcast! :init))

;; render
(rum/mount (Counter)
           (. js/document (getElementById "app")))
