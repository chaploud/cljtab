(ns cljtab.setup
  "Installation and setup logic for bash completion integration."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def bash-completion-script
  "Bash completion script that will be installed to the system."
  "# Clojure CLI tab completion
_clj_completion() {
    local current_word prev_word all_words_str
    current_word=\"${COMP_WORDS[COMP_CWORD]}\"
    prev_word=\"${COMP_WORDS[COMP_CWORD-1]}\"

    all_words_str=\"[\"
    for ((i=0; i<${#COMP_WORDS[@]}; i++)); do
        [[ $i -gt 0 ]] && all_words_str+=\", \"
        all_words_str+=\"\\\"${COMP_WORDS[$i]}\\\"\"
    done
    all_words_str+=\"]\"

    local candidates
    if command -v cljtab >/dev/null 2>&1; then
        candidates=$(cljtab complete \"{:current-word \\\"$current_word\\\" :prev-word \\\"$prev_word\\\" :all-words $all_words_str}\" 2>/dev/null)
    elif command -v bb >/dev/null 2>&1; then
        candidates=$(bb complete \"{:current-word \\\"$current_word\\\" :prev-word \\\"$prev_word\\\" :all-words $all_words_str}\" 2>/dev/null)
    fi

    if [[ -z \"$candidates\" ]]; then
        candidates=\"-A -X -T -M -P -J -Sdeps -Srepro -Spath -Stree -Scp -Sforce -Sverbose -Sthreads -Strace --version -version --help -h -?\"
    fi

    COMPREPLY=( $(compgen -W \"${candidates}\" -- \"${current_word}\") )
}

complete -F _clj_completion clj
complete -F _clj_completion clojure")

(defn get-completion-file-path []
  (str (System/getProperty "user.home") "/.local/share/bash-completion/completions/clj"))

(defn get-bashrc-path []
  (str (System/getProperty "user.home") "/.bashrc"))

(defn completion-file-exists? []
  (.exists (io/file (get-completion-file-path))))

(defn bashrc-exists? []
  (.exists (io/file (get-bashrc-path))))

(defn completion-already-installed? []
  (completion-file-exists?))

(defn bashrc-source-line-exists? []
  (when (bashrc-exists?)
    (let [bashrc-content (slurp (get-bashrc-path))
          completion-file (get-completion-file-path)]
      (str/includes? bashrc-content completion-file))))

(defn install-bash-completion []
  (let [completion-file (get-completion-file-path)
        bashrc-path (get-bashrc-path)
        source-line (str "# Clojure CLI tab completion (cljtab)\n[[ -f \"" completion-file "\" ]] && source \"" completion-file "\"")]

    (cond
      (and (completion-already-installed?) (bashrc-source-line-exists?))
      (println "Clojure CLI tab completion is already installed")

      :else
      (do
        ;; Create completion file
        (io/make-parents completion-file)
        (spit completion-file bash-completion-script)
        (println "✓ Created completion file:" completion-file)

        ;; Add source line to .bashrc if not already present
        (when (not (bashrc-source-line-exists?))
          (if (bashrc-exists?)
            (do
              (spit bashrc-path (str "\n" source-line "\n") :append true)
              (println "✓ Added source line to" bashrc-path))
            (do
              (spit bashrc-path (str source-line "\n"))
              (println "✓ Created" bashrc-path "with completion source"))))

        (println "Please run: source ~/.bashrc")))))