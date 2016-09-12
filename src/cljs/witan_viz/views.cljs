(ns witan-viz.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            ;;
            [witan-viz.views.table :as table]
            [witan-viz.views.lineplot :as lineplot]))

(def     pym (.Child js/pym))

(.onMessage pym "arguments" (fn [args]
                              (log/debug "Got request to reload" args)
                              (re-frame/dispatch [:re-load (str (.. js/window -location -origin) "?" args)])))

(defn re-draw
  [_]
  (let [el (.getElementById js/document "vizapp")
        w  (.-offsetWidth el)
        h  (.-offsetHeight el)]
    (re-frame/dispatch [:force-re-draw {:width w
                                        :height h}])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti visualisation
  (fn [{:keys [style error]}]
    (if error
      :_error
      style)))

(defmethod visualisation
  :default
  [{:keys [style]}]
  [re-com/label :label (str "Unrecognised style: " style)])

(defmethod visualisation
  :lineplot
  [m]
  [lineplot/view m pym])

(defmethod visualisation
  :table
  [m]
  [table/view m pym])

(defmethod visualisation
  :blank
  [m]
  [:div]) ;; purposefully blank

(defmethod visualisation
  :_error
  [m]
  [:div
   [:h1 "ERROR"]
   [:h2 (:error m)]])

(defn main-panel []
  (let [display    (re-frame/subscribe [:display])]
    (r/create-class
     {:component-will-mount
      (fn [this]
        ;; aw: This is horrible hack, but fixes an issue with hosting the app in an iframe.
        (add-watch db/app-db :data
                   (fn [k r old new]
                     (when (get @r k)
                       (r/force-update this)))))
      :component-will-unmount
      (fn [this])
      :component-did-mount
      (fn [this]
        (.addEventListener js/window "resize" re-draw)
        #_(.addEventListener js/window "error"  send-error))
      :reagent-render
      (fn []
        (let [{:keys [error] :as d} @display]
          (if (or error (:ready? d))
            (visualisation d)
            (if (:spinner d)
              [re-com/box
               :width "100%"
               :height "100%"
               :align :center
               :justify :center
               :child [re-com/throbber :size :large]]
              [:div]))))})))
