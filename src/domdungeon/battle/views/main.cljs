(ns domdungeon.battle.views.main
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as be]
            [domdungeon.battle.utils :as bu]))

(defn charGrid
  [[_ {:keys [id maxhealth health maxmana mana name atb skills]}] rowstart colstart]
  ^{:key rowstart}
  [:div.charGrid {:on-mouse-enter #(rf/dispatch [:mouse-is-on-friendly])
                  :on-mouse-leave #(rf/dispatch [:mouse-unset-friendly-state])
                  :style          {:grid-area (str rowstart " / " colstart " / span 2 / span 6")}}

   ;; charname
   [:div.charGrid__name
    [:div.leftPad {:style {:font-size "250%"}} name]]

   ;; hp/mp
   [:div.charGrid__primaryStats
    [:div.charGrid__primaryStatsChild.leftPad (str "HP " health "/" maxhealth)]
    [:div.charGrid__primaryStatsChild.leftPad (str "MP " mana "/" maxmana)]]

   ;; secondary/combat stats
   #_[:div.charGrid__secondaryStats.leftPad
      (map (fn [s] [:div s])
           (repeat 5 " PA 34  "))
      ]

   ;; atb gauge
   [:div.charGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width (str (Math/floor atb) "%")}}]]]

   ;; border
   (if (= atb 100)
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 12"}}]
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 9"}}]
     )

   ;; skills
   (when (<= atb 100)
     [:div.charGrid__skills.leftPadLg
      (map (fn [s]
             ^{:key s} [:div.charGrid__skillName
                        {:on-click #(be/skill-click id s false)}
                        (get bu/skill-names s)])
           skills)])])


(defn battle-viz
  []
  [:div.battleViz])

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

(defn root []
  [:div.screen__background
   [:div#game.screen__grid {:on-mouse-move #(be/record-mouse-coords %)}
    (map charGrid @(rf/subscribe [:characters]) [7 9 11] (repeat 1))
    [battle-viz]
    (map enemy-card @(rf/subscribe [:enemies]) (repeat 6) [2 5 8 11])
    (when @(rf/subscribe [:target-line])
      (bu/draw-targeting-line @(rf/subscribe [:target-line])))
    ]]
  )
