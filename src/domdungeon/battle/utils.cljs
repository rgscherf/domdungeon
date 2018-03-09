(ns domdungeon.battle.utils)

(defn other-team [team]
  (if (= team :enemies) :characters :enemies))

(defn target-random-friendly
  "Target a random friendly."
  [targeter db]
  (let [myself (get-in db targeter)
        targeted-member (->> myself :team (get db) vals (remove #((:status %) :dead)) (map :id) rand-nth)]
    [(:team myself) targeted-member]))

(defn target-random-opponent
  "Target a random opponent."
  [targeter db]
  (let [myself (get-in db targeter)
        other-chars (get db (-> myself :team other-team))
        targeted-member (->> other-chars vals (remove #((:status %) :dead)) (map :id) rand-nth)]
    [(other-team (:team myself)) targeted-member]))

(defn wrap-target-fn
  "Wrap a skill's default targeting fn with the desired coordinates."
  [desired-coords default-fn]
  (fn [targeter db]
    (if (get-in db desired-coords)
      desired-coords
      (default-fn targeter db))))

(defn item-action-fn-wrap
  "Wraps item action-fns to include targeter information."
  [action-fn]
  (fn [targeter target this]
    (let [[newentity msgstub] (action-fn target)]
      [newentity
       (str (:name targeter) msgstub)])))

(def skills {:item         {:name          "ITEM"
                            :action-delay  2000
                            :submenu-items #{:items/potion}
                            :targeting-fn  target-random-friendly
                            :action-fn     nil #_(fn [targeter target this]
                                                   (let [{:keys [action-fn]} (get items (:selected-item this))
                                                         [newentity msgstub] (action-fn target)]
                                                     [newentity
                                                      (str (:name targeter) msgstub)]))}

             :items/potion {:name         "POTION"
                            :description  "Ally: heal 20HP"
                            :action-delay 2000
                            :parent-skill :items
                            :friendly?    true
                            :targeting-fn target-random-friendly
                            :action-fn    (item-action-fn-wrap (fn [target]
                                                                 (let [newhealth (min (+ 20 (:health target))
                                                                                      (:maxhealth target))]
                                                                   [(assoc target :health newhealth)
                                                                    (str " healed "
                                                                         (:name target)
                                                                         " for "
                                                                         (- newhealth (:health target)))])))}
             :rage         {:name         "RAGE"
                            :action-delay 2000
                            :friendly?    false
                            :targeting-fn target-random-opponent
                            :action-fn    (fn [targeter target _]
                                            (let [ragefactor 2
                                                  newhealth (max 0
                                                                 (- (:health target)
                                                                    (* ragefactor
                                                                       (:pstr targeter))))
                                                  newstate (assoc target :health newhealth)]
                                              [(if (>= 0 newhealth)
                                                 (assoc newstate :status #{:dead})
                                                 newstate)
                                               (str
                                                 "RAGE!! "
                                                 (:name targeter)
                                                 " hit "
                                                 (:name target)
                                                 " for "
                                                 (- (:health target) (:health newstate))
                                                 " damage.")]))}
             :tools        {:name "TOOLS"}
             :blackmagic   {:name "B.MAG"}
             :whitemagic   {:name "W.MAG"}

             :fight        {:name         "FIGHT"
                            :action-delay 2000
                            :friendly?    false
                            :targeting-fn target-random-opponent
                            :action-fn    (fn [targeter target _]
                                            (let [newhealth (max 0
                                                                 (- (:health target)
                                                                    (:pstr targeter)))
                                                  newstate (assoc target :health newhealth)]
                                              [(if (>= 0 newhealth)
                                                 (assoc newstate :status #{:dead})
                                                 newstate)
                                               (str
                                                 (:name targeter)
                                                 " hit "
                                                 (:name target)
                                                 " for "
                                                 (- (:health target) (:health newstate))
                                                 " damage!")]))}})

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
                        :maxhealth 60
                        :health    60
                        :team      :enemies
                        :skills    [:fight]
                        :speed     40
                        :pstr      1
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

