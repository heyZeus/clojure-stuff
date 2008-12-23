#!/bin/bash

BREAK_CHARS="(){}[],^%$#@\"\";:''|\\"
JARS=""
for n in `ls $CLJ_JARS/*.jar`; do
   JARS=${JARS}:$n
done

if [ -z "$1" ]; then 
     rlwrap --remember -c -b $BREAK_CHARS -f ~/.clj_completions \
     java -cp $JARS clojure.main -i $CLJRC -e "(repl)"
else
     java -cp $JARS clojure.main $1 -- $2 $3 $4 $5 $6 $7 $8 $9
fi

