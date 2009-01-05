#!/bin/bash

BREAK_CHARS="(){}[],^%$#@\"\";:''|\\"
# this works with java 1.6
CP=${CLJ_JARS}*:classes

if [ -z "$1" ]; then 
     rlwrap --remember -c -b $BREAK_CHARS -f ~/.clj_completions \
     java -cp $CP clojure.main -i $CLJRC 
elif [ $1 == "gorilla" ]; then
     java -cp $CP de.kotka.gorilla
else
     java -cp $CP clojure.main $1 -- $2 $3 $4 $5 $6 $7 $8 $9
fi

