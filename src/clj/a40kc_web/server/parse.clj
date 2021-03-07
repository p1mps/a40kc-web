(ns a40kc-web.server.parse
  (:require
   [a40kc-web.server.xml-select :as xml-select]
   [a40kc-web.server.zip-reader :as zip-reader]))


(defn attrs-name [e]
    (:name (:attrs e)))

(defn attrs-and-content [e]
  [(:name (:attrs e)) (first (:content e))])

(defn content [e]
  (first (:content e)))

(defn keywordize [string]
  (keyword (clojure.string/lower-case string)))


(defn keywordize-chars [chars]
  (reduce (fn [result value]
            (let [[characteristic v]  value
                  c (keywordize characteristic)]
              (when v
                (if (clojure.string/includes? v "+")
                  (assoc result c (read-string (clojure.string/replace v "+" "")))
                  (assoc result c (read-string v))))))
          {}
          chars))


(defn weapons [model]
  (let [weapons (zx/xml->
                 model
                 :selections
                 :selection
                 :profiles
                 :profile
                 (zx/attr= :typeName "Weapon"))]
    (reduce (fn [result w]
              (conj result {:name (attrs-name (first w))
                            :chars (->
                                    (map #(attrs-and-content (first %))
                                         (zx/xml-> w :characteristics :characteristic))
                                    (keywordize-chars))}))

            []
            weapons)))

(defn characteristics [model]
  (let [chars (zx/xml-> model
                        :profiles
                        :profile
                        :characteristics
                        :characteristic)]
    (->
     (map #(attrs-and-content (first %)) chars)
     (keywordize-chars))))


(defn edn [forces]
  (for [f forces]
    {:force-name (attrs-name  (first f))
     :models (for [m (xml-select/models f)]
               {:name    (attrs-name (first m))
                :number  (read-string (:number (:attrs (first m))))
                :chars   (characteristics  m)
                :weapons (weapons m)})

     :units      (for [u (xml-select/units f)]
                   {:name
                    (attrs-name (first u))
                    :models (for [m (xml-select/unit->models u)]
                              {:name    (attrs-name (first m))
                               :number  (read-string (:number (:attrs (first m))))
                               :chars   (characteristics  m)
                               :weapons (weapons m)})})}))

(defn file->edn [file]
  (-> file
      zip-reader/zipper
      xml-select/forces
      edn))


(defn parse [file-rosz]
  ;; TODO: generate random xml name file
  (let [file (zip-reader/unzip-file file-rosz "output.xml")]
    (file->edn file)))

(comment

  (parse "spacemarines.rosz")


  (unzip-file "spacemarines.rosz" "spacemarines.ros")

  (clojure.string/join "" (drop-last "hello"))

  (def file (slurp "spacemarines.ros"))

  (def zipper (zipper file))

  (def forces (forces zipper))


  (parse "spacemarines.rosz")


 ,)
