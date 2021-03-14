(ns a40kc-web.server.fight
  (:require
    [clojure.string :as string]))

(def number-experiments 100)

;; TODO roll d3
(defn roll []
  (let [roll (rand-nth (range 1 7))]

    roll))


(defn bs [unit]
  (read-string (string/replace (:bs (:chars unit)) "+" "")))

(defn strength [weapon]
  (read-string (:s (:chars weapon))))


(defn toughness [unit]
  (read-string (:t (:chars unit))))


(defn success? [roll stat]
  (>= roll stat))

(defn hit? [char]
  (success? (roll) char))

(defn to-wound [weapon target-unit]
  (let [comparison (- (strength weapon) (toughness target-unit))]
    (- 4 comparison)))

(defn wound? [weapon target-unit]
  (success? (roll) (to-wound weapon target-unit)))

(defn damage [weapon]
  (:d (:chars weapon)))


(defn parse-damage [damage]
  (when (string/includes? "d" damage)
    (let [[times dice] (string/split #"d" damage)]
      (if times
        {:times times
         :dice dice}
        {:times 1
         :dice dice}))))

(defn roll-damage [damage]
  (let [parsed-damage (parse-damage damage)]
    (reduce + (for [t (:times parsed-damage)] (roll)))))


(defn shoot [unit1 unit2]
  (println "shooting with" (:name unit1))
  (for [w (:weapons unit1)]
    (do
      (println "shooting with " (:name w))
      (let [bs      (bs unit1)
            _       (println "bs:" bs)
            hit     (hit? bs)
            _       (if hit (println "hit!") (println "no hit!"))
            _       (println "s:" (strength w))
            _       (println "t:" (toughness unit1))
            wounded (wound? w unit2)
            _       (if wounded (println "wound!") (println "no wound!"))
            ]

        (when (and hit wounded) 1)
        )))

  )








(comment

  (def units (:units (a40kc-web.server.parse/parse "spacemarines.rosz")))

  (def captain (first units))

  (get-in captain [:chars])
  (bs captain)

  (shoot captain captain)



)




(defn d6-odds [n]
  (double (/ (- 7 n) 6)))

(defn parse-char [char field]
  (println "parse char" char field)
  (try
    (Integer/parseInt
     (str (clojure.string/replace char "+" "")))
    (catch Exception e
      (when char (read-string char)))))

(defn hit-probs [u]
  (println u)
  (-> u :chars :bs (parse-char "bs hit probs") d6-odds))

;;woundRatio = targetToughness / weaponStrength;
(defn odds-wounding [S T]
  (let [comparison (- S T)]
    (d6-odds (- 4 comparison))))

(defn wound-probs [w u]
  (let [strength (-> w :chars :s (parse-char "strength wounds"))
        toughess (-> u :chars :t (parse-char "toughness wounds"))]
    (odds-wounding strength toughess)))

(defn save-prob [u w]
  (-> u :chars :s (parse-char "strength") (- (:ap (:chars w)))  d6-odds))


(def fight
  {:attacker "Conscripts"
   :defender "Tactical Squad"
   :wounds [{:weapon      "lasgun"
             :wounds      0
             :wounds-rf   0
             :wounds-frsf 0}]})


;; TODO: fight units and fight models
(defn fight-units [u1 u2]
  {:attacker (:name u1)
   :defender (:name u2)
   :weapons (flatten (for [m (:models u1)]
                       (for [m2 (:models u2)]
                         (for [w (:weapons m)]
                           {:weapon (:name w)
                            ;; todo: count attacks per weapons
                            :wounds (format "%.2f"
                                            (* (:number m)
                                               (hit-probs m)
                                               (wound-probs w m2)
                                               (- 1 (save-prob m2 w))))}))))})


(defn fight [list1 list2]
  (group-by :attacker
            (flatten (for [u1 (:units (first list1))]
                       (for [u2 (:units (first list2))]
                         (fight-units u1 u2))
                       ))))


(comment

  ;; Wounds Inflicted = Attacks * (Hit Probability) * (Wound Probability) * (Save Probability)
  ;; *1D6=3.5 *1D3=2 *2D6 pick highest= 4.47 (For Fussion damage at half range)
  (defn wounds-inflicted [u1 u2]
    (for [w (:weapons u1)]
      {:weapon            (:name w)
       :number            (:number u1)
       :hits-prob         (hit-probs u1)
       :wound-probs       (wound-probs w u2)
       :save-probs        (- 1 (save-prob u2 w))
       :wounds            (format "%.2f" (* (:number u1) (hit-probs u1) (wound-probs w u2) (- 1 (save-prob u2 w))))
       :wounds-rapid-fire (format "%.2f" (* (* 2 (:number u1)) (hit-probs u1) (wound-probs w u2) (- 1 (save-prob u2 w))))
       :wounds-ffs        (format "%.2f" (* (* 4 (:number u1)) (hit-probs u1) (wound-probs w u2) (- 1 (save-prob u2 w))))
       :wounds-grenades   (format "%.2f" (* 6 (hit-probs u1) (wound-probs w u2) (- 1 (save-prob u2 w))))}))

  (def model1 (first (:models spacemarines)))
  (def model2 (first (:models conscripts)))

  (def weapon (first (:weapons model1)))

  (fight
   (a40kc-web.server.parse/parse "spacemarines.rosz") (a40kc-web.server.parse/parse "spacemarines.rosz"))



  (* (:number model1) (hit-probs model1) (wound-probs weapon model2) (- 1 (save-prob model2 weapon)))


  (fight-units spacemarines conscripts)

  )
