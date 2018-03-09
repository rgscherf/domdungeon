(ns domdungeon.battle.utils)




(def stats
  [:pstr :mstr :pdef :mdef :speed :maxhealth :maxmp])

(def characters {1 {:name      "ZEKE"
                    :status    #{}
                    :id        1
                    :maxhealth 150
                    :maxmana   50
                    :health    150
                    :mana      12
                    :atb-on?   true
                    :atb       0
                    :team      :characters
                    :pstr      25
                    :pdef      30
                    :mstr      10
                    :mdef      20
                    :speed     30
                    :skills    [:fight :item :blackmagic]}
                 2 {:name      "HELIO"
                    :status    #{}
                    :id        2
                    :maxhealth 100
                    :maxmana   230
                    :health    100
                    :mana      210
                    :atb-on?   true
                    :atb       0
                    :team      :characters
                    :pstr      20
                    :pdef      30
                    :mstr      40
                    :mdef      30
                    :speed     20
                    :skills    [:blackmagic :whitemagic :fight]}
                 3 {:name      "BUSTR"
                    :status    #{}
                    :id        3
                    :maxhealth 50
                    :maxmana   100
                    :health    50
                    :mana      100
                    :atb-on?   true
                    :atb       0
                    :team      :characters
                    :pstr      30
                    :pdef      30
                    :mstr      10
                    :mdef      10
                    :speed     40
                    :skills    [:rage :item :blackmagic]}})

(def bestiary {:goomba {:name      "GOOMBA"
                        :status    #{}
                        :atb-on?   true
                        :atb       0
                        :maxhealth 200
                        :health    200
                        :team      :enemies
                        :skills    [:fight]
                        :speed     40
                        :pstr      10
                        :mstr      40
                        :pdef      40
                        :mdef      20}})

(defn rand-in-10pct-range
  [n]
  (rand-nth (range (* n 0.9) (* n 1.1))))

(defn gen-creature
  [creature-tag]
  (let [prototype (creature-tag bestiary)
        new-max-health (:maxhealth prototype)]
    (assoc prototype :maxhealth new-max-health
                     :health new-max-health
                     :pstr (rand-in-10pct-range (:pstr prototype))
                     :mstr (rand-in-10pct-range (:mstr prototype))
                     :speed (rand-in-10pct-range (:speed prototype))
                     :pdef (rand-in-10pct-range (:pdef prototype))
                     :mdef (rand-in-10pct-range (:mdef prototype))
                     :atb (rand-nth (range 25)))))

(defn make-enemy
  [proto id tag]
  (let [enemy (gen-creature proto)]
    (-> enemy
        (assoc :id id)
        (assoc :tag tag))))

(def enemies
  (reduce (fn [collector n] (assoc collector (:id n) n))
          {}
          (map (partial make-enemy :goomba)
               (range 1 4)
               (seq "ABCDEFGHIJKLMNOP"))))

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

