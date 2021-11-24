(ns timeable-builder.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [timeable-builder.timetable :as timetable]))

(defn home-page []
  (r/with-let [state (r/atom {:drag-and-drop {}
                              :time-blocks   {}})]
    [:div.timetable-builder
     [timetable/timetable
      {:state            state
       :table-config
       {:render-cell-fn (fn [cell-info]
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
                           [:p "hi"]])}}]
     [:pre
      {:style {:min-height :10em}}
      [:code (with-out-str (cljs.pprint/pprint @state))]]]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

