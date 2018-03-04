(ns domdungeon.battle.views.main
  (:require [re-frame.core :as rf]
            [domdungeon.battle.events :as be]
            [domdungeon.battle.utils :as bu]
            [domdungeon.battle.views.enemy :as enemy]
            [domdungeon.battle.views.character :as char]))

(defn battle-viz
  []
  [:div.battleViz])

(defn root []
  [:div.screen__background
   [:div#game.screen__grid {:on-mouse-move #(be/record-mouse-coords %)
                            :on-click #(rf/dispatch [:cancel-click])}
    (map char/charGrid
         @(rf/subscribe [:characters])
         (iterate #(+ 2 %) 7)
         (repeat 1))
    [battle-viz]
    (map enemy/enemy-card
         @(rf/subscribe [:enemies])
         (iterate #(+ 1 %) 7)
         (repeat 14))
    (when @(rf/subscribe [:target-line])
      (bu/draw-targeting-line @(rf/subscribe [:target-line])))
    ]]
  )
