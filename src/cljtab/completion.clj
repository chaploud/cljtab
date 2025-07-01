(ns cljtab.completion
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def base-options
  ["-A" "-X" "-T" "-M" "-P" "-J" "-Sdeps" "-Srepro" "-Spath" "-Stree" "-Scp" 
   "-Sforce" "-Sverbose" "-Sthreads" "-Strace" "--version" "-version" "--help" "-h" "-?"])

(def builtin-tools ["tools"])
(def builtin-aliases [":deps"])
(def deps-functions ["list" "tree" "find-versions" "prep" "mvn-pom" "mvn-install" "aliases" "help/doc" "help/dir"])
(def tools-functions ["install" "install-latest" "list" "remove" "show" "help/doc" "help/dir"])

(defn cache-file-path [project-dir]
  (let [home (System/getProperty "user.home")
        encoded-path (str/replace project-dir "/" "_")]
    (str home "/.cache/cljtab/" encoded-path "/candidates.edn")))

(defn deps-edn-exists? [dir]
  (.exists (io/file dir "deps.edn")))

(defn parse-deps-edn [dir]
  (try
    (when (deps-edn-exists? dir)
      (with-open [reader (java.io.PushbackReader. (io/reader (io/file dir "deps.edn")))]
        (edn/read reader)))
    (catch Exception e
      (println "Error parsing deps.edn:" (.getMessage e))
      nil)))

(defn extract-aliases [deps-edn]
  (when-let [aliases (:aliases deps-edn)]
    (map #(str ":" (name %)) (keys aliases))))

(defn generate-candidates [dir]
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

(defn load-cached-candidates [dir]
  (let [cache-path (cache-file-path dir)]
    (try
      (when (.exists (io/file cache-path))
        (edn/read-string (slurp cache-path)))
      (catch Exception _
        (generate-candidates dir)))))

(defn get-completion-candidates [dir _current-word prev-word all-words]
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