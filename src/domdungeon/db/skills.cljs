(ns domdungeon.db.skills
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :as rf]))

(defn rand-in-20pct-range
  [n]
  (rand-nth (range (* n 0.9) (* n 1.1))))

(defn clamp
  [minn n maxn]
  (min maxn (max minn n)))

(defn new-health-damaging
  [targeter target dmgexpr]
  (let [new-target-health (max 0
                               (Math/round (- (:actor/health target)
                                              dmgexpr)))]
    [new-target-health (- (:actor/health target)
                          new-target-health)]))

(defn calc-fight-dmg
  [targeter target]
  (rand-in-20pct-range
    (* (:actor/bpwr targeter)
       (clamp 0.5 (/
                    (:actor/pstr targeter)
                    (:actor/pdef target))
              1.5))))

(defn calc-spell-dmg
  [targeter target spell]
  (rand-in-20pct-range
    (* (:skill/spellpwr spell)
       (clamp 0.5
              (/ (:actor/mstr targeter)
                 (:actor/mdef target))
              1.5))))

(defn other-team
  [team]
  (s/assert :db/team team)
  (if (= team :db/enemies)
    :db/characters
    :db/enemies))

(defn target-random-friendly
  "Target a random friendly."
  [targeter db]
  (let [myself (get-in db targeter)
        targeted-member (->> myself :actor/team (get db) vals (remove #((:actor/status %) :actor/dead)) (map :actor/id) rand-nth)]
    [(:actor/team myself) targeted-member]))

(defn target-random-opponent
  "Target a random opponent."
  [targeter db]
  (let [myself (get-in db targeter)
        other-chars (get db (-> myself :actor/team other-team))
        targeted-member (->> other-chars vals (remove #((:actor/status %) :actor/dead)) (map :actor/id) rand-nth)]
    [(other-team (:actor/team myself)) targeted-member]))

(defn wrap-target-fn
  "Wrap a skill's default targeting fn with the desired coordinates."
  [desired-coords default-fn]
  (fn [targeter db]
    (if (get-in db desired-coords)
      desired-coords
      (default-fn targeter db))))

(defn add-alpha-tag
  [entity]
  (when (= :db/enemies (:actor/team entity))
    (str "(" (:actor/tag entity) ")")))

(defn wrap-battle-log-msg
  [targeter target this rest]
  (str (:actor/name targeter)
       (add-alpha-tag targeter)
       " "
       (:skill/name this)
       ": "
       (:actor/name target)
       (add-alpha-tag target)
       ", "
       rest))

(defn item-action-fn-wrap
  "Wraps item action-fns to include targeter information."
  [action-fn]
  (fn [targeter target this]
    (let [[newentity msgstub] (action-fn target)]
      [targeter
       newentity
       (wrap-battle-log-msg targeter target this msgstub)])))

(def standard-action-delay 5000)

(def skills #:skill{:item             #:skill{:name          "ITEM"
                                              :action-delay  standard-action-delay
                                              :submenu-items #{:skill/item-potion :skill/item-ambrosia}
                                              :targeting-fn  target-random-friendly}

                    :item-ambrosia    #:skill{:name         "AMBROSIA"
                                              :description  "Ally: heal 20MP"
                                              :action-delay standard-action-delay
                                              :parent-skill :skill/item
                                              :friendly?    true
                                              :targeting-fn target-random-friendly
                                              :action-fn    (fn [targeter target this-skill]
                                                              (list [:domdungeon.battle.events/modify-actor-mana
                                                                     targeter
                                                                     target
                                                                     this-skill
                                                                     +
                                                                     20
                                                                     (fn [mp] "restored " mp " MP")]))}

                    :item-potion      #:skill{:name         "POTION"
                                              :description  "Ally: heal 40HP"
                                              :action-delay standard-action-delay
                                              :parent-skill :skill/item
                                              :friendly?    true
                                              :targeting-fn target-random-friendly
                                              :action-fn    (fn [targeter target this-skill]
                                                              (list [:domdungeon.battle.events/modify-actor-health
                                                                     targeter
                                                                     target
                                                                     this-skill
                                                                     +
                                                                     40
                                                                     (fn [heal] "healed for " heal)]))}

                    :rage             #:skill{:name            "RAGE"
                                              :rage-dmg-factor 2
                                              :action-delay    standard-action-delay
                                              :friendly?       false
                                              :targeting-fn    target-random-opponent
                                              :action-fn       (fn [targeter target this]
                                                                 (list
                                                                   [:domdungeon.battle.events/modify-actor-health
                                                                    targeter
                                                                    target
                                                                    this
                                                                    -
                                                                    (* (:skill/rage-dmg-factor this)
                                                                       (calc-fight-dmg targeter target))
                                                                    (fn [dmg] (str "dealt " dmg " DMG with anger"))]))}

                    :tools            #:skill{:name "TOOLS"}
                    :blackmagic       #:skill{:name          "B.MAG"
                                              :submenu-items #{:skill/blackmagic-fire1}
                                              :action-delay  standard-action-delay}

                    :blackmagic-fire1 #:skill{:name         "FIRE 1"
                                              :action-delay standard-action-delay
                                              :friendly?    false
                                              :targeting-fn target-random-opponent
                                              :parent-skill :skill/blackmagic
                                              :manacost     5
                                              :spellpwr     100
                                              :action-fn    (fn [targeter target this-skill]
                                                              (if (<= (:skill/manacost this-skill)
                                                                      (:actor/mana targeter))
                                                                (list
                                                                  [:domdungeon.battle.events/modify-actor-mana
                                                                   targeter
                                                                   target
                                                                   this-skill
                                                                   +
                                                                   (:skill/manacost this-skill)
                                                                   nil]
                                                                  [:domdungeon.battle.events/modify-actor-health
                                                                   targeter
                                                                   target
                                                                   this-skill
                                                                   -
                                                                   (calc-spell-dmg targeter target this-skill)
                                                                   (fn [dmg] "burned for " dmg " DMG")])
                                                                (list [:domdungeon.battle.events/new-logmsg
                                                                       (wrap-battle-log-msg targeter target this-skill
                                                                                            (str "NOT ENOUGH MANA"))])))}

                    :whitemagic       #:skill{:name "W.MAG"}

                    :fight            #:skill{:name         "FIGHT"
                                              :action-delay standard-action-delay
                                              :friendly?    false
                                              :targeting-fn target-random-opponent
                                              :action-fn    (fn [targeter target this-skill]
                                                              (list [:domdungeon.battle.events/modify-actor-health
                                                                     targeter
                                                                     target
                                                                     this-skill
                                                                     -
                                                                     (calc-fight-dmg targeter target)
                                                                     (fn [dmg] (str "dealt " dmg " DMG"))]))}})

;; VALIDATION
(s/def :skill/name (s/and string? #(<= (count %) 8)))
(s/def :skill/action-delay pos-int?)
(s/def :skill/action-fn fn?)
(s/def :skill/targeting-fn fn?)
(s/def :skill/description (s/and string? #(<= (count %) 24)))
(s/def :skill/parent-skill (s/and keyword?
                                  #((-> skills keys set) %)))
(s/def :skill/friendly? boolean?)
(s/def :skill/submenu-items (s/and set? (s/* keyword?)))
(s/def :skill/targeter (s/tuple :db/team :actor/id))
(s/def :skill/action-time pos-int?)

(s/def :skill/skill (s/keys :req [:skill/name :skill/action-delay]
                            :opt [:skill/description :skill/parent-skill :skill/targeting-fn
                                  :skill/submenu-items :skill/action-fn]))

(s/def :skill/action (s/merge :skill/skill
                              (s/keys :req [:skill/targeter :skill/action-time])))
