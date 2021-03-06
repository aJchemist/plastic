(ns plastic.util.subs
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [reagent.ratom :refer [cursor make-reaction] :refer-macros [reaction]]
            [plastic.reagent.sonar :refer [make-sonar-reaction get-or-create-sonar!]]))

; -------------------------------------------------------------------------------------------------------------------
; note sonar reactions are much faster than regular reactions

(defn path-query-factory [context path-or-fn]
  (fn [db [_query-id & args]]
    (let [sonar (get-or-create-sonar! (:sonar-pool context) db)
          path (vec (if (fn? path-or-fn)
                      (apply path-or-fn args)
                      path-or-fn))]
      (make-sonar-reaction sonar path))))