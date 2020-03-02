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
  ```clj
  (fn handler [reconciler events])
  ;; where `events` is a list of event tuples like this one
  [[ctrl-key event-key event-args]   ; event 1
   [ctrl-key event-key event-args]]  ; event 2
  ```

- When not passing in anything for this option, the handler will default to
  `citrus.reconciler/citrus-default-handler`, which behaves identical to the
  regular event handling of Citrus as of `v3.2.3`.
- The default handler will process all events of the current batch before
  resetting the reconciler `state`. This reduces the number of watch triggers
  for subscriptions and similar tools using `add-watch` with the reconciler
  `state` atom.

### Writing your own handler

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
```

#### Non-multimethod based handlers

With the ability to override the `default-handler` the `controller` and `effect-handlers` options almost become superfluous as all behavior influenced by these options can now also be controlled via `default-handler`. 

- include mixture w/ controller based handlers
- note how non-controller handlers won't work with broadcast
