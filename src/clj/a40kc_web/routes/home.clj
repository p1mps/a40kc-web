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
   {:attacker       {:units  nil
                     :models nil}
    :defender       {:units  nil
                     :models nil}
    ;; after select
    :attacker-unit  nil
    :defender-unit  nil
    :attacker-model nil
    :defender-model nil
    :result-fight   nil}))


(defn home-page [request]
  (layout/render request "home.html"
                 {:attacker-models (:models (:attacker @state))
                  :defender-models (:models (:defender @state))
                  :attacker-units  (:units (:attacker @state))
                  :defender-units  (:units (:defender @state))
                  :attacker-unit   (:attacker-unit @state)
                  :defender-unit   (:defender-unit @state)
                  :attacker-model  (:attacker-model @state)
                  :defender-model  (:defender-model @state)
                  :result-fight    (:result-fight @state)}))


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
     :id   (clojure.string/replace id #"unit-" "")}
    {:unit false
     :id   (clojure.string/replace id #"model-" "")}))


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


(defn parse-weapons [{:keys [weapon-name ap s]}]
  (for [name weapon-name
        s    s
        ap   ap]
    {:name  name
     :chars {:s  s
             :ap ap}}))

(defn parse-weapons [weapon-names s ap]
  (loop [weapon weapon-names
         s      s
         ap     ap
         result []]
    (if (seq weapon)
      (recur (rest weapon) (rest s) (rest ap) (conj result {:name  (first weapon)
                                                            :chars {:s  (first s)
                                                                    :ap (first ap)}}))
      result)))





(defn single-model [{:keys [model-name bs weapon-name number ap s]}]
  {:name   model-name
   :models [{:name model-name
             :number number
             :chars {:bs bs}
             :weapons (parse-weapons weapon-name s ap)}]})

(defn multi-models [{:keys [unit-name model-name bs weapon-name number ap s]}]
  {:name unit-name
   :models (loop [model-name  model-name
                  bs          bs
                  weapon-name weapon-name
                  number      number
                  ap          ap
                  s           s
                  result      []]
             (if (seq model-name)
               (recur
                (rest model-name)
                (rest bs)
                (rest weapon-name)
                (rest number)
                (rest ap)
                (rest s)
                (conj
                 result
                 {:name (first model-name)
                  :number     (first number)
                  :chars {:bs (first bs)}
                  :weapons    (parse-weapons weapon-name s ap)})
                )
               result))})


(defn fight [request]
  (let [data (:params request)
        defender {:name (:defender-unit-name data)
                  :models [{:chars {:t    (:defender-toughness data)
                                    :save (:defender-save data)}}]}
        ]
    (swap! state assoc :form-data data)
    (swap! state assoc :multi-model (multi-models data))
    (swap! state assoc :single-model (single-model data))
    (swap! state assoc :defender defender)
    (if (seq? (:bs data))
      (swap! state assoc :result-fight (json/generate-string (fight/stats (multi-models data) defender)))
      (swap! state assoc :result-fight (json/generate-string (fight/stats (single-model data) defender)))))
  (home-page request))


(comment


  (def unit-vs-model {:defender-unit-name  "Captain",
                      :weapon-name
                      ["Boltgun"
                       "Frag grenades"
                       "Krak grenades"
                       "Bolt pistol"
                       "Frag grenades"
                       "Krak grenades"
                       "Bolt pistol"
                       "Boltgun"],
                      :number              ["4" "4" "4" "4" "1" "1" "1" "1"],
                      :defender-save       "4",
                      :attacker            ["" "" ""],
                      :s                   ["4" "3" "6" "4" "3" "6" "4" "4"],
                      :number-weapons      ["4" "4"],
                      :defender-toughness  "4",
                      :ap                  ["0" "0" "-1" "0" "0" "-1" "0" "0"],
                      :bs                  ["3+" "3+"],
                      :unit-name           "Tactical Squad",
                      :defender-model-name "Captain",
                      :model-name          ["Space Marine" "Space Marine Sergeant"]})




  (def model-vs-unit {:defender-unit-name  "Tactical Squad",
                      :weapon-name
                      ["Frag grenades"
                       "Krak grenades"
                       "Bolt pistol"
                       "Master-crafted boltgun"],
                      :number              ["1" "1" "1" "1"],
                      :defender-save       ["4" "4"],
                      :attacker            ["" "" ""],
                      :s                   ["3" "6" "4" "4"],
                      :number-weapons      "4",
                      :defender-toughness  ["4" "4"],
                      :ap                  ["0" "-1" "0" "-1"],
                      :bs                  "2+",
                      :unit-name           "Captain",
                      :defender-model-name ["Space Marine" "Space Marine Sergeant"],
                      :model-name          "Captain"})


  (def captain {:name "Captain",
                :models
                (list
                 {:name   "Captain",
                  :number 1,
                  :chars
                  {:description
                   "While a friendly <CHAPTER> CORE unit is within 6\" of this model, each time a model in that unit makes an attack, re-roll a hit roll of 1",
                   :ws   "2+",
                   :ld   "9",
                   :w    "5",
                   :m    "6\"",
                   :save "3+",
                   :s    "4",
                   :bs   "2+",
                   :t    "4",
                   :a    "4"},
                  :weapons
                  [{:name "Frag grenades",
                    :chars
                    {:range     "6\"",
                     :type      "Grenade D6",
                     :s         "3",
                     :ap        "0",
                     :d         "1",
                     :abilities "Blast."},
                    :id   0}
                   {:name "Krak grenades",
                    :chars
                    {:range     "6\"",
                     :type      "Grenade 1",
                     :s         "6",
                     :ap        "-1",
                     :d         "D3",
                     :abilities "-"},
                    :id   1}
                   {:name "Bolt pistol",
                    :chars
                    {:range     "12\"",
                     :type      "Pistol 1",
                     :s         "4",
                     :ap        "0",
                     :d         "1",
                     :abilities "-"},
                    :id   2}
                   {:name "Master-crafted boltgun",
                    :chars
                    {:range     "24\"",
                     :type      "Rapid Fire 1",
                     :s         "4",
                     :ap        "-1",
                     :d         "2",
                     :abilities "-"},
                    :id   3}]}),
                :id   0})



  )


(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats
                 middleware/wrap-base]}
   ["/" {:get  home-page
         :post roasters}]
   ["/select" {:post select}]
   ["/fight" {:post fight}]
   ["/reset" {:post reset}]])
