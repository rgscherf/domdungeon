(ns domdungeon.battle.views.main
  (:require [re-frame.core :as rf]
            [domdungeon.battle.subs :as bsubs]
            [domdungeon.battle.events :as bevts]
            [domdungeon.battle.utils :as bu]
            [domdungeon.battle.views.enemy :as enemy]
            [domdungeon.battle.views.character :as char]))

(defn enemy-viz
  []
  [:div.enemyViz
   (map enemy/enemy-card @(rf/subscribe [::bsubs/enemies]))])

(defn battle-viz
  []
  [:div.battleViz
   (map (fn [msg id]
          ^{:key id} [:div.battleVis__logItem msg])
        @(rf/subscribe [::bsubs/battle-log])
        (range))])

(defn submenu-entry
  [char-id entry]
  ^{:key (:id entry)}
  [:div.skillsSubMenu__entry {:on-click #(do
                                           (.stopPropagation %)
                                           (rf/dispatch [::bevts/skill-click char-id (:id entry)]))}
   [:div.skillsSubMenu__entryNum "55"]
   [:div.skillsSubMenu__entryName (:name entry)]
   [:div.skillsSubMenu__entryDesc (:description entry)]
   ])

(defn submenu
  [{:keys [char-id items]}]
  (let [sorted (sort-by :name items)]
    [:div.skillsSubMenu
     (map (partial submenu-entry char-id) sorted)]))

(defn root []
  [:div.screen__background
   (let [battle-outcome @(rf/subscribe [::bsubs/battle-outcome])]
     (cond
       (= battle-outcome :player-wins)
       [:div "YOU WIN."]

       (= battle-outcome :player-loses)
       [:div "YOU LOST."]

       (= battle-outcome :undecided)
       [:div#game.screen__grid {:on-mouse-move #(bevts/record-mouse-coords %)
                                :on-key-up     #(do
                                                  (js/console.log (-> % .-key))
                                                  (if (-> % .-key (= "P"))
                                                    (rf/dispatch [::bevts/toggle-time])))
                                :on-click      #(do
                                                  (js/console.log (.-target %))
                                                  (rf/dispatch [::bevts/cancel-click]))}
        (map char/charGrid
             @(rf/subscribe [::bsubs/characters])
             (iterate #(+ 2 %) 6)
             (repeat 1))
        [enemy-viz]
        [battle-viz]
        (when @(rf/subscribe [::bsubs/skills-submenu])
          [submenu @(rf/subscribe [::bsubs/skills-submenu])])
        (when @(rf/subscribe [::bsubs/target-line])
          (bu/draw-targeting-line @(rf/subscribe [::bsubs/target-line])))
        ]))])
