(ns witan-viz.db
  (:require [taoensso.timbre  :as log]
            [witan-viz.data   :as data]
            [witan-viz.filter :as f]
            [goog.string      :as gstr])
  (:import [goog.Uri QueryData]))

(defn get-query-data
  [url]
  (.getQueryData (goog.Uri. url)))

(defn get-data
  [data ops]
  (let [a (->> (clojure.string/split data ",")
               (remove clojure.string/blank?)
               (map clojure.string/trim)
               (set))]
    (when (not-empty a)
      (log/info "Data:" a)
      (data/fetch-datasets a ops))))

(defn get-style
  [qd]
  (let [a (-> qd (.get "style") (keyword))]
    (log/info "Style:" a)
    a))

(defn get-spinner
  [qd]
  (let [a (-> qd
              (.get "spinner" "true")
              (= "true"))]
    (log/info "Spinner:" a)
    a))

(defn get-filters
  [qd]
  (let [a (-> qd
              (.get "filter")
              (clojure.string/split ","))
        a (->> a
               (map #(rest (re-find #"^(.*::)?([a-zA-Z0-9_-]+)([!=<>]+)([a-zA-Z0-9_-]+)$" %)))
               (filter #(= 4 (count %)))
               (map #(update (vec %) 0 (fn [x] (when x (subs x 0 (- (count x) 2) )))))
               (map #(apply f/->Filter. %)))]
    (when a
      (log/info "Filters:" a))
    a))

(defn get-args
  [qd]
  (let [arg-map
        (->> qd
             (.getKeys)
             (js->clj)
             (filter #(gstr/caseInsensitiveStartsWith % "args"))
             (map #(hash-map (->> %
                                  (re-find #"\[(.+)\]")
                                  (last)
                                  (keyword)) (.get qd %)))
             (into {}))]
    (log/info "Arguments:" arg-map)
    arg-map))

(defn make-db
  ([url]
   (let [qd (get-query-data url)
         filters (get-filters qd)]
     (get-data (.get qd "data") {:filters filters})
     {:data nil
      :spinner (get-spinner qd)
      :style (get-style qd)
      :filters filters
      :args  (get-args qd)}))
  ([]
   (make-db (.-location js/window))))
