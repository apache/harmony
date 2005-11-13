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
    echo '***' run me from the top level directory please
    exit 1
fi

if [ `id -u` -ne 0 ]; then
    echo '***' you must be root
    exit 1
fi

if [ ! -w ${CLASSPATH_HOME}/share/classpath/glibj.zip ]; then
    echo '***' you must build and install classpath first
    exit 1
fi

rm -f java/jc.zip java/api.tgz

sh etc/regen.sh ${1+"$@"}
(cd tools && make && make install) || exit 1
(cd java && make && make install) || exit 1
(cd include && make && make install) || exit 1
(cd libjc/native && make hfiles) || exit 1
make dist

