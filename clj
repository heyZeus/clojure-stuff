#!/bin/bash

BREAK_CHARS="(){}[],^%$#@\"\";:''|\\"
CLOJURE_JAR=~/share/clojure.jar:~/share/clojure-contrib.jar:~/share/charts4j-1.0/lib/charts4j-1.0.jar
JARS=""
for n in `ls $CLJ_JARS/*.jar`; do
   JARS=${JARS}:$n
done

CLJRC=~/share/clojure-stuff/repl-init.clj

if [ -z "$1" ]; then 
     rlwrap --remember -c -b $BREAK_CHARS -f ~/.clj_completions \
     java -cp $JARS clojure.main -i $CLJRC -r 
else
     java -cp $JARS clojure.main $1 -- $2 $3 $4 $5 $6 $7 $8 $9
fi

