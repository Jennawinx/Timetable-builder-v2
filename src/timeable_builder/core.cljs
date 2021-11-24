(ns timeable-builder.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [timeable-builder.timetable :as timetable]))

(defn element-value
  "Gets the value of the targeted element"
  [e]
  (-> e .-target .-value))

(defn toolbar [state]
  (let [selection     (get-in @state [:selected])
        set-property! (fn [field value]
                        (swap! state
                               assoc-in
                               (-> [:time-blocks]
                                   (concat selection)
                                   (concat [field]))
                               value))]
    (when #_true (some? selection)
          [:div
           {:style {:display         :flex
                    :padding         :1em
                    :justify-content :center
                    :align-items     :self-start}}
           [:div.flex-fill
            [:div "Title: "]
            [:input.full-width
             {:type :text
              :on-change #(set-property! :title (element-value %))}]]
           [:div.flex-fill
            [:div "Colour: "]
            [:input
             {:type :color
              :on-change #(set-property! :cell-color (element-value %))}]]
           [:div.flex-fill
            [:div "Tags: "]
            [:input.full-width
             {:type :text
              :on-change #(set-property! :tags (element-value %))}]]
           [:div.flex-fill {:style {:flex-grow 1}}
            [:div "Desc: "]
            [:input.full-width
             {:type :text
              :on-change #(set-property! :tags (element-value %))}]]])))

(defn home-page []
  (r/with-let [state           (r/atom {:timetable {}})
               timetable-state (r/cursor state [:timetable])]
    [:div.timetable-builder
     [toolbar timetable-state]
     [timetable/timetable
      {:state            timetable-state
       :table-config
       {:render-cell-fn (fn [{:keys [title tags] :as cell-info}]
                          (prn "cell-info " cell-info)
                          [:div
                           {:style {:padding           :1em}}
                           [:b {:style {:font-size :120%}} 
                            title]])}}]
     [:pre
      {:style {:min-height :10em}}
      [:code (with-out-str (cljs.pprint/pprint @state))]]]))


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

