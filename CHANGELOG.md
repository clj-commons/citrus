## 3.2.0
- Add more tests @DjebbZ
- Add a single watch to the state per reconciler @DjebbZ
- Add assertions for dispatching functions
- Assert speced effects when corresponding spec exists

## 3.1.0
- Added deps.edn
- Rum 0.11.2
- ClojureScript 1.10.238
- Provide both a schedule-fn and release-fn to customize batched updates 4f9a07d
- Implement co-effects via `citrus.core/defhandler` macro

## 1.0.0-SNAPHOST
- add Reconciler type to get rid of global state [7afd576](https://github.com/roman01la/citrus/commit/7afd576b512d53f3846beb8fca1bcd06066ac289)
- remove ISwap & IReset protocols impl and introduce updates batching & scheduling [00dd5cc](https://github.com/roman01la/citrus/commit/0fdbe26539ccae3a06b2b5c41c7abddf269bc2cb)
- expose Reconciler API for sync updates [0fdbe26](https://github.com/roman01la/citrus/commit/926df2f4cec96185bbcfc7d0dade2f7c8b59cf1d)
- make global schedule and queue local to reconciler instance [41c6e57](https://github.com/roman01la/citrus/commit/e3b6d960012738cff47e28b3181f837c0dd428a0)
- add pluggable batched-updates & chunked-updates fns [e3b6d96](https://github.com/roman01la/citrus/commit/ef2c24130fbd693f24629d58f44fc5b8dd8a6280)
- perform latest scheduled update [b55d48e](https://github.com/roman01la/citrus/commit/31ded3c6327d09a8c16a007ae6d28e5d84500fcf)

## 0.1.0
- Initial release
