(ns waterfirewood.store
  "SSoT for the ISCO-08 9624 water-and-firewood-collection-site
  scheduling/logistics coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics premise'
  — a site scheduling/logistics coordination robot performs crew
  scheduling, collection-log/progress-record logging and collection-
  equipment procurement coordination for a water-and-firewood-collection
  crew under this advisor/governor pair, which never dispatches hardware
  itself, never performs collection work itself, and never finalizes a
  collection-work-execution decision or a site-safety-clearance decision
  (including any water-quality-clearance determination), and never
  overrides a site safety supervisor's judgment — those remain the site
  safety supervisor's exclusive judgment). Modeled closely on
  cloud-itonami-isco-9212's livestockfarm.store for the outdoor-manual-
  labour hazard-domain shape, extended with a second, independent
  water-source/terrain hazard-scope dimension (water and firewood
  collectors carry physical loads over uneven terrain and may draw from
  water sources of uncertain quality, so physical-exertion/carrying-load
  hazard, terrain hazard and water-source-quality-concern stakes stack on
  top of the manual-collection-work hazard).

  Domain:

    worker — a registered water-and-firewood-collection crew member
             (:worker-id, :name)
    site   — a registered collection site {:site-id :name
             :max-supply-cost number}. `:max-supply-cost` is an
             informational registered ceiling used only to decide whether
             a `:coordinate-supply-order` proposal escalates to human
             sign-off (the governor never blocks a within-threshold order
             outright; it only decides commit vs. escalate).
    record — a committed operating record (a logged collection-log/
             progress entry, a scheduled crew operation, a flagged safety
             concern, or a coordinated supply order) — written ONLY via
             commit-record!.
    ledger — append-only audit trail, commit or hold.")

(defprotocol Store
  (worker [s worker-id])
  (site [s site-id])
  (records-of [s worker-id])
  (ledger [s])
  (register-worker! [s worker])
  (register-site! [s site])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (worker [_ worker-id] (get-in @a [:workers worker-id]))
  (site [_ site-id] (get-in @a [:sites site-id]))
  (records-of [_ worker-id] (filter #(= worker-id (:worker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-worker! [s w]
    (swap! a assoc-in [:workers (:worker-id w)] w) s)
  (register-site! [s f]
    (swap! a assoc-in [:sites (:site-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:workers {} :sites {} :records [] :ledger []}
                                    seed)))))
