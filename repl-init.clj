
; all REPL initialization goes here

(set! *print-length* 50)
(set! *print-level* 10)

(defn name-to-symbol [lib-name] 
  "Converts the lib-name to a symbol"
    (-> lib-name (.replaceFirst ".clj$" "") (.replaceAll  "_" "-") 
      (.replaceAll "/" ".") (symbol)))

(defn contrib-ns [jar]
  "Returns a seq of symbols from the clojure.contrib package, is not recursive"
  (for [f (map #(.getName %) 
               (enumeration-seq (.entries (java.util.zip.ZipFile. jar))))
        :when (and (.endsWith f "clj") (= 3 (count (.split f "/"))) (not (.endsWith f "test_clojure.clj")))]
    (name-to-symbol f)))

;sets the variable to the colure-contrib.jar path, otherwise nil
(def contrib-jar (if-let [url (.getResource (ClassLoader/getSystemClassLoader) "clojure-contrib.jar")]
                   (.getFile url))) 

(defn use-contribs []
  "Calls the use function on every clj file in the clojure.contrib package. Not every clj file can
  be loaded because of function name clashes with the core."
  (if contrib-jar 
    (println (str "use " (reduce (fn [ret n] 
                                      (try 
                                        (use n)
                                        (conj ret n)
                                        (catch Exception _ ret)))
                           [] (contrib-ns contrib-jar))))))

(defn print-* 
  "Prints all of the vars in the given namespace that start with *. Uses 'clojure.core by default."
  ([] (print-* 'clojure.core))
  ([ns] 
    (doseq [[key value] (ns-publics ns) 
             :when (.startsWith (str key) "*")] 
      (println key "=>" (var-get value)))))

;calls use on all of the useful contrib stuff
(use-contribs)

;starts up the line numbered REPL
(repl)

