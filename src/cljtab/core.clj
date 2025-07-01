(ns cljtab.core
  (:require [clojure.string :as str]
            [cljtab.completion :as completion]
            [cljtab.setup :as setup]))

(defn setup
  "Install bash completion integration - public tool function"
  [_args]
  (setup/install-bash-completion))

(defn generate
  "Generate completion candidates for current directory - public tool function"
  [_args]
  (completion/generate-candidates (System/getProperty "user.dir")))

(defn complete
  "Provide completion candidates for bash - public tool function"
  [{:keys [current-word prev-word all-words] :or {current-word "" prev-word "" all-words []}}]
  (let [current-dir (System/getProperty "user.dir")
        candidates (completion/get-completion-candidates current-dir current-word prev-word all-words)]
    (println (str/join " " candidates))))

