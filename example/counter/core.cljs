(ns counter.core
  (:require [rum.core :as rum]
            [scrum.core :as scrum]))

;;
;; define controller & action handlers
;;

(def initial-state 0)

(defmulti control (fn [action] action))

(defmethod control :init []
  initial-state)

(defmethod control :inc [_ _ counter]
  (inc counter))

(defmethod control :dec [_ _ counter]
  (dec counter))


;;
;; define UI component
;;

(rum/defc Counter < rum/reactive [r]
  [:div
   [:button {:on-click #(scrum/dispatch! r :counter :dec)} "-"]
   [:span (rum/react (scrum/subscription r [:counter]))]
   [:button {:on-click #(scrum/dispatch! r :counter :inc)} "+"]])


;;
;; start up
;;

;; create Reconciler instance
(defonce reconciler
  (scrum/reconciler {:state (atom {})
                     :controllers {:counter control}}))

;; initialize controllers
(defonce init-ctrl (scrum/broadcast-sync! reconciler :init))

;; render
(rum/mount (Counter reconciler)
           (. js/document (getElementById "app")))
