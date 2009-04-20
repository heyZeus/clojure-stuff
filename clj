#!/bin/bash

BREAK_CHARS="(){}[],^%$#@\"\";:''|\\"

# this works with java 1.6
CP=${CLJ_JARS}/*:.:classes:${CLJ_JARS}/clj-record:${HOME}/share/clj-web-crawler
OPTS=${CLJ_OPTS:-}
export CLASSPATH="${CP}"
PARAMS="clojure.main $1 $2 $3 $4 $5 $6 $7 $8 $9"
PRE=
JAVA=java

if [ -z "$1" ]; then 
     PRE="rlwrap --remember -c -b $BREAK_CHARS -f ${HOME}/.clj_completions "
     PARAMS="clojure.main -i $CLJRC"
elif [ $1 == "gorilla" ]; then
     PARAMS="de.kotka.gorilla"
fi

$PRE $JAVA $OPTS $PARAMS
exit
