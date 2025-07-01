(ns cljtab.setup-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cljtab.setup :as setup]
            [cljtab.core]))

(def test-home-dir "/tmp/cljtab-setup-test-home")

(defn test-fixture [f]
  (let [original-home (System/getProperty "user.home")
        test-dir (io/file test-home-dir)]
    (.mkdirs test-dir)
    (try
      (System/setProperty "user.home" test-home-dir)
      (f)
      (finally
        (System/setProperty "user.home" original-home)
        (doseq [file (reverse (file-seq test-dir))]
          (.delete file))))))

(use-fixtures :each test-fixture)

(deftest test-path-functions
  (testing "file paths"
    (is (str/ends-with? (setup/get-bashrc-path) "/.bashrc"))
    (is (str/ends-with? (setup/get-completion-file-path) "/clj"))
    (is (str/includes? (setup/get-completion-file-path) ".local/share/bash-completion/completions"))))

(deftest test-file-existence-checks
  (testing "initial state"
    (is (not (setup/bashrc-exists?)))
    (is (not (setup/completion-file-exists?)))
    (is (not (setup/completion-already-installed?)))
    (is (not (setup/bashrc-source-line-exists?)))))

(deftest test-bash-completion-script
  (testing "script contains required components"
    (let [script setup/bash-completion-script]
      (is (str/includes? script "_clj_completion"))
      (is (or (str/includes? script "cljtab complete")
              (str/includes? script "bb complete")))
      (is (str/includes? script "complete -F _clj_completion clj")))))

(deftest test-installation
  (testing "complete installation process"
    (let [output (with-out-str (setup/install-bash-completion))
          completion-file (setup/get-completion-file-path)
          bashrc-path (setup/get-bashrc-path)]

      ;; Check output messages
      (is (str/includes? output "Created completion file"))
      (is (str/includes? output "Created"))
      (is (str/includes? output "with completion source"))

      ;; Check files exist
      (is (.exists (io/file completion-file)))
      (is (.exists (io/file bashrc-path)))

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