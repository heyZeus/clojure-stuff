#!/bin/bash

BREAK_CHARS="(){}[],^%$#@\"\";:''|\\"
for n in `ls ${CLJ_JARS}*.jar`; do
   CP=${CP}:$n
done

if [ -z "$1" ]; then 
     rlwrap --remember -c -b $BREAK_CHARS -f ~/.clj_completions \
     java -cp $CP clojure.main -i $CLJRC -e "(repl)"
else
     java -cp $CP clojure.main $1 -- $2 $3 $4 $5 $6 $7 $8 $9
fi

