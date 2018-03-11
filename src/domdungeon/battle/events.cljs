(ns domdungeon.battle.events
  (:require [re-frame.core :as rf]
            [domdungeon.battle.skills :as bs]
            [domdungeon.battle.utils :as bu]))

;; 1: event dispatch : call these to init rf/dispatch

(defn init-app
  []
  (rf/dispatch-sync [::init]))

(defn click-enemy
  [enemy-id]
  (rf/dispatch [::enemy-click enemy-id]))

(defn record-mouse-coords
  [event]
  (rf/dispatch [::mouse-coords (bu/event-game-coords event)]))

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
  ::mouse-unset-friendly-state
  (fn [db [_ event]]
    (println event)
    (if (or (not (#{"screen__grid" "battleViz"} event))
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
  ::mouse-is-on-friendly
  (fn [db _]
    (if (not (:active-targeting db))
      db
      (assoc-in db [:active-targeting :current-pos-is-friendly?] true))))

(rf/reg-event-db
  ::mouse-is-on-enemy
  (fn [db _]
    (if (not (:active-targeting db))
      db
      (assoc-in db [:active-targeting :current-pos-is-friendly?] false))))

(rf/reg-event-db
  ::switch-atb
  (fn [db _]
    (update db :atb-active not)))

(defn inc-atb
  [chars timescale]
  (into {}
        (map (fn [[i c]] [i (cond
                              ((:status c) :dead)
                              (assoc c :atb 0)
                              (:atb-on? c)
                              (assoc c :atb
                                       (min 100 (+ (* (:speed c) timescale) ;; 40 spd is about 50% faster than 0
                                                   (:atb c))))
                              :else
                              c)])
             chars)))

(defn make-enemy-action
  [enemy]
  (let [selected-skill (rand-nth (:skills enemy))
        action (selected-skill bs/skills)]
    [::enqueue-action (assoc action :targeter [:enemies (:id enemy)])]))


(defn is-charged-with-rage
  [char]
  (and ((:status char) :rage)
       (= (:atb char) 100)))

(defn enraged-action
  [char]
  (let [skill (get bs/skills :rage)]
    [::enqueue-action (assoc skill :targeter [:characters (:id char)])]))

(defn check-health
  [team]
  (reduce (fn [coll [k v]]
            (conj coll
                  {k
                   (if (<= (:health v) 0)
                     (update v :status conj :dead)
                     v)}))
          {}
          team))

(defn count-of-dead-on-team
  [team]
  (->> team
       vals
       (filter #((:status %) :dead))
       count))


(rf/reg-event-fx
  ::increment-atb
  (fn [{:keys [db]} _]
    (if (:atb-active db)
      (let [timescale 0.009
            new-db (-> db
                       (update :characters check-health)
                       (update :enemies check-health)
                       (update :characters inc-atb timescale)
                       (update :enemies inc-atb timescale))
            enraged-chars (filter is-charged-with-rage (-> db :characters vals))
            enemies-to-act (->> new-db
                                :enemies
                                (map second)
                                (filter (fn [{:keys [atb-on? atb status]}]
                                          (and (= atb 100)
                                               atb-on?
                                               (not (status :dead))))))
            enemy-actions (map make-enemy-action enemies-to-act)]

        (cond

          ;; player win condition
          (= (count (:enemies db))
             (count-of-dead-on-team (:enemies db)))
          {:db new-db
           :dispatch [::decide-outcome :player-wins]}

          ;; player lose condition
          (= (count (:characters db))
             (count-of-dead-on-team (:characters db)))
          {:db new-db
           :dispatch [::decide-outcome :player-loses]}

          ;; enqueue enemy actions
          (not (empty? enemies-to-act))
          {:db         new-db
           :dispatch-n enemy-actions}

          ;; enqueue actions for ragers, and set their ATB to 0.
          (not (empty? enraged-chars))
          {:db         new-db
           :dispatch-n (map (fn [c] (enraged-action c)) enraged-chars)}

          :else
          {:db new-db})))))

(rf/reg-event-fx
  ::check-action-queue
  (fn [{:keys [db]} _]
    (if (empty? (:action-queue db))
      {:db db}
      (let [sorted-actions (into [] (sort-by :action-time
                                             (:action-queue db)))]
        (if (>= (:current-time db)
                (:action-time (first sorted-actions)))
          {:db       (assoc db :action-queue (rest sorted-actions))
           :dispatch [::perform-action (first sorted-actions)]}
          {:db db})))))

(rf/reg-event-fx
  ::timestamp
  (fn [{:keys [db]} _]
    (if (:time-active db)
      {:db       (assoc db :current-time (js/Date.now))
       :dispatch [::check-action-queue]})))

(rf/reg-event-db
  ::mouse-coords
  (fn [db [_ c]]
    (assoc db :mouse-current-location c)))

(rf/reg-event-db
  ::skill-click
  (fn [db [_ char-id skill-kw]]
    (let [skill-data (get bs/skills skill-kw)
          ;; don't cancel the submenu if we clicked a child skill.
          newdb (if (:parent-skill skill-data)
                  db
                  (assoc db :open-submenu nil))]
      (cond
        (:submenu-items skill-data)
        (assoc newdb :open-submenu {:char-id char-id
                                    :items   (:submenu-items skill-data)})
        (#{:rage} skill-kw)
        (update-in newdb [:characters char-id :status] conj skill-kw)
        :else
        (-> newdb
            (assoc :active-targeting
                   {:char-id                  char-id
                    :skill                    skill-kw
                    :skill-is-friendly?       (:friendly? skill-data)
                    :current-pos-is-friendly? (:friendly? skill-data)})
            (assoc :mouse-anchor-point (:mouse-current-location newdb)))))))

(defn receive-click
  [db target-coords]
  (if (or (not (mouse-pos-is-targetable? db))
          (not (:active-targeting db)))
    {:db db}
    (let [action-data (get bs/skills (get-in db [:active-targeting :skill]))
          action (-> action-data
                     (assoc :targeting-fn (bs/wrap-target-fn target-coords
                                                             (:targeting-fn action-data)))
                     (assoc :targeter [:characters (get-in db [:active-targeting :char-id])]))]
      {:dispatch [::enqueue-action action]
       :db       (-> db
                     (assoc :open-submenu nil)
                     (assoc :active-targeting nil)
                     (assoc :mouse-anchor-point nil))})))

(rf/reg-event-fx
  ::friendly-click
  (fn [{:keys [db]} [_ char-id]]
    (receive-click db [:characters char-id])))

(rf/reg-event-fx
  ::enemy-click
  (fn [{:keys [db]} [_ enemy-id]]
    (receive-click db [:enemies enemy-id])))

(rf/reg-event-db
  ::cancel-click
  (fn [db _]
    (-> db
        (assoc :open-submenu nil)
        (assoc :active-targeting nil)
        (assoc :open-submenu nil))))

(rf/reg-event-db
  ::enqueue-action
  (fn [db [_ action]]
    (let [action-time (+ (:action-delay action)
                         (:current-time db))
          new-targeter-state (-> (get-in db (:targeter action))
                                 (assoc :atb-on? false)
                                 (assoc :atb 0))]
      (->
        (update db :action-queue conj (assoc action :action-time action-time))
        (assoc-in (:targeter action) new-targeter-state)))))

(rf/reg-event-db
  ::is-dead
  (fn [db [_ team id]]
    (let [new-entity-state
          (-> (get-in db [team id])
              (assoc :atb-on? false)
              (assoc :status #{:dead}))]
      (assoc-in db [team id] new-entity-state))))

(rf/reg-event-fx
  ::perform-action
  (fn [{:keys [db]} [_ {:keys [targeting-fn action-fn targeter] :as action}]]
    (let [target-coords (targeting-fn targeter db)
          [new-entity-state logmsg] (action-fn
                                      (get-in db targeter)
                                      (get-in db target-coords)
                                      action)
          new-db (-> db
                     (update :battle-log conj logmsg)
                     (assoc-in target-coords new-entity-state)
                     (assoc-in (conj targeter :atb-on?) true))]
      {:db new-db})))

(rf/reg-event-db
  ::toggle-time
  (fn [db _]
    (-> db
        (update :atb-active not)
        (update :time-active not))))

(rf/reg-event-db
  ::decide-outcome
  (fn [db [_ outcome]]
    (-> db
        (assoc :atb-active false)
        (assoc :outcome outcome))))

(rf/reg-event-db
  ::init
  (fn [_ _]
    {:characters             bu/characters
     :enemies                bu/enemies
     :active-targeting       nil
     :mouse-anchor-point     nil
     :mouse-current-location nil
     :open-submenu           nil
     :current-time           0
     :action-queue           []
     :battle-log             '()
     :atb-active             true
     :outcome                :undecided
     :time-active            true}))


