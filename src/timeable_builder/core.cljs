(ns timeable-builder.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]))

;; -------------------------
;; Const

;; -------------------------
;; Utils

;; -------------------------
;; Views

(defn headers [days]
  [:div
   {:style {:display         :flex
            :flex-direction  :row
            :align-items     :end
            :height          "100%"
            :justify-content :stretch}}
   (for [idx (range (count days))]
     (let [day (get days idx)]
       ^{:key day}
       [:div.day  
        day]))])

(defn format-24int->12hr [the-time]
  (str (int (if (< the-time 12) the-time (- the-time 12)))
       ":"
       (int (* (mod the-time 1) 60))
       " "
       (if (< the-time 12) "AM" "PM")))

(defn time-label [{:keys [from to increment cell-height]}]
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
        max-time      24
        
        header-height "3em"
        cell-height   "4em"
        ]
    [:div.timetable
     {:style {:display        :flex
              :flex-direction :row}}
     ;; Left
     [:div
      {:style {:background-color "rgba(255,255,255,0.2)"
              ;;  :padding-left "1em"
              ;;  :padding-right "1em"
               }}
      ;; Top
      [:div {:style {:height           header-height
                     :background-color "rgba(255,255,255,0.2)"}}]
      ;; Bottom
      [time-label 
       {:from        min-time
        :to          max-time
        :increment   increment
        :cell-height cell-height}]
      ]
     ;; Right
     [:div
      {:style {:flex-grow 1}}
      ;; Top
      [:div {:style {:height           header-height
                     :background-color "rgba(255,255,255,0.2)"}}
       [headers display-days]]
      ;; Bottom
      ]]))




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

