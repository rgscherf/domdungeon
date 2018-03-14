(ns domdungeon.db.skills
  (:require [clojure.spec.alpha :as s]))

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

(defn wrap-battle-log-msg
  [targeter target this rest]
  (str (:actor/name targeter)
       ": "
       (:actor/name this)
       " "
       (:actor/name target)
       ", "
       rest))

(defn item-action-fn-wrap
  "Wraps item action-fns to include targeter information."
  [action-fn]
  (fn [targeter target this]
    (let [[newentity msgstub] (action-fn target)]
      [newentity
       (wrap-battle-log-msg targeter target this msgstub)])))

(def standard-action-delay 5000)

(def skills #:skill{:item          #:skill{:name          "ITEM"
                                           :action-delay  standard-action-delay
                                           :submenu-items #{:skill/item-potion :skill/item-ambrosia}
                                           :targeting-fn  target-random-friendly}

                    :item-ambrosia #:skill{:name         "AMBROSIA"
                                           :description  "Ally: heal 40MP"
                                           :action-delay standard-action-delay
                                           :parent-skill :skill/item
                                           :friendly?    true
                                           :targeting-fn target-random-friendly
                                           :action-fn    (item-action-fn-wrap (fn [target]
                                                                                (let [newmana (min (+ 40 (:actor/mana target))
                                                                                                   (:actor/maxmana target))]
                                                                                  [(assoc target :actor/mana newmana)
                                                                                   (str "restored "
                                                                                        (- newmana (:actor/mana target))
                                                                                        "MP")])))}

                    :item-potion   #:skill{:name         "POTION"
                                           :description  "Ally: heal 20HP"
                                           :action-delay standard-action-delay
                                           :parent-skill :skill/item
                                           :friendly?    true
                                           :targeting-fn target-random-friendly
                                           :action-fn    (item-action-fn-wrap (fn [target]
                                                                                (let [newhealth (min (+ 20 (:actor/health target))
                                                                                                     (:actor/maxhealth target))]
                                                                                  [(assoc target :actor/health newhealth)
                                                                                   (str "healed "
                                                                                        (- newhealth (:actor/health target))
                                                                                        "HP")])))}

                    :rage          #:skill{:name         "RAGE"
                                           :action-delay standard-action-delay
                                           :friendly?    false
                                           :targeting-fn target-random-opponent
                                           :action-fn    (fn [targeter target this]
                                                           (let [ragefactor 2
                                                                 newhealth (max 0
                                                                                (Math/round (- (:actor/health target)
                                                                                               (* ragefactor
                                                                                                  (:actor/pstr targeter)))))
                                                                 newstate (assoc target :actor/health newhealth)]
                                                             [(if (>= 0 newhealth)
                                                                (assoc newstate :actor/status #{:actor/dead})
                                                                newstate)
                                                              (wrap-battle-log-msg targeter target this
                                                                                   (str (- (:actor/health target) (:actor/health newstate))
                                                                                        " DMG"))]))}

                    :tools         #:skill{:name "TOOLS"}
                    :blackmagic    #:skill{:name "B.MAG"}
                    :whitemagic    #:skill{:name "W.MAG"}

                    :fight         #:skill{:name         "FIGHT"
                                           :action-delay standard-action-delay
                                           :friendly?    false
                                           :targeting-fn target-random-opponent
                                           :action-fn    (fn [targeter target this]
                                                           (let [newhealth (max 0
                                                                                (Math/round (- (:actor/health target)
                                                                                               (:actor/pstr targeter))))
                                                                 damage (- (:actor/health target) newhealth)
                                                                 newstate (assoc target :actor/health newhealth)]
                                                             [(if (>= 0 newhealth)
                                                                (assoc newstate :actor/status #{:actor/dead})
                                                                newstate)
                                                              (wrap-battle-log-msg targeter target this
                                                                                   (str damage " DMG"))]))}})

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

(s/def :skill/skill (s/keys :req [:skill/name :skill/action-delay :skill/targeting-fn :skill/action-delay]
                            :opt [:skill/description :skill/parent-skill :skill/friendly? :skill/submenu-items
                                  :skill/action-fn]))

(s/def :skill/action (s/merge :skill/skill
                              (s/keys :req [:skill/targeter :skill/action-time])))
