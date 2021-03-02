(ns a40kc-web.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [a40kc-web.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[a40kc-web started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[a40kc-web has shut down successfully]=-"))
   :middleware wrap-dev})
