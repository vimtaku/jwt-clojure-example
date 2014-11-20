(ns jwt-clojure-example.jwt
  (:require
    [buddy.sign.jws :as jws]
    [buddy.core.keys :as bkeys]
    [buddy.core.hash :as hash]
    [buddy.core.codecs :refer :all]
    [buddy.sign.generic :refer [sign unsign]]

    [clj-time.core :refer [now plus days minutes from-now]]
    [clj-time.coerce :as tc]
    ))


(def privkey (bkeys/private-key "keys/privkey.pem" "secret"))
(def pubkey (bkeys/public-key "keys/pubkey.pem"))

;; The secret key.
;; For security, I think this key should be changed in terms of a day or a week.
;; Get from the file or hardcord or ..
(defn secret-key []
  "this is super secret key"
)

;; Sign data using default `:hs256` algorithm that does not
;; requres special priv/pub key.
(defn gen-token [user]
  (jws/sign user (secret-key)))

;; data should contain string similar to:
;; "eyJ0eXAiOiJKV1MiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyaWQiOjF9.zjenOuIAEG-..."
(defn verify-token [token]
  (jws/unsign token (secret-key)))

(defn randuuid [] (str (java.util.UUID/randomUUID)))

(def iss "http://localhost:3000/")
(def expire-duration (minutes 30))

(defn- gen-jws []
  (def n (now))
  {
    ; see booked claim
    ; http://openid-foundation-japan.github.io/draft-ietf-oauth-json-web-token-11.ja.html#issDef
    :iss iss
    :exp (tc/to-long (plus n expire-duration))
    :iat (tc/to-long n)
    :jti (randuuid)
  }
)

(defn gen-jwt [user]
  (sign (gen-token (merge user (gen-jws))) privkey {:alg :rs256})
)

(defn verify-jwt [jwt]
  (verify-token (unsign jwt pubkey {:alg :rs256}))
)

(defn correct-iss? [jwt]
  (= (jwt :iss) iss)
  )
(defn token-expired? [jwt]
  (< (jwt :exp) (tc/to-long (now)))
)

