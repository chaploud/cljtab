(ns cljtab.setup-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [cljtab.setup :as setup]
            [cljtab.core]))

(def test-home-dir "/tmp/cljtab-setup-test-home")


(defn test-fixture [f]
  (let [original-home (System/getProperty "user.home")
        test-dir test-home-dir
        original-bashrc-path (str original-home "/.bashrc")
        original-zshrc-path (str original-home "/.zshrc")
        original-bashrc-content (when (fs/exists? original-bashrc-path)
                                  (slurp original-bashrc-path))
        original-zshrc-content (when (fs/exists? original-zshrc-path)
                                 (slurp original-zshrc-path))
        original-bash-completion-dir (str original-home "/.local/share/bash-completion/completions")
        original-zsh-completion-dir (str original-home "/.local/share/zsh/site-functions")
        original-bash-completion-exists? (fs/exists? (str original-bash-completion-dir "/clj"))
        original-zsh-completion-exists? (fs/exists? (str original-zsh-completion-dir "/_clj"))]
    ;; Clean up before test
    (fs/delete-tree test-dir {:force true})
    (fs/create-dirs test-dir)
    (try
      (System/setProperty "user.home" test-home-dir)
      (f)
      (finally
        ;; Restore original home
        (System/setProperty "user.home" original-home)

        ;; Restore original .bashrc if it was modified
        (when original-bashrc-content
          (spit original-bashrc-path original-bashrc-content))

        ;; Restore original .zshrc if it was modified
        (when original-zshrc-content
          (spit original-zshrc-path original-zshrc-content))

        ;; Remove completion files if they weren't there originally
        (when (and (not original-bash-completion-exists?)
                   (fs/exists? (str original-bash-completion-dir "/clj")))
          (fs/delete (str original-bash-completion-dir "/clj")))

        (when (and (not original-zsh-completion-exists?)
                   (fs/exists? (str original-zsh-completion-dir "/_clj")))
          (fs/delete (str original-zsh-completion-dir "/_clj")))

        ;; Clean up test directory
        (fs/delete-tree test-dir {:force true})))))

(use-fixtures :each test-fixture)

(deftest test-path-functions
  (testing "bash file paths"
    (is (str/ends-with? (setup/get-bashrc-path) "/.bashrc"))
    (is (str/ends-with? (setup/get-completion-file-path) "/clj"))
    (is (str/includes? (setup/get-completion-file-path) ".local/share/bash-completion/completions")))
  (testing "zsh file paths"
    (is (str/ends-with? (setup/get-zshrc-path) "/.zshrc"))
    (is (str/ends-with? (setup/get-zsh-completion-file-path) "/_clj"))
    (is (str/includes? (setup/get-zsh-completion-file-path) ".local/share/zsh/site-functions"))))

(deftest test-file-existence-checks
  (testing "bash initial state"
    (is (not (setup/bashrc-exists?)))
    (is (not (setup/completion-file-exists?)))
    (is (not (setup/completion-already-installed?)))
    (is (not (setup/bashrc-source-line-exists?))))
  (testing "zsh initial state"
    (is (not (setup/zshrc-exists?)))
    (is (not (setup/zsh-completion-file-exists?)))
    (is (not (setup/zsh-completion-already-installed?)))
    (is (not (setup/zshrc-fpath-exists?)))))

(deftest test-bash-completion-script
  (testing "bash script contains required components"
    (let [script setup/bash-completion-script]
      (is (str/includes? script "_clj_completion"))
      (is (str/includes? script "cljtab complete"))
      (is (str/includes? script "complete -F _clj_completion clj")))))

(deftest test-zsh-completion-script
  (testing "zsh script contains required components"
    (let [script setup/zsh-completion-script]
      (is (str/includes? script "#compdef clj clojure"))
      (is (str/includes? script "_clj_completion"))
      (is (str/includes? script "cljtab complete"))
      (is (str/includes? script "compadd")))))

(deftest test-installation
  (testing "complete installation process"
    (let [output (with-out-str (setup/install-bash-completion))
          completion-file (setup/get-completion-file-path)
          bashrc-path (setup/get-bashrc-path)]

      ;; Check output messages
      (is (str/includes? output "Created bash completion file"))
      (is (str/includes? output "Created"))
      (is (str/includes? output "with completion source"))

      ;; Check files exist
      (is (fs/exists? completion-file))
      (is (fs/exists? bashrc-path))

      ;; Check completion file content
      (is (str/includes? (slurp completion-file) "_clj_completion"))

      ;; Check bashrc content
      (let [bashrc-content (slurp bashrc-path)]
        (is (str/includes? bashrc-content completion-file))
        (is (str/includes? bashrc-content "source")))))

  (testing "existing bashrc append"
    (spit (setup/get-bashrc-path) "# existing content\\n")
    (let [output (with-out-str (setup/install-bash-completion))]
      (is (str/includes? output "Added source line"))
      (let [content (slurp (setup/get-bashrc-path))]
        (is (str/includes? content "# existing content"))
        (is (str/includes? content "source")))))

  (testing "already installed detection"
    ;; First installation
    (setup/install-bash-completion)
    ;; Second installation should detect existing
    (let [output (with-out-str (setup/install-bash-completion))]
      (is (str/includes? output "already installed")))))

(deftest test-zsh-installation
  (testing "complete zsh installation process"
    (let [output (with-out-str (setup/install-zsh-completion))
          completion-file (setup/get-zsh-completion-file-path)
          zshrc-path (setup/get-zshrc-path)]

      ;; Check output messages
      (is (str/includes? output "Created zsh completion file"))
      (is (str/includes? output "Created"))
      (is (str/includes? output "with completion fpath"))

      ;; Check files exist
      (is (fs/exists? completion-file))
      (is (fs/exists? zshrc-path))

      ;; Check completion file content
      (is (str/includes? (slurp completion-file) "_clj_completion"))
      (is (str/includes? (slurp completion-file) "#compdef"))

      ;; Check zshrc content
      (let [zshrc-content (slurp zshrc-path)]
        (is (str/includes? zshrc-content "fpath"))
        (is (str/includes? zshrc-content "compinit")))))

  (testing "existing zshrc append"
    (spit (setup/get-zshrc-path) "# existing zsh content\n")
    (let [output (with-out-str (setup/install-zsh-completion))]
      (is (str/includes? output "Added fpath line"))
      (let [content (slurp (setup/get-zshrc-path))]
        (is (str/includes? content "# existing zsh content"))
        (is (str/includes? content "fpath")))))

  (testing "zsh already installed detection"
    ;; First installation
    (setup/install-zsh-completion)
    ;; Second installation should detect existing
    (let [output (with-out-str (setup/install-zsh-completion))]
      (is (str/includes? output "already installed")))))

(deftest test-install-completion-explicit
  (testing "explicit shell type installation"
    ;; Test explicit bash
    (let [output (with-out-str (setup/install-completion "bash"))]
      (is (str/includes? output "bash completion")))

    ;; Test explicit zsh (in clean environment)
    (fs/delete-tree test-home-dir {:force true})
    (fs/create-dirs test-home-dir)
    (let [output (with-out-str (setup/install-completion "zsh"))]
      (is (str/includes? output "zsh completion")))))