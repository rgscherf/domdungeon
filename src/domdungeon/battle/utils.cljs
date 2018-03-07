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
  "Wrap a skill's default targeting fn with the desired coordinates."
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
                                          (let [newhealth (- (:health target)
                                                             (:pstr targeter))
                                                newstate (assoc target :health newhealth)]
                                            (if (>= 0 newhealth)
                                              (assoc newstate :status #{:dead})
                                              newstate)))}})

(def stats
  [:pstr :mstr :pdef :mdef :speed :maxhealth :maxmp])


(def characters {1 {:name      "ZEKE"
                    :status    #{}
                    :id        1
                    :maxhealth 150
                    :maxmana   50
                    :health    70
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
                    :health    80
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
                    :health    40
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
                        :maxhealth 60
                        :health    60
                        :team      :enemies
                        :speed     40
                        :pstr      40
                        :mstr      40
                        :pdef      40
                        :mdef      20}})

(defn make-enemy
  [proto id tag]
  (let [enemy (get bestiary proto)]
    (-> enemy
        (assoc :id id)
        (assoc :tag tag))))

(def enemies
  (reduce (fn [collector n] (assoc collector (:id n) n))
          {}
          (map (partial make-enemy :goomba)
               (range 1 6)
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

