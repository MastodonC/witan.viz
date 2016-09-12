(ns witan-viz.views.lineplot
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [taoensso.timbre :as log]
            [reagent.core :as r]
            [witan-viz.data :refer [send-ready-message!]]
            ;;
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.svg.adapter :as svgadapter]
            [thi.ng.geom.core.vector :as v]
            [thi.ng.color.core :as col]
            [thi.ng.color.presets.categories :as cc]
            [thi.ng.math.core :as m :refer [PI TWO_PI]]))

(def default-dims [800 400])
(defonce chart-dims (atom nil))

(defn color-preset
  [i]
  (-> (nth cc/cat10 i) col/int24 col/as-css))

(defn test-equation
  [t] (let [x (m/mix (- PI) PI t)] [x (* (Math/cos (* 0.5 x)) (Math/sin (* x x x)))]))

(defn bake-viz
  [viz w h]
  (->> viz (svg/svg {:width w :height h})))

(defmulti common-coerce
  (fn [k v] k))

(defmethod common-coerce
  "year"
  [_ v]
  (js/parseInt v))

(defmethod common-coerce
  "popn"
  [_ v]
  (js/parseFloat v))

(defn show-tool-tip
  [element]
  (let [tt (.querySelector js/document "#lineplot #tooltip")
        mw (/ (first @chart-dims) 3)
        x (.-value (.-baseVal (aget (.-target element) "cx")))
        y (.-value (.-baseVal (aget (.-target element) "cy")))
        data (.-value (aget (.-attributes (.-target element)) "data-viz"))]
    (when tt
      (aset tt "innerText" data)
      (aset (.-style tt) "display" "inherit")
      (aset (.-style tt)
            (if (<= x mw) "left" "right")
            (str (if (<= x mw) x (- (first @chart-dims) x)) "px"))
      (aset (.-style tt) "top"  (str (- y 32) "px")))))

(defn hide-tool-tip
  [element]
  (let [tt (.querySelector js/document "#lineplot #tooltip")]
    (when tt
      (aset (.-style tt) "display" "none")
      (aset (.-style tt) "left" nil)
      (aset (.-style tt) "right" nil))))

(defn svg-scatter-plot-with-hover
  [v-spec {:keys [attribs shape values] :as d-spec}]
  (let [[header attr & circles] (viz/svg-scatter-plot v-spec d-spec)
        circles' (map-indexed
                  (fn [i c] (let [[x y] (nth values i)]
                              (update c 1 assoc
                                      :data-viz (str "x: " x "\ny: " y)
                                      :class "lineplot-point"
                                      :on-mouse-over show-tool-tip
                                      :on-mouse-leave hide-tool-tip))) circles)]
    (vec (concat [header attr] circles'))))

(defn round-up-to-mod
  [t v]
  (loop [x (int v)]
    (if (zero? (mod x t)) x (recur (inc x)))))

(defn viz-spec
  [x y w h data]
  (let [mmfn (juxt (partial apply min) (partial apply max))
        x-minmax-data (map #(mmfn (map first %)) data)
        y-minmax-data (map #(mmfn (map last  %)) data)
        [x-min x-max] [(apply min (map first x-minmax-data))
                       (apply max (map last x-minmax-data))]
        [y-min y-max] [(apply min (map first y-minmax-data))
                       (apply max (map last y-minmax-data))]
        [y-min y-max] [(* y-min 0.9) (* y-max 1.1)]
        x-delta (- x-max x-min)
        y-delta (- y-max y-min)
        x-major (int (/ x-delta 6))
        x-minor (int (/ x-delta 12))
        y-major (int (round-up-to-mod 5 (/ y-delta 6)))
        datav   (reduce
                 (fn [a [i d]]
                   (concat a [{:values d
                               :attribs {:fill "none"
                                         :stroke (color-preset i)
                                         :stroke-width "3"}
                               :layout  viz/svg-line-plot}
                              {:values  d
                               :attribs {:fill (color-preset i)
                                         :stroke "none"}
                               :layout  svg-scatter-plot-with-hover}])) []
                 (map-indexed vector data))]
    {:x-axis (viz/linear-axis
              {:domain [x-min x-max]
               :range  [50 (- w 10)]
               :major  x-major
               :minor  x-minor
               :label  (viz/default-svg-label str)
               :pos    (- h 20)})
     :y-axis (viz/linear-axis
              {:domain      [y-min y-max]
               :range       [(- h 20) 20]
               :major       y-major
               :label       (viz/default-svg-label str)
               :pos         50
               :label-dist  12
               :label-style {:text-anchor "end"}})
     :grid   {:attribs {:stroke "#caa"}
              :minor-y true}
     :data   datav}))

(defn prepare-data
  [[headers & rows] x y]
  (let [x-idx (.indexOf headers x)
        y-idx (.indexOf headers y)]
    (if (or (= -1 x-idx)
            (= -1 y-idx))
      (do
        (re-frame/dispatch [:raise-error
                            (str "Couldn't locate an axis. x:" x " " x-idx " y:" y " " y-idx)])
        nil)
      (reduce (fn [a r] (conj a [(common-coerce x (nth r x-idx))
                                 (common-coerce y (nth r y-idx))])) [] rows))))

(defn view
  [_ _]
  (fn [_ pym]
    (r/create-class
     {:component-will-mount
      (fn [this]
        (let [el (.getElementById js/document "vizapp")
              w  (.-offsetWidth el)
              h  (.-offsetHeight el)
              w' (if (zero? w) (first default-dims) w)
              h' (if (zero? h) (second default-dims) h)]
          (reset! chart-dims [w' h'])))
      :component-did-mount
      (fn [this]
        (send-ready-message! pym (+ (second @chart-dims) 0)))
      :reagent-render
      (fn [{:keys [data args] :as params}]
        (let [{:keys [width height]
               :or {width (first @chart-dims)
                    height (second @chart-dims)}} (:dimensions params)
              {:keys [x y]} args
              datav (mapv (fn [[k v]] (vec (:data v))) data)
              prepped-data (mapv #(prepare-data % x y) datav)]
          (reset! chart-dims [width height])
          (when-not (empty? prepped-data)
            [:div
             {:id "lineplot"}
             (-> (viz-spec x y width (- height 10) prepped-data)
                 (viz/svg-plot2d-cartesian)
                 (svgadapter/inject-element-attribs)
                 (bake-viz width height))
             [:div#tooltip "hello, world"]])))})))
