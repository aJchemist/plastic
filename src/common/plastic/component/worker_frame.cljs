(ns plastic.component.worker-frame
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]
            [plastic.frame :refer [IFrameThread]]
            [plastic.worker.frame :refer [start stop]]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord WorkerFrame [thread-id]

  IFrameThread
  (-thread-id [_] thread-id)

  component/Lifecycle
  (start [component]
    (start component))
  (stop [component]
    (stop component)))

(defn make-worker-frame [thread-id]
  (WorkerFrame. thread-id))