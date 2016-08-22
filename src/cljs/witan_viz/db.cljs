(ns witan-viz.db
  (:require [taoensso.timbre :as log]
            [witan-viz.data :as data])
  (:import goog.Uri))

(defn get-data
  [qd]
  (let [a (-> qd
              (.get "data")
              (clojure.string/split ","))
        a' (set (map clojure.string/trim a))]
    (log/info "Data:" a')
    (data/fetch-datasets a')))

(defn get-style
  [qd]
  (let [a (-> qd
              (.get "style")
              (keyword))]
    (log/info "Style:" a)
    a))

(defn make-db
  []
  (let [qd (.getQueryData (goog.Uri. (.-location js/window)))]
    (get-data qd)
    {:data nil
     :style (get-style qd)}))
