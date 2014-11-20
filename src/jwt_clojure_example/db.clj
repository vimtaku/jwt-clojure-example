(ns jwt-clojure-example.db
  (:require
    [buddy.sign.jws :as jws]
    [clojurewerkz.scrypt.core :as sc]
    ))

(def ^:dynamic state (atom {}))
(defn get-state [key] (@state key))
(defn- update-state [key val] (swap! state assoc key val))
(defn- delete-from-state [key] (swap! state dissoc key))
(defn- uuid [] (str (java.util.UUID/randomUUID)))

(defn delete-all []
  (loop [k (first (keys @state))]
    (swap! state dissoc k)
    )
  )

(defn lookup-user [username password]
  (let [user (get-state username)]
    (if (not (nil? user))
      (if (sc/verify password (user :password))
        (dissoc user :password)
        nil
      )
      nil
    )
  ))

(defn lookup-user-by-username [username]
  (let [user (get-state username)]
    (if-not (nil? user)
      (dissoc user :password)
      nil
    )
  )
)


(defn create-user [username email password]
  (let [h (sc/encrypt password 16384 8 1)]
    (if (nil? (get-state username))
      (dissoc
        ((update-state username {:password h, :email email, :username username}) username)
        :password
      )
      nil
    )
  )
)

