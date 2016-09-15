(ns witan-viz.handlers
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [witan-viz.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (db/make-db)))

(re-frame/reg-event-db
 :re-load
 (fn  [_ [_ data]]
   (db/make-db data)))

(re-frame/reg-event-db
 :got-data
 (fn  [db [_ data]]
   (assoc db :data data)))

(re-frame/reg-event-db
 :force-re-draw
 (fn  [db [_ dims]]
   (if dims
     (assoc db :dimensions dims)
     db)))

(re-frame/reg-event-db
 :raise-error
 (fn  [db [_ msg]]
   (log/error "An error was raised:" msg)
   (assoc db :error msg)))

(re-frame/reg-event-db
 :open-settings
 (fn  [db [_ msg]]
   (log/info "Opening settings window...")
   (assoc db :settings-open? true)))

(re-frame/reg-event-db
 :close-settings
 (fn  [db [_ msg]]
   (log/info "Closing settings window...")
   (assoc db :settings-open? false)))
