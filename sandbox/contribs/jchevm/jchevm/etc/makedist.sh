#!/bin/sh
# $Id: makedist.sh,v 1.11 2005/03/19 19:55:02 archiecobbs Exp $

PREFIX=/usr/local

if [ ! -f etc/makedist.sh ]; then
    echo '***' run me from the top level directory please
    exit 1
fi

if [ `id -u` -ne 0 ]; then
    echo '***' you must be root
    exit 1
fi

if [ ! -w ${PREFIX}/share/classpath/glibj.zip ]; then
    echo '***' you must build and install classpath first
    exit 1
fi

rm -f java/jc.zip java/api.tgz

sh etc/regen.sh
(cd tools && make && make install) || exit 1
(cd java && make && make install) || exit 1
(cd soot && make && make install) || exit 1
(cd include && make && make install) || exit 1
(cd libjc/native && make hfiles) || exit 1
(cd jsrc && make jsrc.tgz) || exit 1
make dist

