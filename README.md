<img src="logo.png" width="251" height="36" alt="scrum logo" />

*State Coordination for [Rum](https://github.com/tonsky/rum/)*

## Table of Contents

- [Motivation](#motivation)
- [Features](#features)
- [Installation](#installation)
- [How it works](#how-it-works)
  - [Dispatcher](#dispatcher)
  - [Controllers](#controllers)
  - [Subscriptions](#subscriptions)
- [Usage](#usage)
- [License](#license)

## Motivation

Have a simple, [re-frame](https://github.com/Day8/re-frame) like state management facilities to build web apps with Rum library while leveraging its API.

## Features

- Decoupled application state in a single atom
- Reactive queries into the state
- A notion of *controller* to keep application domains separate

## Installation

Add to project.clj: `[org.roman01la/scrum "0.1.0-SNAPSHOT"]`

## How it works

With Scrum you build everything around a well known architecture pattern in modern SPA development:

*DISPATCH EVENT*

↓

*HANDLE EVENT*

↓

*QUERY STATE*

↓

*RENDER*

### Dispatcher

Dispatcher communicates intention to perform an action, whether it is state update or a network request.

```clojure
(scrum.dispatcher/dispatch! :controller-name :action-name &args)
```

### Controllers

Controller is a multimethod which performs intended actions against application state. A controller usually have at least an initial state and `:init` method.

```clojure
(def initial-state 0)

(defmulti control (fn [action] action))

(defmethod control :init [action &args db]
  (assoc db :counter initial-state))

(scrum.dispatcher/register! :counter control)
```

### Subscriptions

A subscription is a reactive query into application state. It is basically an atom which holds a part of the state value. Optional second argument is an aggregate function which computes a materialized view.

Actual subscription happens in Rum component via `rum/reactive` mixin and `rum/react` function which hooks in a watch function to update a component when an atom gets an update.

```clojure
(def fname (scrum.core/subscription [:users 0 :fname]))
(def full-name (scrum.core/subscription [:users 0] #(str (:fname %) " " (:lname %))))

;; usage
(rum/defc NameField < rum/reactive []
  [:div
   [:div.fname (rum/react fname)]
   [:div.full-name (rum/react full-name)]])
```

## Usage

```clojure
(ns counter.core
  (:require [rum.core :as rum]
            [scrum.dispatcher :as d]
            [scrum.core :refer [subscription]]))

;;
;; define controller & event handlers
;;

(def initial-state 0)

(defmulti control (fn [action] action))

(defmethod control :init [_ _ db]
  (assoc db :counter initial-state))

(defmethod control :inc [_ _ db]
  (update db :counter inc))

(defmethod control :dec [_ _ db]
  (update db :counter dec))


;;
;; define subscription
;;

(def counter (subscription [:counter]))


;;
;; define UI component
;;

;; create dispatcher for particular controller
(def dispatch-counter! (partial d/dispatch! :counter))

(rum/defc Counter < rum/reactive []
  [:div
   [:button {:on-click #(dispatch-counter! :dec)} "-"]
   [:span (rum/react counter)]
   [:button {:on-click #(dispatch-counter! :inc)} "+"]])


;;
;; start up
;;

;; register controller
(d/register! :counter control)

;; initialize registered controllers
(defonce dispatched-init (d/broadcast! :init))

;; render
(rum/mount (Counter)
           (. js/document (getElementById "app")))
```

## License

Copyright © 2017 Roman Liutikov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
