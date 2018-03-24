(ns domdungeon.core
  (:require [clojure.spec.alpha :as s]
            [domdungeon.battle.subs]
            [domdungeon.battle.events :as battle-events]
            [domdungeon.battle.views.main :as battle-views]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(s/check-asserts true)
(enable-console-print!)

(defn mount []
  (rf/dispatch-sync [::battle-events/init])
  (r/render [battle-views/root]
            (.getElementById js/document "app")))

(def one-frame (/ 1000 24))

(js/window.setInterval #(rf/dispatch [::battle-events/increment-atb]) one-frame)
(js/window.setInterval #(rf/dispatch [::battle-events/timestamp]) one-frame)

(mount)
