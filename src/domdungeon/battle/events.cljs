(ns domdungeon.battle.events
  (:require [re-frame.core :as rf]
            [clojure.spec.alpha :as s]
            [domdungeon.battle.views.utils :as vutils]
            [domdungeon.db.main :as db]
            [domdungeon.db.skills :as skills]))

(defn clamp
  [minn n maxn]
  (min maxn (max minn n)))
;; 1: event dispatch : call these to init rf/dispatch


(defn record-mouse-coords
  [event]
  (rf/dispatch [::mouse-coords (vutils/event-game-coords event)]))

;; 2: event handlers : used with rf/dispatch

(defn mouse-pos-is-targetable?
  [db]
  (when (:db/active-targeting db)
    (let [friendliness (get-in db [:db/active-targeting :db/skill-is-friendly?])
          current-state (get-in db [:db/active-targeting :db/current-pos-is-friendly?])]
      (= friendliness current-state))))

(rf/reg-event-db
  ;; When the mouse leave an element that would display targeting status,
  ;; always reset the target status to a "disallowed" state.
  ;; e.g. the mouse moves to the gutter between two targetable enemies.
  ::mouse-unset-friendly-state
  (fn [db [_ event]]
    (if (or (not (#{"screen__grid" "battleViz"} event))
            (not (:db/active-targeting db))
            (not (mouse-pos-is-targetable? db)))
      db
      (let [state-for-friendliness (not (get-in
                                          db
                                          [:db/active-targeting :db/skill-is-friendly?]))]
        (assoc-in db [:db/active-targeting
                      :db/current-pos-is-friendly?]
                  state-for-friendliness)))))

(rf/reg-event-db
  ::mouse-is-on-friendly
  (fn [db _]
    (if (not (:db/active-targeting db))
      db
      (assoc-in db [:db/active-targeting :db/current-pos-is-friendly?] true))))

(rf/reg-event-db
  ::mouse-is-on-enemy
  (fn [db _]
    (if (not (:db/active-targeting db))
      db
      (assoc-in db [:db/active-targeting :db/current-pos-is-friendly?] false))))

(rf/reg-event-db
  ::switch-atb
  (fn [db _]
    (update db :db/atb-active not)))

(defn inc-atb
  [chars timescale]
  (into {}
        (map (fn [[i c]] [i (cond
                              ((:actor/status c) :actor/dead)
                              (assoc c :actor/atb 0)
                              (:actor/atb-on? c)
                              (assoc c :actor/atb
                                       (min 100 (+ (* (:actor/speed c) timescale) ;; 40 spd is about 50% faster than 0
                                                   (:actor/atb c))))
                              :else
                              c)])
             chars)))

(defn make-enemy-action
  [enemy]
  (let [selected-skill (rand-nth (:actor/skills enemy))
        action (selected-skill skills/skills)]
    [::enqueue-action (assoc action :skill/targeter [:db/enemies (:actor/id enemy)])]))


(defn is-charged-with-rage
  [char]
  (and ((:actor/status char) :skill/rage)
       (= (:actor/atb char) 100)))

(defn enraged-action
  [char]
  (let [skill (get skills/skills :skill/rage)]
    [::enqueue-action (assoc skill :skill/targeter [:db/characters (:actor/id char)])]))

(defn check-health
  [team]
  (reduce (fn [coll [k entity]]
            (conj coll
                  {k
                   (if (<= (:actor/health entity) 0)
                     (assoc entity :actor/status #{:actor/dead})
                     entity)}))
          {}
          team))

(defn count-of-dead-on-team
  [team]
  (->> team
       vals
       (filter #((:actor/status %) :actor/dead))
       count))


(rf/reg-event-fx
  ::increment-atb
  (fn [{:keys [db]} _]
    (if (:db/atb-active db)
      (let [timescale 0.009
            new-db (-> db
                       (update :db/characters check-health)
                       (update :db/enemies check-health)
                       (update :db/characters inc-atb timescale)
                       (update :db/enemies inc-atb timescale))
            enraged-chars (filter is-charged-with-rage (-> db :db/characters vals))
            enemies-to-act (->> new-db
                                :db/enemies
                                (map second)
                                (filter (fn [{:keys [actor/atb-on? actor/atb actor/status]}]
                                          (and (= atb 100)
                                               atb-on?
                                               (not (status :actor/dead))))))
            enemy-actions (map make-enemy-action enemies-to-act)]

        (cond

          ;; player win condition
          (= (count (:db/enemies db))
             (count-of-dead-on-team (:db/enemies db)))
          {:db       new-db
           :dispatch [::decide-outcome :db/player-wins]}

          ;; player lose condition
          (= (count (:db/characters db))
             (count-of-dead-on-team (:db/characters db)))
          {:db       new-db
           :dispatch [::decide-outcome :db/player-loses]}

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
    (if (empty? (:db/action-queue db))
      {:db db}
      (let [sorted-actions (into [] (sort-by :skill/action-time
                                             (:db/action-queue db)))]
        (if (>= (:db/current-time db)
                (:skill/action-time (first sorted-actions)))
          {:db       (assoc db :db/action-queue (rest sorted-actions))
           :dispatch [::perform-action (first sorted-actions)]}
          {:db db})))))

(rf/reg-event-fx
  ::timestamp
  (fn [{:keys [db]} _]
    (if (:db/time-active db)
      {:db       (assoc db :db/current-time (js/Date.now))
       :dispatch [::check-action-queue]})))

(rf/reg-event-db
  ::mouse-coords
  (fn [db [_ c]]
    (assoc db :db/mouse-current-location c)))

(rf/reg-event-db
  ::skill-click
  (fn [db [_ char-id skill-kw]]
    (s/assert :db/id char-id)
    (let [skill-data (get skills/skills skill-kw)
          ;; don't cancel the submenu if we clicked a child skill.
          newdb (if (:skill/parent-skill skill-data)
                  db
                  (assoc db :db/open-submenu nil))]
      (cond
        (:skill/submenu-items skill-data)
        (assoc newdb :db/open-submenu {:db/char-id char-id
                                       :db/items   (:skill/submenu-items skill-data)})
        (#{:skill/rage} skill-kw)
        (update-in newdb [:db/characters char-id :actor/status] conj skill-kw)
        :else
        (-> newdb
            (assoc :db/active-targeting
                   {:db/char-id                  char-id
                    :db/skill                    skill-kw
                    :db/skill-is-friendly?       (:skill/friendly? skill-data)
                    :db/current-pos-is-friendly? (:skill/friendly? skill-data)})
            (assoc :db/mouse-anchor-point (:db/mouse-current-location newdb)))))))

(defn receive-click
  [db target-coords]
  (s/assert :db/team (first target-coords))
  (s/assert :db/id (second target-coords))
  (if (or (not (mouse-pos-is-targetable? db))
          (not (:db/active-targeting db)))
    {:db db}
    (let [action-data (get skills/skills (get-in db [:db/active-targeting :db/skill]))
          action (-> action-data
                     (assoc :skill/targeting-fn (skills/wrap-target-fn target-coords
                                                                       (:skill/targeting-fn action-data)))
                     (assoc :skill/targeter [:db/characters (get-in db [:db/active-targeting :db/char-id])]))]
      {:dispatch [::enqueue-action action]
       :db       (-> db
                     (assoc :db/open-submenu nil)
                     (assoc :db/active-targeting nil)
                     (assoc :db/mouse-anchor-point nil))})))

(rf/reg-event-fx
  ::friendly-click
  (fn [{:keys [db]} [_ char-id]]
    (s/assert pos-int? char-id)
    (receive-click db [:db/characters char-id])))

(rf/reg-event-fx
  ::enemy-click
  (fn [{:keys [db]} [_ enemy-id]]
    (s/assert pos-int? enemy-id)
    (receive-click db [:db/enemies enemy-id])))

(rf/reg-event-db
  ::cancel-click
  (fn [db _]
    (-> db
        (assoc :db/open-submenu nil)
        (assoc :db/active-targeting nil)
        (assoc :db/open-submenu nil))))

;; Enqueue an action.
(rf/reg-event-fx
  ::enqueue-action
  (fn [{:keys [db]} [_ action]]
    (s/assert :skill/skill action)
    (let [action-time (+ (:skill/action-delay action)
                         (:db/current-time db))]
      {:db       (update db :db/action-queue conj (assoc action :skill/action-time action-time))
       :dispatch [::stop-atb (get-in db (:skill/targeter action))]})))

(defn add-alpha-tag
  "For enemy log messages, add the alpha tag of the enemy targeting/targeted."
  [entity]
  (when (= :db/enemies (:actor/team entity))
    (str "(" (:actor/tag entity) ")")))

(defn wrap-battle-log-msg
  "Wrap a battle log message in the standard template for targeter/target information."
  [targeter target skill rest]
  (str (:actor/name targeter)
       (add-alpha-tag targeter)
       " "
       (:skill/name skill)
       ": "
       (:actor/name target)
       (add-alpha-tag target)
       ", "
       rest))

(defn modify-target-basic-stat
  "Modify an actor's health or mana. A skill may also send a log-expr which is a fn that takes the stat modified amount and returns a message to be displayed in the combat log."
  [current-stat-field max-stat-field db targeter-ent target-ent skill plus-or-minus modification-expr log-expr]
  (let [{:keys [actor/team actor/id]} target-ent
        new-target-health (clamp 0
                                 (Math/round (plus-or-minus (current-stat-field target-ent)
                                                            modification-expr))
                                 (max-stat-field target-ent))
        health-amt-modified (- (current-stat-field target-ent)
                               new-target-health)]
    {:db         (assoc-in db [team id current-stat-field] new-target-health)
     :dispatch-n (list [::is-dead? [team id]]
                       (if log-expr
                         [::new-logmsg (wrap-battle-log-msg targeter-ent target-ent skill (log-expr health-amt-modified))]))}))

;; Modify and actor's health count. Performs bounds checking on amount.
(rf/reg-event-fx
  ::modify-actor-health
  (fn [{:keys [db]} [_ targeter-ent target-ent skill plus-or-minus modification-expr log-expr]]
    (modify-target-basic-stat :actor/health :actor/maxhealth db targeter-ent target-ent skill plus-or-minus modification-expr log-expr)))

;; Modify an actor's mana count. Performs bounds checking on amount.
(rf/reg-event-fx
  ::modify-actor-mana
  (fn [{:keys [db]} [_ targeter-ent target-ent skill plus-or-minus modification-expr log-expr]]
    (modify-target-basic-stat :actor/mana :actor/maxmana db targeter-ent target-ent skill plus-or-minus modification-expr log-expr)))

;; Check whether an actor should be dead.
(rf/reg-event-fx
  ::is-dead?
  (fn [{:keys [db]} [_ entity-coords]]
    (s/assert (s/tuple :db/team :actor/id) entity-coords)
    (let [entity (get-in db entity-coords)]
      (if (<= (:actor/health entity) 0)
        {:db       db
         :dispatch [::kill-actor entity-coords]}
        {:db db}))))

;; Perform status changes and cleanup when an actor is declared dead.
(rf/reg-event-db
  ::kill-actor
  (fn [db [_ entity-coords]]
    (s/assert (s/tuple :db/team :actor/id) entity-coords)
    (let [new-entity-state
          (-> (get-in db entity-coords)
              (assoc :actor/health 0)
              (assoc :actor/atb-on? false)
              (assoc :actor/status #{:actor/dead}))]
      (assoc-in db entity-coords new-entity-state))))


;; Stop the ATB for a character.
(rf/reg-event-db
  ::stop-atb
  (fn [db [_ target-coords]]
    (assoc-in db target-coords
              (-> (get-in db target-coords)
                  (assoc :actor/atb-on false)
                  (assoc :actor/atb 0)))))

;; Reset & reactivate the ATB counter, presumably after performing a skill.
;; Automatically called by ::perform-action.
(rf/reg-event-db
  ::reset-atb
  (fn [db [_ target-coords]]
    (assoc-in db target-coords
              (-> (get-in db target-coords)
                  (assoc :actor/atb-on true)
                  (assoc :actor/atb 0)))))

;; Perform an euqueued action. Skills return a diapatchable vector, or nothing.
(rf/reg-event-fx
  ::perform-action
  (fn [{:keys [db]} [_ {:keys [skill/targeting-fn skill/action-fn skill/targeter] :as action}]]
    (s/assert :skill/action action)
    (let [target-coords (targeting-fn targeter db)
          actions-to-perform (action-fn (get-in db targeter)
                                        (get-in db target-coords)
                                        action)]
      {:db         db
       :dispatch-n (conj actions-to-perform [::reset-atb targeter])})))

(rf/reg-event-db
  ::new-logmsg
  (fn [db [_ msg]]
    (let [current-time (:db/current-time db)]
      (update db :db/battle-log conj [current-time msg]))))

(rf/reg-event-db
  ::toggle-time
  (fn [db _]
    (-> db
        (update :db/atb-active not)
        (update :db/time-active not))))

(rf/reg-event-db
  ::decide-outcome
  (fn [db [_ outcome]]
    (s/assert :db/outcome outcome)
    (-> db
        (assoc :db/atb-active false)
        (assoc :db/outcome outcome))))

(rf/reg-event-db
  ::init
  (fn [_ _]
    db/initial-db))


