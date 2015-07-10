(ns quark.cogs.editor.layout.selections
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn emit-item [id selectable info]
  [id (assoc selectable :geometry info)])

(defn build-selections-render-info [selectables geometry]
  (apply hash-map (mapcat (fn [[id info]] (emit-item id (get selectables id) info)) geometry)))