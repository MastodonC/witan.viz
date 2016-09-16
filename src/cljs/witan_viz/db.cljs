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
      (data/fetch-datasets a ops))))

(defn get-style
  [qd]
  (-> qd (.get "style") (keyword)))

(defn get-spinner
  [qd]
  (-> qd
      (.get "spinner" "true")
      (= "true")))

(defn get-settings-button
  [qd]
  (-> qd
      (.get "settings" "true")
      (= "true")))

(defn get-filters
  [qd]
  (let [a (-> qd
              (.get "filter")
              (clojure.string/split ","))
        a (->> a
               (map #(rest (re-find #"^(.*::)?([a-zA-Z0-9_-]+)([!=<>]+)([a-zA-Z0-9_-]*)$" %)))
               (filter #(= 4 (count %)))
               (map #(update (vec %) 0 (fn [x] (when x (subs x 0 (- (count x) 2) )))))
               (mapv #(apply f/->Filter. %)))]
    a))

(defn get-args
  [qd]
  (->> qd
       (.getKeys)
       (js->clj)
       (filter #(gstr/caseInsensitiveStartsWith % "args"))
       (map #(hash-map (->> %
                            (re-find #"\[(.+)\]")
                            (last)
                            (keyword)) (.get qd %)))
       (into {})))

(defn make-db
  "Make the database, as if from scratch"
  ([url]
   (let [qd (get-query-data url)
         filters (get-filters qd)]
     (get-data (.get qd "data") {:filters filters})
     {:data      nil
      :spinner?  (get-spinner qd)
      :settings? (get-settings-button qd)
      :settings-open? false
      :style     (get-style qd)
      :filters   filters
      :args      (get-args qd)}))
  ([]
   (make-db (.-location js/window))))

(defn refresh-db
  "Assume volatile features are changed."
  [db url]
  (let [qd          (get-query-data url)
        new-filters (get-filters qd)
        style       (get-style qd)
        args        (get-args qd)]
    (if (not= style (:style db))
      ;; all bets are off
      (do
        (log/warn "Hard DB reset.")
        (make-db))
      ;; attempt a smooth refresh
      (do
        (log/info "Attempting to smooth-reset data...")
        (get-data (.get qd "data") {:filters new-filters})
        (assoc db
               :filters new-filters
               :args    args)))))
