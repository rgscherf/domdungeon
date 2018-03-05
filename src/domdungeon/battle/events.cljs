(ns domdungeon.battle.events
  (:require [re-frame.core :as rf]
            [domdungeon.battle.utils :as bu]))

;; 1: event dispatch : call these to init rf/dispatch

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
  (rf/dispatch [:mouse-coords (bu/event-game-coords event)]))

;; 2: event handlers : used with rf/dispatch

(defn mouse-pos-is-targetable?
  [db]
  (when (:active-targeting db)
    (let [friendliness (get-in db [:active-targeting :skill-is-friendly?])
          current-state (get-in db [:active-targeting :current-pos-is-friendly?])]
      (= friendliness current-state))))

(rf/reg-event-db
  ;; When the mouse leave an element that would display targeting status,
  ;; always reset the target status to a "disallowed" state.
  ;; e.g. the mouse moves to the gutter between two targetable enemies.
  :mouse-unset-friendly-state
  (fn [db [_ event]]
    (if (or (not (= "screen__grid" event))
            (not (:active-targeting db))
            (not (mouse-pos-is-targetable? db)))
      db
      (let [state-for-friendliness (not (get-in
                                          db
                                          [:active-targeting :skill-is-friendly?]))]
        (assoc-in db [:active-targeting
                      :current-pos-is-friendly?]
                  state-for-friendliness)))))

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
  :increment-atb
  (fn [db _]
    (if (:atb-active db)
      (let [timescale 0.1]
        (-> db
            (update :characters inc-atb timescale)
            (update :enemies inc-atb timescale))))))

(defn increment-action-time
  [delay item]
  (update item :action-time + delay))

(rf/reg-event-fx
  :check-action-queue
  (fn [{:keys [db]} _]
    (if (empty? (:action-queue db))
      {:db db}
      (let [sorted-actions (into [] (sort-by :action-time
                                             (:action-queue db)))]
        (if (>= (:current-time db)
                (:action-time (first sorted-actions)))
          {:db       (assoc db :action-queue (mapv (partial increment-action-time 2000)
                                                   (rest sorted-actions)))
           :dispatch [:perform-action (first sorted-actions)]}
          {:db db})))))

(rf/reg-event-fx
  :timestamp
  (fn [{:keys [db]} _]
    {:db       (assoc db :current-time (js/Date.now))
     :dispatch [:check-action-queue]}))

(rf/reg-event-db
  :mouse-coords
  (fn [db [_ c]]
    (assoc db :mouse-current-location c)))

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

(rf/reg-event-fx
  :enemy-click
  (fn [{:keys [db]} [_ enemy-id]]
    (if (or (not (mouse-pos-is-targetable? db))
            (not (:active-targeting db)))
      {:db db}
      (let [action {:action   (get bu/skills (get-in db [:active-targeting :skill]))
                    :target   (get-in db [:enemies enemy-id])
                    :targeter {:team :characters
                               :id   (get-in db [:active-targeting :char-id])}}]
        {:dispatch [:enqueue-action action]
         :db       (-> db
                       (assoc :active-targeting nil)
                       (assoc :mouse-anchor-point nil))}))))

(rf/reg-event-db
  :cancel-click
  (fn [db _]
    (if (or (not (:active-targeting db))
            (mouse-pos-is-targetable? db))
      db
      (assoc db :active-targeting nil))))

(rf/reg-event-db
  :enqueue-action
  (fn [db [_ action]]
    (let [action-time (+ (get-in action [:action :action-delay])
                         (:current-time db))]
      (update db :action-queue conj
              (assoc action :action-time action-time)))))

(defn entity-coords
  [{:keys [team id]}]
  [team id])

(rf/reg-event-db
  :perform-action
  (fn [db [_ {:keys [action target targeter]}]]
    (assoc-in db (entity-coords target)
              ((:update-target-fn action)
                (get-in db (entity-coords targeter))
                (get-in db (entity-coords target))))))

(rf/reg-event-db
  :init
  (fn [_ _]
    {:characters             bu/characters
     :enemies                bu/enemies
     :active-targeting       nil
     :mouse-anchor-point     nil
     :mouse-current-location nil
     :current-time           0
     :action-queue           []
     :atb-active             true}))


