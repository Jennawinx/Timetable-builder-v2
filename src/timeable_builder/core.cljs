(ns timeable-builder.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]))

;; -------------------------
;; Const

(def table-default-config 
  {:days          [:sun :mon :tue :wed :thu :fri :sat]
   :editing?      false
   :increment     0.5
   :min-time      7
   :max-time      18
   :header-height 60
   :cell-height   60})

;; -------------------------
;; Utils

(defn format-24int->12hr [the-time]
  (str (int (if (< the-time 12) the-time (- the-time 12)))
       ":" (let [v (int (* (mod the-time 1) 60))]
             (if (= v 0) "00" (str v)))
       " " (if (< the-time 12) "AM" "PM")))

(defn hide-default-drag-preview! [e]
  (let [img (.createElement js/document "img")]
    (-> e .-dataTransfer (.setDragImage img 0 0))))

(defn block-style-height [duration increment cell-height]
  (-> duration
      (/ increment)
      (inc)
      (* cell-height)))

(defn pixels [n]
  (str n "px"))


;; -------------------------
;; Events 

(defn select-timeblock! [state day the-time]
  (swap! state assoc :selected [day the-time]))

(defn clear-selected-timeblock! [state]
  (swap! state assoc :selected nil))

(defn make-timeblock! [state]
  (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
        [to-day   to-time]   (get-in @state [:drag-and-drop :to])]
    (if (and (= from-day to-day) (< to-time from-time))
      (swap! state assoc-in [:time-blocks to-day to-time]     {:duration (- from-time to-time)})
      (swap! state assoc-in [:time-blocks from-day from-time] {:duration (- to-time from-time)}))))

(defn move-time-block! [state]
  (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
        [to-day   to-time]   (get-in @state [:drag-and-drop :to])
        time-block           (get-in @state [:time-blocks from-day from-time])]
    (swap! state update-in [:time-blocks from-day] dissoc from-time)
    (swap! state assoc-in  [:time-blocks to-day to-time] time-block)
    (swap! state assoc     :selected [to-day to-time])))

(defn shrink-time-block! [state day the-time duration amount]
  (let [new-value (- duration amount)]
    (when (pos? new-value)
      (swap! state assoc-in  [:time-blocks day the-time :duration] new-value))))

(defn grow-time-block! [state day the-time duration amount]
  (swap! state assoc-in  [:time-blocks day the-time :duration] (+ duration amount)))

(defn delete-time-block! [state day the-time]
  (swap! state update-in [:time-blocks day] dissoc the-time))

;; -------------------------
;; Views

(defn drag-drop-cell-listeners [{:keys [state day the-time hide-preview? custom-handlers]}]
  (let [{:keys [on-drag on-drag-end on-drag-enter on-drag-leave
                on-drag-over on-drag-start on-drop]} custom-handlers]
    {:draggable     true
     :on-drag       (fn [e]
                      #_(prn "on-drag" day the-time)
                      (when (fn? on-drag) (on-drag e))
                      #_(.preventDefault e))

     :on-drag-end   (fn [e]
                      (prn "on-dragend" day the-time)
                      (when (fn? on-drag-end) (on-drag-end e))
                      (swap! state dissoc :drag-and-drop))

     :on-drag-enter (fn [e]
                      (prn "on-dragenter" day the-time)
                      (swap! state assoc-in [:drag-and-drop :to] [day the-time])
                      (when (fn? on-drag-enter) (on-drag-enter e)))

     :on-drag-leave (fn [e]
                      (prn "on-dragleave" day the-time)
                      (when (fn? on-drag-leave) (on-drag-leave e)))

     :on-drag-over  (fn [e]
                      #_(prn "on-dragover" day the-time)
                      (when (fn? on-drag-over) (on-drag-over e)))

     :on-drag-start (fn [e]
                      (prn "on-dragstart" day the-time)
                      (when hide-preview?
                        (hide-default-drag-preview! e))
                      (swap! state assoc :drag-and-drop
                             {:from    [day the-time]
                              :to      [day the-time]
                              :element e})
                      (when (fn? on-drag-start) (on-drag-start e)))

     :on-drop       (fn [e]
                      (prn "on-drop" day the-time)
                      (when (fn? on-drop) (on-drop e)))}))

(defn time-block-control-panel
  [state {:keys [increment] :as table-config} day the-time duration]
  [:div.time-block-control-panel
   [:div.header-movable-target
    (drag-drop-cell-listeners
     {:state           state 
      :day             day 
      :the-time        the-time 
      :hide-preview?   false
      :custom-handlers {:on-drag-end #(move-time-block! state)}})
    #_"Title"]
   [:div.header-tools
    [:div.flat-button.delete-btn {:on-click #(delete-time-block! state day the-time)}
     "✕"]
    [:div
     [:div.flat-button.end-time-btn {:on-click #(shrink-time-block! state day the-time duration increment)}
      " ↥ "]
     [:div.flat-button.end-time-btn {:on-click #(grow-time-block! state day the-time duration increment)}
      " ↧ "]]]])

(defn time-block
  [state {:keys [cell-height increment] :as table-config} day the-time duration]
  (let [[selected-day selected-time] (get @state :selected)
        selected? (and (= selected-day day) (= selected-time the-time))]
    [:div.time-block
     {:class           (when selected? "selected")
      :draggable       false
      :on-click        #(select-timeblock! state day the-time)
      :style           {:height (pixels (block-style-height duration increment cell-height))}}
     (when selected?
       [time-block-control-panel state table-config day the-time duration])
     [:div.time-block-body
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]
      [:p "hi"]]]))

(defn time-select-preview 
  [state {:keys [cell-height increment] :as table-config}]
  (if-let [{:keys [from to element]} (get-in @state [:drag-and-drop :from])]
    (when (nil? (get-in @state [:selected]))
     (let [{:keys [from to element]} (get @state :drag-and-drop)
          [from-day from-time] from
          [to-day   to-time]   to
          reverse?  (< to-time from-time)
          duration  (Math/abs (- to-time from-time))
          rect      (-> element .-target .getBoundingClientRect)
          height    (block-style-height duration increment cell-height)
          width     (.-width rect)
          x         (+ (.-left rect) (.-scrollX js/window))
          y         (+ (.-top rect)  (.-scrollY js/window))]
      [:div.time-select-preview
       {:style {:left   x
                :top    (if reverse? (+ (- y height) cell-height) y)
                :width  width
                :height (pixels height)}}]))
    [:div]))

(defn table-cell
  [state {:keys [cell-height] :as table-config} day the-time]
  (let [duration    (get-in @state [:time-blocks day the-time :duration])]
    [:div.table-cell
     (merge
      (when (nil? duration)
        (drag-drop-cell-listeners
         {:state           state
          :day             day
          :the-time        the-time
          :hide-preview?   true
          :custom-handlers {:on-drag-start #(when-not (some? duration)
                                              (clear-selected-timeblock! state))
                            :on-drag-end   (fn [e]
                                             (make-timeblock! state)
                                             (select-timeblock! state day the-time))}}))
      {:style         {:height cell-height}
       :on-click      #(when-not (some? duration)
                         (clear-selected-timeblock! state))})
     
     [:span.note 
      (str " day " day " time " the-time)]
     (when (some? duration)
       [time-block state table-config day the-time duration])]))

(defn day-column [state {:keys [min-time max-time increment] 
                   :as   table-config} day]
  [:div.day-column
   (for [the-time (range min-time max-time increment)]
     ^{:key the-time}
     [table-cell state table-config day the-time])])

(defn table-body [state {:keys [days] :as table-config}]
  [:div.table-body
   (for [day days]
     ^{:key day}
     [day-column state table-config day])])

(defn table-days [days]
  [:div.days-header
   (for [day days]
     ^{:key day}
     [:div.day day])])

(defn table-time [{:keys [min-time max-time increment cell-height]}]
  [:div.time-column
   (for [the-time (range min-time max-time increment)]
     ^{:key the-time}
     [:div.time {:style {:height cell-height}}
      (format-24int->12hr the-time)])])

(defn timetable [state table-config]
  (let [{:keys [days increment min-time max-time header-height cell-height]
         :as   table-config} (merge table-default-config table-config)]
    [:div.timetable
     ;; Left
     [:div.table-left-col
      ;; Top
      [:div {:style {:height (pixels header-height)}}]
      ;; Bottom
      [table-time
       {:min-time    min-time
        :max-time    max-time
        :increment   increment
        :cell-height cell-height}]]

     ;; Right
     [:div.table-right-col
      ;; Top
      [:div {:style {:height (pixels header-height)}}
       [table-days days]]
      ;; Bottom
      [table-body state table-config]]
     
     ;; Selection preview
     [time-select-preview state table-config]
     ]))

(defn home-page [table-config]
  (let [state (r/atom {:drag-and-drop {}
                       :time-blocks   {}})]
    (fn [table-config]
      [:div.timetable-builder
       [:h2 "Fun stuff with grids"]
       [timetable state table-config]
       [:pre [:code (with-out-str (cljs.pprint/pprint @state))]]
       [:br]
       [:br]
       [:br]
       [:br]
       [:br]
       [:br]
       [:br]])))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

