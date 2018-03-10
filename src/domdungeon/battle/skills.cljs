(ns domdungeon.battle.skills)

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


(def standard-action-delay 5000)
(def skills {:item           {:name          "ITEM"
                              :action-delay  standard-action-delay
                              :submenu-items #{:items/potion :items/ambrosia}
                              :targeting-fn  target-random-friendly
                              :action-fn     nil}

             :items/ambrosia {:name         "AMBROSIA"
                              :description  "Ally: heal 40MP"
                              :action-delay standard-action-delay
                              :parent-skill :items
                              :friendly?    true
                              :targeting-fn target-random-friendly
                              :action-fn    (item-action-fn-wrap (fn [target]
                                                                   (let [newmana (min (+ 40 (:mana target))
                                                                                      (:maxmana target))]
                                                                     [(assoc target :mana newmana)
                                                                      (str " restored "
                                                                           (- newmana (:mana target))
                                                                           "MP to "
                                                                           (:name target))])))}

             :items/potion   {:name         "POTION"
                              :description  "Ally: heal 20HP"
                              :action-delay standard-action-delay
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

             :rage           {:name         "RAGE"
                              :action-delay standard-action-delay
                              :friendly?    false
                              :targeting-fn target-random-opponent
                              :action-fn    (fn [targeter target _]
                                              (let [ragefactor 2
                                                    newhealth (max 0
                                                                   (Math/round (- (:health target)
                                                                                  (* ragefactor
                                                                                     (:pstr targeter)))))
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

             :tools          {:name "TOOLS"}
             :blackmagic     {:name "B.MAG"}
             :whitemagic     {:name "W.MAG"}

             :fight          {:name         "FIGHT"
                              :action-delay standard-action-delay
                              :friendly?    false
                              :targeting-fn target-random-opponent
                              :action-fn    (fn [targeter target _]
                                              (let [newhealth (max 0
                                                                   (Math/round (- (:health target)
                                                                                  (:pstr targeter))))
                                                    damage (- (:health target) newhealth)
                                                    newstate (assoc target :health newhealth)]
                                                [(if (>= 0 newhealth)
                                                   (assoc newstate :status #{:dead})
                                                   newstate)
                                                 (str
                                                   (:name targeter)
                                                   " hit "
                                                   (:name target)
                                                   " for "
                                                   damage
                                                   " damage!")]))}})
