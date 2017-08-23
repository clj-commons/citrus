(defproject org.roman01la/citrus "3.0.0"
  :description "State management library for Rum"
  :url "https://github.com/roman01la/citrus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.521" :scope "provided"]
                 [rum "0.10.8"]]

  :plugins [[lein-cljsbuild "1.1.6" :exclusions [[org.clojure/clojure]]]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "example"]
                :compiler {:main counter.core
                           :asset-path "target/dev"
                           :output-to "target/counter.js"
                           :output-dir "target/dev"
                           :compiler-stats true
                           :parallel-build true}}
               {:id "min"
                :source-paths ["src" "example"]
                :compiler {:main counter.core
                           :output-to "target/counter.js"
                           :output-dir "target/min"
                           :optimizations :advanced
                           :closure-defines {"goog.DEBUG" false}
                           :static-fns true
                           :elide-asserts true
                           :output-wrapper true
                           :compiler-stats true
                           :parallel-build true}}]})
