(ns witan-viz.data
  (:require [taoensso.timbre :as log]
            [ajax.core :refer [GET]]
            [re-frame.core :as re-frame]
            [cljs.core.async :as async]
            [witan.gateway.schema :as wgs]
            [chord.client :refer [ws-ch]]
            [clojure.string :as str])

  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [witan-viz.env :as env :refer [cljs-env]]))

(def config {:gateway/address (or (cljs-env :witan-api-url) "localhost:30015")})

(defonce ws-out (async/chan 10))
(defonce ws-conn (atom nil))
(defonce message-id (atom 0))
(defonce data-results (atom {}))

(defn read-csv
  [data]
  (map #(str/split % #",")
       (str/split data #"\n")))

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
                  workspace/original-location]} params
          handler (fn [response]
                    (let [ch (get @data-results original-location)]
                      (if ch
                        (go (>! ch (hash-map :location original-location :data (read-csv response))))
                        (log/debug "H!?!??!?!" original-location @data-results))))
          error-handler (fn [response]
                          (log/error response)
                          (let [ch (get @data-results original-location)]
                            (if ch
                              (go (>! ch (hash-map :location original-location :error response)))
                              (log/debug "E!?!??!?!" original-location @data-results))))]
      (GET result-url  {:handler handler
                        :error-handler error-handler}))))

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
  (swap! data-results assoc location ch)
  (command! :workspace/create-result-url "1.0.0" {:workspace/result-location location}))

(defn fetch-datasets
  [datasets]
  (if-not (coll? datasets)
    (fetch-datasets [datasets])
    (let [_ (when-not @ws-conn (connect!))
          payloads (map #(hash-map :location % :ch (async/chan)) datasets)
          out (async/merge (map :ch payloads))]
      (run! fetch-dataset payloads)
      (go-loop [agg []]
        (let [{:keys [location data error] :as result} (async/<! out)
              agg' (conj agg result)]
          (log/debug "Got result from" location)
          (if (= (count agg') (count datasets))
            (re-frame/dispatch [:got-data agg'])
            (recur agg')))))))

(defn send-ready-message!
  [pym id]
  (let [h (.toString (.-offsetHeight (.getElementById js/document id)))]
    (.sendMessage pym "height" h)
    (.sendMessage pym "ready" "1") ;; needs a value else regex fail
    (log/info "Ready sent" h)))
