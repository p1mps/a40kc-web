(ns a40kc-web.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [a40kc-web.core-test]))

(doo-tests 'a40kc-web.core-test)

