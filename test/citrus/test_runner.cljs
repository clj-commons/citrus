(ns citrus.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [citrus.core-test]
            [citrus.custom-handler-test]))

(doo-tests 'citrus.core-test 'citrus.custom-handler-test)
