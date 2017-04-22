(defproject org.roman01la/scrum "2.0.0-SNAPSHOT"
  :description "State Coordination for Rum"
  :url "https://github.com/roman01la/scrum"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.456" :scope "provided"]
                 [rum "0.10.8"]]

  :plugins [[lein-cljsbuild "1.1.5" :exclusions [[org.clojure/clojure]]]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "example"]
                :compiler {:main counter.core
                           :asset-path "target/dev"
                           :output-to "target/counter.js"
                           :output-dir "target/dev"
                           :compiler-stats true
                           :parallel-build true}}]})
