(ns a40kc-web.app
  (:require [a40kc-web.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
