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
  (str "http://" internalipaddress "/api/"))

(defn api-url
  ([{:keys [internalipaddress] :as bridge} {:keys [username] :as user}]
     "Given a bridge and a user, returns the corresponsing main URI for accessing the API"
     (str (register-url bridge) username))
  ([bridge user fragment] (str (api-url bridge user) fragment)))

(defn lights
  [bridge user]
  "Returns information about all lights"
  (let [u (api-url bridge user "/lights")
        r (client/get u)]
    (if (= (:status r) 200)
      (json/read-str (:content r) :key-fn keyword)
      r)))

(defn register
  [bridge user]
  "Registers with local hub"
  (let [u (register-url bridge)
        r (client/post u {:body (json/write-str user)})]
    (if (= (:status r) 200)
      (json/read-str (:body r))
      r)))

(defn error?
  [json]
  "Checks whether json contains :error element and returns true in that case,nil otherwise"
  (if (get json :error nil)
    true
    false))

(defn load-all
  []
  "Test function"
  (let [[bridge & bridges] (find-bridges)
        user {:devicetype "clj-hue" :username "cljhue4242"}]
    (println "bridge: " bridge)
    (println "register url: " (register-url bridge))
    (println "api url: " (api-url bridge user))
    (println (register bridge user))
    ))
