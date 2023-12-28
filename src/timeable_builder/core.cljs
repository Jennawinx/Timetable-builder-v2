(ns timeable-builder.core
  (:require
   [cljs.reader :as reader]
   [cljs.pprint :as pprint]
   [clojure.string :as string]
   [markdown.core :as md]
   
   [reagent.core :as r]
   [reagent.dom :as d]
   
   [timeable-builder.timetable :as timetable]

   [syn-antd.button       :as button]
   [syn-antd.checkbox     :as checkbox]
   [syn-antd.col          :as col]
   [syn-antd.form         :as form]
   [syn-antd.input        :as input]
   [syn-antd.input-number :as input-number]
   [syn-antd.message      :as message]
   [syn-antd.row          :as row]
   [syn-antd.select       :as select]
   [syn-antd.space        :as space]
   [syn-antd.tabs         :as tabs]
   [syn-antd.tag          :as tag]
   [syn-antd.upload       :as upload]))

;; --- Utils

(defn element-value
  "Gets the value of the targeted element"
  [e]
  (-> e .-target .-value))

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

;; --- Import/Export

(defn local-save! [timetable]
  (.setItem js/localStorage :timetable (pr-str timetable))
  (message/success-ant-message "Local save complete"))

(defn get-local-save []
  (-> (.getItem js/localStorage :timetable)
      (reader/read-string)))

(defn download-timetable [timetable]
  (download-as-edn! timetable (str "timetable_" (filename-safe-datetime) ".edn")))

(defn load-in-data [state s]
  (let [timetable (reader/read-string s)]
    (swap! state assoc :timetable timetable)
    (local-save! timetable)))

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
                     :on-click #(local-save! (:timetable @state))}
      "Local Save"]]
    [col/col {:lg 24 :sm 8}
     [button/button {:block    true
                     :on-click #(download-timetable (:timetable @state))}
      "Download Data"]]
    [col/col {:lg 24 :sm 8}
     [upload/upload {:name      :file
                     :file-list []
                     :class     "upload-btn"
                     :before-upload
                     (fn [file]
                       (-> (.text file)
                           (.then (partial load-in-data state))))}
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
     [col/col {:lg 8 :sm 12 :xs 24}
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
     [col/col {:lg 12 :sm 12 :xs 24}
      [:div "Desc: "]
       [input/input-text-area
        {:value     (get-property :desc)
         :disabled  disabled?
         :on-change #(set-property! :desc (element-value %))
         :rows      4}]]
     [col/col {:lg 4 :sm 24}
      [action-buttons state]]]))

(defn toolbar-edit-timetable-settings [state]
  [:div
   [row/row {:gutter 12
             :style  {:padding :1em}}
    [col/col {:span 20}
     [row/row
      [col/col
       [:div "Display days: "]
       [checkbox/checkbox-group
        {:value     (get-in @state [:timetable :table-config :days])
         :options   [:sun :mon :tue :wed :thu :fri :sat]
         :on-change (fn [days]
                      (swap! state assoc-in [:timetable :table-config :days] (map keyword days)))}]]]
     [row/row {:gutter 12
               :style  {:margin-top :0.5em}}
      [col/col
       [:div "Start time: "]
       [input-number/input-number
        {:value     (get-in @state [:timetable :table-config :min-time])
         :step      (get-in @state [:timetable :table-config :increment])
         :on-change #(swap! state assoc-in [:timetable :table-config :min-time] %)}]]
      [col/col
       [:div "End time: "]
       [input-number/input-number
        {:value     (get-in @state [:timetable :table-config :max-time])
         :step      (get-in @state [:timetable :table-config :increment])
         :on-change #(swap! state assoc-in [:timetable :table-config :max-time] %)}]]
      [col/col
       [:div "Increment: "]
       [input-number/input-number
        {:value     (get-in @state [:timetable :table-config :increment])
         :step      0.25
         :on-change #(swap! state assoc-in [:timetable :table-config :increment] %)}]]
      [col/col
       [:div "Cell Height: "]
       [input-number/input-number
        {:value     (get-in @state [:timetable :table-config :cell-height])
         :step      10
         :on-change (fn [value]
                      (swap! state assoc-in [:timetable :table-config :cell-height] value))}]]]]
    [col/col {:lg 4}
     [action-buttons state]]]])

(defn toolbar [state]
  [:div.toolbar {:class (when (:show-toolbar? @state) "opened")}
   [:div.open-editor-btn
    {:on-click #(swap! state update :show-toolbar? not)}] 
   [tabs/tabs {:tab-position :left :class "toolbar-tab-group"}
    [tabs/tabs-tab-pane {:tab "Edit" :key "Edit"}
     [toolbar-edit-timeblock state]]
    [tabs/tabs-tab-pane {:tab "Time Table" :key "TimeTable"}
     [toolbar-edit-timetable-settings state]]]])

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
  (let [state           (r/atom
                         {:timetable
                          (or (get-local-save)
                              {:time-blocks   {}
                               :table-config  {:days          [:sun :mon :tue :wed :thu :fri :sat]
                                               :increment     0.5
                                               :min-time      7
                                               :max-time      18
                                               :cell-height   60}})
                          :show-toolbar? true})

        timetable-state (r/cursor state [:timetable])]
    (fn []
      [:div.timetable-builder
       [toolbar state]
       [timetable/timetable
        {:state            timetable-state
         :table-config     {:render-cell-fn  custom-cell-renderer}}]
       #_[:pre
          {:style {:min-height :10em}}
          [:code (with-out-str (pprint/pprint @state))]]])))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

