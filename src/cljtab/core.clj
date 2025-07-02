(ns cljtab.core
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljtab.completion :as completion]
            [cljtab.setup :as setup]))

(defn setup
  "Install shell integration for cljtab"
  []
  (setup/setup (System/getenv "SHELL")))

(defn clean
  "Clean up shell integration and cache files of cljtab"
  []
  (setup/clean))

(defn generate
  "Generate cljtab completion candidates for current directory and cache them"
  []
  (completion/generate (System/getProperty "user.dir")))

(defn complete
  "Provide completion candidates for Clojure CLI based on candidates in cache"
  [{:keys [current-word prev-word all-words] :or {current-word "" prev-word "" all-words []}}]
  (let [current-dir (System/getProperty "user.dir")
        candidates (completion/get-completion-candidates current-dir current-word prev-word all-words)]
    (println (str/join " " candidates))))

(defn -main
  [& args]
  (case (first args)
    "setup" (setup)
    "clean" (clean)
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
        (println "  setup      " (:doc (meta #'setup)))
        (println "  clean      " (:doc (meta #'clean)))
        (println "  generate   " (:doc (meta #'generate)))
        (println "  complete   " (:doc (meta #'complete))))))
