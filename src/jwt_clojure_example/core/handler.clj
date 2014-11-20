(ns jwt-clojure-example.core.handler
  (:require
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
    [ring.middleware.format-response :refer [wrap-json-response]]
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :refer :all]
    [jwt-clojure-example.db :as db]
    [jwt-clojure-example.jwt :refer :all]
    [buddy.auth.backends.token :refer :all]
    [buddy.auth.middleware :refer [wrap-authentication wrap-access-rules ]]
    [clojure.core.memoize :as memo]
    [clojure.data.json :as json]
    [schema.core :as s]
    [jwt-clojure-example.test.util :refer :all]
    [slingshot.slingshot :refer [throw+ try+]]
    ))

(s/defschema User {
  :username String
  :email String
  })

(s/defschema Jwt {
  :token String
  ; refresh token is optional, in this example, not return it.
  (s/optional-key :refresh_token) String
  })

(defapi api-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui"
    :api-url "/swagger-docs")

  (swagger-docs)
  (swaggered "test"
    :description "Swagger test api"
    (context "/api" []
      (GET* "/ticket" []
        :return Jwt
        :summary "return jwt with ticket for create new user"
        (ok {:token (gen-jwt {:ticket (randuuid)}) })
        )
      (POST* "/user/new" {body :body-params}
        :return User
        :body-params [username :- String, email :- String, password :- String]
        :summary "create user endpoint."

        (def res (db/create-user (body :username) (body :email) (body :password)))
        (if-not (nil? res)
          (ok res)
          (bad-request {:message "user already exists."})
          )
        )
      ; login
      (POST* "/token" {body :body-params}
        :return Jwt
        :body-params [grant_type :- String, username :- String, password :- String]
        :summary "login by password"

        (if-not (= (body grant_type) "password")
          (bad-request {:message "invalid grant type"}))

        (def user (db/lookup-user (body :username) (body :password)))
        (if-not (nil? user)
          ;; This hash should include user_info only user_id
          ;; but this example, I don't care
          (ok {:token (gen-jwt user)})
          (not-found {:message "user not found"})
          )
        )
      ; User
      (GET* "/user/me" []
        :return User
        (def user (db/lookup-user-by-username
          (-> +compojure-api-request+ :jwt :username)))
        (if (nil? user)
          (not-found {:message "User does not found."})
          (ok user)
          )
        )
      )
    )
  )


(defroutes* app-routes
  api-routes
  (GET* "/" [] "Hello World")
  (route/not-found "Not Found"))

(def memoize-verify-token (memo/fifo verify-jwt :fifo/threshold 10))

(defn authfn [request token]
  (def jwt (memoize-verify-token token))
  (if-not (correct-iss? jwt) (throw+ {:type :token-invalid}))
  (if (token-expired? jwt) (throw+ {:type :token-expired}))
  (if (re-find #"/user/new" (:uri request))
    ; /user/new endpoint require ticket
    (if (jwt :ticket) true false)
    ; else true
    true
    )
  )

(defn unauthorized-token-invalid [request token]
  (unauthorized {:message "Invalid token"})
  )
(defn unauthorized-token-expire [request token]
  (unauthorized {:message "Token has expired."})
  )

; TODO add header what authorize method is correct?
(defn reject-handler [request]
  (unauthorized-token-invalid)
  )

(defn verify-jwt-by-request [request]
  (let [backend (token-backend {:authfn authfn})
        token (.parse backend request)]
    (try+
       ((.authenticate backend request token)  :identity)
       (catch Exception e
         (throw+ {:type :token-invalid})
       )
    )
  )
  )

(defn should-be-authenticated [request]
  (if (get (System/getenv) "SWAGGER")
    true
    (verify-jwt-by-request request)
  )
)

;; (def rules [{:pattern #"^/admin/.*"
;;              :handler {:or [admin-access operator-access]}}
;;             {:pattern #"^/login$"
;;              :handler any-access}
;;             {:pattern #"^/.*"
;;              :handler authenticated-access}])
(def rules [{
    :pattern #"^/api/(?!(ticket|token)).*"
    :handler should-be-authenticated
  }]
)

(defn wrap-exception [f]
  (fn [request]
    (try+
      (f request)

      (catch [:type :token-expired] _
         (.handle-unauthorized (token-backend
           {:authfn #(%1 %2 false),
            :unauthorized-handler unauthorized-token-expire}
         ) request _)
      )
      (catch [:type :token-invalid] _
         (.handle-unauthorized (token-backend
           {:authfn #(%1 %2 false),
            :unauthorized-handler unauthorized-token-invalid}
         ) request _)
      )
      (catch Exception e
        (internal-server-error "some error ocrrued.")
      )
    ))
  )

(def app
  (->
    (wrap-defaults app-routes api-defaults)
    (wrap-access-rules {:rules rules :reject-handler reject-handler})
    (wrap-exception)
    (wrap-json-response)
  ))

