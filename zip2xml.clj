(ns zip2xml
  (:import (java.util.zip ZipFile)
           (com.csvreader CsvReader)
           (java.nio.charset Charset))
  (:require [clojure.contrib.duck-streams :as streams])
  (:use [clojure.contrib.prxml]))

(def records-per-file 10000)
(def output-dir "/tmp/acxiom-clj2/")
(def fields [[:name 1]
             [:street 2]
             [:city 8]
             [:province_name "province_name"]
             [:province 9]
             [:postal-code 10]
             [:phone 12]
             [:fax 64]
             [:website 32]
             [:model "Opus::Business"]
             [:latitude 25]
             [:longitude 26]
             [:precision 27]
             [:refine "opus_business"]
             [:model_id 0]
             [:updated "NOW"]])

(defn create-doc-xml
  [reader]
  (let [values (vec (.getValues reader))]
    (if (not (empty? values))
      (reduce (fn [ret [field k]] 
                (if (string? k)
                  (conj ret [field k])
                  (conj ret [field (values k)])))
              [:doc]
              fields))))

(defn create-file-xml
  [reader] 
  (doall (loop [line-cnt 0
         xml [:add]
         next-record? true]
    (if (and (< line-cnt records-per-file) next-record?)
      (recur (inc line-cnt) (conj xml (create-doc-xml reader)) (.readRecord reader))
      xml))))
 
(defn process-zip-file 
  [file]
  (let [zfile (ZipFile. file)]
    (doseq [f (enumeration-seq (.entries zfile))]
      (let [basename (str output-dir (.getName f))]
        (with-open [reader (CsvReader. (.getInputStream zfile f) \, (Charset/forName "US-ASCII"))]
          (loop [filenum 0]
            (let [o-file (format "%s-%04d.xml" basename filenum)]
               (with-open [o (streams/writer o-file)]
                   (binding [*out* o] 
                     (prxml [:decl! {:version 1.1}] (create-file-xml reader)))
                   (if (.readRecord reader)
                     (recur (inc filenum)))))))))))

(defn main 
  []
  (println (time (dorun (pmap #(process-zip-file %1) ["/home/bdoyle/tmp/acxiom_oct/busreg1.zip"])))))

; process all of the acxiom files
(main)
