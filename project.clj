(defproject witan-viz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure        "1.8.0"]
                 [org.clojure/clojurescript  "1.9.89"]
                 [reagent "0.6.0-rc"]
                 [binaryage/devtools "0.6.1"]
                 [re-frame "0.8.0"]
                 [re-com "0.8.3"]
                 [garden "1.3.2"]
                 [ns-tracker "0.3.0"]
                 [com.taoensso/timbre "4.7.0"]
                 [cljs-ajax "0.5.8"]
                 [com.fasterxml.jackson.core/jackson-core "2.6.6"]
                 [jarohen/chord "0.7.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [environ "1.0.2"]
                 [witan.gateway.schema "0.1.1"]
                 [thi.ng/geom "0.0.908"]]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-garden "0.2.8"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]
  :repl-options {:init-ns user}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "test/js"
                                    "out"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3448}

  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   witan-viz.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :profiles
  {:dev
   {:dependencies [[com.cemerick/piggieback "0.2.1"]
                   [org.clojure/tools.nrepl "0.2.12"]
                   [ring/ring-defaults "0.1.5"]
                   [compojure "1.4.0"]
                   [figwheel "0.5.4-3"]
                   [figwheel-sidecar "0.5.4-3"]]
    :source-paths ["dev-src"]
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
    :plugins      [[lein-figwheel "0.5.4-3"]
                   [lein-doo "0.1.6"]]}
   :data {:source-paths ["data-src"]
          :dependencies [[amazonica "0.3.73" :exclusions [com.fasterxml.jackson.core/jackson-core]]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "witan-viz.core/mount-root"}
     :compiler     {:main                 witan-viz.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true}}

    {:id           "prod"
     :source-paths ["src/cljs"]
     :compiler     {:main            witan-viz.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :externs ["../js/externs.js"]
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/compiled/test.js"
                    :main          witan-viz.runner
                    :optimizations :none}}]}
  :release-tasks [["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "release-v"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :aliases {"upload-data" ["with-profile" "data" "run" "-m" "witan-viz.upload-data"]})
