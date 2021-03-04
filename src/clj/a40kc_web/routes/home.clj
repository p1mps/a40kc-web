(ns a40kc-web.routes.home
  (:require
   [a40kc-web.layout :as layout]
   [clojure.java.io :as io]
   [a40kc-web.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page [request]
 (layout/render request "about.html"))

(defn calculate [request]
  (let [attacker-tmp (get-in request [:params :attacker :tempfile])
        defender-tmp (get-in request [:params :defender :tempfile])]
    (when (and attacker-tmp defender-tmp)
      (let [attacker-path (.getAbsolutePath (:tempfile (:attacker (:params request))))
            defender-path (.getAbsolutePath (:tempfile (:attacker (:params request))))]
        (layout/render request "home.html" {:attacker (vec (a40kc-web.server.parse/parse attacker-path))
                                            :defender (vec (a40kc-web.server.parse/parse defender-path))

                                            :results
                                            (vec
                                             (a40kc-web.server.fight/fight
                                              (a40kc-web.server.parse/parse attacker-path)
                                              (a40kc-web.server.parse/parse defender-path)))})
        ))))




(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get home-page
         :post calculate}]])
