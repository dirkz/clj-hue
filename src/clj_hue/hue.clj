(ns clj-hue.hue
  (:require [clojure.data.json :as json])
  (:import (java.net HttpURLConnection URL)))

(defn- parse-headers
  [con]
  "Creates headers from the given connection"
  (into {} (for [[k v] (.getHeaderFields con) :when k] [(keyword (.toLowerCase k)) (seq v)])))

(defmulti get-url class)
(defmethod get-url URL [s]
  (let [con (.openConnection s)
        is (.getInputStream con)]
    {:headers (parse-headers con) :status (.getResponseCode con) :content (slurp is)}))
(defmethod get-url String [s] (get-url (URL. s)))

(defmulti post-url (fn [o & rest] (class o)))
(defmethod post-url URL [s body]
  (println "post-url " s body)
  (with-open [con (.openConnection s)
              _ (.setDoOutput con true)
              writer (java.io.OutputStreamWriter. (.getOutputStream con))
              is (.getInputStream con)]
    (.write writer body)
    (.flush writer)
    {:headers (parse-headers con) :status (.getResponseCode con) :content (slurp is)}))
(defmethod post-url String [s body]
  (post-url (URL. s) body))

(defn find-bridges []
  "Finds local bridges and returns data about it, like
  [{\"id\":\"001788fffe0a9184\",\"internalipaddress\":\"192.168.178.40\",\"macaddress\":\"00:17:88:0a:91:84\"}]"
  (let [r (get-url "http://www.meethue.com/api/nupnp")]
    (if (= (:status r) 200)
      (json/read-str (:content r) :key-fn keyword)
      nil)))

(defn register-url
  [{:keys [internalipaddress]}]
  (str "http://" internalipaddress "/api/"))

(defn api-url
  ([{:keys [internalipaddress] :as bridge} {:keys [username] :as user}]
     "Given a bridge and a user, returns the corresponsing main URI for accessing the API"
     (str (register-url bridge) username))
  ([bridge user fragment] (str (api-url bridge user) fragment)))

(defn lights [bridge user]
  "Returns information about all lights"
  (let [u (api-url bridge user "/lights")
        r (get-url u)]
    (if (= (:status r) 200)
      (json/read-str (:content r) :key-fn keyword)
      nil)))

(defn register [bridge user]
  "Registers with local hub"
  (let [u (register-url bridge)
        r (post-url u (json/write-str user))]
    r))

(defn error? [json]
  "Checks whether json contains :error element and returns true in that case,nil otherwise"
  (if (get json :error nil)
    true
    false))

(defn load-all
  []
  (let [[bridge & bridges] (find-bridges)
        user { :devicetype "test user" :username "cljhue"}
        ]
    (println "bridge: " bridge)
    (println "register url: " (register-url bridge))
    (println "api url: " (api-url bridge user))
    (println (register bridge user))
    ))
