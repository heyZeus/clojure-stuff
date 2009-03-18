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

(defn find-cats
  [values all-cats]
  (reduce (fn [[visible search] cat-idx] 
                (let [cat (values cat-idx)]
                  (if (not cat)
                    [visible search]
                    (let [visible-cat ((first all-cats) cat)
                          search-cat ((second all-cats) cat)
                          ret-visible (if visible-cat (conj visible visible-cat) visible)
                          ret-search (if search-cat (conj search search-cat) search)]
                  [visible search]))))
            [#{} #{}]
            (range 48 54)))

(def restaurant-pattern (java.util.regex.Pattern/compile "restaurants" java.util.regex.Pattern/CASE_INSENSITIVE))

(defn add-biz-cats
  [values all-cats xdoc]
  (let [[visible search] (find-cats values all-cats)
        xdoc-place (conj xdoc [:place_type (or (some #(re-seq restaurant-pattern %1) search) "business")])]
    (reduce (fn [xdoc cat]
              (conj xdoc [:category cat]))
            xdoc-place
            visible)))

(def name-idx 1)
(def street-idx 2)
(def city-idx 8)
(def province-idx 9)

(def records-per-file 10000)
(def output-dir "/tmp/acxiom-clj2/")
(def fields [[:name name-idx]
             [:street street-idx]
             [:city city-idx]
             [:province_name "province_name"]
             [:province province-idx]
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
             [:id #(MD5/hash (.toLowerCase (sutils/str-join ":" [(%1 name-idx) 
                                                                 (%1 street-idx) 
                                                                 (%1 city-idx) 
                                                                 (%1 province-idx)])))]
             [:updated "NOW"]])

(defn get-values
  [reader]
  (vec (.getValues reader)))

(defn capitalize [s]
  (sutils/re-gsub #"\b." #(.toUpperCase %) (.toLowerCase s)))

(defn load-cats
  [file]
  (with-open [reader (CsvReader. file \, (Charset/forName "US-ASCII"))]
    (loop [visible {}
           search {}]
      (let [has-next? (.readRecord reader)
            values (get-values reader)]
        (cond 
          (not (empty? values)) (let [id (values 0)
                                     cat2 (capitalize (values 2))
                                     cat4 (capitalize (values 4))
                                     cat6 (capitalize (values 6))]
                                 (recur (assoc visible id (or cat4 cat2)) (assoc search id (remove nil? [cat2 cat4 cat6]))))
          has-next? (recur visible search)
          :else [visible search])))))

(defn create-doc-xml
  [reader cats]
  (let [values (get-values reader)]
    (if (not (empty? values))
      (let [xdoc (reduce (fn [ret [field k]] 
                            (conj ret (cond 
                                        (string? k) [field k]
                                        (number? k) [field (values k)]
                                        :else [field (k values)])))
                        [:doc]
                        fields)]
        (add-biz-cats values cats xdoc)))))

(defn create-file-xml
  [reader
   cats] 
  (loop [line-cnt 0
         xml [:add]
         next-record? true]
    (if (and (< line-cnt records-per-file) next-record?)
      (recur (inc line-cnt) (conj xml (create-doc-xml reader cats)) (.readRecord reader))
      xml)))
 
(defn process-zip-file 
  [file cats]
  (let [zfile (ZipFile. file)]
    (doseq [f (enumeration-seq (.entries zfile))]
      (let [basename (str output-dir (.getName f))]
        (with-open [reader (CsvReader. (.getInputStream zfile f) \, (Charset/forName "US-ASCII"))]
          (loop [filenum 0]
            (let [o-file (format "%s-%04d.xml" basename filenum)]
               (with-open [o (streams/writer o-file)]
                   (binding [*out* o] 
                     (pxml/prxml [:decl! {:version 1.0}] (create-file-xml reader cats)))
                   (if (.readRecord reader)
                     (recur (inc filenum)))))))))))

(defn main 
  []
  (if (empty? *command-line-args*)
     (println "Usage : nacis [zip1 zip2]") 
     (let [cats (load-cats (first *command-line-args*))
           files (or (next *command-line-args*)
                     ["/home/bdoyle/tmp/acxiom_oct/busreg1.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg2.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg3.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg4.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg5.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg6.zip"
                      "/home/bdoyle/tmp/acxiom_oct/busreg4.zip"])]
        (dorun (pmap #(process-zip-file %1 cats) files)))))

; process all of the acxiom files
(main)
