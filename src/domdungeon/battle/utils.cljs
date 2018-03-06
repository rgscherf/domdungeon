(ns domdungeon.battle.utils)

(defn other-team [team]
  (if (= team :enemies) :characters :enemies))

(defn target-random-friendly
  "Target a random friendly."
  [targeter db]
  (let [myself (get-in db targeter)
        targeted-member (->> myself :team (get db) count rand-int inc)]
    [(:team myself) targeted-member]))

(defn target-random-opponent
  "Target a random opponent."
  [targeter db]
  (let [myself (get-in db targeter)
        other-team (get db (other-team (:team myself)))
        targeted-member (-> other-team count rand-int inc)]
    [other-team targeted-member]))

(defn wrap-target-fn
  "Wrap a skill's defauly targeting fn with the desired coordinates."
  [desired-coords default-fn]
  (fn [targeter db]
    (if (get-in db desired-coords)
      desired-coords
      (default-fn targeter db))))

(def items {:potion {:name        "POTION"
                     :description "Heals 20 HP"
                     :action-fn   (fn [target]
                                    (assoc target
                                      :health
                                      (min (+ 20 (:health target))
                                           (:maxhealth target))))}})

(def skills {:item       {:name          "ITEM"
                          :action-delay  2000
                          :friendly?     true
                          :selected-item :potion
                          :targeting-fn  target-random-friendly
                          :action-fn     (fn [_ target this]
                                           (let [{:keys [action-fn]}
                                                 (get items (:selected-item this))]
                                             (action-fn target)))}

             :rage       {:name "RAGE"}
             :tools      {:name "TOOLS"}
             :blackmagic {:name "B.MAG"}
             :whitemagic {:name "W.MAG"}

             :fight      {:name         "FIGHT"
                          :action-delay 2000
                          :friendly?    false
                          :targeting-fn target-random-opponent
                          :action-fn    (fn [targeter target _]
                                          (assoc target
                                            :health
                                            (- (:health target)
                                               (:pstr targeter))))}})

(def characters {1 {:name      "ZEKE"
                    :id        1
                    :maxhealth 150
                    :maxmana   50
                    :health    70
                    :mana      12
                    :speed     1
                    :atb       0
                    :team      :characters
                    :pstr      10
                    :skills    [:fight :item :blackmagic]}
                 2 {:name      "HELIO"
                    :id        2
                    :maxhealth 100
                    :maxmana   230
                    :health    80
                    :mana      210
                    :speed     20
                    :atb       0
                    :team      :characters
                    :pstr      10
                    :skills    [:blackmagic :whitemagic :fight]}
                 3 {:name      "BUSTR"
                    :id        3
                    :maxhealth 50
                    :maxmana   100
                    :health    40
                    :mana      100
                    :speed     40
                    :atb       0
                    :team      :characters
                    :pstr      10
                    :skills    [:rage :item :blackmagic]}})

(def enemies {1 {:name      "GOOMBA1"
                 :id        1
                 :maxhealth 50
                 :health    50
                 :speed     40
                 :team      :enemies
                 :pstr      10
                 :atb       0}
              2 {:name      "GOOMBA2"
                 :id        2
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :team      :enemies
                 :pstr      10
                 :atb       0}
              3 {:name      "GOOMBA3"
                 :id        3
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :team      :enemies
                 :pstr      10
                 :atb       0}
              4 {:name      "GOOMBA4"
                 :id        4
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :team      :enemies
                 :pstr      10
                 :atb       0}
              5 {:name      "GOOMBA5"
                 :id        5
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :pstr      10
                 :team      :enemies
                 :atb       0}})

(defn event-game-coords
  [event]
  (let [rect (.getBoundingClientRect (js/document.getElementById "game"))]
    {:x (Math/round
          (- (.-clientX event)
             (.-left rect)))
     :y (Math/round
          (- (.-clientY event)
             (.-top rect)))}))

(defn draw-targeting-line
  "Draw a line between two points.
  Described in http://www.monkeyandcrow.com/blog/drawing_lines_with_css3/"
  [[origin target targetable?]]
  (let [x1 (:x origin)
        y1 (:y origin)
        x2 (:x target)
        y2 (:y target)]
    (let [length (Math/sqrt (+ (* (- x1 x2)
                                  (- x1 x2))
                               (* (- y1 y2)
                                  (- y1 y2))))
          angle (* (Math/atan2 (- y2 y1) (- x2 x1))
                   (/ 180 Math/PI))
          transform-str (str "rotate(" angle "deg)")]
      [:div.targetLine {:style {:position  "absolute"
                                :transform transform-str
                                :width     length
                                :left      x1
                                :top       y1}
                        :class (if targetable? "targetLine__targetable"
                                               "targetLine__notTargetable")}]
      )))

