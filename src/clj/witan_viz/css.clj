(ns witan-viz.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:body {:background-color 'transparent}]
  [:#app {:height "100%"}]
  [:#table {:width "100%"}])
