(defproject clogo "0.1.0-SNAPSHOT"
  :description "LOGO interpreter written in Clojure controlling a Lego NXT2 turtle robot"
  :url "http://chopp.in/clj/clogo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [seesaw "1.4.3-SNAPSHOT"]]
  :aot [clogo.core]
  :main clogo.core
  :profiles {:nxt {:dependencies [[lejos.pc/pccomm "0.9.1-beta3"]
                                  [net.sf.bluecove/bluecove "2.1.1-SNAPSHOT"]]
                   :aot [clogo.core-nxt]
                   :main clogo.core_nxt}})
