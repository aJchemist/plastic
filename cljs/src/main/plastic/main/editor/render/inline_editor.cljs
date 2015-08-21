(ns plastic.main.editor.render.inline-editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.main :refer [react! dispatch]])
  (:require [reagent.core :as reagent]
            [plastic.onion.api :refer [$]]
            [plastic.util.dom :as dom]
            [plastic.onion.atom.inline-editor :as inline-editor]))

; inline-editor-component is only an empty shell for existing atom editor instance to be attached there
; shared atom editor instance is managed by onion (created outside clojurescript land, one instance per editor)
; hopefully transplanting one single atom editor instance around is faster than always creating new ones

(def log-label "INLINE EDITOR")

(defn activate-inline-editor [$dom-node]
  (if plastic.env.log-inline-editor (fancy-log log-label "activating inline editor"))
  (when-not (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)
          inline-editor-mode (keyword (.data $dom-node "qmode"))
          inline-editor-text (str (.data $dom-node "qtext"))
          root-view (dom/find-closest-plastic-editor-view $dom-node)]
      (if plastic.env.log-inline-editor
        (fancy-log log-label "setup, append, focus and add \"inline-editor-active\" class to the root-view"))
      (inline-editor/setup-inline-editor-for-editing editor-id inline-editor-text inline-editor-mode)
      (inline-editor/append-inline-editor editor-id $dom-node)
      (inline-editor/focus-inline-editor editor-id)
      (.addClass ($ root-view) "inline-editor-active"))))

(defn deactivate-inline-editor [$dom-node]
  (if plastic.env.log-inline-editor (fancy-log log-label "deactivating inline editor"))
  (if (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)
          root-view (dom/find-closest-plastic-editor-view $dom-node)]
      (if plastic.env.log-inline-editor
        (fancy-log log-label "removing \"inline-editor-active\" class from the root-view"))
      (.removeClass ($ root-view) "inline-editor-active")
      (when (inline-editor/is-inline-editor-focused? editor-id)
        (if plastic.env.log-inline-editor
          (fancy-log log-label "returning focus back to root-view"))
        (.focus root-view))
      (inline-editor/deactivate-inline-editor editor-id))))

(defn inline-editor-transplantation [action react-component]
  (let [$dom-node ($ (dom/node-from-react react-component))]
    (if (dom/skelet-node? $dom-node)
      (condp = action
        :activate (activate-inline-editor $dom-node)
        :deactivate (deactivate-inline-editor $dom-node)))))

(defn inline-editor-scaffold [render-fn]
  (reagent/create-class
    {:component-did-mount    (partial inline-editor-transplantation :activate)
     :component-did-update   (partial inline-editor-transplantation :activate)
     :component-will-unmount (partial inline-editor-transplantation :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component []
  (inline-editor-scaffold
    (fn [node-id text mode]
      [:div.inline-editor
       {:data-qnid  node-id
        :data-qtext text
        :data-qmode mode}])))
