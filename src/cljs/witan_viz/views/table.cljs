(ns witan-viz.views.table
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            [reagent.core :as r]))

;; document.getElementsByTagName('body')[0].offsetHeight.toString()
(defn view
  [args pym]
  (fn [{:keys [data]} pym]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [h (.toString (.-offsetHeight (.getElementById js/document "table")))]
          (log/debug "HEIGHT" h)
          (.sendMessage pym "height" h)))
      :reagent-render
      (fn []
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
                   [:td {:key value} value])]))]]))})))
