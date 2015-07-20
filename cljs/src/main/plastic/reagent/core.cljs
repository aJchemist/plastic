(ns plastic.reagent.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end with-group-collapsed stopwatch]])
  (:require [plastic.schema.paths]
            [reagent.impl.batching :as batching]
            [reagent.interop :refer-macros [.' .!]]
            [reagent.impl.component :as comp]))

(defn fake-raf [f]
  (js/setTimeout f 16))

(def next-tick
  (let [w js/window]
    (or (.' w :requestAnimationFrame)
      (.' w :webkitRequestAnimationFrame)
      (.' w :mozRequestAnimationFrame)
      (.' w :msRequestAnimationFrame)
      fake-raf)))

(defn compare-mount-order [c1 c2]
  (- (.' c1 :cljsMountOrder)
    (.' c2 :cljsMountOrder)))

;; sort components by mount order, to make sure parents
;; are rendered before children
(defn run-queue [a]
  (.sort a compare-mount-order)
  (let [len (alength a)]
    (let [[res ms-time] (with-group-collapsed (str "Rendering queue with " len (if (= len 1) " component" " components"))
                          (stopwatch
                            (dotimes [i len]
                              (let [c (aget a i)]
                                (when (.' c :cljsIsDirty)
                                  (let [[res ms-time] (with-group-collapsed (.cljsName c)
                                                        (stopwatch
                                                          (.' c forceUpdate)))]
                                    (log (str "  => " ms-time "ms"))
                                    res))))))]
      (log (str "  => " ms-time "ms"))
      res)))

(defn run-funs [a]
  (dotimes [i (alength a)]
    ((aget a i))))

(deftype RenderQueue [^:mutable queue ^:mutable scheduled? ^:mutable after-render]
  Object
  (queue-render [this c]
    (.push queue c)
    (.schedule this))
  (add-after-render [_ f]
    (.push after-render f))
  (schedule [this]
    (when-not scheduled?
      (set! scheduled? true)
      (next-tick #(.run-queue this))))
  (run-queue [_]
    (let [q queue aq after-render]
      (set! queue (array))
      (set! after-render (array))
      (set! scheduled? false)
      (run-queue q)
      (run-funs aq))))

(def plastic-render-queue (RenderQueue. (array) false (array)))

(log "resetting render queue")
(set! batching/render-queue plastic-render-queue)