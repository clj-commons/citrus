(ns user
  (:require [figwheel-sidecar.repl-api :as f]))

(defn start! []
  (f/start-figwheel!)
  (f/cljs-repl))

(defn stop! []
  (f/stop-figwheel!))
