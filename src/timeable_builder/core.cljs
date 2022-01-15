(ns timeable-builder.core
  (:require
   [cljs.reader :as reader]
   [cljs.pprint :as pprint]
   [clojure.string :as string]
   [markdown.core :as md]
   
   [reagent.core :as r]
   [reagent.dom :as d]
   
   [timeable-builder.timetable :as timetable]

   [syn-antd.button   :as button]
   [syn-antd.col      :as col]
   [syn-antd.form     :as form]
   [syn-antd.input    :as input]
   [syn-antd.message  :as message]
   [syn-antd.row      :as row]
   [syn-antd.select   :as select]
   [syn-antd.space    :as space]
   [syn-antd.tabs     :as tabs]
   [syn-antd.tag      :as tag]
   [syn-antd.upload   :as upload]))

(defn element-value
  "Gets the value of the targeted element"
  [e]
  (-> e .-target .-value))

(defn local-save! [time-blocks]
  (.setItem js/localStorage :time-blocks (pr-str time-blocks))
  (message/success-ant-message "Local save complete"))

(defn get-local-save []
  (-> (.getItem js/localStorage :time-blocks)
      (reader/read-string)))

(defn download-as-edn! [value export-name]
  (let [data-blob (js/Blob. #js [(with-out-str (pprint/pprint value))]  #js {:type "text/plain"})
        link      (.createElement js/document "a")]
    (set! (.-href link) (.createObjectURL js/URL data-blob))
    (.setAttribute link "download" export-name)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)))

(defn filename-safe-datetime []
  (-> (.toLocaleString (js/Date.))
      (string/replace #"[\s]" "_")
      (string/replace #"[,]" "")
      (string/replace #"[/:]" "-")))

(defn tag-suggestions 
  "Walks through timeblocks data for used tags
   Returns [{:keys [value]}]"
  [time-blocks]
  (->> time-blocks
       (reduce
        (fn [result [day time-blocks]]
          (reduce
           (fn [result [time time-block]]
             (if (nil? (:tags time-block))
               result
               (reduce (fn [result tag]
                         (conj result {:value (:value tag)}))
                       result
                       (:tags time-block))))
           result
           time-blocks))
        [])
       (distinct)
       (sort-by :value)))

(defn action-buttons [state]
  [:<>
   [:div "Actions: "]
   [row/row
    [col/col {:lg 24 :sm 8}
     [button/button {:block    true
                     :on-click #(local-save! (get-in @state [:timetable :time-blocks]))}
      "Local Save"]]
    [col/col {:lg 24 :sm 8}
     [button/button {:block    true
                     :on-click #(download-as-edn!
                                 (get-in @state [:timetable :time-blocks])
                                 (str "timetable_" (filename-safe-datetime) ".edn"))}
      "Download Data"]]
    [col/col {:lg 24 :sm 8}
     [upload/upload {:name      :file
                     :file-list []
                     :class     "upload-btn"
                     :before-upload
                     (fn [file]
                       (-> (.text file)
                           (.then
                            (fn [s]
                              (let [value (reader/read-string s)]
                                (swap! state assoc-in [:timetable :time-blocks] value)
                                (local-save! value))))))}
      [button/button {:block true}
       "Load Data"]]]]])

(defn tag-selector [time-blocks disabled? get-property set-property!]
  [row/row
   [:div "Tags: "]
   [select/select
    {:mode           :tags
     :style          {:width :100%}
     :show-arrow     true
     :disabled       disabled?
     :on-select      (fn [obj]
                       (when-not (string/blank? (.-label obj))
                         (let [s             (.-label obj)
                               [label color] (string/split s ":")]
                           (set-property! :tags
                                          (concat (get-property :tags)
                                                  [{:label label
                                                    :value s
                                                    :color color}])))))
     :label-in-value true
     :value          (or (get-property :tags) [])
     :options        (tag-suggestions time-blocks)
     :tag-render     (fn [props]
                       (let [{:strs [value closable onClose]} (js->clj props)
                             [label color]                    (string/split value ":")]
                         (r/as-element
                          [tag/tag
                           {:style         {:background-color color}
                            :class         "tag"
                            :on-mouse-down (fn [e]
                                             (.preventDefault e)
                                             (.stopPropagation e))
                            :closable      closable
                            :on-close      (fn [& args]
                                             (apply onClose args)
                                             (set-property! :tags
                                                            (->> (get-property :tags)
                                                                 (filter (comp (partial not= label) :label))
                                                                 (vec))))}
                           label])))}]])

(defn toolbar-edit-timeblock [state]
  (let [selection     (get-in @state [:timetable :selected])
        disabled?     (nil? selection)
        timeblocks    (get-in @state [:timetable :time-blocks])
        set-property! (fn [field value]
                        (swap! state assoc-in (concat [:timetable :time-blocks] selection [field])
                               value))
        get-property  (fn [field]
                        (get-in @state (concat [:timetable :time-blocks] selection [field])))]
    [row/row {:gutter 12 
              :style  {:padding :1em}}
     [col/col {:lg 8 :sm 12}
      [row/row {:wrap false}
       [col/col {:flex :auto}
        [:div "Title: "]
        [input/input
         {:type      :text
          :value     (get-property :title)
          :disabled  disabled?
          :on-change #(set-property! :title (element-value %))}]]
       [col/col {:flex "50px"}
        [:div "Color: "]
        [input/input
         {:type      :color
          :value     (or (get-property :cell-color) "#add8e6")
          :disabled  disabled?
          :on-change #(set-property! :cell-color (element-value %))}]]
       [col/col {:flex "50px"}
        [:div "Font: "]
        [input/input
         {:type      :color
          :value     (or (get-property :font-color) "black")
          :disabled  disabled?
          :on-change #(set-property! :font-color (element-value %))}]]]
      [tag-selector timeblocks disabled? get-property set-property!]]
     [col/col {:lg 12 :sm 12}
      [:div "Desc: "]
       [input/input-text-area
        {:value     (get-property :desc)
         :disabled  disabled?
         :on-change #(set-property! :desc (element-value %))
         :rows      4}]]
     [col/col {:lg 4 :sm 24}
      [action-buttons state]]]))

(defn toolbar-edit-timetable-settings [state]
  [:div])

(defn toolbar [state]
  [tabs/tabs {:tab-position :left :class "toolbar-tab-group"}
   [tabs/tabs-tab-pane {:tab "Edit" :key "Edit"}
    [toolbar-edit-timeblock state]]
   [tabs/tabs-tab-pane {:tab "Time Table" :key "TimeTable"}
    [toolbar-edit-timetable-settings state]]])

(defn custom-cell-renderer 
  [{:keys [title tags desc font-color]}]
  [:div {:style {:padding "0.75em 1em"
                 :color   font-color}}
   [:div.timeblock__tag-group
    (for [{:keys [label color]} tags]
      ^{:key label}
      [tag/tag {:style {:background-color color}
                :class "tag"}
       label])]
   [:div.timeblock__title " " title " "]
   [:div {:style
          {:margin-top "6px"
           :padding    "0 0.25em"}
          :dangerouslySetInnerHTML
          {:__html (md/md->html desc)}}]])

(defn home-page []
  (r/with-let [time-blocks     (get-local-save)
               state           (r/atom {:timetable
                                        {:time-blocks (or time-blocks {})}
                                        :show-toolbar? true})
               timetable-state (r/cursor state [:timetable])]
    [:div.timetable-builder
     [:div.open-editor-btn
      {:on-click #(swap! state update :show-toolbar? not)}]
     (when (:show-toolbar? @state)
       [toolbar state])
     [timetable/timetable
      {:state            timetable-state
       :table-config     {:render-cell-fn  custom-cell-renderer}}]
     #_[:pre
      {:style {:min-height :10em}}
      [:code (with-out-str (pprint/pprint @state))]]]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

