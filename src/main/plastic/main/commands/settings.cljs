(ns plastic.main.commands.settings
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.commands.utils :refer [toggle-setting]]))

; -------------------------------------------------------------------------------------------------------------------

(defn toggle-headers [db]
  (toggle-setting db :headers-visible))

(defn toggle-code [db]
  (toggle-setting db :code-visible))

(defn toggle-docs [db]
  (toggle-setting db :docs-visible))

(defn toggle-comments [db]
  (toggle-setting db :comments-visible))

(defn toggle-selections-debug [db]
  (toggle-setting db :selections-debug-visible))
