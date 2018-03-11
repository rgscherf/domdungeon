(ns domdungeon.battle.views.utils
  (:require [domdungeon.battle.subs :as bsubs]
            [re-frame.core :as rf]))

(defn atb-pct-fill
  [{:keys [team id atb]}]
  (if-let [enqueued @(rf/subscribe [::bsubs/actor-enqueued-actions team id])]
    (let [time-until-action (- (:action-time enqueued)
                               (:current-time enqueued))
          pct-of-action-time (min 100
                                  (* 100 (/ time-until-action
                                            (:action-delay enqueued))))]
      (str pct-of-action-time "%"))
    (str (Math/floor atb) "%")))
