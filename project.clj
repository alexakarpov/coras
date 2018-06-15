(defproject eplk "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-kafka "0.3.4"]
		 [metosin/compojure-api "2.0.0-alpha18"]
		 [ring "1.7.0-RC1"]
                 [yogthos/config "1.1"]
                 [clj-time "0.14.2"]
                 ]
  :plugins [[lein-pprint "1.2.0"]
            [cider/cider-nrepl "0.17.0"]
            [lein-ring "0.12.4"]]
  :ring {:handler eplk.core/app
         :nrepl {
                 :start? true
                 :port 6710
                 }}
  :profiles {:prod {:resource-paths ^:replace ["config/prod"]
                    :plugins [[cider/cider-nrepl "0.17.0"]]}
             :dev  {:resource-paths ["config/dev"]
                    :plugins [[cider/cider-nrepl "0.17.0"]]}
             :uberjar {:aot :all}}
  :main ^:skip-aot eplk.core
  :target-path "target/%s")
