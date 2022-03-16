(ns re-frame.reactive
  (:refer-clojure :exclude [atom merge])
  (:require ["solid-js" :refer [createSignal createMemo createRoot getOwner runWithOwner mergeProps]]))

(defn- notify-w
  [this watches old new]
  (doseq [[k w] watches]
    (when w (w k this old new))))

(defprotocol ISolidMemo
  (getter [_] "Returns the getter function"))

(defprotocol ISolidSignal
  (setter [_] "Returns the setter function"))

(defprotocol ISolidOwned
  (owner [_] "Returns the saved owner"))

(defprotocol IReactiveAtom)

(deftype SolidRatom [getter setter meta validator ^:mutable watches]
  IAtom
  IReactiveAtom

  IEquiv
  (-equiv [o other] (identical? o other))

  IFn
  (-invoke [_] (getter))
  (-invoke [_ new-val] (setter new-val))

  ISolidMemo
  (getter [_] getter)

  ISolidSignal
  (setter [_] setter)

  IDeref
  (-deref [this] (this))

  IReset
  (-reset! [a new-value]
    (when-not (nil? validator)
      (assert (validator new-value) "Validator rejected reference state"))
    (let [old-value (getter)]
      (a new-value)
      (when-not (nil? watches)
        (notify-w a watches old-value new-value))
      new-value))

  ISwap
  (-swap! [a f]          (-reset! a (f (getter))))
  (-swap! [a f x]        (-reset! a (f (getter) x)))
  (-swap! [a f x y]      (-reset! a (f (getter) x y)))
  (-swap! [a f x y more] (-reset! a (apply f (getter) x y more)))

  IWithMeta
  (-with-meta [_ new-meta] (SolidRatom. getter setter new-meta validator watches))

  IMeta
  (-meta [_] meta)

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (let [v {:val (-deref a)}]
      (-write writer (str "#object[solid-frame.ratom.SolidRatom" " "))
      (pr-writer v writer opts)
      (-write writer "]")))

  IWatchable
  (-notify-watches [this old new]
    (notify-w this watches old new)
    this)
  (-add-watch [this key f]
    (set! watches (assoc watches key f))
    this)
  (-remove-watch [this key]
    (set! watches (dissoc watches key))
    this)

  IHash
  (-hash [this] (goog/getUid this)))

(defn create-signal
  [v]
  (createSignal v #js {:equals =}))

(defn atom
  "Like clojure.core/atom, except that it keeps track of derefs."
  ([x]
   (let [[getter setter] (create-signal x)]
     (->SolidRatom getter setter nil nil nil)))
  ([x & {:keys [meta validator]}]
   (let [[getter setter] (create-signal x)]
     (->SolidRatom getter setter meta validator nil))))

(defprotocol IDisposable
  (dispose! [this])
  (add-on-dispose! [this f]))

(deftype SolidReaction [owner getter ^:mutable disposers meta]
  IReactiveAtom

  IEquiv
  (-equiv [o other] (identical? o other))

  IFn
  (-invoke [_] (runWithOwner owner #(getter)))

  ISolidMemo
  (getter [_] getter)

  ISolidOwned
  (owner [_] owner)

  IDeref
  (-deref [this] (this))

  IDisposable
  (dispose! [this]
    (let [[root & others] disposers]
      (root)
      (doseq [f others]
        (f this))))
  (add-on-dispose! [_ f] (set! disposers (conj disposers f)))

  IWithMeta
  (-with-meta [_ new-meta] (SolidReaction. owner getter disposers new-meta))

  IMeta
  (-meta [_] meta)

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (let [v {:val (-deref a)}]
      (-write writer (str "#object[solid-frame.ratom.SolidReaction" " "))
      (pr-writer v writer opts)
      (-write writer "]")))

  IHash
  (-hash [this] (goog/getUid this)))

(defn create-memo
  [f]
  (createMemo f #js {:equals =}))

(defn make-reaction
  [f]
  (createRoot
   #(->SolidReaction (getOwner) (create-memo f) [%] nil)))

(def reactive? (constantly true))

(defn merge
  [& sources]
  (apply mergeProps sources))

(comment

  (def a (atom {:foo :bar}))
  (def b (make-reaction (fn [] (assoc @a :baz :quux))))

  @a
  @b

  (swap! a assoc :name a)

  ;;
  )
