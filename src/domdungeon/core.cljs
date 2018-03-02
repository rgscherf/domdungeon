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

(defn skill-card
  [{:keys [atb skills]} rowstart colstart]
  (when (>= atb 1)
    ^{:key rowstart}
    [:div.charCard.charCard__skillCard
     {:style {:grid-area (str rowstart "/ " colstart "/ span 2 / span 2")}}
     #_[:img {:src "/img/scroll_w_003.png"}]
     [:div.charCard__arrow
      [:i.fas.fa-chevron-double-right]]
     (map (fn [s] [:div.charCard__skillName (get skill-names s)]) skills)
     ]))

(defn character-card
  [{:keys [maxhealth health maxmana mana name atb skills]} rowstart colstart]
  ^{:key rowstart}
  [:div.charCard {:style {:grid-area (str rowstart " / " colstart " / span 2 / span 5")}}
   [:div.charCard__row #_{:style {:margin-top "5px" :margin-bottom "5px"}}
    [:div.charCard__name name]
    [:div.charCard__hp (str health "/" maxhealth)]
    [:div.charCard__mp (str mana "/" maxmana)]]
   [:div.charCard__row.charCard__atb
    [:div {:style {:background "white"

                   :height     "100%"
                   :width      (str (Math/floor atb) "%")}}]]
   #_[:div.charCard__row {:style {:flex            "2 2 auto"
                                  :align-items     "center"
                                  :justify-content "space-between"}}
      (map skill-card skills)]]
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
    (map character-card @(rf/subscribe [:characters]) [7 9 11] (repeat 1))
    (map skill-card @(rf/subscribe [:characters]) [7 9 11] (repeat 6))
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
