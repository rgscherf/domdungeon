(ns domdungeon.battle.views.character
  (:require [re-frame.core :as rf]
            [domdungeon.battle.utils :as bu]
            [domdungeon.battle.events :as be]))

(defn charGrid
  [[_ {:keys [id maxhealth health maxmana mana name atb skills]}] rowstart colstart]
  ^{:key rowstart}
  [:div.charGrid {:on-mouse-enter #(rf/dispatch [:mouse-is-on-friendly])
                  :on-mouse-leave #(rf/dispatch [:mouse-unset-friendly-state (-> % .-relatedTarget .-className)])
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

