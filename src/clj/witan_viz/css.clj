(ns witan-viz.css
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
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
     :padding (px 3)
     :white-space 'nowrap
     :border-radius (px 4)}]])
