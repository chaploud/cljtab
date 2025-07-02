(ns cljtab.core
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljtab.completion :as completion]
            [cljtab.setup :as setup]))

(defn setup
  "Install shell completion integration - public tool function"
  ([]
   (setup/install-completion))
  ([shell-type]
   (setup/install-completion shell-type)))

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
    "setup" (if (second args)
              (setup (second args))
              (setup))
    "generate" (generate)
    "complete" (let [parsed-args (-> (second args)
                                     (or "{}")
                                     edn/read-string)]
                 (complete parsed-args))
    (do (println "cljtab - Clojure CLI Tab Completion")
        (println "")
        (println "Usage: cljtab <command> [options]")
        (println "")
        (println "Commands:")
        (println "  setup [shell]  Install shell completion integration")
        (println "                 shell: bash, zsh, both (default: auto-detect)")
        (println "  generate       Generate completion candidates for current directory")
        (println "  complete       Provide completion candidates for shell"))))
