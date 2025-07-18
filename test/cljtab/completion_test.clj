(ns cljtab.completion-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [cljtab.completion :as completion]))

(def test-project-dir "/tmp/cljtab-test-project")
(def test-deps-edn "{:paths [\"src\"] :deps {} :aliases {:dev {} :test {} :build {} :nrepl {}}}")

(defn test-fixture [f]
  (let [test-dir test-project-dir]
    ;; Clean up before test
    (fs/delete-tree test-dir {:force true})
    (fs/create-dirs test-dir)
    (spit (str (fs/path test-dir "deps.edn")) test-deps-edn)
    (try (f)
         (finally
           ;; Clean up after test
           (fs/delete-tree test-dir {:force true})))))

(use-fixtures :each test-fixture)

(deftest test-basic-completion
  (testing "Base options available"
    (let [candidates (completion/get-completion-candidates test-project-dir "" "clj" ["clj"])]
      (is (some #(= "-A" %) candidates))
      (is (some #(= "-X" %) candidates))))

  (testing "Alias completion after -A with generated cache"
    (let [_ (completion/generate-candidates test-project-dir)
          candidates (completion/get-completion-candidates test-project-dir ":d" "-A" ["clj" "-A" ":d"])]
      (is (some #(= ":dev" %) candidates))
      (is (some #(= ":deps" %) candidates))))

  (testing "Function completion after -X:deps with generated cache"
    (let [_ (completion/generate-candidates test-project-dir)
          candidates (completion/get-completion-candidates test-project-dir "l" "-X:deps" ["clj" "-X:deps" "l"])]
      (is (some #(= "list" %) candidates))
      (is (some #(= "tree" %) candidates))))

  (testing "Tools completion after -Ttools with generated cache"
    (let [_ (completion/generate-candidates test-project-dir)
          candidates (completion/get-completion-candidates test-project-dir "i" "-Ttools" ["clj" "-Ttools" "i"])]
      (is (some #(= "install" %) candidates)))))

(deftest test-cache-operations
  (testing "Cache generation and loading"
    (let [generated (completion/generate-candidates test-project-dir)
          loaded (completion/load-cached-candidates test-project-dir)]
      (is (= (:base-options generated) (:base-options loaded)))))

  (testing "Find nearest cache file"
    (let [_ (completion/generate-candidates test-project-dir)
          subdir (str test-project-dir "/subdir")
          deep-subdir (str subdir "/deep")]
      (fs/create-dirs deep-subdir)
      (let [cache-path (completion/find-nearest-cache-file deep-subdir)]
        (is (some? cache-path))
        (is (str/includes? cache-path test-project-dir)))))

  (testing "Cache file path uses full directory structure"
    (let [cache-path (completion/cache-file-path test-project-dir)]
      (is (str/includes? cache-path test-project-dir))
      (is (str/includes? cache-path "/.cache/cljtab"))
      (is (str/ends-with? cache-path "/candidates.edn")))))

(deftest test-edge-cases
  (testing "Empty inputs"
    (let [candidates (completion/get-completion-candidates test-project-dir "" "" [])]
      (is (some #(= "-A" %) candidates))))

  (testing "No deps.edn directory"
    (let [temp-dir "/tmp/no-deps"]
      (fs/delete-tree temp-dir {:force true})
      (fs/create-dirs temp-dir)
      (try
        (let [candidates (completion/get-completion-candidates temp-dir "-" "clj" ["clj" "-"])]
          (is (some #(= "-A" %) candidates)))
        (finally (fs/delete-tree temp-dir {:force true}))))))