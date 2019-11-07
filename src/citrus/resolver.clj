(ns citrus.resolver
  (:require [manifold.deferred :as d]))

;; changed from
;; https://github.com/clj-commons/citrus/blob/master/src/citrus/resolver.clj

(deftype Resolver [state resolver path reducer]

  clojure.lang.IDeref
  (deref [_]
    (let [[key & path] path
          ;; why get ? what if resolver is a fn?
          resolve (get resolver key (fn [] (throw (ex-info "Resolver not found" {:key key}))))]
      ;; async by default - rum/reactive will deref again
      (d/chain (resolve)
               (fn [data]
                 (when state
                   (swap! state assoc key data))
                 data)
               (fn [data]
                 (if reducer
                   (reducer (get-in data path))
                   (get-in data path))))))

  clojure.lang.IRef
  (setValidator [this vf]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/setValidator")))

  (getValidator [this]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/getValidator")))

  (getWatches [this]
    (throw (UnsupportedOperationException. "citrus.resolver.Resolver/getWatches")))

  (addWatch [this key callback]
    this)

  (removeWatch [this key]
    this))

(defn make-resolver [state resolver path reducer]
  (Resolver. state resolver path reducer))
