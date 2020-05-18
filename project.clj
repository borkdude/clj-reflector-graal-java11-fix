(defproject borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-19.3.2"
  :description "A fix for an issue with clojure.lang.Reflector in GraalVM native-image JDK11."
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"}
  :url "https://github.com/borkdude/clj-reflector-graal-java11-fix"
  :scm {:name "git"
        :url "https://github.com/borkdude/clj-reflector-graal-java11-fix"}
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [org.graalvm.nativeimage/svm "19.3.2"]]
  :java-source-paths ["src-java"]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
