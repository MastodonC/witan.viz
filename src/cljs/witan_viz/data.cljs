(ns witan-viz.data
  (:require [taoensso.timbre :as log]
            [ajax.core :refer [GET]]
            [re-frame.core :as re-frame]
            [cljs.core.async :as async]
            [witan.gateway.schema :as wgs]
            [chord.client :refer [ws-ch]]
            [clojure.string :as str]
            [witan-viz.filter :as f])

  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [witan-viz.env :as env :refer [cljs-env]]))

(def config {:gateway/address (or (cljs-env :witan-api-url) "localhost:30015")})

(defonce ws-out (async/chan 10))
(defonce ws-conn (atom nil))
(defonce message-id (atom 0))
(defonce data-results (atom {}))
(defonce data-cache (atom {}))

(defn read-csv
  [data]
  (map #(str/split % #",")
       (str/split data #"\n")))

(defn get-response-handler
  [original-location response]
  (let [ch (get @data-results original-location)]
    (if ch
      (go (>! ch (hash-map :location original-location :data (read-csv response))))
      (log/debug "H!?!??!?!" original-location @data-results))))

(defn get-error-response-handler
  [original-location response]
  (log/error response)
  (let [ch (get @data-results original-location)]
    (if ch
      (go (>! ch (hash-map :location original-location :error response)))
      (log/debug "E!?!??!?!" original-location @data-results))))

(defn download-csv
  [original-location url]
  (GET url
       {:handler (partial get-response-handler original-location)
        :error-handler (partial get-error-response-handler original-location)}))

(defmulti handle-server-message
  (fn [{:keys [message/type]}] type))

(defmethod handle-server-message
  :default
  [msg]
  (log/warn "Unknown message:" msg))

(defmethod handle-server-message
  :command-receipt
  [msg])

(defmethod handle-server-message
  :event
  [{:keys [event/key event/params] :as event}]
  (when (= :workspace/result-url-created key)
    (let [{:keys [workspace/result-url
                  workspace/original-location]} params]
      (download-csv original-location result-url))))

(defn command!
  [command-key version params]
  (let [id (swap! message-id inc)
        m {:message/type :command
           :command/key command-key
           :command/version version
           :command/id id
           :command/params params}]
    (log/debug "Sending command:" m)
    (go (>! ws-out m))))

(defn connect!
  []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch (str "ws://"
                                                     (get config :gateway/address)
                                                     "/ws")))]
      (if-not error
        (do
          (reset! ws-conn ws-channel)
          (async/pipe ws-out @ws-conn)
          (go-loop []
            (let [{:keys [message] :as resp} (<! ws-channel)]
              (if message
                (if (contains? message :error)
                  (log/error (str "Received message error: " message))
                  (if-let [err (wgs/check-message "1.0.0" message)]
                    (log/error (str "Received message failed validation: " (str err)))
                    (do
                      (handle-server-message message)
                      (recur))))
                (log/warn "Websocket connection lost" resp)))))
        (log/error (str "WS connection error: " (pr-str error)))))))

(defn fetch-dataset
  [{:keys [location ch]}]
  (if-let [data (get @data-cache location)]
    (do
      (log/debug "Using cached data for" location)
      (go (>! ch (hash-map :location location :data data))))
    (do
      (swap! data-results assoc location ch)
      (if (.startsWith location "http")
        (download-csv location location)
        (command! :workspace/create-result-url "1.0.0" {:workspace/result-location location})))))

(defn build-schema
  [label {:keys [location data] :as dataset}]
  (let [[headers & rows] data]
    {:label label
     :columns (mapv #(let [idx (.indexOf headers %)
                           vals (map (fn [r] (nth r idx)) rows)
                           number? (not (js/isNaN (js/parseFloat (first vals))))
                           vals' (if number? (map js/parseFloat vals) vals)]
                       (hash-map
                        :name %
                        :idx idx
                        :type (if number? :number :string)
                        :range (if number? [(apply min vals')
                                            (apply max vals')]
                                   (set vals')))) headers)}))

(defn fetch-datasets
  [datasets {:keys [filters]}]
  (if-not (coll? datasets)
    (fetch-datasets [datasets])
    (let [_ (when-not @ws-conn (connect!))
          payloads (map-indexed (fn [i ds]
                                  (let [grps (re-find #"^(.*)::(.+)$" ds)
                                        id (str (or (nth grps 1) i))
                                        ds (or (nth grps 2) ds)]
                                    (hash-map :id (if (clojure.string/blank? id) i id)
                                              :location ds
                                              :ch (async/chan)))) datasets)
          out (async/merge (map :ch payloads))]
      (run! fetch-dataset payloads)
      (go-loop [agg {}]
        (let [{:keys [location data error] :as result} (async/<! out)
              id (some #(when (= location (:location %)) (:id %)) payloads)
              agg' (merge agg {id result})]
          (log/debug "Got result from" id location)
          (if error
            (re-frame/dispatch [:raise-error (str error)])
            (do
              (swap! data-cache assoc location data)
              (if (= (count agg') (count datasets))
                (let [result (into {} (map (fn [[k v]]
                                             {k (f/apply-filters filters k v)}) agg'))
                      schema (into {} (map (fn [[k v]]
                                             {k (build-schema k v)}) agg'))]
                  (re-frame/dispatch [:got-data {:data result
                                                 :data-schema schema}]))
                (recur agg')))))))))

(defn send-ready-message!
  [pym h]
  (.sendMessage pym "height" h)
  (.sendMessage pym "ready" "1") ;; needs a value else regex fail
  (log/info "Ready sent" h))
