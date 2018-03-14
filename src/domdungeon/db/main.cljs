(ns domdungeon.db.main
  (:require [clojure.spec.alpha :as s]
            [domdungeon.db.entities :as bu]))


(s/def :db/id pos-int?)
(s/def :db/current-time int?)
(s/def :db/team #{:db/characters :db/enemies})
(s/def :db/outcome #{:db/undecided :db/player-wins :db/player-loses})

(def initial-db
  #:db{:characters             bu/characters
       :enemies                bu/enemies
       :active-targeting       nil
       :mouse-anchor-point     nil
       :mouse-current-location nil
       :open-submenu           nil
       :current-time           0
       :action-queue           []
       :battle-log             '()
       :atb-active             true
       :outcome                :db/undecided
       :time-active            true})