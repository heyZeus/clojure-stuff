#!/bin/sh

rm -rf /tmp/clojure
svn checkout http://clojure.googlecode.com/svn/trunk/ /tmp/clojure
(cd /tmp/clojure && ant jar && mv *.jar $CLJ_JARS)

rm -rf /tmp/clojure-contrib
svn checkout http://clojure-contrib.googlecode.com/svn/trunk/ /tmp/clojure-contrib
(cd /tmp/clojure-contrib && ant jar && mv *.jar $CLJ_JARS)
