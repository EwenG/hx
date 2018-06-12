(ns hx.react
  (:require [goog.object :as gobj]
            ["react" :as react]
            [hx.utils :as utils])
  (:require-macros [hx.react]))

(defn props->clj [props]
  (utils/shallow-js->clj props :keywordize-keys true))

(defn styles->js [props]
  (cond
    (and (map? props) (:style props))
    (assoc props :style (clj->js (:style props)))

    (gobj/containsKey props "style")
    (do (->> (gobj/get props "style")
             (clj->js)
             (gobj/set props "style"))
        props)

    :default props))

(defn clj->props [props & {:keys [styles?]}]
  (-> (if styles? (styles->js props) props)
      (utils/reactify-props)
      (utils/shallow-clj->js props)))

(defn is-hx? [el]
  ;; TODO: detect hx component
  true)

(defn create-element [el p & c]
  (if (or (string? p) (number? p) (react/isValidElement p))
    (apply react/createElement el nil p c)

    ;; if el is a keyword, or is not marked as an hx component,
    ;; we recursively convert styles
    (let [js-interop? (or (string? el) (not (is-hx? el)))
          props (clj->props p :styles? js-interop?)]
      (apply react/createElement el props c))))

(defn create-class [super-class init-fn static-properties method-names]
  (let [ctor (fn [props]
               (this-as this
                 ;; auto-bind methods
                 (doseq [method method-names]
                   (gobj/set this (munge method)
                             (.bind (gobj/get this (munge method)) this)))

                 (init-fn this props)))]
    ;; set static properties on prototype
    (doseq [[k v] static-properties]
      (gobj/set ctor k v))
    (goog/inherits ctor super-class)
    ctor))

(defn create-pure-component [init-fn static-properties method-names]
  (create-class react/PureComponent init-fn static-properties method-names))

(def fragment react/Fragment)
