(ns cljtab.setup
  "Installation and setup logic for bash completion integration."
  (:require [babashka.fs :as fs]
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
    fi

    if [[ -z \"$candidates\" ]]; then
        candidates=\"-A -X -T -M -P -J -Sdeps -Srepro -Spath -Stree -Scp -Sforce -Sverbose -Sthreads -Strace --version -version --help -h -?\"
    fi

    COMPREPLY=( $(compgen -W \"${candidates}\" -- \"${current_word}\") )
}

complete -F _clj_completion clj
complete -F _clj_completion clojure")

(def zsh-completion-script
  "Zsh completion script that will be installed to the system."
  "#compdef clj clojure

_clj_completion() {
    local -a words
    local current_word prev_word all_words_str candidates

    words=(${(z)BUFFER})
    current_word=\"$words[$CURRENT]\"
    if [[ $CURRENT -gt 1 ]]; then
        prev_word=\"$words[$((CURRENT-1))]\"
    else
        prev_word=\"\"
    fi

    all_words_str=\"[\"
    for ((i=1; i<=${#words[@]}; i++)); do
        [[ $i -gt 1 ]] && all_words_str+=\", \"
        all_words_str+=\"\\\"${words[$i]}\\\"\"
    done
    all_words_str+=\"]\"

    if command -v cljtab >/dev/null 2>&1; then
        candidates=$(cljtab complete \"{:current-word \\\"$current_word\\\" :prev-word \\\"$prev_word\\\" :all-words $all_words_str}\" 2>/dev/null)
    fi

    if [[ -z \"$candidates\" ]]; then
        candidates=\"-A -X -T -M -P -J -Sdeps -Srepro -Spath -Stree -Scp -Sforce -Sverbose -Sthreads -Strace --version -version --help -h -?\"
    fi

    local -a completions
    completions=(${(z)candidates})
    compadd -a completions
}

_clj_completion \"$@\"")

(defn get-completion-file-path []
  (str (System/getProperty "user.home") "/.local/share/bash-completion/completions/clj"))

(defn get-zsh-completion-file-path []
  (str (System/getProperty "user.home") "/.local/share/zsh/site-functions/_clj"))

(defn get-bashrc-path []
  (str (System/getProperty "user.home") "/.bashrc"))

(defn get-zshrc-path []
  (str (System/getProperty "user.home") "/.zshrc"))

(defn completion-file-exists? []
  (fs/exists? (get-completion-file-path)))

(defn zsh-completion-file-exists? []
  (fs/exists? (get-zsh-completion-file-path)))

(defn bashrc-exists? []
  (fs/exists? (get-bashrc-path)))

(defn zshrc-exists? []
  (fs/exists? (get-zshrc-path)))

(defn completion-already-installed? []
  (completion-file-exists?))

(defn zsh-completion-already-installed? []
  (zsh-completion-file-exists?))

(defn bashrc-source-line-exists? []
  (when (bashrc-exists?)
    (let [bashrc-content (slurp (get-bashrc-path))
          completion-file (get-completion-file-path)]
      (str/includes? bashrc-content completion-file))))

(defn zshrc-fpath-exists? []
  (when (zshrc-exists?)
    (let [zshrc-content (slurp (get-zshrc-path))
          completion-dir (str (System/getProperty "user.home") "/.local/share/zsh/site-functions")]
      (str/includes? zshrc-content completion-dir))))

(defn install-zsh-completion []
  (let [completion-file (get-zsh-completion-file-path)
        zshrc-path (get-zshrc-path)
        completion-dir (str (System/getProperty "user.home") "/.local/share/zsh/site-functions")
        fpath-line (str "# Clojure CLI tab completion (cljtab)\nfpath=(\"" completion-dir "\" $fpath)\nautoload -U compinit\ncompinit")]

    (cond
      (and (zsh-completion-already-installed?) (zshrc-fpath-exists?))
      (println "Clojure CLI zsh completion is already installed")

      :else
      (do
        ;; Create completion file
        (fs/create-dirs (fs/parent completion-file))
        (spit completion-file zsh-completion-script)
        (println "✓ Created zsh completion file:" completion-file)

        ;; Add fpath line to .zshrc if not already present
        (when (not (zshrc-fpath-exists?))
          (if (zshrc-exists?)
            (do
              (spit zshrc-path (str "\n" fpath-line "\n") :append true)
              (println "✓ Added fpath line to" zshrc-path))
            (do
              (spit zshrc-path (str fpath-line "\n"))
              (println "✓ Created" zshrc-path "with completion fpath"))))

        (println "Please run: source ~/.zshrc")))))

(defn install-bash-completion []
  (let [completion-file (get-completion-file-path)
        bashrc-path (get-bashrc-path)
        source-line (str "# Clojure CLI tab completion (cljtab)\n[[ -f \"" completion-file "\" ]] && source \"" completion-file "\"")]

    (cond
      (and (completion-already-installed?) (bashrc-source-line-exists?))
      (println "Clojure CLI bash completion is already installed")

      :else
      (do
        ;; Create completion file
        (fs/create-dirs (fs/parent completion-file))
        (spit completion-file bash-completion-script)
        (println "✓ Created bash completion file:" completion-file)

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

(defn remove-cljtab-lines
  "Remove cljtab-added lines from shell config files."
  []
  (let [bashrc-path (get-bashrc-path)
        zshrc-path (get-zshrc-path)]

    (when (bashrc-exists?)
      (let [content (slurp bashrc-path)
            lines (str/split-lines content)
            filtered-lines (remove #(str/includes? % "cljtab") lines)
            new-content (str/join "\n" filtered-lines)]
        (when (not= content new-content)
          (spit bashrc-path new-content)
          (println "✓ Removed cljtab lines from" bashrc-path))))

    (when (zshrc-exists?)
      (let [content (slurp zshrc-path)
            lines (str/split-lines content)
            filtered-lines (remove #(str/includes? % "cljtab") lines)
            new-content (str/join "\n" filtered-lines)]
        (when (not= content new-content)
          (spit zshrc-path new-content)
          (println "✓ Removed cljtab lines from" zshrc-path))))))

(defn clean
  "Remove all cljtab cache files and shell configuration lines."
  []
  (let [cache-dir (str (System/getProperty "user.home") "/.cache/cljtab")]
    (when (fs/exists? cache-dir)
      (fs/delete-tree cache-dir)
      (println "✓ Removed cache directory:" cache-dir))
    (remove-cljtab-lines)
    (println "✓ Cleaned up cljtab installation")))

(defn setup
  ([]
   (let [shell (System/getenv "SHELL")]
     (cond
       (str/includes? shell "zsh") (install-zsh-completion)
       (str/includes? shell "bash") (install-bash-completion)
       :else (do
               (println "Installing both bash and zsh completion:")
               (install-bash-completion)
               (install-zsh-completion)))))
  ([shell-type]
   (case shell-type
     "bash" (install-bash-completion)
     "zsh" (install-zsh-completion)
     "both" (do
              (install-bash-completion)
              (install-zsh-completion))
     (println "Unknown shell type. Use 'bash', 'zsh', or 'both'"))))