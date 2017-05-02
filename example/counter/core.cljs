(ns counter.core
  (:require [rum.core :as rum]
            [scrum.core :as scrum]
            [goog.dom :as dom]))

;;
;; define controller & event handlers
;;

(def initial-state 0)

(defmulti control (fn [event] event))

(defmethod control :init []
  {:state initial-state})

(defmethod control :load [_ [key]]
  {:local-storage {:op :get
                   :key key
                   :on-success :load-ready}})

(defmethod control :load-ready [_ [counter]]
  {:state (int counter)})

(defmethod control :save [_ [key] counter]
  {:local-storage {:op :set
                   :value counter
                   :key key}})

(defmethod control :inc [_ _ counter]
  (let [next-counter (inc counter)]
    {:state next-counter
     :local-storage {:op :set
                     :value next-counter
                     :key :count}}))

(defmethod control :dec [_ _ counter]
  (let [next-counter (dec counter)]
    {:state next-counter
     :local-storage {:op :set
                     :value next-counter
                     :key :count}}))


;;
;; define UI component
;;

(rum/defc Counter < rum/reactive [r]
  [:div
   [:button {:on-click #(scrum/dispatch! r :counter :dec)} "-"]
   [:span (rum/react (scrum/subscription r [:counter]))]
   [:button {:on-click #(scrum/dispatch! r :counter :inc)} "+"]])


;;
;; define effects handler
;;

(defn local-storage [r c {:keys [op key value on-success]}]
  (case op
    :get
    (->> (name key)
         js/localStorage.getItem
         (scrum/dispatch! r c on-success))
    :set
    (-> (name key)
        (js/localStorage.setItem value))))


;;
;; start up
;;

;; create Reconciler instance
(defonce reconciler
  (scrum/reconciler
    {:state (atom {})
     :controllers {:counter control}
     :effect-handlers {:local-storage local-storage}}))

;; initialize controllers
(defonce init-ctrl (scrum/broadcast-sync! reconciler :init))

;; load from localStorage
(defonce load (scrum/dispatch-sync! reconciler :counter :load :count))

;; render
(rum/mount (Counter reconciler)
           (dom/getElement "app"))
