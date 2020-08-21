# Citrus Handler

Citrus event handling is implemented via a handler function that processes
a batch of events. By default Citrus handles events with a controller/multimethod-based system.
This can be customized by passing an alternative handler function as
`:citrus/handler` when creating a reconciler instance.

By providing a custom `:citrus/handler` you can adapt your event handling
in ways that haven't been anticipated by the Citrus framework.

Initially this customization option has been motivated by [the wish to access
all controllers state in event handlers](https://github.com/clj-commons/citrus/issues/50).

:bulb: This feature is experimental and subject to change. Please report your
experiences using them [in this GitHub issue](https://github.com/clj-commons/citrus/issues/50).

### Usage

When constructing a reconciler the `:citrus/handler` option can be used to
pass a custom handler that processes a batch of events.

- `:citrus/handler` should be a function like
  ```clj
  (fn handler [reconciler events])
  ;; where `events` is a list of event tuples like this one
  [[ctrl-key event-key event-args]   ; event 1
   [ctrl-key event-key event-args]]  ; event 2
  ```

- When not passing in anything for this option, the handler will default to
  [`citrus.reconciler/citrus-default-handler`](https://github.com/clj-commons/citrus/blob/220d6608c62e5deb91f0efb3ea37a6e435807148/src/citrus/reconciler.cljs#L17-L55), which behaves identical to the
  regular event handling of Citrus as of `v3.2.3`.
- The handler will process all events of the current batch before
  resetting the reconciler `state`. This reduces the number of watch triggers
  for subscriptions and similar tools using `add-watch` with the reconciler
  `state` atom.

#### Open extension

With the ability to override the `citrus/handler` the `controller` and
`effect-handlers` options almost become superfluous as all behavior influenced
by these options can now also be controlled via `:citrus/handler`. Some ideas
for things that are now possible to build on top of Citrus that previously
weren't:

- Mix controller handlers with custom event handling logic, i.e. any function. This could mean:
  - Having handlers that use [interceptors](https://github.com/metosin/sieppari) to customize their behavior
  - Having handlers that can write to the full app state instead of a subtree (in contrast to controllers that can only write to their respective subtree)
- Completely replace Citrus multimethod dispatching with a custom handler registry

> **Note** that breaking out of controllers as Citrus provides them impacts how
> Citrus' `broadcast` functions work. `broadcast!` and `broadcast-sync!` rely
> on what is being passed to the reconciler as `:controllers`.

### Recipes

:wave: Have you used `:citrus/handler` to do something interesting? Open a PR and share your approach here!

#### Passing the entire state as fourth argument

Event handlers currently take four arguments `[controller-kw event-kw
event-args co-effects]`. As described above one motivation for custom handlers
has been to give event handlers access to the entire state of the application.

By implementing a new handler based on [`citrus.reconciler/citrus-default-handler`](https://github.com/clj-commons/citrus/blob/220d6608c62e5deb91f0efb3ea37a6e435807148/src/citrus/reconciler.cljs#L17-L55) we can change how our controller multimethods are called, replacing the `co-effects` argument with the full state of the reconciler.

> Co-effects are largely undocumented right now and might be removed in a
> future release. Please [add a note to this
> issue](https://github.com/clj-commons/citrus/issues/51) if you are using
> them.

:point_right: Here's [**a commit**](https://github.com/clj-commons/citrus/commit/a620e8e77a62b16a9d6006600cccd02dda82c046) that adapts Citrus' default handler to pass the reconciler's full state as the fourth argument. Part of the diff is replicated below:

```diff
diff --git a/src/citrus/reconciler.cljs b/src/citrus/reconciler.cljs
index f8de8c5..5a95a77 100644
--- a/src/citrus/reconciler.cljs
+++ b/src/citrus/reconciler.cljs
@@ -14,17 +14,15 @@
     (release-fn id))
   (vreset! scheduled? (schedule-fn f)))
 
-(defn citrus-default-handler
-  "Implements Citrus' default event handling (as of 3.2.3).
-
-  This function can be copied into your project and adapted to your needs.
+(defn adapted-default-handler
+  "An adapted event handler for Citrus that passes the entire reconciler
+  state as fourth argument to controller methods.
 
   `events` is expected to be a list of events (tuples):
 
      [ctrl event-key event-args]"
   [reconciler events]
   (let [controllers (.-controllers reconciler)
-        co-effects (.-co_effects reconciler)
         effect-handlers (.-effect_handlers reconciler)
         state-atom (.-state reconciler)]
     (reset!
@@ -36,13 +34,7 @@
           (do
             (assert (contains? controllers ctrl) (str "Controller " ctrl " is not found"))
             (let [ctrl-fn (get controllers ctrl)
-                  cofx (get-in (.-meta ctrl) [:citrus event-key :cofx])
-                  cofx (reduce
-                         (fn [cofx [k & args]]
-                           (assoc cofx k (apply (co-effects k) args)))
-                         {}
-                         cofx)
-                  effects (ctrl-fn event-key event-args (get state ctrl) cofx)]
+                  effects (ctrl-fn event-key event-args (get state ctrl) state)]
               (m/doseq [effect (dissoc effects :state)]
                 (let [[eff-type effect] effect]
                   (when (s/check-asserts?)
```
