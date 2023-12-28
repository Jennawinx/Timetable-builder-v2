(ns timeable-builder.timetable
  (:require
   [clojure.math :as math]
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
  (str (int (if (< the-time 12)
              the-time
              (if (< (- the-time 12) 1)
                12
                (- the-time 12))))
       ":" (let [v (int (* (mod the-time 1) 60))]
             (if (= v 0) "00" (str v)))
       #_#_" " (if (< the-time 12) "AM" "PM")))

(defn hide-default-drag-preview! [e]
  (let [img (.createElement js/document "img")]
    (-> e .-dataTransfer (.setDragImage img 0 0))))

(defn block-style-height [duration increment cell-height]
  (-> duration
      (/ increment)
      (* cell-height)))

(defn pixels [n]
  (str n "px"))

(defn calc-duration [to-time from-time increment]
  (+ (- to-time from-time) increment))

;; -------------------------
;; Events 

(defn select-timeblock! [state day the-time]
  (swap! state assoc :selected [day the-time]))

(defn clear-selected-timeblock! [state]
  (swap! state assoc :selected nil))

(defn make-timeblock! [state increment]
  (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
        [to-day   to-time]   (get-in @state [:drag-and-drop :to])]
    (if (and (= from-day to-day) (< to-time from-time))
      (swap! state assoc-in [:time-blocks to-day to-time]     {:duration (calc-duration from-time to-time increment)})
      (swap! state assoc-in [:time-blocks from-day from-time] {:duration (calc-duration to-time from-time increment)}))))

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

(defn drag-drop-cell-listeners [{:keys [state day the-time cell-height increment hide-preview? custom-handlers]}]
  (let [{:keys [on-drag on-drag-end on-drag-enter on-drag-leave
                on-drag-over on-drag-start on-drop]} custom-handlers]
    (let [mouse-events
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

           #_#_ ;; not supported for touch
           :on-drag-leave (fn [e]
                            (prn "on-dragleave" day the-time)
                            (when (fn? on-drag-leave) (on-drag-leave e)))

           #_#_ ;; too much noise
           :on-drag-over  (fn [e]
                            #_(prn "on-dragover" day the-time)
                            (when (fn? on-drag-over) (on-drag-over e)))

           :on-drag-start (fn [e]
                            (prn "on-dragstart" day the-time)
                            (when hide-preview?
                              (try 
                                (hide-default-drag-preview! e)
                                (catch :default e
                                  #_(js/console.error e))))
                            (swap! state assoc :drag-and-drop
                                   {:from    [day the-time]
                                    :to      [day the-time]
                                    :element e})
                            (when (fn? on-drag-start) (on-drag-start e)))

           :on-drop       (fn [e]
                            (prn "on-drop" day the-time)
                            (when (fn? on-drop) (on-drop e)))}
          
          touch-events
          {:on-touch-start (fn [e]
                             (prn "on-touchstart" day the-time)
                             (js/console.log e)
                             ((:on-drag-start mouse-events) e))
           
           :on-touch-move  (fn [e]
                             #_#_ ;; Too much noise
                             (prn "on-touchmove" day the-time)
                             ((:on-drag-over mouse-events) e)
                             
                             (let [cell-top  (-> e (.-target) (.getBoundingClientRect) (.-y))
                                   touch-y   (-> e (.-touches) (first) (.-clientY))
                                   distance  (- touch-y cell-top)
                                   cells     (if (pos? distance)
                                               (math/ceil (/ distance cell-height))
                                               (math/floor (/ distance cell-height)))
                                   new-time  (+ the-time (* cells increment))
                                   to-time   (second (get-in @state [:drag-and-drop :to]))]
                               (js/console.log cell-top touch-y)
                               
                               (when (and to-time (not= new-time to-time))
                                 ;; Approximation to drag enter
                                 (prn "on-touchenter" day the-time)
                                 (js/console.log (-> e (.-target) (.getBoundingClientRect))
                                                 (-> e (.-touches) (first)))
                                 (swap! state assoc-in [:drag-and-drop :to] [day new-time]))))
           
           :on-touch-end   (fn [e]
                             (prn "on-touchend" day the-time)
                             ((:on-drag-end mouse-events) e))}]
      (merge mouse-events 
             touch-events))))



(defn time-block-control-panel
  [state {:keys [increment] :as table-config} day the-time duration]
  [:div.time-block-control-panel
   #_[:div.header-movable-target
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
  [state {:keys [cell-height increment render-cell-fn]
          :or   {render-cell-fn (fn [_] [:div])}
          :as table-config}
   day
   the-time
   duration]
  (let [selection                    (get @state :selected)
        [selected-day selected-time] selection
        selected?                    (and (= selected-day day) (= selected-time the-time))
        cell-info                    (get-in @state [:time-blocks day the-time])]
    [:div.time-block
     (merge
      (drag-drop-cell-listeners
       {:state           state
        :day             day
        :the-time        the-time
        :cell-height     cell-height
        :increment       increment
        :hide-preview?   false
        :custom-handlers {:on-drag-end #(move-time-block! state)}})
      {:class           (when selected? "selected")
       :draggable       selected?
       :on-click        #(select-timeblock! state day the-time)
       :style           {:background-color (:cell-color cell-info)
                         :height           (pixels (block-style-height duration increment cell-height))}})

     (when selected?
       [time-block-control-panel state table-config day the-time duration])
     [:div.time-block-body
      [render-cell-fn cell-info]]]))

(defn time-select-preview
  [state {:keys [cell-height increment] :as table-config}]
  (if-let [{:keys [from to element]} (get-in @state [:drag-and-drop :from])]
    (when (nil? (get-in @state [:selected]))
      (let [{:keys [from to element]} (get @state :drag-and-drop)
            [from-day from-time] from
            [to-day   to-time]   to
            reverse?  (< to-time from-time)
            duration  (if reverse?
                        (calc-duration from-time to-time increment)
                        (calc-duration to-time from-time increment))
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
  [state {:keys [cell-height increment] :as table-config} day the-time]
  (let [duration    (get-in @state [:time-blocks day the-time :duration])]
    [:div.table-cell
     (merge
      (when (nil? duration)
        (drag-drop-cell-listeners
         {:state           state
          :day             day
          :the-time        the-time
          :cell-height     cell-height
          :increment       increment
          :hide-preview?   true
          :custom-handlers {:on-drag-start #(when-not (some? duration)
                                              (clear-selected-timeblock! state))
                            :on-drag-end   (fn [e]
                                             (make-timeblock! state increment)
                                             (select-timeblock! state day the-time))}}))
      {:style         {:height cell-height}
       :on-click      #(when-not (some? duration)
                         (clear-selected-timeblock! state))})

    ;; DEBUG: debug cells here     
     #_[:span.note
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

(defn timetable [{:keys [state table-config]}]
  (let [{:keys [days increment min-time max-time header-height cell-height]
         :as   table-config} (merge table-default-config   ;; default config
                                    table-config           ;; init config
                                    (:table-config @state) ;; dynamic config
       )]
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
     [time-select-preview state table-config]]))
