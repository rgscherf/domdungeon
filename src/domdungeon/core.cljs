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

(def characters [{:name      "ZEKE"
                  :maxhealth 150
                  :maxmana   50
                  :health    70
                  :mana      12
                  :speed     1
                  :atb       0
                  :skills    [:attack :item :blackmagic]}
                 {:name      "HELIO"
                  :maxhealth 100
                  :maxmana   230
                  :health    80
                  :mana      210
                  :speed     20
                  :atb       0
                  :skills    [:blackmagic :whitemagic :attack]}
                 {:name      "BUSTR"
                  :maxhealth 50
                  :maxmana   100
                  :health    40
                  :mana      100
                  :speed     40
                  :atb       0
                  :skills    [:rage :item :blackmagic]}])

;; 1: event dispatch : call these to init rf/dispatch

(defn increment-time []
  (rf/dispatch [:increment-time]))

(defn init-app
  []
  (rf/dispatch-sync [:init]))

(defn switch-atb
  []
  (rf/dispatch [:switch-atb]))

;; 2: event handlers : used with rf/dispatch

(rf/reg-event-db
  :switch-atb
  (fn [db _]
    (update db :atb-active not)))

(defn inc-atb
  [chars timescale]
  (map (fn [c] (update c :atb
                       #(min 100 (+ (/ (:speed c) 500)      ;; 40 spd is about 50% faster than 0
                                    timescale
                                    %))))
       chars))

(rf/reg-event-db
  :increment-time
  (fn [db _]
    (if (:atb-active db)
      (let [timescale 0.1]
        (-> db
            (update :characters inc-atb timescale))))))

(rf/reg-event-db
  :init
  (fn [_ _]
    {:characters characters
     :atb-active true}))

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

;; 5: views

(defn charGrid
  [{:keys [maxhealth health maxmana mana name atb skills]} rowstart colstart]
  ^{:key rowstart}
  [:div.charGrid {:style {:grid-area (str rowstart " / " colstart " / span 2 / span 6")}}

   ;; charname
   [:div.charGrid__name
    [:div.leftPad {:style {:font-size "250%"}} name]]

   ;; hp/mp
   [:div.charGrid__primaryStats
    [:div.charGrid__primaryStatsChild.leftPad (str "HP " health "/" maxhealth)]
    [:div.charGrid__primaryStatsChild.leftPad (str "MP " mana "/" maxmana)]]

   ;; secondary/combat stats
   [:div.charGrid__secondaryStats.leftPad
    (map (fn [s] [:div s])
         (repeat 5 " PA 34  "))
    ]

   ;; atb gauge
   [:div.charGrid__atb
    [:div.charGrid__atbOutline
     [:div.charGrid__atbFill {:style {:width (str (Math/floor atb) "%")}}]]]

   ;; skills
   (when (= atb 100)
     [:div.charGrid__skills.leftPadLg
      (map (fn [s]
             ^{:key s} [:div.charGrid__skillName (get skill-names s)])
           skills)])

   ;; border
   (if (= atb 100)
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 12"}}]
     [:div.charGrid__border {:style {:grid-area "1/1/span 4/span 9"}}]
     )]
  )


(defn battle-viz
  []
  [:div.battleViz])

(defn enemy-card
  [rowstart colstart]
  [:div.enemyCard {:style {:grid-area (str rowstart " / " colstart " / span 1 / span 3")}}
   [:div.charCard__row
    [:div.charCard__name "ENEMY"]
    [:div.charCard__hp "HEALTHY"]]
   [:div.charCard__row
    [:div.charCard__name "Waiting..."]]])

#_(map character-card characters [7 9 11] (repeat 1))

(defn root []
  [:div.screen__background
   [:div.screen__grid
    (map charGrid @(rf/subscribe [:characters]) [7 9 11] (repeat 1))
    #_(map skill-card @(rf/subscribe [:characters]) [7 9 11] (repeat 6))
    [battle-viz]
    [enemy-card 6 2]
    [enemy-card 6 5]
    [enemy-card 6 8]
    [enemy-card 6 11]
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
