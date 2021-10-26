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

;; -------------------------
;; Views

(defn table-cell [state
                  {:keys [cell-height days increment]
                   :as   table-config}
                  day-idx
                  time-idx
                  the-time]
  (let [day      (get days day-idx)
        duration (get-in @state [:time-blocks day the-time :duration])]
    [:div.table-cell
     {:style {:height    cell-height}
      :draggable     true
      :on-drag       (fn [e]
                       (prn "on-drag" day-idx time-idx)
                       #_(.preventDefault e)
                       )
      :on-drag-end   (fn [e]
                       (prn "on-dragend" day-idx time-idx)
                       (let [[from-day from-time] (get-in @state [:drag-and-drop :from])
                             [to-day   to-time]   (get-in @state [:drag-and-drop :to])]
                         (when (= from-day to-day)
                           (swap! state assoc-in [:time-blocks day (min from-time to-time)] 
                                  {:duration (Math/abs (- to-time from-time))}))))
      :on-drag-enter (fn [e] (prn "on-dragenter" day-idx time-idx))
      :on-drag-leave (fn [e] (prn "on-dragleave" day-idx time-idx)
                       (swap! state assoc-in [:drag-and-drop :to] [day the-time]))
      :on-drag-over  (fn [e] 
                       (prn "on-dragover" day-idx time-idx)
                       #_(.preventDefault e))
      :on-drag-start (fn [e]
                       (prn "on-dragstart" day-idx time-idx)
                       (hide-default-drag-preview! e)
                       (swap! state assoc-in [:drag-and-drop :from] [day the-time]))
      :on-drop       (fn [e] (prn "on-drop" day-idx time-idx))}
     [:div {:style  {:background-color :lightblue
                     :position         :relative
                     :height           (when duration
                                         (-> duration
                                             (/ increment)
                                             (inc)
                                             (* cell-height)
                                             (str "px")))}}]
     #_[:div (str " day-idx: "  day-idx)]
     #_[:div (str " day: "      day)]
     #_[:div (str " time-idx: " time-idx)]
     #_[:div (str " the-time: " the-time)]]))

(defn day-column [state {:keys [min-time max-time increment] 
                   :as   table-config} day-idx]
  [:div.day-column
   (for [the-time (range min-time max-time increment)]
     (let [time-idx (/ (- the-time min-time) increment)]
       ^{:key the-time}
       [table-cell state table-config day-idx time-idx the-time]))])

(defn table-body [state {:keys [days] :as table-config}]
  [:div.table-body
   (for [day-idx (range (count days))]
     ^{:key day-idx}
     [day-column state table-config day-idx])])

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

