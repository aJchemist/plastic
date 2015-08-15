(ns plastic.worker.frame.counters
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch]])
  (:require [plastic.worker.frame.router :refer [event-chan purge-chan]]))

(def ^:dynamic *pending-jobs-counters* {})

(defn inc-counter-for-job [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id inc)))

(defn dec-counter-for-job [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id dec))
  (get *pending-jobs-counters* job-id))

(defn remove-counter-for-job [job-id]
  (set! *pending-jobs-counters* (dissoc *pending-jobs-counters* job-id)))

(defn update-counter-for-job [job-id]
  (when (zero? (dec-counter-for-job job-id))
    (remove-counter-for-job job-id)
    (main-dispatch :worker-job-done job-id)))