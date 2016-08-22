(ns witan-viz.handlers
    (:require [re-frame.core :as re-frame]
              [witan-viz.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (db/make-db)))

(re-frame/reg-event-db
 :got-data
 (fn  [db [_ data]]
   (assoc db :data data)))
