(ns witan-viz.filter
  (:require [taoensso.timbre :as log]
            [reagent.core :as r]
            [re-frame.core :as re-frame]
            [goog.string :as gstr]
            [clojure.string :as str]
            [re-com.core :as re-com]))

(defrecord Filter
    [label column operation variable]
  Object
  (toString [_]
    (str column (gstr/urlEncode operation) variable)))

(def operations
  [{:id 1 :symbol "="  :label "Equal To"                 :function =}
   {:id 2 :symbol "!=" :label "Not Equal To"             :function (comp not =)}
   {:id 3 :symbol ">"  :label "Greater Than"             :function >}
   {:id 4 :symbol "<"  :label "Less Than"                :function <}
   {:id 5 :symbol ">=" :label "Greater Than or Equal To" :function >=}
   {:id 6 :symbol "<=" :label "Less Than or Equal To"    :function <=}])

(defn str->operation
  [op]
  (some #(when (= op (:symbol %)) (:function %)) operations))

(defn apply-filters
  [filters id {:keys [location data] :as dataset}]
  (let [[headers & rows] data
        result (if rows
                 (concat [headers]
                         (reduce (fn [a {:keys [column label variable operation] :as f}]
                                   (if (and (not (str/blank? variable))
                                            (or (not label) (= label id)))
                                     (let [col-idx (.indexOf headers column)
                                           op (str->operation operation)]
                                       (if (= -1 col-idx)
                                         (do (log/warn "Filter couldn't find column:" column) a)
                                         (if-not op
                                           (do (log/warn "Filter couldn't match operation:" operation) a)
                                           (filter #(op (nth % col-idx) variable) a))))
                                     a))
                                 rows filters)) data)]
    (assoc dataset :data result)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn adjust-filter
  [old key value]
  (re-frame/dispatch [:update-filter old key value]))

(defn id->label
  [coll val]
  (some (fn [c] (when (= val (:id c)) (:label c))) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti variable-control
  (fn [schema filter] (:type schema)))

(defmethod variable-control
  :default
  [_ _]
  [:div])

(defmethod variable-control
  :string
  [{:keys [range]}
   {:keys [variable] :as filter}]
  (let [choices (map-indexed #(hash-map :id %1 :label %2) range)
        idx (some (fn [c] (when (= variable (:label c)) (:id c))) choices)]
    [re-com/single-dropdown
     :choices choices
     :placeholder "Select value..."
     :on-change #(adjust-filter filter :variable (id->label choices %))
     :model idx]))

(defmethod variable-control
  :number
  [schema filter]
  (let [v (r/atom nil)
        t (atom nil)]
    (fn [{:keys [range]}
         {:keys [variable] :as filter}]
      (let [v2 (js/parseFloat variable)
            value? (not (js/isNaN v2))
            v2 (if value? v2 (first range))]
        [re-com/h-box
         :class "filter-widget-slider-container"
         :width "100%"
         :children[[re-com/slider
                    :width "100%"
                    :min (first range)
                    :max (last range)
                    :on-change #(do
                                  (reset! v %)
                                  (when @t
                                    (js/clearTimeout @t))
                                  (reset! t (js/setTimeout
                                             (fn []
                                               (adjust-filter filter :variable @v)) 300)))
                    :model (or @v v2)]
                   [re-com/box
                    :align :center
                    :width "100px"
                    :child [re-com/label
                            :class "filter-widget-slider-label"
                            :label (if value? (or @v v2) "???")]]]]))))

(defn filter-dialog
  []
  (let [filters     (re-frame/subscribe [:filters])
        data-schema (re-frame/subscribe [:data-schema])]
    (fn []
      (let [{:keys [filters]} @filters
            {:keys [data-schema]} @data-schema
            col-choices (vec (map-indexed
                              (fn [i a]
                                {:id i :label a})
                              (reduce concat []
                                      (map (fn [[label {:keys [columns]}]]
                                             (map :name columns)) data-schema))))
            schema  (reduce (fn [a c]
                              (reduce #(assoc %1 (:name %2) %2) a c))
                            {}
                            (map (comp :columns second) data-schema))]
        [re-com/box
         :size "auto"
         :child [re-com/v-box
                 :gap "8px"
                 :align :stretch
                 :children [[re-com/md-circle-icon-button
                             :md-icon-name "zmdi-plus"
                             :size :smaller
                             :on-click #(re-frame/dispatch [:add-filter])]
                            (doall
                             (for [{:keys [column operation variable] :as f} filters]
                               (let [col-idx (some #(when (= column (:label %)) (:id %)) col-choices)
                                     op-idx  (some #(when (= operation (:symbol %)) (:id %)) operations)
                                     other-columns (->> filters
                                                        (remove #{f})
                                                        (map :column)
                                                        (set))
                                     box-width "130px"]
                                 ^{:key (or column "new")}
                                 [re-com/h-box
                                  :class "filter-row"
                                  :gap "5px"
                                  :width "100%"
                                  :children [[re-com/box
                                              :child [re-com/md-circle-icon-button
                                                      :md-icon-name "zmdi-delete"
                                                      :size :smaller
                                                      :on-click #(re-frame/dispatch [:delete-filter f])]]
                                             [re-com/box
                                              :class "filter-control-column"
                                              :width box-width
                                              :child [re-com/single-dropdown
                                                      :choices (remove #(contains? other-columns (:label %)) col-choices)
                                                      :placeholder "Select column..."
                                                      :on-change #(adjust-filter f :column (id->label col-choices %))
                                                      :model col-idx]]
                                             [re-com/box
                                              :class "filter-control-operation"
                                              :width box-width
                                              :child [re-com/single-dropdown
                                                      :choices operations
                                                      :placeholder "Select operation..."
                                                      :on-change #()
                                                      :label-fn #(str (:label %) " (" (:symbol %) ")")
                                                      :model op-idx]]
                                             [re-com/box
                                              :class "filter-control-variable"
                                              :width box-width
                                              :child [variable-control (get schema column) f]]]])))]]]))))
