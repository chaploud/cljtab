(ns cljtab.test-runner
  (:require [clojure.test :as test]
            [cljtab.completion-test]
            [cljtab.core-test]
            [cljtab.setup-test]))

(defn run-all-tests
  "Run all tests and exit with appropriate code"
  []
  (let [result (test/run-tests
                'cljtab.completion-test
                'cljtab.core-test
                'cljtab.setup-test)]
    (printf "\n=== Test Summary ===\nTests: %d, Pass: %d, Fail: %d, Error: %d\n"
            (+ (:pass result) (:fail result) (:error result))
            (:pass result) (:fail result) (:error result))
    (when (or (pos? (:fail result)) (pos? (:error result)))
      (System/exit 1))))

(defn -main [& _args]
  (run-all-tests))