(ns jwt-clojure-example.core.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [jwt-clojure-example.core.handler :refer :all]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [jwt-clojure-example.db :as db]
            [jwt-clojure-example.test.util :refer :all]
            [conjure.core :refer :all]
            [clj-time.core :as t]
            [jwt-clojure-example.jwt :as jwt]
            [slingshot.slingshot :refer [throw+ try+]]
            [slingshot.test :refer :all]
            ))


(deftest test-app
  (testing "api route"
      (defn get-token []
        (let [response (app (mock/request :get "/api/ticket"))
              res-json (-> response body-str json/read-str walk/keywordize-keys)
             ]
          (res-json :token)
        ))
      (defn get-expire-token []
        (stubbing [clj-time.core/now (t/date-time 2014 10 10 9 0 0 000)]
          (get-token)
        ))
      (testing "test token expire"
        (stubbing [clj-time.core/now (t/date-time 2014 10 10 9 29 59 999)]
          (is (= true (authfn (mock/request :get "/api/ticket") (get-expire-token))))
        )
        (stubbing [clj-time.core/now (t/date-time 2014 10 10 9 30 0 000)]
          (is (= true (authfn (mock/request :get "/api/ticket") (get-expire-token))))
        )
        (stubbing [clj-time.core/now (t/date-time 2014 10 10 9 30 0 001)]
          (is
            (thrown+?
              #(->> % :type (= :token-expired))
              (authfn (mock/request :get "/api/ticket") (get-expire-token))))
        )
        (testing "test expire token returns unauthorized"
          (def req (->
            (mock/request :post "/api/user/new" {})
            (mock/header "Authorization" (str "Token " (get-expire-token)))
            (mock/header "Accept" "application/json")
            (mock/header "Content-type" "application/json")
            ))
          (let [res (app req)
                res-json (-> res body-str json/read-str walk/keywordize-keys)
                ]
            (is (= (:status res) UNAUTHORIZED))
            (is (re-find #"expired" (res-json :message)))
            )
        )
      )

      (testing "test api"
        (testing "GET /api/ticket"
          (let [response (app (mock/request :get "/api/ticket"))
                res-json (-> response body-str json/read-str walk/keywordize-keys)
                token (res-json :token)]
            (is (= (:status response) OK))
            (is (string? token))
            (is (<= 1 (count token)))
            (is (:ticket (memoize-verify-token token)))
            )
          )
        (testing "POST /api/token"
          (testing "user not found"
            (let [
                  param {:grant_type :password, :username "mogemoge", :password "mogamoga"}
                  body-param (json/write-str param)
                  req (mock/request :post "/api/token" body-param)
                  response (app (mock/content-type req "application/json"))
                  ]
              (is (= (:status response) NOT_FOUND))
              )
            )
          (testing "user found"
            (def username "vimtaku")
            (def password "hogehoge")
            (db/create-user username "vimtaku@example.com" password)
            (let [
                  param {:grant_type :password, :username username, :password password}
                  body-param (json/write-str param)
                  req (mock/request :post "/api/token" body-param)
                  response (app (mock/content-type req "application/json"))
                  res-json (-> response body-str json/read-str walk/keywordize-keys)
                  token (res-json :token)
                  ]
              (is (= (:status response) OK))
              (is (string? token))
              (is (<= 1 (count token)))
              )
            )
          )

        (testing "POST /api/user/new"
          (testing "jwt not given, auth error"
            (let [response (app (mock/request :post "/api/user/new"))]
              (is (= (:status response) UNAUTHORIZED))
              )
            )
          (testing "success, created user"
            (binding [db/state (atom {})]
              (let [
                    param {:username "hoge",
                           :email "hoge@gmail.com",
                           :password "hogeFuga"}
                    req (->
                          (mock/request :post "/api/user/new" (json/write-str param))
                          (mock/header "Authorization" (str "Token " (get-token)))
                          (mock/header "Accept" "application/json")
                          (mock/header "Content-type" "application/json")
                          (mock/body (json/write-str param))
                          )
                    response (app req)
                    body-json (walk/keywordize-keys (json/read-str (body-str response)))
                    ]
                (is (= (:status response) OK))
                (is (= (-> body-json :email) "hoge@gmail.com"))
                (is (= (-> body-json :username) "hoge"))
                )
              )
            )
          )
        )

  ;; (testing "main route"
  ;;   (let [response (app (mock/request :get "/"))]
  ;;     (is (= (:status response) 200))
  ;;     (is (= (:body response) "Hello World"))))
  ;;
  ;; (testing "not-found route"
  ;;   (let [response (app (mock/request :get "/invalid"))]
  ;;     (is (= (:status response) 404))))
  )


)
