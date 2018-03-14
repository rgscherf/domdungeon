(ns domdungeon.battle.views.utils
  (:require [domdungeon.battle.subs :as bsubs]
            [re-frame.core :as rf]))

(defn atb-pct-fill
  [{:keys [actor/team actor/id actor/atb]}]
  (if-let [enqueued @(rf/subscribe [::bsubs/actor-enqueued-actions team id])]
    (let [time-until-action (- (:skill/action-time enqueued)
                               (:db/current-time enqueued))
          pct-of-action-time (min 100
                                  (* 100 (/ time-until-action
                                            (:skill/action-delay enqueued))))]
      (str pct-of-action-time "%"))
    (str (Math/floor atb) "%")))

(defn event-game-coords
  [event]
  (let [rect (.getBoundingClientRect (js/document.getElementById "game"))]
    {:x (Math/round
          (- (.-clientX event)
             (.-left rect)))
     :y (Math/round
          (- (.-clientY event)
             (.-top rect)))}))

(defn draw-targeting-line
  "Draw a line between two points.
  Described in http://www.monkeyandcrow.com/blog/drawing_lines_with_css3/"
  [[origin target targetable?]]
  (let [x1 (:x origin)
        y1 (:y origin)
        x2 (:x target)
        y2 (:y target)]
    (let [length (Math/sqrt (+ (* (- x1 x2)
                                  (- x1 x2))
                               (* (- y1 y2)
                                  (- y1 y2))))
          angle (* (Math/atan2 (- y2 y1) (- x2 x1))
                   (/ 180 Math/PI))
          transform-str (str "rotate(" angle "deg)")]
      [:div.targetLine {:style {:position  "absolute"
                                :transform transform-str
                                :width     length
                                :left      x1
                                :top       y1}
                        :class (if targetable? "targetLine__targetable"
                                               "targetLine__notTargetable")}])))

