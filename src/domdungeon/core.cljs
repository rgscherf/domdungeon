(ns domdungeon.core
  (:require [domdungeon.battle.subs]
            [domdungeon.battle.events :as be]
            [domdungeon.battle.views.main :as bv]
            [reagent.core :as r]))
(enable-console-print!)

(defn mount []
  (be/init-app)
  (r/render [bv/root]
            (.getElementById js/document "app")))

(defonce set-interval!
         (.setInterval js/window be/increment-time (/ 1000 60)))

(mount)
