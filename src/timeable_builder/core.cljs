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
   :header-height "60px"
   :cell-height   "50px"})

;; -------------------------
;; Utils

(defn format-24int->12hr [the-time]
  (str (int (if (< the-time 12) the-time (- the-time 12)))
       ":" (int (* (mod the-time 1) 60))
       " " (if (< the-time 12) "AM" "PM")))

;; -------------------------
;; Views

(defn table-cell [{:keys [cell-height days]
                   :as   table-config} 
                  day-idx  
                  time-idx
                  the-time]
  [:div {:style {:height cell-height}}
   [:div (str " day-idx: "  day-idx)]
   [:div (str " day: "      (get days day-idx))]
   [:div (str " time-idx: " time-idx)]
   [:div (str " the-time: " the-time)]])

(defn day-column [{:keys [min-time max-time increment] 
                   :as   table-config} day-idx]
  [:div.day-column
   (for [the-time (range min-time max-time increment)]
     (let [time-idx (/ (- the-time min-time) increment)]
       ^{:key the-time}
       [table-cell table-config day-idx time-idx the-time]))])

(defn table-body [{:keys [days] :as table-config}]
  [:div.table-body
   (for [day-idx (range (count days))]
     ^{:key day-idx}
     [day-column table-config day-idx])])

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

(defn timetable [table-config]
  (let [{:keys [days increment min-time max-time header-height cell-height]
         :as   table-config} (merge table-default-config table-config)]
    [:div.timetable
     ;; Left
     [:div.table-left-col
      ;; Top
      [:div {:style {:height header-height}}]
      ;; Bottom
      [table-time
       {:min-time    min-time
        :max-time    max-time
        :increment   increment
        :cell-height cell-height}]]

     ;; Right
     [:div.table-right-col
      ;; Top
      [:div {:style {:height header-height}}
       [table-days days]]
      ;; Bottom
      [table-body table-config]]]))

(defn home-page []
  [:div.timetable-builder
   [:h2 "Fun stuff with grids"]
   [timetable]])


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

