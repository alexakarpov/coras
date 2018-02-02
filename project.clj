(defproject coras "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-kafka "0.3.4"]
                 [yogthos/config "1.1"]
                 [clj-time "0.14.2"]]
  :plugins [[lein-pprint "1.2.0"]]
  :profiles {:prod {:resource-paths ^:replace ["config/prod"]
                    :plugins [[cider/cider-nrepl "0.16.0"]]}
             :dev  {:resource-paths ["config/dev"]
                    :plugins [[cider/cider-nrepl "0.16.0"]]}
             :uberjar {:aot :all}}
  :main ^:skip-aot coras.core
  :target-path "target/%s")
