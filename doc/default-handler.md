# Default Handlers

Default handlers are a way to adapt Citrus' event handling to users needs that
haven't been anticipated by the framework. Initially they have been motivated
by [the need to access all controllers state in event
handlers](https://github.com/clj-commons/citrus/issues/50) but theoretically
they open up Citrus' event handling to many more customizations. 

:warning: Default handlers are experimental and subject to change. Please
report your experiences using them [in this GitHub issue]().

### Usage 

When constructing a reconciler the `:default-handler` option can be used to 
pass a default handler.

- `:default-handler` should be a function like 

	(fn handler [reconciler ctrl-key event-key event-args])

- When not passing in anything for this option, the handler will default to
  `citrus.reconciler/citrus-default-handler`, which behaves identical to the
  regular event handling of Citrus as of `v3.2.3`.
- The handler function is expected to return a new value for the `:state` atom
  of the reconciler.

### Recipes

#### Passing the entire state as fifth argument

Event handlers currently take four arguments `[controller-kw event-kw
event-args co-effects]`. As described above one motivation for default handlers
has been to give event handlers access to the entire state of the application.

By implementing a new default handler based on [`citrus.reconciler/citrus-default-handler`]() we can change how our controller multimethods are called, passing an additional argument containing the applications full state.

> Co-effects are largely undocumented right now and might be removed in a
> future release. Please [add a note to this
> issue](https://github.com/clj-commons/citrus/issues/51) if you are using
> them.

```diff
-(defn citrus-default-handler
-  "Implements Citrus' default event handling (as of 3.2.3)."
+(defn pass-full-state-handler
+  "Implements Citrus' default event handling (as of 3.2.3) but passes
+  the applications entire state as a fifth argument to controller methods."
   [reconciler ctrl event-key event-args]
   (assert (contains? (.-controllers reconciler) ctrl)
           (str "Controller " ctrl " is not found"))
   (let [ctrl-fn (get (.-controllers reconciler) ctrl)
         cofx (get-in (.-meta ctrl) [:citrus event-key :cofx])
         cofx (reduce
                (fn [cofx [key & args]]
                  (assoc cofx key (apply ((.-co_effects reconciler) key) args)))
                {}
                cofx)
         state @reconciler
-        effects (ctrl-fn event-key event-args (get state ctrl) cofx)]
+        effects (ctrl-fn event-key event-args (get state ctrl) cofx state)]
     (m/doseq [effect (dissoc effects :state)]
       (let [[eff-type effect] effect]
         (when (s/check-asserts?)
           (when-let [spec (s/get-spec eff-type)]
             (s/assert spec effect)))
         (when-let [handler (get (.-effect_handlers reconciler) eff-type)]
           (handler reconciler ctrl effect))))
     (if (contains? effects :state)
       (assoc state ctrl (:state effects))
       state)))
```

#### Non-multimethod based handlers

TBD 

- include mixture w/ controller based handlers
- note how non-controller handlers won't work with broadcast

### Appendix

#### Why return `state` from default handler instead of calling `reset!` within handler?

Citrus processes events that are dispatched asynchronously in batches. As
Citrus processes one batch of events the new app state is computed and then
`reset!` into the state atom.  If handlers would call `reset!` themselves this
could in theory cause multiple recomputations of subscriptions. More
investigation would be useful to diagnose if this is an actual issue.
