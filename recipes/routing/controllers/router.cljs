(ns routing.controllers.router)

(defmulti control (fn [action _ _] action))

(defmethod control :init [_ [route] _]
  {:state route})

(defmethod control :push [_ [route] _]
  {:state route})
