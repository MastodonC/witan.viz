(ns witan-viz.views.table
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]))

(defn view
  [args]
  (fn [{:keys [data]}]
    (let [{:keys [location data]} (first data)]
      [:table.pure-table.pure-table-horizontal
       {:key "results-table"
        :id "table"}
       [:thead
        [:tr
         (for [header (first data)]
           ^{:key header}
           [:th header])]]
       [:tbody
        (for [result-idx (range (count (rest data)))]
          (let [result (nth (rest data) result-idx)]
            [:tr
             {:key result-idx}
             (for [value result]
               [:td {:key value} value])]))]])))
