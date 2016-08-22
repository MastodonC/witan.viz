(ns witan-viz.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :display
 (fn [db]
   (merge
    {:ready? (-> (:data db) nil? not)}
    (select-keys db [:data :style]))))
