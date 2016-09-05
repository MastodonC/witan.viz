(ns witan-viz.db-test
  (:require [witan-viz.db :as db]
            [witan-viz.filter :as f])
  (:require-macros
   [cljs.test :refer [deftest testing is]]))

(defn make-url
  [suffix]
  (str "http://localhost/" suffix))

(deftest filters
  (testing "simple filter ="
    (is (= (vec
            (db/get-filters
             (db/get-query-data (make-url "?filter=foo%3Dbar"))))
           [(f/Filter. nil "foo" "=" "bar")])))
  (testing "multiple filters ="
    (is (= (vec
            (db/get-filters
             (db/get-query-data (make-url "?filter=foo%3Dbar,baz%3Cqux"))))
           [(f/Filter. nil "foo" "=" "bar")
            (f/Filter. nil "baz" "<" "qux")]))))
