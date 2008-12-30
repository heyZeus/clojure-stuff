
(comment "REPL initialization goes in this file.")

(set! *print-length* 50)
(set! *print-level* 10)

(defn name-to-symbol [name] 
  "Converts the name to something that can be passed to the symbol function"
  (symbol (.replaceAll (.replaceAll (.substring name 0 (- (.lastIndexOf name "clj") 1)) "_" "-") "/" ".")))

(defn contrib-ns [jar]
  "Returns a seq of symbols from the clojure.contrib package, is not recursive and doesn't include test files."
  (for [f (map #(.getName %) 
               (enumeration-seq (.entries (java.util.zip.ZipFile. jar))))
        :when (and (.endsWith f "clj") (= 3 (count (.split f "/"))) (not (.contains f "test")))]
    (name-to-symbol f)))

; sets the variable to the colure-contrib.jar path, otherwise nil
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

(use-contribs)

; load in the line numbered repl
(repl)
