(ns a40kc-web.figth-test
  (:require
   [clojure.test :refer :all]
   [a40kc-web.server.fight :as fight]))

(def weapon {:name "bolter"
             :chars {:s "4"}})

(def target {:chars {:t "4"
                     :save "6"}})

(def shooter {:chars {:t "4"
                      :bs "4+"}
              :weapons [weapon]})

(def multi-damage-weapon
  {:name "bolter"
   :chars {:s "4"
           :d "D6"}})


(def multi-damage-shooter
  {:chars {:t "4"
           :bs "4+"}
   :weapons [multi-damage-weapon]})



(deftest fight
  (testing "hit?"
    (with-redefs [fight/roll-dice (fn [dice] 5)]

      (is (= true (fight/hit? 4)))
      (is (= false (fight/hit? 6)))

      ;; needs a 4 to wound
      (is (= true (fight/wound? weapon target)))

      (is (= true (fight/save? 4)))
      (is (= false (fight/save? 6)))

      (is (= 4 (fight/to-wound weapon target)))

      (is (= (list {:weapon-name "bolter", :weapon-chars {:s "4"}, :wounds 1})
             (fight/shoot shooter target)))

      (is (= (list {:weapon-name "bolter", :weapon-chars {:s "4" :d "D6"}, :wounds 5})
             (fight/shoot multi-damage-shooter target)))

      (is (= {"bolter" (list 1 1)} (fight/monte-carlo-shoot shooter target 2)))

      (is (= {"bolter" (list 5 5)} (fight/monte-carlo-shoot multi-damage-shooter target 2)))

      ))

  (testing "no wounds"
    (with-redefs [fight/roll-dice (fn [dice] 1)]

      (is (= {"bolter" (list 0 0)} (fight/monte-carlo-shoot shooter target 2)))

      (is (= {"bolter" (list 0 0)} (fight/monte-carlo-shoot multi-damage-shooter target 2)))

      ))
  )



(def model-1 {:chars {:bs "2+"
                         :t "4"}
              :weapons [{:name "weapon"
                         :chars {:s "20"
                                  :ap "-"
                                  :d "1"}}]})


(def model-2 {:chars {:t "2"
                      :save "3+"}})


(fight/stats {:models [model-1]} {:models [model-2]})
