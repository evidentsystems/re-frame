(ns re-frame.reactive)

(defmacro reaction [& body]
  `(re-frame.reactive/make-reaction
    (fn [] ~@body)))
