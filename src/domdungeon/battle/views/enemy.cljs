(ns domdungeon.battle.views.enemy
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as be]))

(defn enemy-card
  [[_ {:keys [id name maxhealth health atb]}] rowstart colstart]
  ^{:key id}
  [:div.enemyGrid {:on-click      #(be/click-enemy id)
                   :on-mouse-over #(rf/dispatch [:mouse-is-on-enemy])
                   :on-mouse-out  #(if
                                     (= "screen__grid" (-> % .-relatedTarget .-className))
                                     (rf/dispatch [:mouse-unset-friendly-state]))
                   :style         {:grid-area (str rowstart " / " colstart " / span 1 / span 3")}}
   [:div.enemyGrid__name name]
   [:div.enemyGrid__status (let [healthpct (* 100 (/ health maxhealth))]
                             (cond
                               (< healthpct 10) "CRIT"
                               (< healthpct 25) "LOW"
                               (< healthpct 50) "WOUND"
                               (< healthpct 75) "OK"
                               :else "FINE"))]
   [:div.enemyGrid__stats]
   [:div.enemyGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width (str (Math/floor atb) "%")}}]]]
   ])

