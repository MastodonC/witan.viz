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
        (dissoc :settings?)
        (dissoc :filters)
        (dissoc :url)))))

(re-frame/reg-sub
 :settings
 (fn [db]
   (select-keys db [:settings-open? :settings?])))

(re-frame/reg-sub
 :filters
 (fn [db]
   (select-keys db [:filters])))

(re-frame/reg-sub
 :data-schema
 (fn [db]
   (select-keys db [:data-schema])))
