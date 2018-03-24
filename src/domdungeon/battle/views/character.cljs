(ns domdungeon.battle.views.character
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as bevts]
            [domdungeon.battle.subs :as bsubs]
            [domdungeon.db.skills :as bs]
            [domdungeon.battle.views.utils :as vu]))

(defn char-border
  []
  [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 12"}
                          :class (if (= :db/characters @(rf/subscribe [::bsubs/current-target-team]))
                                   "charGrid__border--isTargetable")}])

(defn charGrid
  [[_ {:keys [actor/id actor/maxhealth actor/health actor/maxmana actor/mana
              actor/name actor/atb actor/skills actor/status] :as char}] rowstart colstart]
  ^{:key id}
  [:div.charGrid {:on-click       #(do
                                     (.stopPropagation %)
                                     (rf/dispatch [::bevts/friendly-click id]))
                  :on-mouse-enter #(rf/dispatch [::bevts/mouse-is-on-friendly])
                  :on-mouse-leave #(rf/dispatch [::bevts/mouse-unset-friendly-state (-> % .-relatedTarget .-className)])
                  :style          {:grid-area (str rowstart " / " colstart " / span 2 / span 6")}}

   (if (status :actor/dead)
     (list
       ^{:key 99}
       [char-border]
       ^{:key id}
       [:div "DEAD"])
     (list
       ;; border
       ^{:key 99}
       [char-border]

       ;; charname
       ^{:key id}
       [:div.charGrid__name
        [:div {:style {:font-size "250%"}} name]]

       ;; hp/mp
       ^{:key "primarystats"}
       [:div.charGrid__primaryStats
        [:div.charGrid__primaryStatsChild.leftPad (str "HP " health "/" maxhealth)]
        [:div.charGrid__primaryStatsChild.leftPad (str "MP " mana "/" maxmana)]]

       ^{:key "effects"}
       [:div.charGrid__effects "EFFECTS"]

       ;; atb gauge
       ^{:key "atb"}
       [:div.charGrid__atb
        [:div.charGrid__atbOutline
         [:div.charGrid__atbFill {:style {:background (if (:actor/atb-on? char) "white" "cyan")
                                          :width      (vu/atb-pct-fill char)}}]]]

       ;; skills
       ^{:key "skills"}
       [:div.charGrid__skills.leftPadLg
        (map (fn [s]
               ^{:key s} [:div.charGrid__skillName
                          {:class    (if (= atb 100) "charGrid__skillName--isReady")
                           :on-click #(when (= atb 100)
                                        (do
                                          (.stopPropagation %)
                                          (rf/dispatch [::bevts/skill-click id s])))}
                          (get-in bs/skills [s :skill/name])])
             skills)]))])

