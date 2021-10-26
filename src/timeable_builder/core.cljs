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
  [:div {:style  {:background-color :lightblue
                  :border           "2px solid pink"
                  :position         :relative
                  :height           (block-style-height duration increment cell-height)}}])

(defn table-cell
  [state {:keys [cell-height] :as table-config} day the-time]
  (let [duration (get-in @state [:time-blocks day the-time :duration])]
    [:div.table-cell
     {:style         {:height cell-height}
      :draggable     true
      :on-drag       (fn [e]
                       (prn "on-drag" day the-time)
                       #_(.preventDefault e))
      :on-drag-end
      (fn [e]
        (prn "on-dragend" day the-time)
        (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
              [to-day   to-time]   (get-in @state [:drag-and-drop :to])]
          (swap! state assoc-in [:time-blocks from-day from-time] {:duration (- to-time from-time)})
          (swap! state assoc-in [:drag-and-drop :from] nil)
          (swap! state assoc-in [:drag-and-drop :to]   nil)))

      :on-drag-enter
      (fn [e] (prn "on-dragenter" day the-time)
        (let [[from-day from-time] (get-in @state [:drag-and-drop :from])]
          (if (> from-time the-time)
            (swap! state assoc-in [:drag-and-drop :from] [from-day the-time])
            (swap! state assoc-in [:drag-and-drop :to]   [from-day the-time]))))

      :on-drag-leave
      (fn [e] (prn "on-dragleave" day the-time))

      :on-drag-over
      (fn [e]
        (prn "on-dragover" day the-time)
        #_(.preventDefault e))
      :on-drag-start
      (fn [e]
        (prn "on-dragstart" day the-time)
        (hide-default-drag-preview! e)
        (swap! state assoc-in [:drag-and-drop :from] [day the-time])
        (swap! state assoc-in [:drag-and-drop :to]   [day the-time]))
      :on-drop       (fn [e] (prn "on-drop" day the-time))}
     (when duration
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
      [table-body state table-config]]]))

(defn home-page [table-config]
  (let [state (r/atom {:drag-and-drop {}
                       :time-blocks   {}})]
    (fn [table-config]
      [:div.timetable-builder
       [:h2 "Fun stuff with grids"]
       [timetable state table-config]
       [:pre [:code (with-out-str (cljs.pprint/pprint @state))]]])))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

