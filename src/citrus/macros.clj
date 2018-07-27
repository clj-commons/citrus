(ns citrus.macros
  (:refer-clojure :exclude [doseq]))

(defmacro doseq
  "Lightweight `doseq`"
  [[item coll] & body]
  `(loop [xs# ~coll]
     (when-some [~item (first xs#)]
       (do ~@body)
       (recur (next xs#)))))
