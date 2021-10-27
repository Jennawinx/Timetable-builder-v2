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
      (* cell-height)
      (str "px")))

;; -------------------------
;; Views

(defn time-block
  [state {:keys [cell-height increment] :as table-config} day the-time duration]
  (let [[selected-day selected-time] (get @state :selected)
        selected? (and (= selected-day day) (= selected-time the-time))]
    [:div.time-block
     {:class         (when selected? "selected")
      :draggable     true
      :on-click      (fn [e]
                     ;; select this time block
                       (swap! state assoc :selected [day the-time]))
      :on-drag-start (fn [e]
                     ;; select this time block
                       (swap! state assoc :selected [day the-time]))
      :style         {:height (block-style-height duration increment cell-height)}}]))

(defn time-select-preview 
  [state {:keys [cell-height increment] :as table-config}]
  (if-let [{:keys [from to element]} (get-in @state [:drag-and-drop :from])]
    (when (nil? (get-in @state [:selected]))
     (let [{:keys [from to element]} (get @state :drag-and-drop)
          [from-day from-time] from
          [to-day   to-time]   to
          duration  (- to-time from-time)
          rect      (-> element .-target .getBoundingClientRect)
          x         (+ (.-left rect) (.-scrollX js/window))
          y         (+ (.-top rect)  (.-scrollY js/window))
          width     (.-width rect)]
      [:div.time-select-preview
       {:style {:left   x
                :top    y
                :width  width
                :height (block-style-height duration increment cell-height)}}]))
    [:div]))

(defn drag-drop-cell-listeners [state day the-time hide-preview? & [custom-handlers]]
  (let [{:keys [on-drag on-drag-end on-drag-enter on-drag-leave 
                on-drag-over on-drag-start on-drop]} custom-handlers]
    {:draggable     true
   :on-drag
   (fn [e]
     #_(prn "on-drag" day the-time)
     (when (fn? on-drag) (on-drag e))
     #_(.preventDefault e))

   :on-drag-end
   (fn [e]
     (prn "on-dragend" day the-time)
     (when (fn? on-drag-end) (on-drag-end e))
     (swap! state dissoc :drag-and-drop))

   :on-drag-enter
   (fn [e]
     (prn "on-dragenter" day the-time)
     (let [[from-day from-time] (get-in @state [:drag-and-drop :from])]
       (if (and (= from-day day) (> from-time the-time))
         (swap! state update :drag-and-drop merge
                {:from    [day the-time]
                 :element e})
         (swap! state assoc-in [:drag-and-drop :to] [day the-time])))
     (when (fn? on-drag-enter) (on-drag-enter e)))

   :on-drag-leave
   (fn [e] 
     (prn "on-dragleave" day the-time)
     (when (fn? on-drag-leave) (on-drag-leave e)))

   :on-drag-over
   (fn [e]
     #_(prn "on-dragover" day the-time)
     (when (fn? on-drag-over) (on-drag-over e)))

   :on-drag-start
   (fn [e]
     (prn "on-dragstart" day the-time)
     (when hide-preview? 
       (hide-default-drag-preview! e))
     (swap! state assoc :drag-and-drop
            {:from    [day the-time]
             :to      [day the-time]
             :element e})
     (when (fn? on-drag-start) (on-drag-start e)))

   :on-drop
   (fn [e]
     (prn "on-drop" day the-time)
     (when (fn? on-drop) (on-drop e)))}))

(defn table-cell
  [state {:keys [cell-height] :as table-config} day the-time]
  (let [duration    (get-in @state [:time-blocks day the-time :duration])]
    [:div.table-cell
     (merge
      (if (nil? duration)
        (drag-drop-cell-listeners
         state day the-time true
         {:on-drag-start
          (fn [e]
            ;; deselect when clicking empty cell
            (when-not (some? duration)
              (swap! state assoc :selected nil)))
          :on-drag-end
          (fn [e]
            (prn "Make time block!")
            (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
                  [to-day   to-time]   (get-in @state [:drag-and-drop :to])]
              (swap! state assoc-in [:time-blocks from-day from-time]
                     {:duration (- to-time from-time)})))})
        (drag-drop-cell-listeners
         state day the-time false
         {:on-drag-end
          (fn [e]
            (prn "Move time block!")
            (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
                  [to-day   to-time]   (get-in @state [:drag-and-drop :to])
                  time-block           (get-in @state [:time-blocks from-day from-time])]
              (swap! state update-in [:time-blocks from-day] dissoc from-time)
              (swap! state assoc-in  [:time-blocks to-day to-time] time-block)))}))
      
      {:style         {:height cell-height}
       :on-click      (fn [e]
                       ;; deselect when clicking empty cell
                        (when-not (some? duration)
                          (swap! state assoc :selected nil)))})
     [:span {:style {:position :absolute :z-index 2}} 
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
      [:div {:style {:height (str header-height "px")}}]
      ;; Bottom
      [table-time
       {:min-time    min-time
        :max-time    max-time
        :increment   increment
        :cell-height cell-height}]]

     ;; Right
     [:div.table-right-col
      ;; Top
      [:div {:style {:height (str header-height "px")}}
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

