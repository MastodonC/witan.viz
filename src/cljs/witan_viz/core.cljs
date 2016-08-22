(ns witan-viz.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [devtools.core :as devtools]
            [witan-viz.handlers]
            [witan-viz.subs]
            [witan-viz.views :as views]
            [witan-viz.config :as config]
            [taoensso.timbre :as log]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (log/debug "DEV MODE")
    (devtools/install!)))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
