(ns witan-viz.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            ;;
            [witan-viz.views.table-diff :as table-diff]
            [witan-viz.views.table :as table]))

(def pym (.Child js/pym))

(def supported-styles
  #{:table})

(defmulti visualisation
  (fn [{:keys [style]}] style))

(defmethod visualisation
  :default
  [{:keys [style]}]
  [re-com/label :label (str "Unrecognised style: " style)])

(defmethod visualisation
  :table-diff
  [m]
  [table-diff/view m])

(defmethod visualisation
  :table
  [m]
  [table/view m pym])

(defn main-panel []
  (let [display (re-frame/subscribe [:display])]
    (fn []
      (let [d @display]
        (if (:ready? d)
          (visualisation d)
          [re-com/box
           :width "100%"
           :height "100%"
           :align :center
           :justify :center
           :child [re-com/throbber :size :large]])))))
