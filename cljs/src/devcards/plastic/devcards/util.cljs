(ns plastic.devcards.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard defcard* deftest reagent]])
  (:require [devcards.core :refer [card-base register-card]]
            [meld.parser :as parser]
            [meld.zip :as zip]
            [meld.support :refer [zipviz-component meldviz-component histogram-display histogram-component]]))

(defn markdown-source [source & [lang]]
  (str "```" (or lang "clojure") "\n" source "\n```"))

(defn parse-with-stable-ids [source]
  (binding [meld.ids/*last-node-id!* (volatile! 0)]
    (parser/parse! source)))

; -------------------------------------------------------------------------------------------------------------------

(defn def-zip-card [ns name source & [move]]
  (let [top-loc (zip/zip (parse-with-stable-ids source))
        loc (if move (move top-loc) top-loc)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation (markdown-source source)
                             :main-obj      (reagent zipviz-component)
                             :initial-data  (atom {:loc loc})
                             :options       {}})})))

(defn def-meld-card [ns name source]
  (let [meld (parse-with-stable-ids source)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation nil
                             :main-obj      (reagent meldviz-component)
                             :initial-data  (atom {:meld meld})
                             :options       {}})})))


(defn def-hist-card [ns name source & [compounds?]]
  (let [meld (parse-with-stable-ids source)
        histogram (histogram-display meld 100 compounds?)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation nil
                             :main-obj      (reagent histogram-component)
                             :initial-data  (atom {:histogram histogram})
                             :options       {}})})))

(defn def-meld-data-card [ns name source]
  (let [meld (parse-with-stable-ids source)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation (markdown-source source)
                             :main-obj      meld
                             :initial-data  nil
                             :options       {}})})))