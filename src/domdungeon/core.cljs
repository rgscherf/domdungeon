(ns domdungeon.core
  (:require [domdungeon.battle.subs]
            [domdungeon.battle.events :as be]
            [domdungeon.battle.views.main :as bv]
            [re-frame.core :as rf]
            [reagent.core :as r]))
(enable-console-print!)

(defn mount []
  (be/init-app)
  (r/render [bv/root]
            (.getElementById js/document "app")))

(def one-frame (/ 1000 24))

(js/window.setInterval #(rf/dispatch [:increment-atb]) one-frame)
(js/window.setInterval #(rf/dispatch [:timestamp]) one-frame)

(mount)
