(ns domdungeon.battle.views.enemy
  (:require [re-frame.core :as rf]
            [domdungeon.battle.views.utils :as vu]
            [domdungeon.battle.events :as be]))

(defn enemy-card
  [[_ {:keys [id name maxhealth health
              pstr pdef mstr mdef status] :as enemy}] rowstart colstart]
  ^{:key id}
  [:div.enemyGrid {:on-click      #(do
                                     (.stopPropagation %)
                                     (be/click-enemy id))
                   :on-mouse-over #(rf/dispatch [:mouse-is-on-enemy])
                   :on-mouse-out  #(rf/dispatch [:mouse-unset-friendly-state (-> % .-relatedTarget .-className)])
                   :style         {:grid-area (str rowstart " / " colstart " / span 1 / span 3")}}
   [:div.enemyGrid__name.leftPad name]
   [:div.enemyGrid__status (let [healthpct (* 100 (/ health maxhealth))]
                             (cond
                               (status :dead) "DEAD"
                               (< healthpct 10) "CRIT"
                               (< healthpct 25) "LOW"
                               (< healthpct 50) "WOUND"
                               (< healthpct 75) "OK"
                               :else "FINE"))]

   [:div.enemyGrid__statsAndResists
    [:div.enemyGrid__stats
     [:div.charGrid__small pstr]
     [:div.charGrid__small pdef]
     [:div.charGrid__small mstr]
     [:div.charGrid__small mdef]
     [:div.charGrid__small "pstr"]
     [:div.charGrid__small "pdef"]
     [:div.charGrid__small "mstr"]
     [:div.charGrid__small "mdef"]
     ]
    [:div.enemyGrid__resists]]

   [:div.enemyGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width (vu/atb-pct-fill enemy)
                                      :background (if (:atb-on? enemy) "white" "cyan")}}]]]
   ])

