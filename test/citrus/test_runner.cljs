(ns citrus.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [citrus.core-test]
            [citrus.handler-test]))

(doo-tests 'citrus.core-test 'citrus.handler-test)
