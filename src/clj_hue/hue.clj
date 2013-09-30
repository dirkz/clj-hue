(ns clj-hue.hue
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]))

(defn find-bridges
  []
  "Finds local bridges and returns data about it, like
  [{\"id\":\"001788fffe0a9184\",\"internalipaddress\":\"192.168.178.40\",\"macaddress\":\"00:17:88:0a:91:84\"}]"
  (let [r (client/get "http://www.meethue.com/api/nupnp")]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn register-url
  [{:keys [internalipaddress]}]
  "Returns the URL for registering, given a valid bridge (as returned from find-bridges)."
  (str "http://" internalipaddress "/api/"))

(defn api-url
  ([{:keys [internalipaddress] :as bridge} {:keys [username] :as user}]
     "Given a bridge and a user, returns the corresponsing main URI for accessing the API"
     (str (register-url bridge) username))
  ([bridge user fragment] (str (api-url bridge user) fragment)))

(defn get-lights
  [bridge user]
  "Returns information about all lights"
  (let [u (api-url bridge user "/lights")
        r (client/get u)]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn register
  [bridge user]
  "Registers with local hub"
  (let [u (register-url bridge)
        r (client/post u {:body (json/write-str user)})]
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
  [bridge user]
  "Returns complete configuration"
  (let [r (client/get (api-url bridge user "/config"))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r)))

(defn get-full-state
  [bridge user]
  "Returns the complete hue state"
  (let [r (client/get (api-url bridge user))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r))) 

(defn get-light
  [bridge user light-id]
  "Returns the state of the given light"
  (let [r (client/get (api-url bridge user (str "/lights/" light-id)))]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r))) 

(defn set-light
  [bridge user light-id settings]
  "Returns the state of the given light"
  (let [to-set (json/write-str settings)
        r (client/put (api-url bridge user (str "/lights/" light-id "/state")) {:body to-set :content-type :json})]
    (if (= (:status r) 200)
      (json/read-str (:body r) :key-fn keyword)
      r))) 

(defn test-bridge
  []
  "Returns a test bridge"
  {:internalipaddress "192.168.178.40"})

(defn test-user
  []
  "Returns a test user"
  {:devicetype "clj-hue" :username "cljhue4242"})

(defn register-test
  []
  "Test function for registering"
  (let [[bridge & bridges] (find-bridges)
        user (test-user)]
    (println "bridge: " bridge)
    (println "register url: " (register-url bridge))
    (println "api url: " (api-url bridge user))
    (println (register bridge user))))

(defn lights-test
  []
  "Test function for loading lights"
  (let [bridge (test-bridge) 
        user (test-user)]
    ;(println (get-lights bridge user))
    ;(println (get-light bridge user 3))
    (println (set-light bridge user 3 {:hue 56100 :bri 128 :sat 255}))
    ))

(defn configuration-test
  []
  "Test function for loading configuration"
  (let [bridge (test-bridge)
        user (test-user)]
    (get-configuration bridge user)))

(defn full-state-test
  []
  "Test function for loading configuration"
  (let [bridge (test-bridge) 
        user (test-user)]
    (get-full-state bridge user)))

