(ns zip2xml
  (:import (java.util.zip ZipFile)
           (com.csvreader CsvReader)
           (java.nio.charset Charset)
           (java.io File)
           (java.security MessageDigest)
           (md5 MD5))
  (:load  "prxml/prxml")
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.str-utils :as sutils]))

(def name-idx 1)
(def street-idx 2)
(def city-idx 10)
(def province-idx 11)
(def records-per-file 10000)
(def output-dir "/tmp/acxiom-clj/")
(def *provinces* {})
(def *categories* {})

(defn md5
  [s]
  (let [md (MessageDigest/getInstance "MD5")
        _ (.update md (.getBytes s))
        digest (.digest md)]
    (loop [length 0 hex ""]
      (if (< length (alength digest))
        (recur (inc length) (str hex (format "%02x" (aget digest length))))
        hex))))

(defn format-float
  [s]
  (if (not (empty? s))
    (format "%.6f" (Float/parseFloat s))))

(defn add-field
  [n v]
  ["field" {:name n} v])

(defn find-cats
  [values cats]
  (reduce (fn [[visible search] cat-idx] 
                (let [cat (values cat-idx)]
                  (if (not cat)
                    [visible search]
                    (let [visible-cat ((:visible cats) cat)
                          search-cat ((:search cats) cat)
                          ret-visible (if (not (empty? visible-cat)) 
                                        (conj visible visible-cat) visible)
                          ret-search (if (not (empty? search-cat)) 
                                       (apply conj search search-cat) search)]
                      [ret-visible ret-search]))))
            [#{} #{}]
            (range 48 54)))

(def restaurant-pattern 
  (java.util.regex.Pattern/compile "restaurants" java.util.regex.Pattern/CASE_INSENSITIVE))

(defn biz-cats
  [cats xdoc]
  (reduce (fn [xdoc cat]
             (conj xdoc (add-field "category" cat)))
           xdoc
           cats))

(defn add-biz-cats
  [values xdoc]
  (let [[visible search] (find-cats values *categories*)
        place-type (or (some #(and (re-find restaurant-pattern %1) "restaurant") search) "business")
        xdoc (conj xdoc (add-field "place_type" place-type))
        xdoc (biz-cats visible xdoc)
        md5-str (.toLowerCase (sutils/str-join ":" [(values name-idx) 
                                                    (values street-idx) 
                                                    (values city-idx) 
                                                    (values province-idx)
                                                    place-type]))]
    (conj xdoc (add-field "id" (md5 md5-str)))))

(defn format-phone
  [values]
  (let [phone (values 16)]
    (if (= 10 (count phone))
      (str "(" (.substring phone 0 3) ") " (.substring phone 3 6) "-" (.substring phone 6))
      phone)))

(defn format-web
  [values]
  (let [url (values 38)]
    (if (and url (not (empty? url)) (not (.startsWith url "http")))
      (str "http://" url)
      url)))

(def fields [["name" name-idx]
             ["street" street-idx]
             ["city" city-idx]
             ["province_name" #(*provinces* (%1 province-idx))]
             ["province" province-idx]
             ["postal_code" 13]
             ["phone" #(format-phone %1)]
             ["fax" 59]
             ["website" #(format-web %1)]
             ["model" "Opus::Business"]
             ["latitude" #(format-float (%1 29))]
             ["longitude" #(format-float (%1 30))]
             ["precision" 27]
             ["refine" "opus_business"]
             ["model_id" 0]
             ["updated" "NOW"]])

(defn get-values
  [reader]
  (vec (.getValues reader)))

(defn capitalize 
  [s]
  (if (not (empty? s))
    (sutils/re-gsub #"\b." #(.toUpperCase %) (.toLowerCase s))))

(defn load-cats
  [file]
  (with-open [reader (CsvReader. file \, (Charset/forName "US-ASCII"))]
    (loop [visible {} search {}]
      (let [has-next? (.readRecord reader)
            values (get-values reader)]
        (cond 
          (not (empty? values)) (let [id (values 0)
                                     cat2 (capitalize (values 2))
                                     cat4 (capitalize (values 4))
                                     cat6 (capitalize (values 6))]
                                 (recur (assoc visible id (or cat4 cat2)) 
                                        (assoc search id (remove nil? [cat2 cat4 cat6]))))
          has-next? (recur visible search)
          :else {:visible visible :search search})))))

(defn load-provinces
  [file]
  (with-open [reader (CsvReader. file \, (Charset/forName "US-ASCII"))]
    (loop [provinces {}]
      (let [has-next? (.readRecord reader)
            values (get-values reader)]
        (if (seq values)
          (recur (assoc provinces (values 0) (capitalize (values 1))))
          provinces)))))

(defn create-doc-xml
  [reader]
  (let [values (get-values reader)]
    (if (seq values)
      (let [xdoc (reduce (fn [ret [field k]] 
                            (conj ret (add-field field (cond 
                                                         (string? k) k
                                                         (number? k) (values k)
                                                         :else (k values)))))
                        [:doc]
                        fields)]
        (add-biz-cats values xdoc)))))

(defn create-file-xml
  [reader] 
  (loop [line-cnt 0 
         xml [:add]] 
      (cond 
        (and (< line-cnt records-per-file) (.readRecord reader))
          (let [new-xdoc (create-doc-xml reader)
                xdoc (if new-xdoc (conj xml new-xdoc) xml)
                cnt (if new-xdoc (inc line-cnt) line-cnt)]
             (recur cnt xdoc))
        (> line-cnt 0) xml
        :else nil)))
 
(defn process-zip-file 
  [file provinces categories] 
  (let [zfile (ZipFile. file)]
    (doseq [f (enumeration-seq (.entries zfile))]
      (binding [*categories* categories
                *provinces* provinces]
        (let [basename (str output-dir (.getName f))]
          (with-open [reader (CsvReader. (.getInputStream zfile f) \, (Charset/forName "US-ASCII"))]
            (loop [filenum 0]
              (if-let [xdoc (create-file-xml reader)]
                (let [o-file (format "%s-%04d.xml" basename filenum)]
                  (with-open [o (streams/writer o-file)]
                    (binding [*out* o
                              prxml/*print-newlines* true]
                      (prxml/prxml [:decl! {:version 1.0}] xdoc)))
                  (recur (inc filenum)))))))))))

(defn acxiom-files 
  [basedir]
  (reduce (fn [ret file]
            (if (and (.startsWith file "busreg") 
                     (.endsWith file "zip")
                     (.isFile (File. (str basedir file))))
              (conj ret (str basedir file))
              ret))
          []
          (.list (File. basedir))))

(defn main 
  [args]
  (if (empty? args)
     (println "Usage : nacis [zip1 zip2]") 
     (let [cats (load-cats (first args))
           provs (load-provinces "/home/bdoyle/tmp/acxiom_feb/prov.csv")
           afiles (acxiom-files "/home/bdoyle/tmp/acxiom_feb/")
           files (or (next args) afiles)]
       (dorun (pmap #(process-zip-file %1 provs cats) files)))))

(main *command-line-args*)


