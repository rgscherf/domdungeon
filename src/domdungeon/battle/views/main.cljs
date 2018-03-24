(ns domdungeon.battle.views.main
  (:require [re-frame.core :as rf]
            [domdungeon.battle.subs :as bsubs]
            [domdungeon.battle.events :as bevts]
            [domdungeon.battle.views.utils :as vutils]
            [domdungeon.battle.views.enemy :as enemy]
            [domdungeon.battle.views.character :as char]))

(defn enemy-viz
  []
  [:div.enemyViz
   (doall
     (map enemy/enemy-card @(rf/subscribe [::bsubs/enemies])))])

(defn battle-viz
  []
  [:div.battleViz
   (map (fn [[time msg]]
          ^{:key time} [:div.battleVis__logItem msg])
        @(rf/subscribe [::bsubs/battle-log]))])

(defn submenu-entry
  [char-id entry]
  ^{:key (:id entry)}
  [:div.skillsSubMenu__entry {:on-click #(do
                                           (.stopPropagation %)
                                           (rf/dispatch [::bevts/skill-click char-id (:skill/id entry)]))}
   [:div.skillsSubMenu__entryNum "55"]
   [:div.skillsSubMenu__entryName (:skill/name entry)]
   [:div.skillsSubMenu__entryDesc (:skill/description entry)]
   ])

(defn submenu
  [{:keys [db/char-id db/items]}]
  (let [sorted (sort-by :skill/name items)]
    [:div.skillsSubMenu
     (map (partial submenu-entry char-id) sorted)]))

(defn root []
  [:div.screen__background
   (let [battle-outcome @(rf/subscribe [::bsubs/battle-outcome])]
     (cond
       (= battle-outcome :db/player-wins)
       [:div "YOU WIN."]

       (= battle-outcome :db/player-loses)
       [:div "YOU LOST."]

       (= battle-outcome :db/undecided)
       [:div#game.screen__grid {:on-mouse-move #(bevts/record-mouse-coords %)
                                :on-key-up     #(do
                                                  (js/console.log (str "Pressed key: " (-> % .-key)))
                                                  (if (-> % .-key (= "P"))
                                                    (rf/dispatch [::bevts/toggle-time])))
                                :on-click      #(do
                                                  (js/console.log (.-target %))
                                                  (rf/dispatch [::bevts/cancel-click]))}
        (doall
          (map char/charGrid
               @(rf/subscribe [::bsubs/characters])
               (iterate #(+ 2 %) 6)
               (repeat 1)))
        [enemy-viz]
        [battle-viz]
        (when @(rf/subscribe [::bsubs/skills-submenu])
          [submenu @(rf/subscribe [::bsubs/skills-submenu])])
        (when @(rf/subscribe [::bsubs/target-line])
          (vutils/draw-targeting-line @(rf/subscribe [::bsubs/target-line])))]))])
