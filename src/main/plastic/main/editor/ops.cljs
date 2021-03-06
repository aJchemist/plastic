(ns plastic.main.editor.ops
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer-macros [dispatch worker-dispatch]]
            [plastic.main.editor.ops.editing :as editing]
            [plastic.main.editor.ops.editing.helpers :refer [editing-string?]]
            [plastic.main.editor.ops.cursor :as cursor]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn spatial-up [editor]
  (-> editor editing/stop-editing cursor/spatial-up))

(defn spatial-down [editor]
  (-> editor editing/stop-editing cursor/spatial-down))

(defn spatial-left [editor]
  (-> editor editing/stop-editing cursor/spatial-left))

(defn spatial-right [editor]
  (-> editor editing/stop-editing cursor/spatial-right))

(defn structural-left [editor]
  (-> editor editing/stop-editing cursor/structural-left))

(defn structural-right [editor]
  (-> editor editing/stop-editing cursor/structural-right))

(defn structural-up [editor]
  (-> editor editing/stop-editing cursor/structural-up))

(defn can-edit? [editor]
  (let [new-editor (cursor/structural-down editor)]
    (identical? new-editor editor)))                                                                                  ; we are at bottom "token level"

(defn structural-down [editor]
  (let [new-editor (cursor/structural-down editor)]
    (if (identical? new-editor editor)
      (dispatch (editor/get-context editor) [:editor-op (editor/get-id editor) :toggle-editing]))
    new-editor))

(defn start-editing [editor]
  (if (and (not (editor/editing? editor)) (can-edit? editor))
    (editing/start-editing editor)))

(defn stop-editing [editor]
  (if (editor/editing? editor)
    (editing/stop-editing editor)))

(defn toggle-editing [editor]
  (if (editor/editing? editor)
    (editing/stop-editing editor)
    (editing/start-editing editor)))

(defn enter [editor]
  {:pre [(not (editing-string? editor))]}
  (editing/perform-enter editor))

(defn alt-enter [editor]
  {:pre [(not (editing-string? editor))]}
  (editing/perform-alt-enter editor))

(defn space [editor]
  {:pre [(not (editing-string? editor))]}
  (editing/perform-space editor))

(defn backspace [editor]
  (if (editor/editing? editor)
    (editing/perform-backspace-in-empty-cell editor)
    (editing/perform-backspace editor)))

(defn undo [editor]
  (dispatch (editor/get-context editor) [:undo (editor/get-id editor)]))

(defn redo [editor]
  (dispatch (editor/get-context editor) [:redo (editor/get-id editor)]))

; -------------------------------------------------------------------------------------------------------------------

(def ops
  {:nop                identity
   :undo               undo
   :redo               redo
   :spatial-up         spatial-up
   :spatial-down       spatial-down
   :spatial-left       spatial-left
   :spatial-right      spatial-right
   :structural-left    structural-left
   :structural-right   structural-right
   :structural-up      structural-up
   :structural-down    structural-down
   :start-editing      start-editing
   :stop-editing       stop-editing
   :toggle-editing     toggle-editing
   :enter              enter
   :alt-enter          alt-enter
   :space              space
   :backspace          backspace
   :delete             editing/delete-after-cursor
   :alt-delete         editing/delete-before-cursor
   :activate-puppets   editing/activate-puppets
   :deactivate-puppets editing/deactivate-puppets
   :toggle-puppets     editing/toggle-puppets
   :next-interest      editing/next-interest
   :prev-interest      editing/prev-interest
   :open-list          editing/open-list
   :open-vector        editing/open-vector
   :open-map           editing/open-map
   :open-set           editing/open-set
   :open-fn            editing/open-fn
   :open-meta          editing/open-meta
   :open-quote         editing/open-quote
   :open-deref         editing/open-deref})

(defn dispatch-op* [context db [editor-id op]]
  (let [editors (get db :editors)
        editor (get editors editor-id)]
    (if-let [handler (op ops)]
      (let [editor (editor/set-context editor context)]
        (if-let [new-editor (handler editor)]
          (if-not (identical? new-editor editor)
            (update db :editors assoc editor-id (editor/strip-context new-editor)))))
      (error (str "Unknown editor operation '" op "'")))))

(defn dispatch-op [context db & args]
  (or (apply dispatch-op* context db args) db))