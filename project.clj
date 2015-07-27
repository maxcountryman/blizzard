(defproject blizzard "0.3.1"
  :description "HTTP Flake ID generate service."
  :url "https://github.com/maxcountryman/blizzard"
  :license {:name "BSD 3-Clause license"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[clout "1.2.0"]
                 [com.cognitect/transit-clj "0.8.247"]
                 [flake "0.3.0"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/clojure "1.6.0"]]
  :main blizzard.core
  :jvm-opts ^:replace ["-server"])
