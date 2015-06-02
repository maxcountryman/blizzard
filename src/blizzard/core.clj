(ns blizzard.core
  "HTTP Flake ID generation service.

  Blizzard provides a Ring middleware which adds two routes, /flake and
  /flake/:n which generate a single flake and N flakes, respectively. Responses
  are of content type application/transit+json and as such a transit client
  should be used to read them.

  While Blizzard may to used as a standalone service, because it is implemented
  as as a middleware it can be attached to any application where the routes do
  not conflict. For example:

    => (def handler (-> (constantly {:status 404 :body \"Not Found\"})
                        wrap-flake))

  Of course, other middleware may be added as desired. Note that above handler
  should be passed to an adapter such as ring-jetty.

  For standalone use, the following environment variables provide runtime
  configuration:

  BLIZZARD_HOST       - Defaults to localhost.
  BLIZZARD_PORT       - Defaults to 3000.
  BLIZZARD_MAX_IDS    - The maximum allowed IDs per batch request. Default
                        1000.
  BLIZZARD_JETTY_OPTS - Defaults to {:join? true}."
  (:require [clojure.java.io      :as io]
            [clout.core           :refer [route-compile route-matches]]
            [cognitect.transit    :as transit]
            [flake.core           :as flake]
            [flake.utils          :as utils]
            [ring.adapter.jetty   :as ring-jetty])
  (:import [java.io ByteArrayOutputStream InputStream])
  (:gen-class))

(def ^{:private true} flake-init (promise))

;; Helpers
(defn string->integer
  "Converts s to an integer."
  [s]
  (Integer/parseInt s))

(defn get-env
  "Gets an environment variable k. An optional default is used when values are
  missing."
  [k & [default]]
  (or (System/getenv k) default))

(defn n-or-1
  "Returns n if n is greater than 0 otherwise 1."
  [n]
  (if (pos? n) n 1))

(defn n-or-max-ids
  "Returns n if n is equal to or less than max-ids."
  [n max-ids]
  (if (<= n max-ids) n max-ids))

(defn take-ids
  "Repeatedly calls flake/generate n times, returns the result."
  [n]
  (repeatedly n flake/generate))

(defn transit-write
  "Writes data with the given fmt (either :json or :msgpack) into a
  ByteArrayOutputStream as transit data. Returns the output stream."
  [data fmt]
  (let [out    (ByteArrayOutputStream.)
        writer (transit/writer out fmt)]
    (transit/write writer data)
    out))

(defn ids-response
  "Returns a response with an appropriate number of ids."
  [method max-ids fmt & [{:keys [n] :or {n "1"}}]]
  ;; Ensure Flake IDs are generated safely!
  (when-not (realized? flake-init)
    (flake/init!)
    (deliver flake-init true))

  (when (= method :get)
    (let [n         (-> n
                        string->integer
                        n-or-1
                        (n-or-max-ids max-ids))
          o         (transit-write (take-ids n) fmt)
          ct-format "application/transit+%s; charset=utf-8"]
      {:status  201
       :body    (io/input-stream (.toByteArray o))
       :headers {"Content-Type" (format ct-format (name fmt))}})))

(def method-not-allowed
  {:status 405 :body "Method Not Allowed"})

(defn accept->fmt
  "Returns a transit format (either :json or :msgpack, defaulting to :json)
  based on the value of the Accept header."
  [accept]
  (let [formats {"application/x-msgpack" :msgpack
                 "application/json"      :json}]
    (if-let [fmt (get formats accept)]
      fmt
      :json)))

(defn wrap-flake
  "Middleware that adds two routes to a Ring application:

    /flake    - Returns a single Flake ID.
    /flake/:n - Returns N Flake IDs.

  Takes an optional map containing a key max-id, the maximum allowed number
  of keys. Defaults to 1000."
  [handler & [{:keys [max-ids]}]]
  (fn [{:keys [request-method] {:strs [accept]} :headers :as request}]
    (let [fmt      (accept->fmt accept)
          response (partial ids-response request-method max-ids fmt)]
      (condp route-matches request
        (route-compile "/flake")        (or (response) method-not-allowed)
        (route-compile "/flake/:n") :>> #(or (response %) method-not-allowed)
        (handler request)))))

(defn -main
  [& _]
  (let [host       (get-env "BLIZZARD_HOST" "localhost")
        port       (string->integer (get-env "BLIZZARD_PORT" "3000"))
        max-ids    (string->integer (get-env "BLIZZARD_MAX_IDS" "1000"))
        jetty-opts (read-string
                     (get-env "BLIZZARD_JETTY_OPTS" "{:join? true}"))]
    (-> (constantly {:status 404 :body "Not Found"})
        (wrap-flake {:max-ids max-ids})
        (ring-jetty/run-jetty (merge {:host host
                                      :port port}
                                     jetty-opts)))))
