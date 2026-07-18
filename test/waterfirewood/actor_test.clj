(ns waterfirewood.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [waterfirewood.actor :as actor]
            [waterfirewood.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-worker! st {:worker-id "worker-1" :name "Amara Diallo"})
    (store/register-site! st {:site-id "SITE-1" :name "Kolda Collection Point" :max-supply-cost 2000})
    st))

(deftest commits-a-registered-work-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :log-work-record :stake :low
                  :site-id "SITE-1" :task "water collection progress log"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "worker-1"))))))

(deftest holds-an-unregistered-site-proposal
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :log-work-record :stake :low
                  :site-id "SITE-ghost" :task "water collection progress log"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "worker-1")))))

(deftest interrupts-then-approves-safety-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:worker-id "worker-1" :op :flag-safety-concern :stake :low
                  :site-id "SITE-1" :hazard-type :water-quality-hazard}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "worker-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "worker-1")))))))

(deftest holds-a-scope-excluded-collection-execution-op-even-at-high-confidence
  (testing "an actor run can never commit a proposal that would finalize a collection-work-execution decision, regardless of disposition path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:worker-id "worker-1" :op :finalize-collection-operation :stake :low
                    :site-id "SITE-1" :task "collection execution decision"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "worker-1"))))))

(deftest holds-a-scope-excluded-site-safety-clearance-op-even-at-high-confidence
  (testing "an actor run can never commit a proposal that would finalize a site-safety-clearance decision (e.g. declaring a water source safe for collection), regardless of disposition path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:worker-id "worker-1" :op :declare-water-source-safe-for-collection :stake :low
                    :site-id "SITE-1" :task "water source safety clearance"}
          result (actor/run-request! graph request {} "thread-5")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "worker-1"))))))
