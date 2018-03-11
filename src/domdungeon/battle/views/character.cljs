(ns domdungeon.battle.views.character
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as bevts]
            [domdungeon.battle.skills :as bs]
            [domdungeon.battle.views.utils :as vu]))

(defn charGrid
  [[_ {:keys [id maxhealth health maxmana mana name atb skills
              pstr pdef mstr mdef status] :as char}] rowstart colstart]
  ^{:key rowstart}
  [:div.charGrid {:on-click       #(do
                                     (.stopPropagation %)
                                     (rf/dispatch [::bevts/friendly-click id]))
                  :on-mouse-enter #(rf/dispatch [::bevts/mouse-is-on-friendly])
                  :on-mouse-leave #(rf/dispatch [::bevts/mouse-unset-friendly-state (-> % .-relatedTarget .-className)])
                  :style          {:grid-area (str rowstart " / " colstart " / span 2 / span 6")}}

   ;; charname
   [:div.charGrid__name
    [:div {:style {:font-size "250%"}} name]]

   ;; hp/mp
   [:div.charGrid__primaryStats
    ;(if (status :dead)
    ;  [:div "DEAD"])
    ;(list)
    [:div.charGrid__primaryStatsChild.leftPad (str "HP " health "/" maxhealth)]
    [:div.charGrid__primaryStatsChild.leftPad (str "MP " mana "/" maxmana)]]

   [:div.charGrid__secondaryAndResists
    [:div.charGrid__secondaryStats
     [:div.charGrid__large pstr]
     [:div.charGrid__large pdef]
     [:div.charGrid__large mstr]
     [:div.charGrid__large mdef]
     [:div.charGrid__small "pstr"]
     [:div.charGrid__small "pdef"]
     [:div.charGrid__small "mstr"]
     [:div.charGrid__small "mdef"]]
    [:div.charGrid__resists]]

   ;; atb gauge
   [:div.charGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:background (if (:atb-on? char) "white" "cyan")
                                      :width (vu/atb-pct-fill char)}}]]]

   ;; border
   (if (= atb 100)
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 12"}}]
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 9"}}]
     )

   ;; skills
   (when (= atb 100)
     [:div.charGrid__skills.leftPadLg
      (map (fn [s]
             ^{:key s} [:div.charGrid__skillName
                        {:on-click #(do
                                      (.stopPropagation %)
                                      (rf/dispatch [::bevts/skill-click id s]))}
                        (get-in bs/skills [s :name])])
           skills)])])

