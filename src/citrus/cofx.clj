(ns citrus.cofx)

(require '[clojure.spec.alpha :as s])

(defn parse-defhandler [args]
  (s/conform
    (s/cat
      :ctrl-name symbol?
      :event-name keyword?
      :meta (s/? map?)
      :args (s/spec
              (s/cat
                :event some?
                :args some?
                :state some?
                :cofx some?))
      :body (s/* some?))
    args))

(defmacro defhandler [& args]
  (let [result (parse-defhandler args)]
    (if (not= result ::s/invalid)
      (let [{:keys [ctrl-name event-name meta args body]} result
            {:keys [event args state cofx]} args]
        `(do
           (set! (.-meta ~ctrl-name) (assoc-in (.-meta ~ctrl-name) [:citrus ~event-name] ~meta))
           (defmethod ~ctrl-name ~event-name [~event ~args ~state ~cofx] ~@body))))))

(comment
  (defmulti control (fn [event _ _ _] event))

  (defhandler control :fetch-profiles
    {:cofx {:local-store #(identity "auth-token")}}
    [_ [account-id] state coeffects]
    {:state (assoc state :loading? true)})

  (-> (meta #'control) :citrus :fetch-profiles))
