(ns witan-viz.views.table
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            [reagent.core :as r]
            [witan-viz.data :refer [send-ready-message!]]))

(defn view
  [args pym]
  (fn [{:keys [data args]} pym]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (send-ready-message!
         pym
         (.toString (.-offsetHeight (.getElementById js/document "vizapp")))))
      :reagent-render
      (fn []
        (let [{:keys [dataset]} args
              ds (if (and dataset (contains? data dataset))
                   (get data dataset)
                   (-> data first second))]
          [:div#table
           (let [{:keys [location data]} ds]
             [:table.pure-table.pure-table-horizontal
              {:key "results-table"}
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
                      [:td {:key value} value])]))]])]))})))
