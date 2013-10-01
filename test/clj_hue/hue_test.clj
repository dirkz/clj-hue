(ns clj-hue.hue-test
  (:require [clojure.test :refer :all]
            [clj-hue.hue :refer :all]))

;; This must be a registered hue user!
(def test-user {:devicetype "clj-hue" :username "cljhue4242"})

(deftest test-find-bridge
  ;; assumes that the bridge can be found via http://www.meethue.com/api/nupnp
  ;; expecting:
  ;; [{:id "id", :internalipaddress "192.168.178.40", :macaddress "00:17:88:0a:ff:ff"}]
  (is (count (find-bridges)) 1)
  (let [b ((find-bridges) 0)]
    (is true (contains? b :id))
    (is true (contains? b :internalipaddress))
    (is true (contains? b :macaddress))))

(deftest test-register-url
  (is (= "http://hue/api/" (register-url {:internalipaddress "hue"}))))

(deftest test-api-url
  (is (= "http://hue/api/sherlock" (api-url {:internalipaddress "hue" :username "sherlock"}))))

(deftest test-failing-register
  ;; expecting [{:error {:type 101, :address /, :description link button not pressed}}]
  (let [b (merge test-user ((find-bridges) 0))
        r (register b)]
    (is (> (count r) 0))
    (is (contains? (r 0) :error))
    (is (contains? ((r 0) :error) :description))))

(deftest test-get-lights
  ;; expecting {:1 {:name name1}, :2 {:name name2}, :3 {:name name3}}
  (let [b (merge test-user ((find-bridges) 0))
        lights (get-lights b)]
    (is (<= 1 (count lights)))
    (is (contains? lights :1))))

(deftest test-get-light
  (let [b (merge test-user ((find-bridges) 0))
        l (get-light b 1)]
    (is (contains? l :state))
    (is (contains? l :type))
    (is (contains? l :name))))

(deftest test-get-full-state
  (let [b (merge test-user ((find-bridges) 0))
        s (get-full-state b)]
    (is (contains? s :lights))
    (is (contains? s :schedules))))

(deftest test-get-configuration
  (let [b (merge test-user ((find-bridges) 0))
        s (get-configuration b)]
    (is (contains? s :linkbutton))
    (is (contains? s :whitelist))))

(deftest test-set-light
  (let [b (merge test-user ((find-bridges) 0))
        r (set-light b 1 {:hue 56100 :bri 128 :sat 255})]
    (is (= 3 (count r)))
    (is (contains? (r 0) :success))
    (is (contains? (r 1) :success))
    (is (contains? (r 2) :success))))
