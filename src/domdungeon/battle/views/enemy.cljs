(ns domdungeon.battle.views.enemy
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as be]))

(defn enemy-card
  [[_ {:keys [id name maxhealth health atb]}] rowstart colstart]
  ^{:key id}
  [:div.enemyCard {:on-click       #(be/click-enemy id)
                   :on-mouse-enter #(rf/dispatch [:mouse-is-on-enemy])
                   :on-mouse-leave #(rf/dispatch [:mouse-unset-friendly-state])
                   :style          {:grid-area (str rowstart " / " colstart " / span 1 / span 3")}}
   [:div.charCard__row
    [:div.charCard__name name]
    [:div.charCard__hp "HEALTHY"]]
   [:div.charCard__row
    [:div.charCard__name atb]]])

