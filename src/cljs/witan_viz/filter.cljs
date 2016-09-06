(ns witan-viz.filter
  (:require [taoensso.timbre :as log]))

(defrecord Filter
    [label column operation variable])

(defn str->operation
  [op]
  (condp = op
    "="  =
    "!=" (comp not =)
    ">"  >
    "<"  <
    ">=" >=
    "<=" <=
    nil))

(defn apply-filters
  [filters id {:keys [location data] :as dataset}]
  (let [[headers & rows] data
        result (concat [headers]
                       (reduce (fn [a {:keys [column label variable operation]}]
                                 (if (or (not label) (= label id))
                                   (let [col-idx (.indexOf headers column)
                                         op (str->operation operation)]
                                     (if (= -1 col-idx)
                                       (do (log/warn "Filter couldn't find column:" column) a)
                                       (if-not op
                                         (do (log/warn "Filter couldn't match operation:" operation) a)
                                         (filter #(op (nth % col-idx) variable) a))))
                                   a))
                               rows filters))]
    (assoc dataset :data result)))
