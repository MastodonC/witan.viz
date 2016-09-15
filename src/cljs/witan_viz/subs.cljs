(ns witan-viz.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

(re-frame/reg-sub
 :display
 (fn [db]
   (merge
    {:ready? (-> (:data db) nil? not)}
    (-> db
        (dissoc :settings-open?)
        (dissoc :settings?)))))

(re-frame/reg-sub
 :settings
 (fn [db]
   (select-keys db [:settings-open? :settings?])))
