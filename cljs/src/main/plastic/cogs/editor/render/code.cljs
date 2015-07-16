(ns plastic.cogs.editor.render.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [wrap-specials classv]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame.core :refer [subscribe]]
            [plastic.onion.api :refer [$]]
            [plastic.cogs.editor.layout.utils :as utils]))

(declare code-block-component)

(defn apply-shadowing-subscripts [text shadows]
  (if (>= shadows 2)
    (str text (utils/wrap-in-span shadows "shadowed"))
    text))

(defn code-token-component [editor-id form-id node-id]
  (let [selection-subscription (subscribe [:editor-selection-node editor-id node-id])]
    (fn [node]
      (let [{:keys [decl-scope call selectable? type text shadows decl? def-name? id geometry editing?]} node
            _ (log "R! token" id)
            props (merge
                    {:data-qnid id
                     :class     (classv
                                  (if selectable? "selectable")
                                  (if (and selectable? @selection-subscription) "selected")
                                  (if type (name type))
                                  (if call "call")
                                  (if editing? "editing")
                                  (if decl-scope (str "decl-scope decl-scope-" decl-scope))
                                  (if def-name? "def-name")
                                  (if decl? "decl"))}
                    (if geometry {:style {:transform (str "translateY(" (:top geometry) "px) translateX(" (:left geometry) "px)")}}))
            emit-token (fn [html] [:div.token props
                                   (if editing?
                                     [inline-editor-component id text (or type :symbol)]
                                     [raw-html-component html])])]
        (condp = type
          :string (emit-token (-> text (wrap-specials)))
          (emit-token (-> text (apply-shadowing-subscripts shadows))))))))

(defn break-nodes-into-lines [accum node]
  (let [new-accum (assoc accum (dec (count accum)) (conj (last accum) node))]
    (if (= (:tag node) :newline)
      (conj new-accum [])
      new-accum)))

(defn emit-code-block [editor-id form-id node]
  ^{:key (:id node)} [code-block-component editor-id form-id node])

(defn is-simple? [node]
  (empty? (:children node)))

(defn is-newline? [node]
  (or (nil? node) (= (:tag node) :newline)))

(defn is-double-column-line? [line]
  (and (is-simple? (first line)) (not (is-newline? (second line))) (is-newline? (nth line 2 nil))))

(defn elements-table [emit nodes]
  (let [lines (reduce break-nodes-into-lines [[]] nodes)]
    (if (<= (count lines) 1)
      [:div.elements
       (for [node (first lines)]
         (emit node))]
      [:table.elements
       [:tbody
        (let [first-line-is-double-column? (is-double-column-line? (first lines))]
          (for [[index line] (map-indexed (fn [i l] [i l]) lines)]
            (if (is-double-column-line? line)
              ^{:key index} [:tr
                             [:td
                              (if (and
                                    (not first-line-is-double-column?)
                                    (not= line (first lines)))
                                [:div.indent])
                              (emit (first line))]
                             [:td
                              (for [node (rest line)]
                                (emit node))]]
              ^{:key index} [:tr
                             [:td {:col-span 2}
                              (if (not= line (first lines)) [:div.indent])
                              (for [node line]
                                (emit node))]])))]])))

(defn code-element-component []
  (fn [editor-id form-id node]
    (let [{:keys [tag children]} node]
      (condp = tag
        :newline [:span.newline "↵"]
        :token [(code-token-component editor-id form-id (:id node)) node]
        (elements-table (partial emit-code-block editor-id form-id) children)))))

(defn wrapped-code-block-component [editor-id form-id node opener closer]
  (let [selection-subscription (subscribe [:editor-selection-node editor-id (:id node)])]
    (fn []
      (let [{:keys [id scope selectable? depth tag scope-depth]} node
            tag-name (name tag)]
        (log "R! block" id)
        [:div.block {:data-qnid id
                     :class     (classv
                                  tag-name
                                  (if selectable? "selectable")
                                  (if (and selectable? @selection-subscription) "selected")
                                  (if scope (str "scope scope-" scope " scope-depth-" scope-depth))
                                  (if depth (str "depth-" depth)))}
         [:div.punctuation.opener opener]
         [code-element-component editor-id form-id node]
         [:div.punctuation.closer closer]]))))

(defn code-block-component []
  (fn [editor-id form-id node]
    (let [wrapped-code-block (partial wrapped-code-block-component editor-id form-id node)]
      (condp = (:tag node)
        :list [wrapped-code-block "(" ")"]
        :vector [wrapped-code-block "[" "]"]
        :set [wrapped-code-block "#{" "}"]
        :map [wrapped-code-block "{" "}"]
        :fn [wrapped-code-block "#(" ")"]
        :meta [wrapped-code-block "^" ""]
        [code-element-component editor-id form-id node]))))

(defn extract-first-child-name [node]
  (:text (first (:children node))))

(defn code-box-component []
  (fn [editor-id form-id code-render-info]
    (let [node (first (:children code-render-info))
          name (extract-first-child-name node)]
      [:div.code-box {:class (if name (str "sexpr-" name))}
       [code-block-component editor-id form-id node]])))