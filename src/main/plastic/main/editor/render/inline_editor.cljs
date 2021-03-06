(ns plastic.main.editor.render.inline-editor
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [reagent.core :as reagent]
            [plastic.onion.api :refer [$]]
            [plastic.util.dom :as dom]
            [plastic.onion.inline-editor :as inline-editor]
            [plastic.env :as env :include-macros true]))

; -------------------------------------------------------------------------------------------------------------------
; inline-editor-component is only an empty shell for existing atom editor instance to be attached there.
; Shared atom editor instance is managed by onion (created outside clojurescript land, one instance per editor).
; Hopefully transplanting one single atom editor element around is faster than always creating a new one.

(def log-label "INLINE EDITOR")

(defn activate-transplantation [context $dom-node]
  (if (env/get context :log-inline-editor)
    (fancy-log log-label "activate transplantation to" $dom-node "activeElement:" (.-activeElement js/document)))
  (let [editor-id (dom/lookup-editor-id $dom-node)]
    (inline-editor/append-inline-editor editor-id $dom-node)
    (inline-editor/focus-inline-editor editor-id)
    (if (env/get context :log-inline-editor)
      (fancy-log log-label "activeElement after transplantation:" (.-activeElement js/document)))))

; inline-editor element will be eventually removed from the dom by react
; plastic.onion.atom.inline-editor still holds reference to that element so it can re-append it somewhere else
;
; note: I know what you are probably asking:
;       "wouldn't it be symmetric to detach inline-editor-element and return focus to root-view here?"
;
;       Trying to remove element and append it to parking position proved to be tricky here.
;       In one rendering update, one node can lose editing status and another can gain it,
;       so we receive multiple activate/deactivate calls during this one render update.
;       But react/reagent does not guarantee ordering of those calls - it depends on position of affected
;       nodes in the dom tree. We would have to queue them and do deactivation first and activation later.
;       And by the time we get to do queued deactivation, the node would be probably detached anyways.
;
(defn deactivate-transplantation [context $dom-node]
  (let [editor-id (dom/lookup-editor-id $dom-node)
        $root-view ($ (dom/find-closest-plastic-editor-view $dom-node))]
    (when (inline-editor/is-inline-editor-focused? editor-id)
      (if (env/get context :log-inline-editor)
        (fancy-log log-label "returning focus back to root-view"))
      (.focus $root-view))))

(defn transplant-inline-editor [context action react-component]
  (let [$dom-node ($ (dom/node-from-react react-component))]
    (condp = action
      :activate (activate-transplantation context $dom-node)
      :deactivate (deactivate-transplantation context $dom-node))))

(defn inline-editor-class [context render-fn]
  (reagent/create-class
    {:component-did-mount    (partial transplant-inline-editor context :activate)
     :component-did-update   (partial transplant-inline-editor context :activate)
     :component-will-unmount (partial transplant-inline-editor context :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component [context _node-id]
  (inline-editor-class context
    (fn [_context node-id]
      [:div.inline-editor
       {:data-pnid node-id}])))
