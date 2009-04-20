(ns search-solr
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.zip-filter.xml :as zf]
            [clojure.zip :as zip]
            [clojure.xml :as xml]))


(defn main 
  [file]
  (let [solr-doc (zip/xml-zip (xml/parse file))]
    (println solr-doc)
    (println)
    (println)
    ;(println (zf/xml-> solr-doc :add :doc :field (zf/attr= :name "name")))))
    (println (zf/xml-> solr-doc :doc :field (zf/attr= :name "street") zf/text))))
    ;(println (zf/xml-> solr-doc :id zf/text))))
    ;(println (zf/xml-> solr-doc :add :doc :blah zf/text))))

(main (first *command-line-args*))


