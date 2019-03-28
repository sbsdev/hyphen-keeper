(defproject hyphen-keeper "0.1.0-SNAPSHOT"
  :description "A small web app to keep a whitelist of approved hyphenation patterns"
  :url "https://github.com/sbsdev/hyphen-keeper"
  :license {:name "GNU Affero General Public License"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.1.0-beta3"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.immutant/web "2.1.6"]
                 [ring-server "0.4.0"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-middleware-accept "2.0.3"]
                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [yesql "0.5.3"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.daisy.bindings/jhyphen "1.0.0"]
                 [org.clojure/clojurescript "1.9.229" :scope "provided"]
                 [reagent "0.6.0"]
                 [reagent-utils "0.2.0"]
                 [yogthos/config "0.8"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.1.7" :exclusions [org.clojure/tools.reader]]
                 [cljs-ajax "0.5.8"]
                 [org.tobereplaced/nio.file "0.4.0"]
                 [com.taoensso/tempura "1.0.0"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-immutant "2.1.0"]
            [lein-cljsbuild "1.1.1"]
            [lein-asset-minifier "0.4.6"
             :exclusions [org.clojure/clojure]]
            [org.clojars.cvillecsteele/lein-git-version "1.0.3"]]

  :ring {:handler hyphen-keeper.handler/app
         :uberwar-name "hyphen-keeper.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "hyphen-keeper.jar"

  :main hyphen-keeper.server

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  [[:css {:source "resources/public/css/site.css"
          :target "resources/public/css/site.min.css"}]]

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :compiler
             {:output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/uberjar"
              :optimizations :advanced
              :pretty-print  false
              :closure-defines {hyphen-keeper.core/context-path "/hyphenations"}}}
            :app
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :compiler
             {:main "hyphen-keeper.dev"
              :asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :source-map true
              :optimizations :none
              :pretty-print  true}}


            :devcards
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:devcards true}
             :compiler {:main "hyphen-keeper.cards"
                        :asset-path "js/devcards_out"
                        :output-to "target/cljsbuild/public/js/app_devcards.js"
                        :output-dir "target/cljsbuild/public/js/devcards_out"
                        :source-map-timestamp true
                        :optimizations :none
                        :pretty-print true}}
            }
   }


  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                      "cider.nrepl/cider-middleware"
                      "refactor-nrepl.middleware/wrap-refactor"
                      ]
   :css-dirs ["resources/public/css"]
   :ring-handler hyphen-keeper.handler/app}

  :immutant {:war {:context-path "/hyphenations"
                   :name "%p%v%t"
                   :nrepl {:port 40021
                           :start? true}}}

  :profiles {:dev {:repl-options {:init-ns hyphen-keeper.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.5.0"]
                                  [prone "1.1.2"]
                                  [figwheel-sidecar "0.5.8"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                  [devcards "0.2.1-7"]
                                  [pjstadig/humane-test-output "0.8.1"]
                                  ]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.8"]
                             [cider/cider-nrepl "0.15.0-SNAPSHOT"]
                             [org.clojure/tools.namespace "0.3.0-alpha2"
                              :exclusions [org.clojure/tools.reader]]
                             [refactor-nrepl "2.3.0-SNAPSHOT"
                              :exclusions [org.clojure/clojure]]
                             ]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
