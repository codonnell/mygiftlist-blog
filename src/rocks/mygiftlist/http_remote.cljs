(ns rocks.mygiftlist.http-remote
  (:refer-clojure :exclude [send])
  (:require
   [clojure.string :as str]
   [cognitect.transit :as ct]
   [com.fulcrologic.fulcro.algorithms.transit :as t]
   [com.fulcrologic.fulcro.algorithms.tx-processing :as txn]
   [com.fulcrologic.fulcro.networking.http-remote :as f.http]
   [clojure.core.async :refer [go <!]]
   [com.wsscode.async.async-cljs :refer [let-chan]]
   [edn-query-language.core :as eql]
   [goog.events :as events]
   [taoensso.timbre :as log]
   [rocks.mygiftlist.authentication :as auth])
  (:import [goog.net EventType]))

(defn wrap-fulcro-request
  ([handler addl-transit-handlers transit-transformation]
   (let [writer (t/writer (cond-> {}
                            addl-transit-handlers
                            (assoc :handlers addl-transit-handlers)

                            transit-transformation
                            (assoc :transform transit-transformation)))]
     (fn [{:keys [headers] :as request}]
       (go
         (let [access-token (<! (auth/get-access-token))
               [body response-type] (f.http/desired-response-type request)
               body    (ct/write writer body)
               headers (assoc headers
                         "Content-Type" "application/transit+json"
                         "Authorization" (str "Bearer " access-token))]
           (handler (merge request
                      {:body body
                       :headers headers
                       :method :post
                       :response-type response-type})))))))
  ([handler addl-transit-handlers]
   (wrap-fulcro-request handler addl-transit-handlers nil))
  ([handler]
   (wrap-fulcro-request handler nil nil))
  ([]
   (wrap-fulcro-request identity nil nil)))

(defn fulcro-http-remote
  "Create a remote that (by default) communicates with the given url
  (which defaults to `/api`).

  The request middleware is a `(fn [request] modified-request)`. The
  `request` will have `:url`, `:body`, `:method`, and `:headers`. The
  request middleware defaults to `wrap-fulcro-request` (which encodes
  the request in transit+json). The result of this middleware chain on
  the outgoing request becomes the real outgoing request. It is
  allowed to modify the `url`.

  If the the request middleware returns a corrupt request or throws an
  exception then the remote code will immediately abort the request.
  The return value of the middleware will be used to generate a
  request to `:url`, with `:method` (e.g. :post), and the given
  headers. The body will be sent as-is without further translation.
  `response-middleware` is a function that returns a function `(fn
  [response] mod-response)` and defaults to `wrap-fulcro-response`
  which decodes the raw response and transforms it back to a response
  that Fulcro can merge.

  The response will be a map containing the `:outgoing-request` which
  is the exact request sent on the network; `:body`, which is the raw
  data of the response. Additionally, there will be one or more of the
  following to indicate low-level details of the result:
  `:status-code`, `:status-text`, `:error-code` (one of :none,
  :exception, :http-error, :abort, or :timeout), and `:error-text`.

  Middleware is allowed to morph any of this to suit its needs.

  DEPRECATED: If the response middleware includes a `:transaction` key
  in the response with EQL, then that EQL will be used in the
  resulting Fulcro merge steps. This can seriously screw up built-in
  behaviors. You are much better off ensuring that your query matches
  the shape of the desired response in most cases.

  The definition of `remote-error?` in the application will deterimine
  if happy-path or error handling will be applied to the response. The
  default setting in Fulcro will cause a result with a 200 status code
  to cause whatever happy-path logic is configured for that specific
  response's processing.

  For example, see `m/default-result-action!` for mutations, and
  `df/internal-load` for loads. The `:body` key will be considered the
  response to use, and the optional `:transaction` key an override to
  the EQL query used for any merges.

  See the top-level application configuration and Developer's Guide
  for more details."
  [{:keys [url request-middleware response-middleware make-xhrio]
    :or   {url                 "/api"
           response-middleware (f.http/wrap-fulcro-response)
           request-middleware  (wrap-fulcro-request)
           make-xhrio          f.http/make-xhrio}
    :as   options}]
  (merge options
    {:active-requests (atom {})
     :transmit!
     (fn transmit! [{:keys [active-requests]}
                    {::txn/keys [ast result-handler update-handler]
                     :as        send-node}]
       (go (let [edn              (eql/ast->query ast)
                 ok-handler       (fn [result]
                                    (try
                                      (result-handler result)
                                      (catch :default e
                                        (log/error e "Result handler for remote" url "failed with an exception."))))
                 progress-handler (fn [update-msg]
                                    (let [msg {:status-code      200
                                               :raw-progress     (select-keys update-msg [:progress-phase :progress-event])
                                               :overall-progress (f.http/progress% update-msg :overall)
                                               :receive-progress (f.http/progress% update-msg :receiving)
                                               :send-progress    (f.http/progress% update-msg :sending)}]
                                      (when update-handler
                                        (try
                                          (update-handler msg)
                                          (catch :default e
                                            (log/error e "Update handler for remote" url "failed with an exception."))))))
                 error-handler    (fn [error-result]
                                    (try
                                      (result-handler (merge error-result {:status-code 500}))
                                      (catch :default e
                                        (log/error e "Error handler for remote" url "failed with an exception."))))]
             (let-chan [real-request (try
                                       (request-middleware {:headers {} :body edn :url url :method :post})
                                       (catch :default e
                                         (log/error e "Send aborted due to middleware failure ")
                                         nil))]
               (if real-request
                 (let [abort-id                                        (or
                                                                         (-> send-node ::txn/options ::txn/abort-id)
                                                                         (-> send-node ::txn/options :abort-id))
                       xhrio                                           (make-xhrio)
                       {:keys [body headers url method response-type]} real-request
                       http-verb                                       (-> (or method :post) name str/upper-case)
                       extract-response                                #(f.http/extract-response body real-request xhrio)
                       extract-response-mw                             (f.http/response-extractor* response-middleware edn real-request xhrio)
                       gc-network-resources                            (f.http/cleanup-routine* abort-id active-requests xhrio)
                       progress-routine                                (f.http/progress-routine* extract-response progress-handler)
                       ok-routine                                      (f.http/ok-routine* progress-routine extract-response-mw ok-handler error-handler)
                       error-routine                                   (f.http/error-routine* extract-response-mw ok-routine progress-routine error-handler)
                       with-cleanup                                    (fn [f] (fn [evt] (try (f evt) (finally (gc-network-resources)))))]
                   (when abort-id
                     (swap! active-requests update abort-id (fnil conj #{}) xhrio))
                   (when (and (f.http/legal-response-types response-type) (not= :default response-type))
                     (.setResponseType ^js xhrio (get f.http/response-types response-type)))
                   (when progress-handler
                     (f.http/xhrio-enable-progress-events xhrio)
                     (events/listen xhrio (.-DOWNLOAD_PROGRESS ^js EventType) #(progress-routine :receiving %))
                     (events/listen xhrio (.-UPLOAD_PROGRESS ^js EventType) #(progress-routine :sending %)))
                   (events/listen xhrio (.-SUCCESS ^js EventType) (with-cleanup ok-routine))
                   (events/listen xhrio (.-ABORT ^js EventType) (with-cleanup #(ok-handler {:status-text   "Cancelled"
                                                                                            ::txn/aborted? true})))
                   (events/listen xhrio (.-ERROR ^js EventType) (with-cleanup error-routine))
                   (f.http/xhrio-send xhrio url http-verb body headers))
                 (error-handler {:error :abort :error-text "Transmission was aborted because the request middleware returned nil or threw an exception"}))))))
     :abort!          (fn abort! [this id]
                        (if-let [xhrios (get @(:active-requests this) id)]
                          (doseq [xhrio xhrios]
                            (f.http/xhrio-abort xhrio))
                          (log/info "Unable to abort. No active request with abort id:" id)))}))
