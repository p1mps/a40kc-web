(ns a40kc-web.routes.home
  (:require
   [a40kc-web.layout :as layout]
   [a40kc-web.middleware :as middleware]
   [a40kc-web.server.parse :as parse]
   [cheshire.core :as json]
   [a40kc-web.server.fight :as fight]
   [ring.util.response :as response]))

(def state
  (atom
   {:attacker {:units nil
               :models nil}
    :defender {:units nil
               :models nil}
    ;; after select
    :attacker-unit nil
    :defender-unit nil
    :attacker-model nil
    :defender-model nil
    :result-fight nil}))


(defn home-page [request]
  (layout/render request "home.html"
                   {:attacker-models (:models (:attacker @state))
                    :defender-models (:models (:defender @state))
                    :attacker-units (:units (:attacker @state))
                    :defender-units (:units (:defender @state))
                    :attacker-unit (:attacker-unit @state)
                    :defender-unit (:defender-unit @state)
                    :attacker-model (:attacker-model @state)
                    :defender-model (:defender-model @state)
                    :result-fight (:result-fight @state)}))


(defn load-rosters [request]
  (let [attacker-tmp (get-in request [:params :attacker :tempfile])
        defender-tmp (get-in request [:params :defender :tempfile])]
    (when (and attacker-tmp
               defender-tmp)
      (let [attacker-path (.getAbsolutePath (:tempfile (:attacker (:params request))))
            defender-path (.getAbsolutePath (:tempfile (:defender (:params request))))]

        [(parse/parse attacker-path)
         (parse/parse defender-path)]))))


(defn roasters [request]
  (let [[attacker defender] (load-rosters request)]
    (when (and attacker defender)
      (swap! state assoc :attacker attacker)
      (swap! state assoc :defender defender))
    (home-page request)))

(defn parse-id [id]
  (if (clojure.string/includes? id "unit")
    {:unit true
     :id (clojure.string/replace id #"unit-" "")}
    {:unit false
     :id (clojure.string/replace id #"model-" "")}))


(defn select [request]
  (let [attacker-id (parse-id (get-in request [:params :attacker]))
        defender-id (parse-id (get-in request [:params :defender]))]
    (if (:unit attacker-id)
      (do
        (swap! state assoc :attacker-unit (first (filter #(= (str (:id %)) (:id attacker-id)) (:units (:attacker @state)))))
        (swap! state assoc :attacker-model nil))
      (do
        (swap! state assoc :attacker-model (first (filter #(= (str (:id %)) (:id attacker-id)) (:models (:attacker @state)))))
        (swap! state assoc :attacker-unit nil)))
    (if (:unit defender-id)
      (do
        (swap! state assoc :defender-unit (first (filter #(= (str (:id %)) (:id defender-id)) (:units (:defender @state)))))
        (swap! state assoc :defender-model nil))
      (do
        (swap! state assoc :defender-model (first (filter #(= (str (:id %)) (:id defender-id)) (:models (:defender @state)))))
        (swap! state assoc :defender-unit nil)))


    (home-page request)))

(defn reset [request]
  (reset! state nil)
  (response/redirect "/" 301))

(defn fight [request]
  ;; {"bs" "2+",
  ;; "ws" "2+",
  ;; "number" ["1" "1" "1" "1"],
  ;; "s" ["3" "6" "4" "4"],
  ;; "ap" ["0" "-1" "0" "-1"],
  ;; "fname"
  ;; ["2+" "2+" "1" "3" "0" "1" "6" "-1" "1" "4" "0" "1" "4" "-1"],
  ;; "attacker" ["" "" ""]}
  (clojure.pprint/pprint request)
  (let [params (:params request)
        model-1 {:chars {:bs (:bs params)
                         :t "4"}
                 :weapons [{:name "weapon"
                            :chars {:s "20"
                                  :ap "-"
                                  :d "1"}}]}
        model-2 {:chars {:t "2"
                         :save "3+"}}
        stats (fight/stats {:models [model-1]} {:models [model-2]})
        ]
    (println "STATS!")
    (println stats)
    (println (first (vals (first stats))))
    (swap! state assoc :result-fight (json/generate-string (first (vals (first stats)))))
    (home-page request))
  )


(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get home-page
         :post roasters}]
   ["/select" {:post select}]
   ["/fight" {:post fight}]
   ["/reset" {:post reset}]])
