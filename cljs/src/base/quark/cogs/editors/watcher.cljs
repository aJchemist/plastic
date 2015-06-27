(ns quark.cogs.editors.watcher
  (:require [quark.frame.core :refer [subscribe]]
            [clojure.data :as data])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(defonce ^:dynamic prev nil)

(def editors-substription (subscribe [:editors]))

(react!
  (when-let [editors @editors-substription]
    (log "editors changed:" (data/diff prev editors))
    (set! prev editors)))