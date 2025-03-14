(ns ^:integration web.service-test
  (:require
   [clojure.test :refer [deftest is]]
   [matcher-combinators.test]
   [web.service]
   [web.test.html :as test.html]
   [web.test.service :as t]
   [web.test.system :as test.system]))

;;; ----------------------------------------------------------------------------
;;; Security headers

(deftest security-headers
  (test.system/with-system [{:keys [service]} (test.system/system)]
    (let [response (t/request service {:request-method :get
                                       :url            "/"})
          policy   #"^default-src 'self'; script-src 'self';"]
      (is (match?
           {"content-security-policy"           policy
            "referrer-policy"                   "strict-origin"
            "x-content-type-options"            "nosniff"
            "x-download-options"                "noopen"
            "x-frame-options"                   "DENY"
            "x-permitted-cross-domain-policies" "none"
            "x-xss-protection"                  "1; mode=block"}
           (:headers response))))))

;;; ----------------------------------------------------------------------------
;;; Manifest

(deftest manifest
  (test.system/with-system [{:keys [service]} (test.system/system)]
    (let [response (t/request service {:as             :json
                                       :accept         :json
                                       :request-method :get
                                       :url            "/manifest.json"})]
      (is (match?
           {:headers {"content-type" #"^application/json"}
            :body
            {:background_color "#171717"
             :display          "standalone"
             :name             "Clojure Web Application"
             :icons            [{:src   "/android-chrome-192x192.png"
                                 :sizes "192x192"
                                 :type  "image/png"}
                                {:src   "/android-chrome-512x512.png"
                                 :sizes "512x512"
                                 :type  "image/png"}]
             :short_name       "cwa"}}
           response)))))

;;; ----------------------------------------------------------------------------
;;; Not found

(deftest not-found
  (test.system/with-system [{:keys [service]} (test.system/system)]
    (let [request  {:request-method :get
                    :url            (str "/expecting-a-404/" (random-uuid))}
          response (t/request service request)]
      (when (is (match? {:status  404
                         :headers {"content-type" "text/html;charset=utf-8"}
                         :body    #"^<!DOCTYPE html>"}
                        response))
        (let [doc (test.html/parse (:body response))]
          (is (match?
               [{:content ["Not found"]}]
               (test.html/elements doc :title)))
          (is (match?
               [{:content ["Not found"]}]
               (test.html/elements doc :h1))))))))

;;; ----------------------------------------------------------------------------
;;; GET /

(deftest get-root
  (test.system/with-system [{:keys [service]} (test.system/system)]
    (let [response (t/request service {:request-method :get
                                       :url            "/"})]
      (when (is (match?
                 {:status  200
                  :headers {"content-type" "text/html;charset=utf-8"}
                  :body    #"^<!DOCTYPE html>"}
                 response))
        (let [doc (test.html/parse (:body response))]
          (is (match?
               [{:content ["Coming soon!"]}]
               (test.html/elements doc :title)))
          (is (match?
               [{:content ["Coming soon!"]}]
               (test.html/elements doc :h1))))))))
