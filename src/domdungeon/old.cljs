(ns domdungeon.old
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))
(enable-console-print!)

;; 1: event dispatch

(defn select-div
  [n]
  (rf/dispatch [:select n]))

(defn init-app
  []
  (rf/dispatch-sync [:init]))

(defn add-clickable
  []
  (rf/dispatch [:add-div]))
;; 2: event handlers

(rf/reg-event-db
  :add-div
  (fn [db _]
    (update db :divs inc)))

(rf/reg-event-db
  :select
  (fn [db [_ n]]
    (assoc db :cursor-position n)))

(rf/reg-event-db
  :init
  (fn [_ _]
    {:divs            5
     :cursor-position 0}))

;; 3: effect handlers
;; done

;; 4: query
(rf/reg-sub
  :cursor
  (fn [db _]
    (:cursor-position db)))

(rf/reg-sub
  :n-divs
  (fn [db _]
    (:divs db)))

;; 5: views
(def selectable-style
  {:style {:margin          "10px"
           :display         "flex"
           :align-items     "center"
           :justify-content "center"
           :height          "50px"
           :width           "300px"
           :background      "blue"}})

(defn selectable-div [n]
  (let [style (if (= n @(rf/subscribe [:cursor]))
                (assoc-in selectable-style [:style :background] "purple")
                selectable-style)]
    [:div (merge {:on-click #(select-div n)} style)
     [:span (str "I am div #" n)]]))

(defn root []
  [:div
   [:div
    [:button {:on-click #(add-clickable)} "Add a clickable button!"]]
   [:div (str "Current pos: " @(rf/subscribe [:cursor]))]
   (map #(selectable-div %)
        (range @(rf/subscribe [:n-divs])))])

(defn mount []
  (init-app)
  (r/render [root]
            (.getElementById js/document "app")))

;; render!
;; done

(mount)