(ns domdungeon.battle.subs
  (:require [domdungeon.db.skills :as bs]
            [re-frame.core :as rf]))

(rf/reg-sub
  ::atb-time
  (fn [db _]
    (:db/atb-time db)))

(rf/reg-sub
  ::characters
  (fn [db _]
    (:db/characters db)))

(rf/reg-sub
  ::enemies
  (fn [db _]
    (:db/enemies db)))

(rf/reg-sub
  ::target-line
  (fn [db _]
    (when (:db/active-targeting db)
      [(:db/mouse-anchor-point db)
       (:db/mouse-current-location db)
       (= (get-in db [:db/active-targeting :db/current-pos-is-friendly?])
          (get-in db [:db/active-targeting :db/skill-is-friendly?]))])))

(rf/reg-sub
  ::current-target-team
  (fn [db _]
    (when (:db/active-targeting db)
      (if (get-in db [:db/active-targeting :db/skill-is-friendly?])
        :db/characters
        :db/enemies))))

(rf/reg-sub
  ::battle-log
  (fn [db _]
    (:db/battle-log db)))

(rf/reg-sub
  ::skills-submenu
  (fn [db _]
    (let [sub (:db/open-submenu db)]
      (when sub
        {:db/char-id (:db/char-id sub)
         :db/items   (map #(assoc (get bs/skills %) :skill/id %) (:db/items sub))}))))


(rf/reg-sub
  ::actor-enqueued-actions
  (fn [db [_ team id]]
    (when (not (empty? (:db/action-queue db)))
      (if-let [this-char-actions (seq (filter #(= [team id]
                                                  (:skill/targeter %))
                                              (:db/action-queue db)))]
        (assoc (first this-char-actions) :db/current-time (:db/current-time db))))))

(rf/reg-sub
  ::battle-outcome
  (fn [db _]
    (:db/outcome db)))