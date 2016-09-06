(ns witan-viz.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [witan-viz.db-test]))

(doo-tests 'witan-viz.db-test)
