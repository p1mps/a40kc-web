(ns a40kc-web.server.fight
  (:require
    [clojure.string :as string]))


(def number-experiments 100)


;; TODO: handle grenades (either shoot or grenade)

;; TODO roll d3

(defn roll-dice [dice]
  (rand-nth (range 1 (+ 1 dice))))


(defn roll
  ;; TODO handle 3D6+2
  ([dice]
   (if (= dice "1")
     1
     (when (string/includes? dice "D")
       (let [[times dice] (string/split dice #"D")
             dice         (Integer/parseInt dice)]
         (if (seq times)
           (reduce + (take (Integer/parseInt times) (repeatedly (partial roll-dice dice))))
           (roll-dice dice)))))))


(defn bs [unit]
  (read-string (string/replace (:bs (:chars unit)) "+" "")))

(defn strength [weapon]
  (read-string (:s (:chars weapon))))


(defn toughness [unit]
  (read-string (:t (:chars unit))))


(defn success? [roll stat]
  (>= roll stat))

(defn hit? [char]
  (success? (roll-dice 6) char))

(defn to-wound [weapon target-unit]
  (let [comparison (- (strength weapon) (toughness target-unit))]
    (- 4 comparison)))

(defn wound? [weapon target-unit]
  (success? (roll-dice 6) (to-wound weapon target-unit)))

(defn save? [armor-save]
  (success? (roll-dice 6) armor-save))

(defn damage [weapon]
  (:d (:chars weapon)))

(defn save [model]
  (read-string (string/replace (:save (:chars model)) "+" "")))

;; TODO: number of attacks * number of units
(defn shoot [model1 model2]
  (for [w (:weapons model1)]
    {:weapon-name (:name w)
     :weapon-chars (:chars w)
     :wounds      (if (and (hit? (bs model1)) (not (save? (save model2))) (wound? w model2))
                    (let [damage (damage w)]
                      (if (and damage (not (string/includes? damage "D")))
                        (roll damage)
                        1))
                    0)}))

(defn monte-carlo-shoot [model1 model2 n]
  (->>
   (let [shooting (flatten (repeatedly n #(shoot model1 model2)))]
       (loop [result {}
              s      shooting]
         (if-not (seq s)
           result
           (recur (update result (:weapon-name (first s))
                          conj (:wounds (first s))) (rest s)))))
   (reduce (fn [result value]
             (assoc result (first value) (second value))

             )

           {}))

  )



(defn stats [unit1 unit2]
  (vec (for [m (:models unit1)]
         (monte-carlo-shoot m (first (:models unit2)) 1)

         ))



  )








(comment

  (def units (:units (a40kc-web.server.parse/parse "spacemarines.rosz")))
  (def captain (first units))

  (def squad (second units))

  (def captain-model (first (:models captain)))




  (shoot captain-model captain-model)

  (monte-carlo-shoot captain-model squad 100)

  (get-in captain [:chars])
  (bs captain)

  (stats captain squad)


  (stats squad squad)



)
