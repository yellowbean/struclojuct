(defproject clojucture "0.0.2-SNAPSHOT"
  :description "a clojure library for modelling & analysis structure products (CLO/MBS/ABS)"
  :url "https://yellowbean.github.io/clojucture/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.10.0"]
    [tech.tablesaw/tablesaw-core "0.30.1"]
    [org.apache.commons/commons-math3 "3.6.1"]
    [org.apache.commons/commons-lang3 "3.7"]
    [clojure.java-time "0.3.2"]
    [org.clojure/data.csv "0.1.4"]
    [org.clojure/data.json "0.2.6"]
    [org.clojure/data.zip "0.1.2"]
   ]
  ;:plugins [
  ;          [lein-codox "0.10.4"]]
  ;:codox
  ;  {:output-path "docs"
  ;   :metadata {:doc/format :markdown} }

  :profiles {:dev {:dependencies [[midje "1.9.2" :exclusions [org.clojure/clojure]]]
                   :plugins [[lein-midje "3.2.1"]]}}
  :mirrors {"central" {:name "aliyun maven"
                       :url "https://maven.aliyun.com/nexus/content/groups/public/"}}
  ;:source-paths [ "src/clojucture"]
  ;:java-source-paths [
  ;                    "src/java"
  ;  ]
  ;:javac-options     ["-target" "1.8" "-source" "1.8"]
  )
