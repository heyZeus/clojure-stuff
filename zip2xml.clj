(ns zip2xml
  (:import (java.util.zip ZipFile)
           (com.csvreader CsvReader)
           (java.nio.charset Charset))
  (:require [clojure.contrib.duck-streams :as streams]))

(def records-per-file 10000)
(def output-dir "/tmp/acxiom-clj/")
(def columns {:model_id 0
              :name 1
              :street 2
              :city 8
              :province 9
              :postal-code 10
              :phone 12
              :fax 64
              :website 32
              :hours 66
              :latitude 25
              :longitude 26
              :precision 27
              :business-flg 23
              :categories (range 48 53)})

(defn write-xml
  [reader file-name]
  (with-open [o (streams/writer file-name)]
    (loop [line-cnt 0]
      (if (and (< line-cnt records-per-file) (.readRecord reader))
         (do
           (.write o (.getRawRecord reader))
           (.println o) 
           (recur (inc line-cnt)))))))
 
(defn process-zip-file 
  [file]
  (let [zfile (ZipFile. file)]
    (doseq [f (enumeration-seq (.entries zfile))]
      (let [basename (str output-dir (.getName f))]
        (with-open [reader (CsvReader. (.getInputStream zfile f) \, (Charset/forName "US-ASCII"))]
          (loop [filenum 0]
            (let [o-file (format "%s-%04d.xml" basename filenum)]
               (write-xml reader o-file) 
               (recur (inc filenum)))))))))

(defn main 
  []
  (pmap #(process-zip-file %1) ["/home/bdoyle/tmp/acxiom_oct/busreg1.zip"])) 

; process all of the acxiom files
(main)
