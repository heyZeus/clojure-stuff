(ns test-stuff
  (:use (clojure.contrib duck-streams str-utils except)))

(def a (lazy-cat ["1" "2"] (throw (Exception. "a Booby Trap!"))))
(def b (map #(.toString %) (lazy-cat "1" "2" (throwf))))
(def c (map (fn [a] (println a)) (lazy-cat "1" "2")))
(def d (lazy-cat "1" "2"))

(println (take 1 c))
(println "yes")

