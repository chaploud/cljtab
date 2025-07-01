(ns cljtab.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cljtab.core :as core]))

(def test-project-dir "/tmp/cljtab-core-test")
(def test-deps-edn "{:paths [\"src\"] :aliases {:dev {} :test {}}}")

(defn test-fixture [f]
  (let [test-dir (io/file test-project-dir)
        original-dir (System/getProperty "user.dir")]
    (.mkdirs test-dir)
    (spit (str test-project-dir "/deps.edn") test-deps-edn)
    (try
      (System/setProperty "user.dir" test-project-dir)
      (f)
      (finally
        (System/setProperty "user.dir" original-dir)
        (doseq [file (reverse (file-seq test-dir))]
          (.delete file))))))

(use-fixtures :each test-fixture)

(deftest test-core-functions
  (testing "Setup function"
    (is (nil? (core/setup {}))))
  
  (testing "Generate function"
    (let [output (with-out-str (core/generate {}))]
      (is (str/includes? output "Generated completion candidates"))))
  
  (testing "Complete function basic"
    (let [output (with-out-str (core/complete {:current-word "-" :prev-word "clj" :all-words ["clj" "-"]}))]
      (is (str/includes? output "-A"))
      (is (str/includes? output "-X"))))
  
  (testing "Complete function with aliases"
    (let [output (with-out-str (core/complete {:current-word ":d" :prev-word "-A" :all-words ["clj" "-A" ":d"]}))]
      (is (str/includes? output ":dev"))
      (is (str/includes? output ":deps"))))
  
  (testing "Complete function handles missing keys"
    (let [output (with-out-str (core/complete {}))]
      (is (string? output)))))

(deftest test-edge-cases
  (testing "Empty arguments"
    (is (nil? (core/setup {})))
    (is (string? (with-out-str (core/generate {}))))
    (is (string? (with-out-str (core/complete {}))))))