(ns citrus.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [citrus.core-test]))

(doo-tests 'citrus.core-test)
