(ns witan-viz.handlers
    (:require [re-frame.core :as re-frame]
              [witan-viz.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (db/make-db)))

(re-frame/reg-event-db
 :re-fetch-data
 (fn  [db [_ data]]
   (db/remake-db db data)))

(re-frame/reg-event-db
 :got-data
 (fn  [db [_ data]]
   (assoc db :data data)))
