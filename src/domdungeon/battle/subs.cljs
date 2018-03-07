(ns domdungeon.battle.subs
  (:require [re-frame.core :as rf]))

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
