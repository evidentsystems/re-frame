(ns re-frame.interop
  (:require [goog.async.nextTick]
            [goog.events :as events]
            [re-frame.reactive :as reactive]))

(defn on-load
      [listener]
      ;; events/listen throws an exception in react-native environments because addEventListener is not available.
      (try
        (events/listen js/self "load" listener)
        (catch :default _)))

(def next-tick goog.async.nextTick)

(def empty-queue #queue [])

(def after-render (.-requestAnimationFrame js/window))

;; Make sure the Google Closure compiler sees this as a boolean constant,
;; otherwise Dead Code Elimination won't happen in `:advanced` builds.
;; Type hints have been liberally sprinkled.
;; https://developers.google.com/closure/compiler/docs/js-for-compiler
(def ^boolean debug-enabled? "@define {boolean}" ^boolean goog/DEBUG)

(defn ratom [x]
  (reactive/atom x))

(defn ratom? [x]
  ;; ^:js suppresses externs inference warnings by forcing the compiler to
  ;; generate proper externs. Although not strictly required as
  ;; re-frame.reactive/IReactiveAtom is not JS interop it appears to be harmless.
  ;; See https://shadow-cljs.github.io/docs/UsersGuide.html#infer-externs
  (satisfies? reactive/IReactiveAtom ^js x))

(defn deref? [x]
  (satisfies? IDeref x))

(defn make-reaction [f]
  (reactive/make-reaction f))

(defn add-on-dispose! [a-ratom f]
  (reactive/add-on-dispose! a-ratom f))

(defn dispose! [a-ratom]
  (reactive/dispose! a-ratom))

(defn set-timeout! [f ms]
  (js/setTimeout f ms))

(defn now []
  (if (and
       (exists? js/performance)
       (exists? js/performance.now))
    (js/performance.now)
    (js/Date.now)))

(defn reagent-id
  "Produces an id for reactive Reagent values
  e.g. reactions, ratoms, cursors."
  [reactive-val]
  ;; ^:js suppresses externs inference warnings by forcing the compiler to
  ;; generate proper externs. Although not strictly required as
  ;; reactive/IReactiveAtom is not JS interop it appears to be harmless.
  ;; See https://shadow-cljs.github.io/docs/UsersGuide.html#infer-externs
  (when (implements? reactive/IReactiveAtom ^js reactive-val)
    (str (condp instance? reactive-val
           reactive/SolidRatom "ra"
           reactive/SolidReaction "re"
           "other")
         (hash reactive-val))))

(defn reactive?
  []
  (reactive/reactive?))
