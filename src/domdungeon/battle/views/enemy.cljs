(ns domdungeon.battle.views.enemy
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as bevts]
            [domdungeon.battle.views.utils :as vu]))

(defn enemy-card
  [[_ {:keys [actor/id actor/name actor/maxhealth actor/health
              actor/pstr actor/pdef actor/mstr actor/mdef actor/status actor/tag] :as enemy}]]
  ^{:key id}
  [:div.enemyGrid {:on-click      #(do
                                     (.stopPropagation %)
                                     (rf/dispatch [::bevts/enemy-click id]))
                   :on-mouse-over #(rf/dispatch [::bevts/mouse-is-on-enemy])
                   :on-mouse-out  #(rf/dispatch [::bevts/mouse-unset-friendly-state (-> % .-relatedTarget .-className)])}
   [:div.enemyGrid__portrait]
   [:div.enemyGrid__name name]
   [:div.enemyGrid__identifier tag]


   [:div.enemyGrid__statsAndResists
    [:div.enemyGrid__health (str "HEALTH: "
                                 (let [healthpct (* 100 (/ health maxhealth))]
                                   (cond
                                     (status :actor/dead) "DEAD"
                                     (< healthpct 10) "CRIT"
                                     (< healthpct 25) "LOW"
                                     (< healthpct 50) "WOUND"
                                     (< healthpct 75) "OK"
                                     :else "FINE")))]
    [:div.enemyGrid__status "EFFECTS"]
    [:div.enemyGrid__resists "RESISTS"]]

   [:div.enemyGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width      (vu/atb-pct-fill enemy)
                                      :background (if (:actor/atb-on? enemy) "white" "cyan")}}]]]
   ])

