(ns a40kc-web.figth-test
  (:require
   [clojure.test :refer :all]
   [a40kc-web.server.fight :as fight]))

(def weapon {:name  "bolter"
             :chars {:s "4"
                     :d "1"
                     :ap "-"}})

(def target {:chars {:t    "4"
                     :save "6"}})

(def shooter {:chars   {:t  "4"
                        :bs "4+"}
              :weapons [weapon]})

(def multi-damage-weapon
  {:name  "bolter-multidamage"
   :chars {:s "4"
           :d "D6"
           :ap "-"}})


(def multi-damage-shooter
  {:chars   {:t  "4"
             :bs "4+"}
   :weapons [multi-damage-weapon]})


(def multi-weapons-shooter
  {:chars   {:t  "4"
             :bs "4+"}
   :weapons [weapon
             multi-damage-weapon]})



(deftest bs
  (testing "bs"
    (is (= 3 (fight/bs {:chars {:bs "3+"}})))))


(deftest fight
  (testing "hit?"
    (with-redefs [rand-nth (fn [_] 5)]
      (is (= true (fight/hit? 1)))
      (is (= true (fight/hit? 4)))
      (is (= false (fight/hit? 6)))
      ;; needs a 4 to wound
      (is (= true (fight/wound? weapon target)))
      (is (= true (fight/save? 4)))
      (is (= false (fight/save? 6)))
      (is (= 4 (fight/to-wound weapon target)))
      (is (= (list {:weapon-name "bolter-multidamage", :weapon-chars {:s "4" :d "D6" :ap "-"}, :wounds 5})
             (fight/shoot multi-damage-shooter target)))

      (is (= (list {:weapon-name "bolter", :weapon-chars {:s "4" :d "1" :ap "-"}, :wounds 5})
             (fight/shoot shooter target)))

      (is (= {"bolter" (list 5 5)} (fight/monte-carlo-shoot shooter target 2)))
      (is (= {"bolter-multidamage" (list 5 5)} (fight/monte-carlo-shoot multi-damage-shooter target 2)))))

  (testing "no wounds"
    (with-redefs [rand-nth (fn [_] 0)]
      (is (= {"bolter" (list 0 0)} (fight/monte-carlo-shoot shooter target 2)))
      (is (= {"bolter-multidamage" (list 0 0)} (fight/monte-carlo-shoot multi-damage-shooter target 2))))))


(deftest roll-damage
  (testing "1 damage"
    (is (= 1 (fight/roll-dice "1"))))

  (testing "2 damage"
    (is (= 2 (fight/roll-dice "2"))))


  (testing "multi damage"
    (with-redefs [fight/roll (fn [_] 5)]
      (is (= 5 (fight/roll-dice "D6")))
      (is (= 15 (fight/roll-dice "3D6")))
      (is (= 17 (fight/roll-dice "3D6+2"))))))

(deftest roll
  (testing "roll"
    (with-redefs [rand-nth (fn [_] 1)]
      (is (= 1 (fight/roll 6)))
      (is (= 1 (fight/roll 1)))
      (is (= 1 (fight/roll 3))))))


(deftest parse-dice
  (testing "parsing"
    (is (= {:times 1
            :dice 6
            :add 0} (fight/parse-dice "D6")))

    (is (= {:times 1
            :dice 1
            :add 0} (fight/parse-dice "1")))

    (is (= {:times 1
            :dice 3
            :add 0} (fight/parse-dice "D3")))

    (is (= {:times 3
            :dice 3
            :add 0} (fight/parse-dice "3D3")))

    (is (= {:times 1
            :dice 3
            :add 2} (fight/parse-dice "D3+2")))

    (is (= {:times 3
            :dice 3
            :add 2} (fight/parse-dice "3D3+2")))))



(deftest simple-functions
  (testing "success"
    (is (= true (fight/success?
              6 3)
           ))
    (is (= true (fight/success?
              3 3)
           ))
    (is (= false (fight/success?
              2 3)
)))


  (testing "save"
    (with-redefs [fight/roll (fn [_] 3)]
      (is (= true (fight/save?
                   3)))
      (is (= 5 (fight/save
                {:chars {:save "3+"}} "-2")))

      (is (= 7 (fight/save
                {:chars {:save "3+"}} "-4")))

      (is (= 3 (fight/save
                    {:chars {:save "3+"}} "-")))
      (is (= false (fight/save?
                    4)))))

  (testing "hit"
    (with-redefs [fight/roll (fn [_] 3)]
      (is (= 2 (fight/bs {:chars {:bs "2+"}})))
      (is (= false (fight/hit? (fight/bs {:chars {:bs "6+"}}))))
      (is (= true (fight/hit? (fight/bs {:chars {:bs "3+"}}))))))

  (testing "wound"
    (with-redefs [fight/roll (fn [_] 3)]
      (is (= true (fight/wound?
                   {:chars {:s "8"}}
                   {:chars {:t "4"}}
                   )))))

  (testing "to-wound"
    (is (= 2 (fight/to-wound {:chars {:s "8"}} {:chars {:t "4"}})))
    (is (= 6 (fight/to-wound {:chars {:s "3"}} {:chars {:t "6"}})))
    (is (= 5 (fight/to-wound {:chars {:s "4"}} {:chars {:t "5"}})))
    (is (= 3 (fight/to-wound {:chars {:s "5"}} {:chars {:t "4"}})))
    (is (= 4 (fight/to-wound {:chars {:s "4"}} {:chars {:t "4"}})))))
