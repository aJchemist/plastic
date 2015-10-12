(ns plastic.worker.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.env]
            [plastic.worker.paths]
            [plastic.worker.subs]
            [plastic.worker.servant]
            [plastic.worker.editor]
            [plastic.worker.db]
            [plastic.worker.undo]
            [plastic.worker.frame :refer [register-handler]]
            [plastic.worker.editor.watcher :as watcher]))

(defn init [db [_state]]
  (watcher/init)
  db)

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)