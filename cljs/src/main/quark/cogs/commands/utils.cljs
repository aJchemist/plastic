(ns quark.cogs.commands.utils
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn toggle-setting [settings key]
  (let [sanitized-settings (or settings {})]
    (assoc sanitized-settings key (not (get sanitized-settings key)))))