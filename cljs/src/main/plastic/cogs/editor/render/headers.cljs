(ns plastic.cogs.editor.render.headers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [wrap-specials classv]]))

(defn header-component [header-info]
  (let [{:keys [name id]} header-info]
    ^{:key id}
    [:div.header
     (if name [:div.name [:div name]])]))

(defn headers-component [header-info-list]
  [:div.headers-group
   (for [header-info header-info-list]
     (header-component header-info))])

(defn headers-wrapper-component []
  (fn [editor-id form-id node]
    [:div.headers-wrapper
     [headers-component (:children node)]]))