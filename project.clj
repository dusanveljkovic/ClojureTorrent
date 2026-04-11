(defproject djirtorrent "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [cljfx "1.9.3"]
            [clj-http "3.13.0"]
                 [aleph "0.8.2"]
                 [manifold "0.4.3"]
                 [org.clj-commons/byte-streams "0.3.4"]
                 [com.taoensso/timbre "6.6.1"]
                 [bytebuffer "0.2.0"]
                 [gloss "0.2.6"]
                 ]
  :repl-options {:init-ns djirtorrent.core})
