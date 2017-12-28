(defproject org.roman01la/citrus "3.1.0-SNAPSHOT"
  :description "State management library for Rum"
  :url "https://github.com/roman01la/citrus"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [rum "0.10.8"]]

  :plugins [[lein-cljsbuild "1.1.6" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.14" :exclusions [org.clojure/clojure]]]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [binaryage/devtools "0.9.7"]
                                  [figwheel-sidecar "0.5.14"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["src" "dev"]}}

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
                               :parallel-build  true}}]})
