(ns counter.core
  (:require-macros [citrus.core :as citrus])
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [goog.dom :as dom]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]))

;;
;; enable printing readable specs
;;

(s/check-asserts true)
(set! s/*explain-out* expound/printer)

;;
;; spec effects
;;

(s/def :http/url string?)
(s/def :http/on-ok keyword?)
(s/def :http/on-failed keyword?)

(s/def :effect/http
  (s/keys :req-un [:http/url :http/on-ok :http/on-failed]))

;;
;; define controller & event handlers
;;

(defmulti control-github (fn [event] event))

(defmethod control-github :init [_ _ state]
  (let [state (assoc state :github {:repos    []
                                    :loading? false
                                    :error    nil})]
    {:state state}))

(defmethod control-github :fetch-repos [_ [username] state]
  {:effect/http {:url       (str "https://api.github.com/users/" username "/repos")
                 :on-ok     :fetch-repos-ok
                 :on-failed :fetch-repos-failed}
   :state       (-> state
                    (assoc-in [:github :error] nil)
                    (assoc-in [:github :loading?] true))})

(defmethod control-github :fetch-repos-ok [_ [resp] state]
  {:state (-> state
              (assoc-in [:github :repos] resp)
              (assoc-in [:github :loading?] false))})

(defmethod control-github :fetch-repos-failed [_ [error] state]
  {:state (-> state
              (assoc-in [:github :repos] [])
              (assoc-in [:github :error] (.-message error))
              (assoc-in [:github :loading?] false))})

(def controllers
  {:github control-github})

(defn handler [ctrl event args state cofx]
  (let [ctrl (get controllers ctrl)]
    (ctrl event args state cofx)))

(defn broadcast-sync! [r event & args]
  (doseq [ctrl (keys controllers)]
    (citrus/dispatch-sync! r ctrl event args)))

;;
;; define UI component
;;

(rum/defcs App <
  rum/reactive
  (rum/local "" :github/username)
  [{username :github/username}
   r]
  (let [{:keys [repos loading? error]} (rum/react (citrus/subscription r [:github]))]
    [:div {:style {:font-size   "12px"
                   :font-family "sans-serif"}}
     [:form {:on-submit #(.preventDefault %)}
      [:input {:value     @username
               :on-change #(reset! username (.. % -target -value))
               :style     {:padding       "8px"
                           :border-radius "3px"
                           :font-size     "14px"
                           :outline       "none"
                           :border        "1px solid blue"}}]
      [:button {:on-click #(citrus/dispatch! r :github :fetch-repos @username)
                :style    {:border-radius    "5px"
                           :outline          "none"
                           :font-size        "11px"
                           :background-color "blue"
                           :color            "#fff"
                           :border           "none"
                           :padding          "8px 16px"
                           :display          "block"
                           :margin           "8px 0"
                           :text-transform   "uppercase"
                           :font-weight      700}}
       "Fetch repos"]]
     (cond
       loading? "Fetching repos..."
       (some? error) error

       (seq repos)
       [:ul {:style {:margin 0}}
        (for [repo repos]
          [:li (:name repo)])]

       :else nil)]))


;;
;; define effects handler
;;

(defn http [r c {:keys [url on-ok on-failed]}]
  (-> (js/fetch url)
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then #(citrus/dispatch! r c on-ok %))
      (.catch #(citrus/dispatch! r c on-failed %))))


;;
;; start up
;;

;; create Reconciler instance
(defonce reconciler
         (citrus/reconciler
           {:state           (atom {})
            :citrus/handler  handler
            :effect-handlers {:effect/http http}}))

;; initialize controllers
(defonce init-ctrl (broadcast-sync! reconciler :init))

;; render
(rum/mount (App reconciler)
           (dom/getElement "app"))
