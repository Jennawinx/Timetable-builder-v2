(ns timeable-builder.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]))

;; -------------------------
;; Const

;; -------------------------
;; Utils

(defn format-24int->12hr [the-time]
  (str (int (if (< the-time 12) the-time (- the-time 12)))
       ":"
       (int (* (mod the-time 1) 60))
       " "
       (if (< the-time 12) "AM" "PM")))

;; -------------------------
;; Views

(defn table-cells [{:keys [from to increment cell-height days]}]

  [:div
   {:style {:display          :flex
            :flex-direction   :row
            :align-items      :end
            :justify-content  :stretch}}
   (for [day-idx  (range (count days))]
     (let [day      (get days day-idx)]
       ^{:key day}
       [:div {:style {:width "100%"}}
        (for [the-time (range from to increment)]
          (let [time-idx (/ (- the-time from) increment)]
            ^{:key time-idx}
            [:div
             {:style {:height cell-height
                      :border "1px solid rgba(0,0,0,0.2)"
                      :box-sizing :border-box}}
             [:div (str " day-idx: "  day-idx)]
             [:div (str " day: "      day)]
             [:div (str " time-idx: " time-idx)]
             [:div (str " the-time: " the-time)]
             ]))]))])

(defn table-days [days]
  [:div
   {:style {:display         :flex
            :flex-direction  :row
            :align-items     :end
            :height          "100%"
            :justify-content :stretch}}
   (for [day days]
     ^{:key day}
     [:div.day
      day])])

(defn table-time [{:keys [from to increment cell-height]}]
  [:div
   (for [the-time (range from to increment)]
     ^{:key the-time}
     [:div.time
      {:style {:height cell-height}}
      
      (format-24int->12hr the-time)])])

(defn timetable []
  (let [display-days  [:sun :mon :tue :wed :thu :fri :sat]
        editing?      false
        increment     0.5
        min-time      7
        max-time      18
        
        header-height "60px"
        cell-height   "50px"
        ]
    [:div.timetable
     {:style {:display        :flex
              :flex-direction :row}}
     ;; Left
     [:div {:style {:min-width "15ch"}}
      ;; Top
      [:div {:style {:height header-height}}]
      ;; Bottom
      [table-time
       {:from        min-time
        :to          max-time
        :increment   increment
        :cell-height cell-height}]]
     ;; Right
     [:div {:style {:flex-grow 1}}
      ;; Top
      [:div {:style {:height header-height}}
       [table-days display-days]]
      ;; Bottom
      [table-cells 
       {:from        min-time
        :to          max-time
        :increment   increment
        :cell-height cell-height
        :days        display-days}]]]))




(defn home-page []
  [:div
   [:h2 "Fun stuff with grids"]
   [timetable true]])


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

