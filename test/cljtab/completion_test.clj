(ns cljtab.completion-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [cljtab.completion :as completion]))

(def test-project-dir "/tmp/cljtab-test-project")
(def test-deps-edn "{:paths [\"src\"] :deps {} :aliases {:dev {} :test {} :build {} :nrepl {}}}")

(defn test-fixture [f]
  (let [test-dir (io/file test-project-dir)]
    (.mkdirs test-dir)
    (spit (str test-project-dir "/deps.edn") test-deps-edn)
    (try (f)
         (finally
           (doseq [file (reverse (file-seq test-dir))]
             (.delete file))))))

(use-fixtures :each test-fixture)

(deftest test-basic-completion
  (testing "Base options available"
    (let [candidates (completion/get-completion-candidates test-project-dir "" "clj" ["clj"])]
      (is (some #(= "-A" %) candidates))
      (is (some #(= "-X" %) candidates))))

  (testing "Alias completion after -A"
    (let [candidates (completion/get-completion-candidates test-project-dir ":d" "-A" ["clj" "-A" ":d"])]
      (is (some #(= ":dev" %) candidates))
      (is (some #(= ":deps" %) candidates))))

  (testing "Function completion after -X:deps"
    (let [candidates (completion/get-completion-candidates test-project-dir "l" "-X:deps" ["clj" "-X:deps" "l"])]
      (is (some #(= "list" %) candidates))
      (is (some #(= "tree" %) candidates))))

  (testing "Tools completion after -Ttools"
    (let [candidates (completion/get-completion-candidates test-project-dir "i" "-Ttools" ["clj" "-Ttools" "i"])]
      (is (some #(= "install" %) candidates)))))

(deftest test-cache-operations
  (testing "Cache generation and loading"
    (let [generated (completion/generate-candidates test-project-dir)
          loaded (completion/load-cached-candidates test-project-dir)]
      (is (= (:base-options generated) (:base-options loaded))))))

(deftest test-edge-cases
  (testing "Empty inputs"
    (let [candidates (completion/get-completion-candidates test-project-dir "" "" [])]
      (is (some #(= "-A" %) candidates))))

  (testing "No deps.edn directory"
    (let [temp-dir "/tmp/no-deps"]
      (.mkdirs (io/file temp-dir))
      (try
        (let [candidates (completion/get-completion-candidates temp-dir "-" "clj" ["clj" "-"])]
          (is (some #(= "-A" %) candidates)))
        (finally (.delete (io/file temp-dir)))))))