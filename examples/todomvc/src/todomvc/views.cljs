(ns todomvc.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-frame.reactive :as reactive]
            [clojure.string :as str]
            ["solid-js" :refer [Show For]]))

(defn todo-input
  [{:keys [title onSave onStop] :as props}]
  (js/console.log ::todo-input props)
  (let [val  (reactive/atom title)
        stop #(do (reset! val "")
                  (when onStop (onStop %)))
        save #(let [v (-> @val str str/trim)]
                (js/console.log ::save v)
                (onSave v)
                (stop %))]
    [:input (merge (dissoc props :title :onSave :onStop)
                   {:type      "text"
                    #_#_:value     #(val)
                    :autoFocus true
                    #_#_:onBlur    save
                    #_#_:onChange  #(reset! val (-> % .-target .-value))
                    #_#_:onKeyDown #(case (.-which %)
                                  13 (save %)
                                  27 (stop %)
                                  nil)})]))

(defn todo-item
  [{:keys [id done title] :as item}]
  (js/console.log ::todo-item item :type (type item))
  (let [editing (reactive/atom false)]
    [:li {:class #(str (when done "completed ")
                       (when @editing "editing"))}
     [:div.view
      [:input.toggle
       {:type     "checkbox"
        :checked  done
        :onchange (fn [_e] (dispatch [:toggle-done id]))}]
      [:label
       {:ondblclick (fn [_e] (reset! editing true))}
       title]
      [:button.destroy
       {:onclick (fn [_e] (dispatch [:delete-todo id]))}]]
     [:> Show {:when #(editing)}
      [todo-input
       {:class  "edit"
        :title  title
        :onSave #(if (seq %)
                   (dispatch [:save id %])
                   (dispatch [:delete-todo id]))
        :onStop (fn [_e] (reset! editing false))}]]]))

(defn task-list
  []
  (let [visible-todos (subscribe [:visible-todos])
        all-complete? (subscribe [:all-complete?])]
    [:section#main
     [:input#toggle-all
      {:type "checkbox"
       :checked @all-complete?
       :onChange #(dispatch [:complete-all-toggle])}]
     [:label {:for "toggle-all"} "Mark all as complete"]
     [:ul#todo-list
      [:> For {:each (fn [e] (js/console.log ::task-list e) (into-array @visible-todos))}
       [todo-item]]]]))

(defn footer-controls
  []
  (let [counts     (subscribe [:footer-counts])
        active     #(first @counts)
        plural?    #(case (active) 1 "item" "items")
        some-done? #(pos? (last @counts))
        showing    (subscribe [:showing])
        a-fn       (fn [filter-kw txt]
                     [:a {:class #(when (= filter-kw @showing) "selected")
                          :href  (str "#/" (name filter-kw))} txt])]
    [:footer#footer
     [:span#todo-count
      [:strong active] " " plural? " left"]
     [:ul#filters
      [:li (a-fn :all    "All")]
      [:li (a-fn :active "Active")]
      [:li (a-fn :done   "Completed")]]
     [:> Show {:when some-done?}
      [:button#clear-completed
       {:onclick (fn [_e] (dispatch [:clear-completed]))}
       "Clear completed"]]]))

(defn task-entry
  []
  [:header#header
   [:h1 "todos"]
   [todo-input
    {:id          "new-todo"
     :placeholder "What needs to be done?"
     :onSave      #(when (seq %)
                     (dispatch [:add-todo %]))}]])

(defn todo-app
  []
  (let [todos (subscribe [:todos])]
    [:<>
     [:section#todoapp
      [task-entry]
      [:> Show {:when #(seq @todos)}
       [task-list]]
      [footer-controls]]
     [:footer#info
      [:p "Double-click to edit a todo"]]]))

(comment

  (re-frame.views/hyperscript (todo-app))

  ;;
  )
