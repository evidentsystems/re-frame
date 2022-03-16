(ns re-frame.views
  (:require ["solid-js/h$default" :as h]
            ["solid-js/web" :as web]
            [re-frame.loggers :as log]))

(defn clj-attrs->js
  [attrs]
  (clj->js attrs))

(defn js-attrs->clj
  [attrs]
  (js->clj attrs :keywordize-keys true))

(defn el-name
  [el]
  (if (keyword? el)
    (name el)
    el))

(defn attrs-and-children
  [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [nil args]))

(declare hyperscript)

(defn element
  [el attrs children]
  (if attrs
    (apply h el (clj-attrs->js attrs) (map hyperscript children))
    (apply h el (map hyperscript children))))

(defn hyperscript-vector
  [v]
  (let [[el & args] v]
    (cond
      ;; Fragment
      (= el :<>)
      (into-array (map hyperscript args))

      ;; JavaScript Component
      (= el :>)
      (let [el-fn (first args)

            [attrs children]
            (attrs-and-children (rest args))]
        (element el-fn attrs children))

      ;; HyperScript Component
      (or (string? el) (keyword? el))
      (let [[attrs children]
            (attrs-and-children args)]
        (element (el-name el) attrs children))

      ;; ClojureScript Component
      (fn? el)
      (let [el-fn (fn [& a]
                    (log/console :debug :el-fn a)
                    (hyperscript (el (js-attrs->clj (first a)))))

            [attrs children]
            (attrs-and-children args)]
        (element el-fn attrs children))

      :else
      (throw (ex-info "can't compile vector to hyperscript" {:vector v})))))

(defn hyperscript
  [o]
  (try
    (cond
      (vector? o)
      (hyperscript-vector o)

      (or (array? o) (sequential? o))
      (into-array (map hyperscript o))

      (fn? o)
      #(hyperscript (o))

      o
      (str o))
    (catch :default e
      (log/console :error e))))

(defn render
  [component el]
  (web/render #(hyperscript (component)) el))

(comment

  (def foo (hyperscript [:div.foo#bar {:style {:font-weight "bold"}} "foo"]))

  (log/console :log foo)

  ;;
  )
