(ns a40kc-web.routes.home
  (:require
   [a40kc-web.layout :as layout]
   [clojure.java.io :as io]
   [a40kc-web.middleware :as middleware]
   [a40kc-web.server.parse :as parse]
   [a40kc-web.server.fight :as fight]
   [ring.util.response]
   [ring.util.http-response :as response]))

(def state (atom {}))


(defn home-page [request]
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page [request]
 (layout/render request "about.html"))


(defn load-rosters [request]
  (let [attacker-tmp (get-in request [:params :attacker :tempfile])
        defender-tmp (get-in request [:params :defender :tempfile])]
    (when (and attacker-tmp
               defender-tmp)
      (let [attacker-path (.getAbsolutePath (:tempfile (:attacker (:params request))))
            defender-path (.getAbsolutePath (:tempfile (:defender (:params request))))]

        [(parse/parse attacker-path)
         (parse/parse defender-path)]))))


;; (defn calculate [request]
;;   (let [attacker-tmp (get-in request [:params :attacker :tempfile])
;;         defender-tmp (get-in request [:params :defender :tempfile])]
;;     (when (and attacker-tmp defender-tmp)
;;       (let [attacker-path (.getAbsolutePath (:tempfile (:attacker (:params request))))
;;             defender-path (.getAbsolutePath (:tempfile (:attacker (:params request))))]
;;         (layout/render request "home.html" {:attacker (vec (parse/parse attacker-path))
;;                                             :defender (vec (parse/parse defender-path))

;;                                             :results
;;                                             (vec
;;                                              (fight/fight
;;                                               (parse/parse attacker-path)
;;                                               (parse/parse defender-path)))})
;;         ))))



(defn roasters [request]
  (let [[attacker defender] (load-rosters request)]
    (when (and attacker defender)
      (swap! state assoc :attacker attacker)
      (swap! state assoc :defender defender))
    (layout/render request "home.html"
                   {:attacker-units (:units (:attacker @state))
                    :defender-units (:units (:defender @state))})))

(defn fight [request]
  (let [attacker-id (get-in request [:params :attacker])
        defender-id (get-in request [:params :defender])
        attacker-unit (vec (filter #(= (str (:id %)) attacker-id) (:units (:attacker @state))))]
    (layout/render request "home.html"
                   {:attacker attacker-unit
                    :defender defender-id})))



(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get home-page
         :post roasters}]
   ["/fight" {:post fight}]])
