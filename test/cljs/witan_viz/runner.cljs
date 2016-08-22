(ns witan-viz.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [witan-viz.core-test]))

(doo-tests 'witan-viz.core-test)
