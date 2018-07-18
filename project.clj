(defproject org.roman01la/citrus "3.1.0"
  :description "State management library for Rum"
  :url "https://github.com/roman01la/citrus"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.238" :scope "provided"]
                 [rum "0.11.2"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.15" :exclusions [org.clojure/clojure]]
            [lein-doo "0.1.8"]]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.15"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["src" "dev"]}}

  :aliases {"cljs-test" ["do"
                         ["clean"]
                         ["doo" "firefox" "test"]]}

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src" "example"]
                :figwheel     true
                :compiler     {:main           counter.core
                               :asset-path     "js/compiled/out"
                               :output-to      "resources/public/js/compiled/main.js"
                               :output-dir     "resources/public/js/compiled/out"
                               :compiler-stats true
                               :parallel-build true}}

               {:id           "min"
                :source-paths ["src" "example"]
                :compiler     {:main            counter.core
                               :output-to       "resources/public/js/compiled-min/main.js"
                               :output-dir      "resources/public/js/compiled-min/out"
                               :optimizations   :advanced
                               :closure-defines {"goog.DEBUG" false}
                               :static-fns      true
                               :elide-asserts   true
                               :output-wrapper  true
                               :compiler-stats  true
                               :parallel-build  true}}
               {:id           "test"
                :source-paths ["src" "test"]
                :compiler     {:output-to     "target/test.js"
                               :main          citrus.test-runner
                               :optimizations :none}}]}

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}})
