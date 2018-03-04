(ns domdungeon.core
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))
(enable-console-print!)

(def skill-names
  {:attack     "FIGHT"
   :item       "ITEM"
   :rage       "RAGE"
   :tools      "TOOLS"
   :blackmagic "B.MAG"
   :whitemagic "W.MAG"})

(def characters {1 {:name      "ZEKE"
                    :id        1
                    :maxhealth 150
                    :maxmana   50
                    :health    70
                    :mana      12
                    :speed     1
                    :atb       0
                    :skills    [:attack :item :blackmagic]}
                 2 {:name      "HELIO"
                    :id        2
                    :maxhealth 100
                    :maxmana   230
                    :health    80
                    :mana      210
                    :speed     20
                    :atb       0
                    :skills    [:blackmagic :whitemagic :attack]}
                 3 {:name      "BUSTR"
                    :id        3
                    :maxhealth 50
                    :maxmana   100
                    :health    40
                    :mana      100
                    :speed     40
                    :atb       0
                    :skills    [:rage :item :blackmagic]}})

(def enemies {1 {:name      "GOOMBA1"
                 :id        1
                 :maxhealth 50
                 :health    50
                 :speed     40
                 :atb       0}
              2 {:name      "GOOMBA2"
                 :id        2
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :atb       0}
              3 {:name      "GOOMBA3"
                 :id        3
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :atb       0}
              4 {:name      "GOOMBA4"
                 :id        4
                 :maxhealth 50
                 :health    40
                 :speed     40
                 :atb       0}})

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
                                               "targetLine__notTargetable")}]
      )))

;; 1: event dispatch : call these to init rf/dispatch

(defn increment-time []
  (rf/dispatch [:increment-time]))

(defn init-app
  []
  (rf/dispatch-sync [:init]))

(defn skill-click
  [char-id skill friendly?]
  (rf/dispatch [:skill-click char-id skill friendly?]))

(defn click-enemy
  [enemy-id]
  (rf/dispatch [:enemy-click enemy-id]))

(defn record-mouse-coords
  [event]
  (rf/dispatch [:mouse-coords (event-game-coords event)]))

;; 2: event handlers : used with rf/dispatch

(rf/reg-event-db
  ;; When the mouse leave an element that would display targeting status,
  ;; always reset the target status to a "disallowed" state.
  ;; e.g. the mouse moves to the gutter between two targetable enemies.
  :mouse-unset-friendly-state
  (fn [db _]
    (if (not (:active-targeting db))
      db
      (let [state-for-friendliness (not (get-in db [:active-targeting :skill-is-friendly?]))]
        (assoc-in db [:active-targeting :current-pos-is-friendly?] state-for-friendliness)))))

(rf/reg-event-db
  :mouse-is-on-friendly
  (fn [db _]
    (if (not (:active-targeting db))
      db
      (assoc-in db [:active-targeting :current-pos-is-friendly?] true))))

(rf/reg-event-db
  :mouse-is-on-enemy
  (fn [db _]
    (if (not (:active-targeting db))
      db
      (assoc-in db [:active-targeting :current-pos-is-friendly?] false))))

(rf/reg-event-db
  :switch-atb
  (fn [db _]
    (update db :atb-active not)))

(defn inc-atb
  [chars timescale]
  (into {}
        (map (fn [[i c]] [i (assoc c :atb
                                     (min 100 (+ (/ (:speed c) 500) ;; 40 spd is about 50% faster than 0
                                                 timescale
                                                 (:atb c))))])
             chars)))

(rf/reg-event-db
  :increment-time
  (fn [db _]
    (if (:atb-active db)
      (let [timescale 0.1]
        (-> db
            (update :characters inc-atb timescale))))))

(rf/reg-event-db
  :mouse-coords
  (fn [db [_ c]]
    (assoc db :mouse-current-location c)))

(rf/reg-event-db
  :init
  (fn [_ _]
    {:characters             characters
     :enemies                enemies
     :active-targeting       nil
     :mouse-anchor-point     nil
     :mouse-current-location nil
     :atb-active             true}))

(rf/reg-event-db
  :skill-click
  (fn [db [_ char-id skill friendly?]]
    (-> db
        (assoc :active-targeting
               {:char-id                  char-id
                :skill                    skill
                :skill-is-friendly?       friendly?
                :current-pos-is-friendly? false})
        (assoc :mouse-anchor-point (:mouse-current-location db)))))

(rf/reg-event-db
  :enemy-click
  (fn [db [_ enemy-id]]
    (if (not (:active-targeting db))
      db
      (-> db
          (update-in [:enemies enemy-id :health] dec)
          (assoc :active-targeting nil)
          (assoc :mouse-anchor-point nil)))))

;; 3: effect handlers
;; done

;; 4: query : used with rf/subscribe
(rf/reg-sub
  :atb-time
  (fn [db _]
    (:atb-time db)))

(rf/reg-sub
  :characters
  (fn [db _]
    (:characters db)))

(rf/reg-sub
  :enemies
  (fn [db _]
    (:enemies db)))

(rf/reg-sub
  :target-line
  (fn [db _]
    (when (:active-targeting db)
      [(:mouse-anchor-point db)
       (:mouse-current-location db)
       (= (get-in db [:active-targeting :current-pos-is-friendly?])
          (get-in db [:active-targeting :skill-is-friendly?]))])))

;; 5: views

(defn charGrid
  [[_ {:keys [id maxhealth health maxmana mana name atb skills]}] rowstart colstart]
  ^{:key rowstart}
  [:div.charGrid {:on-mouse-enter #(rf/dispatch [:mouse-is-on-friendly])
                  :on-mouse-leave #(rf/dispatch [:mouse-unset-friendly-state])
                  :style          {:grid-area (str rowstart " / " colstart " / span 2 / span 6")}}

   ;; charname
   [:div.charGrid__name
    [:div.leftPad {:style {:font-size "250%"}} name]]

   ;; hp/mp
   [:div.charGrid__primaryStats
    [:div.charGrid__primaryStatsChild.leftPad (str "HP " health "/" maxhealth)]
    [:div.charGrid__primaryStatsChild.leftPad (str "MP " mana "/" maxmana)]]

   ;; secondary/combat stats
   #_[:div.charGrid__secondaryStats.leftPad
      (map (fn [s] [:div s])
           (repeat 5 " PA 34  "))
      ]

   ;; atb gauge
   [:div.charGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width (str (Math/floor atb) "%")}}]]]

   ;; border
   (if (= atb 100)
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 12"}}]
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 9"}}]
     )

   ;; skills
   (when (<= atb 100)
     [:div.charGrid__skills.leftPadLg
      (map (fn [s]
             ^{:key s} [:div.charGrid__skillName
                        {:on-click #(skill-click id s false)}
                        (get skill-names s)])
           skills)])])


(defn battle-viz
  []
  [:div.battleViz])

(defn enemy-card
  [[_ {:keys [id name maxhealth health atb]}] rowstart colstart]
  ^{:key id}
  [:div.enemyCard {:on-click       #(click-enemy id)
                   :on-mouse-enter #(rf/dispatch [:mouse-is-on-enemy])
                   :on-mouse-leave #(rf/dispatch [:mouse-unset-friendly-state])
                   :style          {:grid-area (str rowstart " / " colstart " / span 1 / span 3")}}
   [:div.charCard__row
    [:div.charCard__name name]
    [:div.charCard__hp "HEALTHY"]]
   [:div.charCard__row
    [:div.charCard__name atb]]])

(defn root []
  [:div.screen__background
   [:div#game.screen__grid {:on-mouse-move #(record-mouse-coords %)}
    (map charGrid @(rf/subscribe [:characters]) [7 9 11] (repeat 1))
    [battle-viz]
    (map enemy-card @(rf/subscribe [:enemies]) (repeat 6) [2 5 8 11])
    (when @(rf/subscribe [:target-line])
      (draw-targeting-line @(rf/subscribe [:target-line])))
    ]]
  )

(defn mount []
  (init-app)
  (r/render [root]
            (.getElementById js/document "app")))

(defonce set-interval!
         (.setInterval js/window increment-time (/ 1000 60)))
;; render!
;; done

(mount)
