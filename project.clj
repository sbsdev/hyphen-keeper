(defproject hyphen-keeper "0.1.0-SNAPSHOT"
  :description "A small web app to keep a whitelist of approved hyphenation patterns"
  :url "https://github.com/sbsdev/hyphen-keeper"
  :license {:name "GNU Affero General Public License"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.xml "0.1.0-beta3"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.5.0-alpha.1"]
                 [org.immutant/web "2.1.10" :exclusions [ring/ring-core]]
                 [ring-server "0.5.0"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]]
                 [ring-middleware-accept "2.0.3"]
                 [compojure "1.6.1" :exclusions [ring/ring-codec]]
                 [hiccup "1.0.5"]
                 [yesql "0.5.3" :exclusions [instaparse]]
                 [mysql/mysql-connector-java "8.0.15"]
                 [org.daisy.bindings/jhyphen "1.0.0"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [yogthos/config "1.1.1"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.4"]
                 [cljs-ajax "0.8.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [org.tobereplaced/nio.file "0.4.0"]
                 [com.taoensso/tempura "1.2.1"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-immutant "2.1.0"]
            [lein-cljsbuild "1.1.7"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]

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
            }
   }


  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware [cider.piggieback/wrap-cljs-repl
                      cider.nrepl/cider-middleware
                      refactor-nrepl.middleware/wrap-refactor
                      ]
   :css-dirs ["resources/public/css"]
   :ring-handler hyphen-keeper.handler/site}

  :git-version {:root-ns "hyphen-keeper"
                :path "env/prod/clj/hyphen_keeper"}
  :immutant {:war {:context-path "/hyphenations"
                   :name "%p%v%t"
                   :nrepl {:port 40021
                           :start? true}}}

  :profiles {:dev {:repl-options {:init-ns hyphen-keeper.repl}
                   :dependencies [[ring/ring-mock "0.3.2" :exclusions [cheshire ring/ring-codec]]
                                  [ring/ring-devel "1.7.1"]
                                  [prone "1.6.1"]
                                  [figwheel-sidecar "0.5.18" :exclusions [args4j]]
                                  [cider/piggieback "0.4.0"]
                                  [pjstadig/humane-test-output "0.9.0"]
                                  ]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.18" :exclusions [org.clojure/clojure]]
                             [cider/cider-nrepl "0.21.1"]
                             [org.clojure/tools.namespace "0.3.0-alpha2"
                              :exclusions [org.clojure/tools.reader]]
                             [refactor-nrepl "2.4.0"
                              :exclusions [org.clojure/clojure]]
                             ]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :uberjar {:source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
