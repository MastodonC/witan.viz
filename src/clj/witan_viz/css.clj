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
    :top (em 0.3)
    :right (em 0.3)
    :width (percent 50)}
   [:.settings-button
    {:background-color "#eee"
     :color "#777"
     :padding (em 0.1)
     :display :inline-table
     :border-radius [[(px 3) (px 3) (px 0) (px 0)]]}]
   [:.settings-box
    {:border-radius [[(px 3) (px 0) (px 3) (px 3)]]
     :background-color "#eee"
     :padding (em 0.3)}
    [:.level1
     {:font-size (em 1.3)}]
    [:.level2
     {:font-size (em 1.1)
      :font-weight :bold}]]])
