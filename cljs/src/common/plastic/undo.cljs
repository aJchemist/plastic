(ns plastic.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]))

(defn remove-undo-redo-for-editor [db editor-id]
  (update db :undo-redo dissoc editor-id))

(defn debug-print-queue [q]
  (apply str (interpose "," (map #(hash (last %)) q))))

(defn debug-print-undo-redo [db editor-id]
  (let [undos (get-in db [:undo-redo editor-id :undos])
        redos (get-in db [:undo-redo editor-id :redos])]
    (str
      (count undos) " undos=[" (debug-print-queue undos) "] "
      (count redos) " redos=[" (debug-print-queue redos) "]")))

(defn- limit-queue [q]
  (if plastic.env.limit-undo-redo-queue
    (vec (take-last plastic.env.limit-undo-redo-queue q))
    q))

; -------------------------------------------------------------------------------------------------------------------

(defn peek-queue [db editor-id key]
  (peek (get-in db [:undo-redo editor-id key])))

(defn push-queue [db editor-id key description data]
  (let [updater (fn [items] (limit-queue (conj (or items []) [description data])))
        res (update-in db [:undo-redo editor-id key] updater)]
    (if plastic.env.log-undo-redo
      (fancy-log "UNDO-REDO"
        "push" editor-id key description (hash data)
        "=>" (debug-print-undo-redo res editor-id)))
    res))

(defn pop-queue [db editor-id key]
  (when-let [undo-record (peek-queue db editor-id key)]
    (let [res (update-in db [:undo-redo editor-id key] pop)]
      (if plastic.env.log-undo-redo
        (fancy-log "UNDO-REDO"
          "pop" editor-id key (hash (last undo-record)) undo-record
          "=>" (debug-print-undo-redo res editor-id)))
      res)))

; -------------------------------------------------------------------------------------------------------------------

(declare push-redo)

(defn peek-undo [db editor-id]
  (peek-queue db editor-id :undos))

(defn can-undo? [db editor-id]
  (not (nil? (peek-undo db editor-id))))

(defn push-undo [db [editor-id description data]]
  (push-queue db editor-id :undos description data))

(defn pop-undo [db editor-id]
  (pop-queue db editor-id :undos))

(defn do-undo [db [editor-id]]
  (if-let [undo (peek-undo db editor-id)]
    (let [[description editor] undo
          current-editor (get-in db [:editors editor-id])
          new-db (-> db
                   (pop-undo editor-id)
                   (push-redo [editor-id description current-editor])
                   (assoc-in [:editors editor-id] editor))]
      new-db)
    db))

; -------------------------------------------------------------------------------------------------------------------

(defn peek-redo [db editor-id]
  (peek-queue db editor-id :redos))

(defn can-redo? [db editor-id]
  (not (nil? (peek-redo db editor-id))))

(defn push-redo [db [editor-id description data]]
  (push-queue db editor-id :redos description data))

(defn pop-redo [db editor-id]
  (pop-queue db editor-id :redos))

(defn do-redo [db [editor-id]]
  (if-let [redo (peek-redo db editor-id)]
    (let [[description editor] redo
          current-editor (get-in db [:editors editor-id])
          new-db (-> db
                   (pop-redo editor-id)
                   (push-undo [editor-id description current-editor])
                   (assoc-in [:editors editor-id] editor))]
      new-db)
    db))
