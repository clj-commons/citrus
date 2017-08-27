(ns routing.router
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]))

(defn start! [on-set-page routes]
  (let [history (pushy/pushy on-set-page (partial bidi/match-route routes))]
       (pushy/start! history)
       history))

(defn stop! [history]
  (pushy/stop! history))
