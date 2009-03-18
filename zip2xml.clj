(ns zip2xml
  (:import (java.util.zip ZipFile)
           (com.csvreader CsvReader)
           (java.nio.charset Charset)
           (md5 MD5))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.prxml :as pxml]
            [clojure.contrib.str-utils :as sutils]))

(defn to-float
  [s]
  (if (not (empty? s))
    (Float/parseFloat s)))

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
             [:latitude #(to-float (%1 25))]
             [:longitude #(to-float (%1 26))]
             [:precision 27]
             [:refine "opus_business"]
             [:model_id 0]
             [:id #(MD5/hash (.toLowerCase (sutils/str-join ":" [(%1 1) (%1 2) (%1 8) (%1 9)])))]
             [:updated "NOW"]])

(defn create-doc-xml
  [reader]
  (let [values (vec (.getValues reader))]
    (if (not (empty? values))
      (reduce (fn [ret [field k]] 
                (conj ret (cond 
                            (string? k) [field k]
                            (number? k) [field (values k)]
                            :else [field (k values)])))
              [:doc]
              fields))))

(defn create-file-xml
  [reader] 
  (loop [line-cnt 0
         xml [:add]
         next-record? true]
    (if (and (< line-cnt records-per-file) next-record?)
      (recur (inc line-cnt) (conj xml (create-doc-xml reader)) (.readRecord reader))
      xml)))
 
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
                     (pxml/prxml [:decl! {:version 1.0}] (create-file-xml reader)))
                   (if (.readRecord reader)
                     (recur (inc filenum)))))))))))

(defn main 
  []
  (let [files (or (seq *command-line-args*)
                  ["/home/bdoyle/tmp/acxiom_oct/busreg1.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg2.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg3.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg4.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg5.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg6.zip"
                   "/home/bdoyle/tmp/acxiom_oct/busreg4.zip"])]
    (dorun (pmap #(process-zip-file %1) files))))

; process all of the acxiom files
(main)
