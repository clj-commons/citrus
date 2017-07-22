<img src="logo.png" width="251" height="36" alt="scrum logo" />

*State Coordination for [Rum](https://github.com/tonsky/rum/)*

<a href="http://clojurians.net/">
  <img src="slack.png" width="64" height="64" />
  <span>Discuss on Slack #scrum</span>
</a>

## Table of Contents

- [Motivation](#motivation)
- [Features](#features)
- [Apps built with Scrum](#apps-built-with-scrum)
- [Installation](#installation)
- [Usage](#usage)
- [How it works](#how-it-works)
  - [Reconciler](#reconciler)
  - [Dispatching events](#dispatching-events)
  - [Handling events](#handling-events)
  - [Side effects](#side-effects)
  - [Subscriptions](#subscriptions)
  - [Scheduling and batching](#scheduling-and-batching)
  - [Server-side rendering](#server-side-rendering)
- [Best practices](#best-practices)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Motivation

Have a simple, [re-frame](https://github.com/Day8/re-frame) like state management facilities for building web apps with [Rum](https://github.com/tonsky/rum/) while leveraging its API.

## Features

⚛️ Decoupled application state in a single atom

📦 No global state, everything lives in `Reconciler` instance

🎛 A notion of a *controller* to keep application domains separate

🚀 Reactive queries

📋 Side-effects are described as data

⚡️ Async batched updates for better performance

🚰 Server-side rendering with convenient state hydration

## Apps built with Scrum

- [Hacker News clone with server-side rendering](https://github.com/roman01la/scrum-ssr-example)
- [“Real world” example app](https://github.com/roman01la/cljs-rum-realworld-example-app)

## Installation

Add to *project.clj* / *build.boot*: `[org.roman01la/scrum "2.2.0-SNAPSHOT"]`

## Usage

```clojure
(ns counter.core
  (:require [rum.core :as rum]
            [scrum.core :as scrum]))

;;
;; define controller & event handlers
;;

(def initial-state 0) ;; initial state

(defmulti control (fn [event] event))

(defmethod control :init []
  {:local-storage
   {:method :get
    :key :counter
    :on-read :init-ready}}) ;; read from local storage

(defmethod control :init-ready [_ [counter]]
  (if-not (nil? counter)
    {:state counter} ;; init with saved state
    {:state initial-state})) ;; or with predefined initial state

(defmethod control :inc [_ _ counter]
  (let [next-counter (inc counter)]
    {:state next-counter ;; update state
     :local-storage
     {:method :set
      :data next-counter
      :key :counter}})) ;; persist to local storage

(defmethod control :dec [_ _ counter]
  (let [next-counter (dec counter)]
    {:state next-counter ;; update state
     :local-storage
     {:method :set
      :data next-counter
      :key :counter}})) ;; persist to local storage


;;
;; define effect handler
;;

(defn local-storage [reconciler controller-name effect]
  (let [{:keys [method data key on-read]} effect]
    (case method
      :set (js/localStorage.setItem (name key) data)
      :get (->> (js/localStorage.getItem (name key))
                (scrum/dispatch! reconciler controller-name on-read))
      nil)))


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
  (scrum/reconciler
    {:state
     (atom {}) ;; application state
     :controllers
     {:counter control} ;; controllers
     :effect-handlers
     {:local-storage local-storage}})) ;; effect handlers

;; initialize controllers
(defonce init-ctrl (scrum/broadcast-sync! reconciler :init))

;; render
(rum/mount (Counter reconciler)
           (. js/document (getElementById "app")))
```

## How it works

With _Scrum_ you build everything around a well known architecture pattern in modern SPA development:

📦 *Model application state* (with `reconciler`)

📩 *Dispatch events* (with `dispatch!`, `dispatch-sync!`, `broadcast!` and `broadcast-sync!`)

📬 *Handle events* (with `:controllers` functions)

🕹 *Handle side effects* (with `:effect-handlers` functions)

🚀 *Query state reactively* (with `subscription`, `rum/react` and `rum/reactive`)

✨ *Render* (automatic & efficient ! profit :+1:)

### Reconciler

Reconciler is the core of _Scrum_. An instance of `Reconciler` takes care of application state, handles events, side effects and subscriptions, and performs async batched updates (via `requestAnimationFrame`):

```clojure
(defonce reconciler
  (scrum/reconciler {:state (atom {})
                     :controllers {:counter control}
                     :effect-handlers {:http http}}))
```

**:state**

The value at the `:state` key is the initial state of the reconciler represented as an atom which holds a hash map. The atom is created and passed explicitly.

**:controllers**

The value at the `:controllers` key is a hash map from controller name to controller function. The controller stores its state in reconciler's `:state` atom at the key which is the name of the controller in `:controllers` hash map. That is, the keys in `:controllers` are reflected in the `:state` atom. This is where modeling state happens and application domains keep separated.

Usually controllers are initialized with a predefined initial state value by dispatching `:init` event.

*NOTE*: the `:init` event pattern isn't enforced at all in _Scrum_, but we consider it is a good idea for 2 reasons:
- it separates setup of the reconciler from initialization phase, because initialization could happen in several ways (hardcoded, read from global JSON/Transit data rendered into HTML from the server, user event, etc.)
- allows setting a global watcher on the atom for ad-hoc stuff outside of the normal _Scrum_ cycle for maximum flexibility

**:effect-handlers**

The value at the `:effect-handlers` key is a hash map of side effect handlers. Handler function asynchronously performs impure computations such as state change, HTTP request, etc. The only built-in effects handler is `:state`, everything else should be implemented and provided by user.

### Dispatching events

Dispatched events communicate intention to perform a side effect, whether it is updating the state or performing a network request. By default effects are executed asynchronously, use `dispatch-sync!` when synchronous execution is required:

```clojure
(scrum.core/dispatch! reconciler :controller-name :event-name &args)
(scrum.core/dispatch-sync! reconciler :controller-name :event-name &args)
```

`broadcast!` and its synchronous counterpart `broadcast-sync!` should be used to broadcast an event to all controllers:

```clojure
(scrum.core/broadcast! reconciler :event-name &args)
(scrum.core/broadcast-sync! reconciler :event-name &args)
```

### Handling events

A controller is a multimethod that returns effects. It usually has at least an initial state and `:init` event method.
An effect is key/value pair where the key is the name of the effect handler and the value is description of the effect that satisfies particular handler.

```clojure
(def initial-state 0)

(defmulti control (fn [event] event))

(defmethod control :init [event args state]
  {:state initial-state})

(defmethod control :inc [event args state]
  {:state (inc state)})

(defmethod control :dec [event args state]
  {:state (dec state)})
```

It's important to understand that `state` value that is passed in won't affect the whole state, but only the part corresponding to its associated key in the `:controllers` map of the reconciler.

### Side effects

A side effect is an impure computation e.g. state mutation, HTTP request, storage access, etc. Because handling side effects is inconvenient and usually leads to cumbersome code, this operation is pushed outside of user code. In *Scrum* you don't perform effects directly in controllers. Instead controller methods return a hash map of effects represented as data. In every entry of the map the key is a name of the corresponding effects handler and the value is a description of the effect.

Here's an example of an effect that describes HTTP request:

```clojure
{:http {:url "/api/users"
        :method :post
        :body {:name "John Doe"}
        :headers {"Content-Type" "application/json"}
        :on-success :create-user-ready
        :on-error :create-user-failed}}
```

And corresponding handler function:

```clojure
(defn http [reconciler ctrl-name effect]
  (let [{:keys [on-success on-error]} effect]
    (-> (fetch effect)
        (then #(scrum/dispatch! reconciler ctrl-name on-success %))
        (catch #(scrum/dispatch! reconciler ctrl-name on-error %)))))
```

Handler function accepts three arguments: reconciler instance, the name key of the controller which produced the effect and the effect value itself.

Notice how the above effect provides callback event names to handle HTTP response/error which are dispatched once request is done. This is a frequent pattern when it is expected that an effect can produce another one e.g. update state with response body.

*NOTE*: `:state` is the only handler built into *Scrum*. Because state change is the most frequently used effect it is handled a bit differently, in efficient way (see [Scheduling and batching](#scheduling-and-batching) section).

### Subscriptions

A subscription is a reactive query into application state. It is an atom which holds a part of the state value retrieved with provided path. Optional second argument is an aggregate function that computes a materialized view. You can also do parameterized and aggregate subscriptions.

Actual subscription happens in Rum component via `rum/reactive` mixin and `rum/react` function which hooks in a watch function to update a component when an atom gets updated.

```clojure
;; normal subscription
(defn fname [reconciler]
  (scrum.core/subscription reconciler [:users 0 :fname]))

;; a subscription with aggregate function
(defn full-name [reconciler]
  (scrum.core/subscription reconciler [:users 0] #(str (:fname %) " " (:lname %))))

;; parameterized subscription
(defn user [reconciler id]
  (scrum.core/subscription reconciler [:users id]))

;; aggregate subscription
(defn discount [reconciler]
  (scrum.core/subscription reconciler [:user :discount]))

(defn goods [reconciler]
  (scrum.core/subscription reconciler [:goods :selected]))

(defn shopping-cart [reconciler]
  (rum/derived-atom [(discount reconciler) (goods reconciler)] ::key
    (fn [discount goods]
      (let [price (->> goods (map :price) (reduce +))]
        (- price (* discount (/ price 100)))))))

;; usage
(rum/defc NameField < rum/reactive [reconciler]
  (let [user (rum/react (user reconciler 0))])
    [:div
     [:div.fname (rum/react (fname reconciler))]
     [:div.lname (:lname user)]
     [:div.full-name (rum/react (full-name reconciler))]
     [:div (str "Total: " (rum/react (shopping-cart reconciler)))]])
```

### Scheduling and batching

This section describes how effects execution works in *Scrum*. It is considered an advanced topic and is not necessary to read to start working with *Scrum*.

**Scheduling**

Events dispatched using `scrum/dispatch!` are always executed asynchronously. Execution is scheduled via `requestAnimationFrame` meaning that events that where dispatched in 16ms timeframe will be executed sequentially by the end of this time.

```clojure
;; |--×-×---×---×--|---
;; 0ms            16ms
```

**Batching**

Once 16ms timer is fired a queue of scheduled events is being executed to produce a sequence of effects. This sequence is then divided into two: state updates and other side effects. First, state updates are executed in a single `swap!`, which triggers only one re-render, and after that other effects are being executed.

```clojure
;; queue = [state1 http state2 local-storage]

;; state-queue = [state1 state2]
;; other-queue = [http local-storage]

;; swap! reduce old-state state-queue → new-state
;; doseq other-queue
```

### Server-side rendering

Server-side rendering in *Scrum* doesn't require any changes in UI components code, the API is the same. However it works differently under the hood when the code is executed in Clojure.

Here's a list of the main differences from client-side:
- reconciler accepts subscriptions resolving function and optional `:state` atom
- subscriptions are resolved synchronously
- controllers are not used
- all dispatching functions are disabled

**subscriptions resolver**

To understand what is *subscription resolving function* let's start with a small example:

```clojure
;; used in both Clojure & ClojureScript
(rum/defc Counter < rum/reactive [r]
  [:div
   [:button {:on-click #(scrum/dispatch! r :counter :dec)} "-"]
   [:span (rum/react (scrum/subscription r [:counter]))]
   [:button {:on-click #(scrum/dispatch! r :counter :inc)} "+"]])
```

```clojure
;; server only
(let [state (atom {})
      r (scrum/reconciler {:state state
                           :resolvers resolver})] ;; create reconciler
  (->> (Counter r) ;; initialize components tree
       rum/render-html ;; render to HTML
       (render-document @state))) ;; render into document template
```

```clojure
;; server only
(defmulti resolver (fn [[key]] key))

(defmethod resolver :counter [_] ;; [:counter] subscription resolving function
  0)
```

`resolver` is a function or a multimethod, which is being called by Scrum's subscriptions when rendering on server. Resolver function receives subscription path vector that is defined when creating a subscription. Normally resolving function would access database or any other data source used on the backend. While it's fine to use a function, we recommend to use a multimethod since it is a good abstraction for decoupling data resolvers.

**resolver**

A value returned from resolving function is stored in `Resolver` instance which is atom-like type that is used under the hood in subscriptions.

**resolved data**

In the above example you may have noticed that we create `state` atom, pass it into reconciler and then dereference it once rendering is done. When rendering on server *Scrum* collects resolved data into an atom behind `:state` key of the reconciler, if the atom is provided. This data should be rendered into HTML to rehydrate the app once it is initialized on the client-side.

*NOTE*: in order to retrieve resolved data the atom should be dereferenced only after `rum/render-html` call.

**synchronous subscriptions**

Every subscription created inside of components that are being rendered triggers corresponding resolving function which blocks rendering until a value is returned. The downside is that the more subscriptions there are down the components tree, the more time it will take to render the whole app. On the other hand it makes it possible to both render and retrieve state in one render pass. To reduce rendering time make sure you don't have too much subscriptions in components tree. Usually it's enough to have one or two in root component for every route.

**request bound caching**
If you have multiple subscriptions to same data source in UI tree you'll see that data is also fetched multiple times when rendering on server. To reduce database access load it's recommended to reuse data from resolved subscriptions. Here's an implementation of a simple cache:

```clojure
(defn make-resolvers [resolver req]
  (let [cache (volatile! {})] ;; cache
    (fn [[key & p :as path]]
      (if-let [data (get-in @cache path)] ;; cache hit
        (get-in data p) ;; return data from cache
        (let [data (resolver [key] req)] ;; cache miss, resolve subscription
          (vswap! cache assoc key data) ;; cache data
          (get-in data p))))))
```

## Best practices

- Pass the reconciler explicity from parent components to children. Since it is a reference type it won't affect `rum/static` (`shouldComponentUpdate`) optimization. But if you prefer to do it _Redux-way_, you can use context in _Rum_ as well https://github.com/tonsky/rum/#interop-with-react
- Set up the initial state value by `broadcast-sync!`ing an `:init` event before first render. This enforces controllers to keep state initialization in-place where they are defined.
- Use a multimethod as resolver function when rendering on server.

## Testing

Testing state management logic in *Scrum* is really simple. Here's what can be tested:
- controllers output (effects)
- state changes

*NOTE:* Using synchronous dispatch `scrum.core/dispatch-sync!` makes it easier to test state updates.

```clojure
(ns app.controllers.counter)

(defmulti control (fn [event] event))

(defmethod control :init [_ [initial-state] _]
  {:state initial-state})

(defmethod control :inc [_ _ counter]
  {:state (inc counter)})

(defmethod control :dec [_ _ counter]
  {:state (dec counter)})
```

```clojure
(ns app.test.controllers.counter-test
  (:require [clojure.test :refer :all]
            [scrum.core :as scrum]
            [app.controllers.counter :as counter]))

(def state (atom {}))

(def r
  (scrum/reconciler
    {:state state
     :controllers
     {:counter counter/control}}))

(deftest counter-control
  (testing "Should return initial-state value"
    (is (= (counter/control :init 0 nil) {:state 0})))
  (testing "Should return incremented value"
    (is (= (counter/control :inc nil 0) {:state 1})))
  (testing "Should return decremented value"
    (is (= (counter/control :dec nil 1) {:state 0}))))

(deftest counter-state
  (testing "Should initialize state value with 0"
    (scrum/dispatch-sync! r :counter :init 0)
    (is (zero? (:counter @state))))
  (testing "Should increment state value"
    (scrum/dispatch-sync! r :counter :inc)
    (is (= (:counter @state) 1)))
  (testing "Should deccrement state value"
    (scrum/dispatch-sync! r :counter :dec)
    (is (= (:counter @state) 0))))
```

## Roadmap
- <strike>Get rid of global state</strike>
- Make scrum isomorphic (in progress, see [this issue](https://github.com/roman01la/scrum/issues/3))
- Storage agnostic architecture? (Atom, DataScript, etc.)
- Better effects handling (network, localStorage, etc.) (in progress, see [this issue](https://github.com/roman01la/scrum/issues/7))
- Provide better developer experience using `clojure.spec`

## Contributing

If you've encountered an issue or want to request a feature or any other kind of contribution, please file an issue and provide detailed description.

This project is using [Leiningen](https://leiningen.org/) build tool, make sure you have it installed.

Run tests with `lein test`.

## License

Copyright © 2017 Roman Liutikov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
