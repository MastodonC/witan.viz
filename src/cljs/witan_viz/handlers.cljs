(ns witan-viz.handlers
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [witan-viz.db :as db]
            [witan-viz.filter :as f]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; https://github.com/venantius/accountant/blob/c7609e47f4be76c2a7e20bb305df3a9e9f242617/src/accountant/core.cljs#L115
(defn map->params [query]
  (let [params (map #(name %) (keys query))
        values (vals query)
        pairs (partition 2 (interleave params values))]
    (str/join "&" (map #(str/join "=" %) pairs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (db/make-db)))

(re-frame/reg-event-db
 :reload-db
 (fn  [_ [_ data]]
   (db/make-db data)))

(re-frame/reg-event-db
 :refresh-db
 (fn  [db [_ uri]]
   (db/refresh-db db uri)))

(re-frame/reg-event-db
 :got-data
 (fn  [db [_ {:keys [data data-schema]}]]
   (assoc db
          :data data
          :data-schema data-schema)))

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

(re-frame/reg-event-db
 :toggle-settings
 (fn  [db [_ msg]]
   (log/info "Toggling settings window...")
   (update db :settings-open? not)))

(re-frame/reg-event-db
 :update-filter
 (fn [{:keys [filters] :as db} [_ old key value]]
   (log/info "Update filter" old "key" key "value" value)
   (let [filter-idx  (.indexOf filters old)
         new-filter (assoc old key value)
         new-filter (if (= :column key) (dissoc new-filter :variable) new-filter)]
     (when (not= -1 filter-idx)
       (re-frame/dispatch [:update-uri])
       (assoc-in db [:filters filter-idx] (f/map->Filter. new-filter))))))

(re-frame/reg-event-db
 :delete-filter
 (fn [db [_ f]]
   (log/info "Delete filter" f)
   (re-frame/dispatch [:update-uri])
   (update db :filters #(remove #{f} %))))

(re-frame/reg-event-db
 :add-filter
 (fn [{:keys [filters] :as db} _]
   (log/debug "f" filters)
   (if (some #(when (nil? (:column %)) 1) filters)
     db
     (do
       (log/info "Add filter")
       (update db :filters #(conj (vec %) (f/->Filter. nil nil "=")))))))

(re-frame/reg-event-db
 :update-uri
 (fn [db _]
   (let [p            {:data      ["data"     #(str/join ","
                                                         (map (fn [[k v]]
                                                                (str k "::" (:location v)) ) %))]
                       :style     ["style"    name]
                       :filters   ["filter"   #(str/join "," (map str %))]
                       :spinner?  ["spinner"  str]
                       :settings? ["settings" str]}
         qs           (->> p
                           (map (fn [[k [v f]]] (hash-map v (->> k (get db) (f)))))
                           (into {}))
         qs-with-args (->>  (:args db)
                            (map (fn [[k v]] (hash-map (str "args[" (name k) "]") (str v))))
                            (into qs))
         qs-str       (map->params qs-with-args)
         path         (.. js/document -location -pathname)
         uri          (str path "?" qs-str)]
     (log/debug "New URI" uri)
     (.replaceState js/history nil nil uri)
     (re-frame/dispatch [:refresh-db uri]))
   db))
