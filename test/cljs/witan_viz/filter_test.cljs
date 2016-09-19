(ns witan-viz.filter-test
  (:require [witan-viz.filter :as f])
  (:require-macros
   [cljs.test :refer [deftest testing is]]))

(deftest make-filters
  (testing "simple filter ="
    (let [filter (f/Filter. "foo" "bar" "=" "baz")]
      (is filter)
      (is (= "foo" (:label filter)))
      (is (= "bar" (:column filter)))
      (is (= "="   (:operation filter)))
      (is (= "baz" (:variable filter)))
      (is (= "foo::bar%3Dbaz" (str filter))))))
