(ns a40kc-web.routes.home
  (:require
   [a40kc-web.layout :as layout]
   [a40kc-web.middleware :as middleware]
   [a40kc-web.server.parse :as parse]
   [a40kc-web.server.fight :as fight]
   [ring.util.response]))

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
    :defender-model nil}))


(defn home-page [request]
  (println "attacker")
  (println (:attacker @state))
  (layout/render request "home.html"
                   {:attacker-models (:models (:attacker @state))
                    :defender-models (:models (:defender @state))
                    :attacker-units (:units (:attacker @state))
                    :defender-units (:units (:defender @state))
                    :attacker-unit (:attacker-unit @state)
                    :defender-unit (:defender-unit @state)
                    :attacker-model (:attacker-model @state)
                    :defender-model (:defender-model @state)}))


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
  ;; TODO: parse unit model ids
  (let [attacker-id (parse-id (get-in request [:params :attacker]))
        defender-id (parse-id (get-in request [:params :defender]))]
    (if (:unit attacker-id)
      (do
        (swap! state assoc :attacker-unit (first (filter #(= (str (:id %)) (:id attacker-id)) (:units (:attacker @state)))))
        (swap! state assoc :attacker-model nil))
      (do
        (swap! state assoc :attacker-model (first (filter #(= (str (:id %)) (:id attacker-id)) (:models (:attacker @state)))))
        (swap! state assoc :attacker-unit nil))


      )
    (if (:unit defender-id)
      (do
        (swap! state assoc :defender-unit (first (filter #(= (str (:id %)) (:id defender-id)) (:units (:defender @state)))))
        (swap! state assoc :defender-model nil)



        )
      (do
        (swap! state assoc :defender-model (first (filter #(= (str (:id %)) (:id defender-id)) (:models (:defender @state)))))
        (swap! state assoc :defender-unit nil))


      )


    (home-page request)))



(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get home-page
         :post roasters}]
   ["/select" {:post select}]])
