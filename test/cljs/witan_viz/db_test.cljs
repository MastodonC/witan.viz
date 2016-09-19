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

(deftest style
  (testing "style"
    (is (= (db/get-style (db/get-query-data (make-url "?style=lineplot")))
           :lineplot))))

(deftest spinner
  (testing "true"
    (is (= (db/get-spinner (db/get-query-data (make-url "?spinner=true")))
           true)))
  (testing "false"
    (is (= (db/get-spinner (db/get-query-data (make-url "?spinner=false")))
           false)))
  (testing "default"
    (is (= (db/get-spinner (db/get-query-data (make-url "?")))
           true)))
  (testing "foo?"
    (is (= (db/get-spinner (db/get-query-data (make-url "?spinner=foo")))
           false))))

(deftest settings
  (testing "true"
    (is (= (db/get-settings-button (db/get-query-data (make-url "?settings=true")))
           true)))
  (testing "false"
    (is (= (db/get-settings-button (db/get-query-data (make-url "?settings=false")))
           false)))
  (testing "default"
    (is (= (db/get-settings-button (db/get-query-data (make-url "?")))
           true)))
  (testing "foo?"
    (is (= (db/get-settings-button (db/get-query-data (make-url "?settings=foo")))
           false))))

(deftest args
  (testing "args 1"
    (is (= (db/get-args (db/get-query-data (make-url "?args[foo]=bar&args[baz]=qux")))
           {:foo "bar" :baz "qux"})))
  (testing "args overwrite"
    (is (= (db/get-args (db/get-query-data (make-url "?args[foo]=bar&args[foo]=qux")))
           {:foo "bar"}))))
