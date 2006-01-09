#!/bin/sh
# $Id$

CLASSPATH_HOME=/usr/local/classpath
for ARG in $@; do
  case $ARG in
    --with-classpath=*)
    	CLASSPATH_HOME=`echo $ARG | sed 's/--with-classpath=\(.*\)$/\1/g'`
    	;;
  esac
done

if [ ! -f etc/makedist.sh ]; then
    echo '*** Error: run me from the top level directory please'
    exit 1
fi

GLIBJ="${CLASSPATH_HOME}/share/classpath/glibj.zip"
if [ ! -f "${GLIBJ}" ]; then
    echo "*** Error: build and install classpath first (${GLIBJ} not found)"
    exit 1
fi

rm -f java/jc.zip java/api.tgz

sh etc/regen.sh ${1+"$@"}
(cd java && make) || exit 1
(cd include && make) || exit 1
make dist

