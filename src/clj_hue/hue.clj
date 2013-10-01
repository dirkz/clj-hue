(ns clj-hue.hue
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]))

(defn find-bridges
  []
  "Finds local bridges and returns data about it, like
  [{\"id\":\"001788ff184\",\"internalipaddress\":\"192.168.178.40\",\"macaddress\":\"00:17:88:0a:ff:ff\"}]"
  (let [r (client/get "http://www.meethue.com/api/nupnp")]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn register-url
  [{:keys [internalipaddress]}]
  "Returns the URL for registering, given a valid bridge (as returned from find-bridges)."
  (str "http://" internalipaddress "/api/"))

(defn api-url
  ([{:keys [internalipaddress username] :as bridge}]
     "Given a bridge and a user, returns the corresponsing main URI for accessing the API"
     (str (register-url bridge) username))
  ([bridge fragment] (str (api-url bridge) fragment)))

(defn get-lights
  [bridge]
  "Returns information about all lights"
  (let [u (api-url bridge "/lights")
        r (client/get u)]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn user-from-bridge
  [{:keys [devicetype username] :as bridge}]
  {:devicetype devicetype :username username})

(defn register
  [{:keys [devicetype username] :as b}]
  "Registers with local hub"
  (let [u (register-url b)
        r (client/post u {:body (json/write-str (user-from-bridge b))})]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn error?
  [json]
  "Checks whether json contains :error element and returns true in that case,nil otherwise"
  (if (get json :error nil)
    true
    false))

(defn get-configuration
  [bridge]
  "Returns complete configuration"
  (let [r (client/get (api-url bridge "/config"))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn get-full-state
  [bridge]
  "Returns the complete hue state"
  (let [r (client/get (api-url bridge))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn get-light
  [bridge light-id]
  "Returns the state of the given light"
  (let [r (client/get (api-url bridge (str "/lights/" light-id)))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn set-light
  [bridge light-id settings]
  "Sets the state of the given light"
  (let [to-set (json/write-str settings)
        r (client/put (api-url bridge (str "/lights/" light-id "/state")) {:body to-set :content-type :json})]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(comment
  (def bridge ((find-bridges) 0))
  (set-light bridge 3 {:hue 56100 :bri 128 :sat 255})
  (set-light bridge 3 {:effect "colorloop" :bri 64 :sat 255})
  (set-light bridge 3 {:effect "none" :bri 64 :sat 255})
  (get-light bridge 3))
