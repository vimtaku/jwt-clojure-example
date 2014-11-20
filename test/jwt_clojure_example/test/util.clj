(ns jwt-clojure-example.test.util)

(defmacro def-expect-status-code [status]
    (let [
          fn-name (symbol (clojure.string/upper-case status))
          http_status (symbol (str "HTTP_" fn-name))
          ]
        `(def ~fn-name
            (. java.net.HttpURLConnection ~http_status)
        )
    ))

(defn p [& arg]
  (apply println arg)
)

(defn body-str [response]
  (with-open [rdr (clojure.java.io/reader (:body response))]
  (apply str (line-seq rdr))
  )
)

(def-expect-status-code ok)
(def-expect-status-code unauthorized)
(def-expect-status-code not_found)
