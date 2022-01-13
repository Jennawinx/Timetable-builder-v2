(ns timeable-builder.core
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [reagent.dom :as d]
   [timeable-builder.timetable :as timetable]

   [syn-antd.col      :as col]
   [syn-antd.form     :as form]
   [syn-antd.input    :as input]
   [syn-antd.row      :as row]
   [syn-antd.select   :as select]
   [syn-antd.space    :as space]
   [syn-antd.tag      :as tag]
   ))

(defn element-value
  "Gets the value of the targeted element"
  [e]
  (-> e .-target .-value))

(defn toolbar [state]
  (let [selection     (get-in @state [:selected])
        set-property! (fn [field value]
                        (swap! state
                               assoc-in
                               (-> [:time-blocks]
                                   (concat selection)
                                   (concat [field]))
                               value))
        get-property  (fn [field]
                        (get-in @state
                                (-> [:time-blocks]
                                    (concat selection)
                                    (concat [field]))))]
    [row/row {:style {:padding :1em}}
     [col/col {:span 8}
      [row/row
       [col/col {:flex :auto}
        [:div "Title: "]
        [input/input
         {:type      :text
          :value     (get-property :title)
          :on-change #(set-property! :title (element-value %))}]]
       [col/col
        [:div "Colour: "]
        [input/input
         {:type      :color
          :value     (get-property :cell-color)
          :on-change #(set-property! :cell-color (element-value %))}]]]
      [row/row
       [:div "Tags: "]
       [select/select 
        {:mode           :tags
         :style          {:width :100%}
         :show-arrow     true
         :on-select      (fn [obj]
                           (let [s             (.-label obj)
                                 [label color] (string/split s ":")]
                             (set-property! :tags
                                            (concat (get-property :tags)
                                                    [{:label label
                                                      :value s
                                                      :color color}]))))
         :label-in-value true
         :value          (or (get-property :tags) [])
         :tag-render     (fn [props]
                           (js/console.log props)
                           #_(r/as-element
                              [tag/tag "hello"])
                           (let [{:strs [value closable onClose] :as props} (js->clj props)
                                 [label color]                              (string/split value ":")] 
                             (r/as-element
                              [tag/tag
                               {:style         {:background-color (or color :gainsboro)}
                                :class         "tag"
                                :on-mouse-down (fn [e]
                                                 (.preventDefault e)
                                                 (.stopPropagation e))
                                :closable      closable
                                :on-close      onClose}
                               label])))}]]]
     [col/col {:span 16}
      [:div.flex-fill {:style {:flex-grow 1}}
       [:div "Desc: "]
       [input/input-text-area
        {:value     (get-property :desc)
         :on-change #(set-property! :desc (element-value %))
         :rows      4}]]]]))

(defn home-page []
  (r/with-let [state           (r/atom {:timetable     {}
                                        :show-toolbar? true})
               timetable-state (r/cursor state [:timetable])]
    [:div.timetable-builder
     [:div
      {:on-click #(swap! state update :show-toolbar? not)
       :style    {:position         :absolute
                  :background-color "teal"
                  :height           "10px"
                  :width            "10px"
                  :margin           "5px"
                  :border-radius    "5px"}}]
     (when (:show-toolbar? @state)
       [toolbar timetable-state])
     [timetable/timetable
      {:state            timetable-state
       :table-config
       {:render-cell-fn (fn [{:keys [title tags] :as cell-info}]
                          (prn "cell-info " cell-info)
                          [:div
                           {:style {:padding :1em}}
                           [:p.timeblock_title title]
                           (for [{:keys [label color]} tags]
                             ^{:key label}
                             [tag/tag {:style {:background-color (or color :gainsboro)}
                                       :class "tag"}
                              label])])}}]
     [:pre
      {:style {:min-height :10em}}
      [:code (with-out-str (cljs.pprint/pprint @state))]]]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

