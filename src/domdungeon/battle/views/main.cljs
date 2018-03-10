(ns domdungeon.battle.views.main
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as be]
            [domdungeon.battle.utils :as bu]
            [domdungeon.battle.views.enemy :as enemy]
            [domdungeon.battle.views.character :as char]))

(defn battle-viz
  []
  [:div.battleViz
   (map (fn [msg id]
          ^{:key id} [:div.battleVis__logItem msg])
        (take 8 @(rf/subscribe [:battle-log]))
        (range 8))])

(defn submenu-entry
  [char-id entry]
  ^{:key (:id entry)}
  [:div.skillsSubMenu__entry {:on-click #(do
                                           (.stopPropagation %)
                                           (rf/dispatch [:skill-click char-id (:id entry)]))}
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
   (let [battle-outcome @(rf/subscribe [:battle-outcome])]
     (cond
       (= battle-outcome :player-wins)
       [:div "YOU WIN."]

       (= battle-outcome :player-loses)
       [:div "YOU LOST."]

       (= battle-outcome :undecided)
       [:div#game.screen__grid {:on-mouse-move #(be/record-mouse-coords %)
                                :on-key-up     #(do
                                                  (js/console.log (-> % .-key))
                                                  (if (-> % .-key (= "P"))
                                                    (rf/dispatch [:toggle-time])))
                                :on-click      #(do
                                                  (js/console.log (.-target %))
                                                  (rf/dispatch [:cancel-click]))}
        (map char/charGrid
             @(rf/subscribe [:characters])
             (iterate #(+ 2 %) 6)
             (repeat 1))
        [battle-viz]
        (when @(rf/subscribe [:skills-submenu])
          [submenu @(rf/subscribe [:skills-submenu])])
        (map enemy/enemy-card
             @(rf/subscribe [:enemies])
             (iterate #(+ 1 %) 6)
             (repeat 14))
        (when @(rf/subscribe [:target-line])
          (bu/draw-targeting-line @(rf/subscribe [:target-line])))
        ]))])
