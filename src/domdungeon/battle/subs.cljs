(ns domdungeon.battle.subs
  (:require [domdungeon.battle.skills :as bs]
            [re-frame.core :as rf]))

(rf/reg-sub
  :atb-time
  (fn [db _]
    (:atb-time db)))

(rf/reg-sub
  :characters
  (fn [db _]
    (:characters db)))

(rf/reg-sub
  :enemies
  (fn [db _]
    (:enemies db)))

(rf/reg-sub
  :target-line
  (fn [db _]
    (when (:active-targeting db)
      [(:mouse-anchor-point db)
       (:mouse-current-location db)
       (= (get-in db [:active-targeting :current-pos-is-friendly?])
          (get-in db [:active-targeting :skill-is-friendly?]))])))

(rf/reg-sub
  :battle-log
  (fn [db _]
    (:battle-log db)))

(rf/reg-sub
  :skills-submenu
  (fn [db _]
    (let [sub (:open-submenu db)]
      (when sub
        {:char-id (:char-id sub)
         :items   (map #(assoc (get bs/skills %) :id %) (:items sub))}))))


(rf/reg-sub
  :actor-enqueued-actions
  (fn [db [_ team id]]
    (when (not (empty? (:action-queue db)))
      (if-let [this-char-actions (seq (filter #(= [team id]
                                               (:targeter %))
                                           (:action-queue db)))]
        (assoc (first this-char-actions) :current-time (:current-time db))))))