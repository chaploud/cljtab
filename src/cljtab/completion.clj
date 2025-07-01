(ns cljtab.completion
  "Tab completion logic for Clojure CLI commands."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private base-options
  "Basic Clojure CLI options available in all contexts."
  ["-A" "-X" "-T" "-M" "-P" "-J" "-Sdeps" "-Srepro" "-Spath" "-Stree" "-Scp"
   "-Sforce" "-Sverbose" "-Sthreads" "-Strace" "--version" "-version" "--help" "-h" "-?"])

(def ^:private builtin-tools
  "Built-in Clojure CLI tools."
  ["tools"])

(def ^:private builtin-aliases
  "Built-in Clojure CLI aliases."
  [":deps"])

(def ^:private deps-functions
  "Functions available in the :deps alias."
  ["list" "tree" "find-versions" "prep" "mvn-pom" "mvn-install" "aliases" "help/doc" "help/dir"])

(def ^:private tools-functions
  "Functions available in tools namespace."
  ["install" "install-latest" "list" "remove" "show" "help/doc" "help/dir"])

(defn cache-file-path
  "Generate cache file path for a given project directory."
  [project-dir]
  (let [home (System/getProperty "user.home")
        encoded-path (str/replace project-dir "/" "_")]
    (str home "/.cache/cljtab/" encoded-path "/candidates.edn")))

(defn deps-edn-exists?
  "Check if deps.edn exists in the given directory."
  [dir]
  (.exists (io/file dir "deps.edn")))

(defn parse-deps-edn
  "Parse deps.edn file from the given directory."
  [dir]
  (try
    (when (deps-edn-exists? dir)
      (edn/read-string (slurp (io/file dir "deps.edn"))))
    (catch Exception e
      (println "Error parsing deps.edn:" (.getMessage e))
      nil)))

(defn extract-aliases
  "Extract aliases from parsed deps.edn as keyword strings."
  [deps-edn]
  (when-let [aliases (:aliases deps-edn)]
    (map #(str ":" (name %)) (keys aliases))))

(defn generate-candidates
  "Generate completion candidates for the given directory and cache them."
  [dir]
  (let [deps-edn (parse-deps-edn dir)
        project-aliases (extract-aliases deps-edn)
        all-candidates {:base-options base-options
                        :builtin-tools builtin-tools
                        :builtin-aliases builtin-aliases
                        :project-aliases (or project-aliases [])
                        :deps-functions deps-functions
                        :tools-functions tools-functions}
        cache-path (cache-file-path dir)]
    (io/make-parents cache-path)
    (spit cache-path (pr-str all-candidates))
    (println "Generated completion candidates for" dir)
    (println "Cache file:" cache-path)
    all-candidates))

(defn load-cached-candidates
  "Load cached completion candidates or generate new ones if cache doesn't exist."
  [dir]
  (let [cache-path (cache-file-path dir)]
    (try
      (when (.exists (io/file cache-path))
        (edn/read-string (slurp cache-path)))
      (catch Exception _
        (generate-candidates dir)))))

(defn get-completion-candidates
  "Get appropriate completion candidates based on context."
  [dir _current-word prev-word all-words]
  (let [candidates (or (load-cached-candidates dir) (generate-candidates dir))
        {:keys [base-options builtin-tools builtin-aliases project-aliases
                deps-functions tools-functions]} candidates]
    (cond
      (= prev-word "-A") (concat builtin-aliases project-aliases)
      (= prev-word "-X") (concat builtin-aliases project-aliases)
      (and (>= (count all-words) 2)
           (= (nth all-words (- (count all-words) 2)) "-X:deps")) deps-functions
      (= prev-word "-T") (concat builtin-tools builtin-aliases project-aliases)
      (and (>= (count all-words) 2)
           (= (nth all-words (- (count all-words) 2)) "-Ttools")) tools-functions
      (= prev-word "-M") (concat builtin-aliases project-aliases)
      (= prev-word "-P") ["-A" "-X" "-T" "-M"]
      :else base-options)))