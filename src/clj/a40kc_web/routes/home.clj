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
        ;;defender-tmp (get-in request [:params :defender :tempfile])

        ]
    (when (and attacker-tmp
               ;;defender-tmp

               )
      (let [attacker-path (.getAbsolutePath (:tempfile (:attacker (:params request))))
            ;;defender-path (.getAbsolutePath (:tempfile (:attacker (:params request))))

            ]
        (vec (parse/parse attacker-path))
        ;;(vec (parse/parse defender-path))

        ))))


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
    (swap! state assoc :attacker attacker)
    (swap! state assoc :defender attacker)
    (layout/render request "home.html"
                   {:attacker-units (:units attacker)
                    :attacker-models (:models attacker)
                    ;;:defender-units (:units defender)


                    })))




(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get home-page
         :post roasters}]])
