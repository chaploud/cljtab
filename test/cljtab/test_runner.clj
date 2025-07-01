(ns cljtab.test-runner
  (:require [clojure.test :as test]
            [cljtab.completion-test]
            [cljtab.core-test]
            [cljtab.setup-test]))

(defn -main [& _args]
  (println "Running cljtab tests...")
  (let [result (test/run-tests 'cljtab.completion-test 'cljtab.core-test 'cljtab.setup-test)]
    (printf "\n=== Test Summary ===\nTests run: %d, Passed: %d, Failed: %d, Errors: %d\n"
            (+ (:pass result) (:fail result) (:error result))
            (:pass result) (:fail result) (:error result))
    (when (or (pos? (:fail result)) (pos? (:error result)))
      (System/exit 1))))