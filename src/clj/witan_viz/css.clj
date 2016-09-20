(ns witan-viz.css
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px em percent]]
            [garden.selectors :as s]))

(defstyles screen
  [:body {:background-color 'transparent}]
  [:html :body :table :#app :#table :#vizapp
   {:width "100%"
    :height "100%"
    :overflow :hidden}]
  [:#table
   {:overflow :auto}]

  [:#lineplot
   [:.lineplot-point
    {:r 4}
    [:&:hover
     {:stroke "#aaa"
      :r 10
      :stroke-width 3}]]
   [:#tooltip
    {:position :absolute
     :pointer-events :none
     :display :none
     :color "#fff"
     :background-color "#000"
     :border [[(px 2) :transparent 'solid]]
     :padding (px 3)
     :white-space 'nowrap
     :border-radius (px 4)}]]

  [:.settings
   {:position :absolute
    :top (em 0)
    :right (em 0)
    :width (px 500)}
   [:.settings-button
    {:background-color "#f3f3ef"
     :padding (em 0.1)
     :display :inline-table
     :border-radius [[(px 3) (px 3) (px 0) (px 0)]]}]
   [:.settings-box
    {:border-radius [[(px 3) (px 0) (px 3) (px 3)]]
     :background-color "#f3f3ef"
     :padding (em 0.3)
     :padding-bottom (em 1)}
    [:.level1
     {:font-size (em 1.3)}]
    [:.level2
     {:font-size (em 1.1)
      :font-weight :bold
      :color "#3c3530"}]
    [:.filter-row
     {:margin-top (px 4)}
     [:.chosen-container
      {:font-size (em 0.8)}]]
    [:.filter-widget-slider-container
     [:div:first-child
      {:width (percent 75)}]]
    [:.filter-widget-slider-label
     {:font-size (em 0.85)
      :margin-top (px -10)
      :margin-left (px 8)}]]]

  [:.settings-open
   {:box-shadow [[(px 2) (px 2) (px 4) :silver]]
    :border [[(px 1) :silver 'solid]]}]

  [:.settings-permanent
   {:top (em 0.3)
    :right (em 0.3)
    :border :none}])
