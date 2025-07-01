(ns cljtab.core
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljtab.completion :as completion]
            [cljtab.setup :as setup]))

(defn setup
  "Install bash completion integration - public tool function"
  []
  (setup/install-bash-completion))

(defn generate
  "Generate completion candidates for current directory - public tool function"
  []
  (completion/generate-candidates (System/getProperty "user.dir")))

(defn complete
  "Provide completion candidates for bash - public tool function"
  [{:keys [current-word prev-word all-words] :or {current-word "" prev-word "" all-words []}}]
  (let [current-dir (System/getProperty "user.dir")
        candidates (completion/get-completion-candidates current-dir current-word prev-word all-words)]
    (println (str/join " " candidates))))


(defn -main
  "Main entry point for cljtab command line tool"
  [& args]
  (case (first args)
    "setup" (setup)
    "generate" (generate)
    "complete" (let [parsed-args (-> (second args)
                                     (or "{}")
                                     edn/read-string)]
                 (complete parsed-args))
    (do (println "cljtab - Clojure CLI Tab Completion")
        (println "")
        (println "Usage: cljtab <command>")
        (println "")
        (println "Commands:")
        (println "  setup     Install bash completion integration")
        (println "  generate  Generate completion candidates for current directory")
        (println "  complete  Provide completion candidates for bash"))))
