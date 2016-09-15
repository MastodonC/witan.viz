(ns witan-viz.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            ;;
            [witan-viz.filter :as f]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn settings-dialog
  []
  (let [settings (re-frame/subscribe [:settings])]
    (fn []
      (let [{:keys [settings-open? settings?]} @settings
            setting-widget (fn [label control]
                             [re-com/h-box
                              :children [[re-com/label
                                          :label label
                                          :width "50px"]
                                         [control]]])
            settings-children [(setting-widget "Filters" f/filter-dialog)]]
        [re-com/v-box
         :class "settings"
         :justify :end
         :align :end
         :children [(when (or settings? settings-open?)
                      [re-com/md-icon-button
                       :md-icon-name (if settings-open? "zmdi-close" "zmdi-settings")
                       :style (if settings-open? {} {:border-radius "3px"})
                       :class "settings-button"
                       :on-click #(if settings-open?
                                    (re-frame/dispatch [:close-settings])
                                    (re-frame/dispatch [:open-settings]))])
                    (when settings-open?
                      [re-com/v-box
                       :class "settings-box"
                       :width "100%"
                       :children [[re-com/title
                                   :label "Settings"
                                   :level :level1]
                                  [re-com/line]
                                  [re-com/gap :size "5px"]
                                  [re-com/title
                                   :label "Data Settings"
                                   :level :level2]
                                  [re-com/gap :size "5px"]
                                  [re-com/v-box
                                   :children settings-children]]])]]))))

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
        (let [{:keys [error
                      spinner?
                      ready?] :as d} @display]
          [:div
           {:style {:width "100%"
                    :height "100%"}}
           (if (or error ready?)
             (visualisation d)
             (if spinner?
               [re-com/box
                :width "100%"
                :height "100%"
                :align :center
                :justify :center
                :child [re-com/throbber :size :large]]
               [:div]))
           (when ready?
             [settings-dialog])]))})))
