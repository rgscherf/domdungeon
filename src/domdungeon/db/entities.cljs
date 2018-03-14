(ns domdungeon.db.entities
  (:require [clojure.spec.alpha :as s]))

(s/def :actor/name (s/and string? #(<= (count %) 8)))
(s/def :actor/status set?)
(s/def :actor/id pos-int?)
(s/def :actor/maxhealth pos-int?)
(s/def :actor/maxmana pos-int?)
(s/def :actor/health pos-int?)
(s/def :actor/mana pos-int?)
(s/def :actor/atb-on? boolean?)
(s/def :actor/atb (s/and pos? #(<= % 100)))
(s/def :actor/pstr int?)
(s/def :actor/mstr int?)
(s/def :actor/speed int?)
(s/def :actor/skills (s/and vector?
                            (s/* keyword?)))

(def characters {1 #:actor{:name      "ZEKE"
                           :status    #{}
                           :id        1
                           :maxhealth 150
                           :maxmana   50
                           :health    150
                           :mana      12
                           :atb-on?   true
                           :atb       0
                           :team      :db/characters
                           :pstr      25
                           :pdef      30
                           :mstr      10
                           :mdef      20
                           :speed     30
                           :skills    [:skill/fight :skill/item :skill/blackmagic]}
                 2 #:actor{:name      "HELIO"
                           :status    #{}
                           :id        2
                           :maxhealth 100
                           :maxmana   230
                           :health    100
                           :mana      210
                           :atb-on?   true
                           :atb       0
                           :team      :db/characters
                           :pstr      20
                           :pdef      30
                           :mstr      40
                           :mdef      30
                           :speed     20
                           :skills    [:skill/blackmagic :skill/whitemagic :skill/fight]}
                 3 #:actor{:name      "BUSTR"
                           :status    #{}
                           :id        3
                           :maxhealth 50
                           :maxmana   100
                           :health    50
                           :mana      100
                           :atb-on?   true
                           :atb       0
                           :team      :db/characters
                           :pstr      30
                           :pdef      30
                           :mstr      10
                           :mdef      10
                           :speed     40
                           :skills    [:skill/rage :skill/item :skill/blackmagic]}})

(def bestiary #:actor{:goomba #:actor{:name      "GOOMBA"
                                      :status    #{}
                                      :atb-on?   true
                                      :atb       0
                                      :maxhealth 200
                                      :health    200
                                      :team      :db/enemies
                                      :skills    [:skill/fight]
                                      :speed     40
                                      :pstr      10
                                      :mstr      40
                                      :pdef      40
                                      :mdef      20}})

(defn rand-in-10pct-range
  [n]
  (rand-nth (range (* n 0.9) (* n 1.1))))

(defn gen-creature
  "Generate a creature (from the bestiary map),
  with stats within a certain range of the creature's prototype."
  [creature-tag]
  (let [prototype (creature-tag bestiary)
        new-max-health (rand-in-10pct-range (:actor/maxhealth prototype))]
    (assoc prototype :actor/maxhealth new-max-health
                     :actor/health new-max-health
                     :actor/pstr (rand-in-10pct-range (:actor/pstr prototype))
                     :actor/mstr (rand-in-10pct-range (:actor/mstr prototype))
                     :actor/speed (rand-in-10pct-range (:actor/speed prototype))
                     :actor/pdef (rand-in-10pct-range (:actor/pdef prototype))
                     :actor/mdef (rand-in-10pct-range (:actor/mdef prototype))
                     :actor/atb (rand-nth (range 25)))))

(defn make-enemy
  [proto id tag]
  (let [enemy (gen-creature proto)]
    (-> enemy
        (assoc :actor/id id)
        (assoc :actor/tag tag))))

(def enemies
  (reduce (fn [collector n] (assoc collector (:actor/id n) n))
          {}
          (map (partial make-enemy :actor/goomba)
               (range 1 6)
               (seq "ABCDEFGHIJKLMNOP"))))

