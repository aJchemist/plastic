(ns plastic.worker.editor.analysis.calls
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.util.helpers :as helpers]
            [meld.zip :as zip]))

; -------------------------------------------------------------------------------------------------------------------

(defn is-call? [loc]
  (if-let [parent-loc (zip/up loc)]
    (and (zip/list? parent-loc) (zip/leftmost? loc))))

(defn find-call-locs [loc]
  (filter is-call? (take-while zip/good? (iterate zip/next loc))))

(defn analyze-calls [analysis loc]
  (let [call-locs (find-call-locs loc)
        call-analysis (into {} (map (fn [loc] [(zip/get-id loc) {:call? true}]) call-locs))]
    (helpers/deep-merge analysis call-analysis)))