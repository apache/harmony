#!/bin/sh
# $Id$

#
# Script to regenerate all the GNU auto* gunk.
# Run this from the top directory of the source tree.
#
# This is customized for my FreeBSD machine. It probably
# won't work right on your machine, please edit to suit.
#
# If it looks like I don't know what I'm doing here, you're right.
#

if [ ! -f libjc/Makefile.am ]; then
    echo '***' run me from the top level directory please
    exit 1
fi

echo "cleaning up"
find . -name 'Makefile.in' -print | xargs rm -f
rm -rf autom4te*.cache scripts aclocal.m4 configure
rm -f include/config.h.in include/config.h
rm -f install-sh missing
mkdir scripts

LIBTOOLIZE="libtoolize"
ACLOCAL="aclocal"
AUTOHEADER="autoheader"
AUTOMAKE="automake"
AUTOCONF="autoconf"

echo "running libtoolize"
${LIBTOOLIZE} -c -f

echo "running aclocal"
${ACLOCAL} ${ACLOCAL_ARGS} -I scripts

echo "running autoheader"
${AUTOHEADER} -I include -I libjc/arch -I libjc/native

echo "running automake"
${AUTOMAKE} --add-missing -c --foreign

echo "running autoconf"
${AUTOCONF} -f -i
cp scripts/install-sh install-sh
cp scripts/missing missing

CONFIGFLAGS="--enable-werror --enable-assertions"
echo running configure ${CONFIGFLAGS} "$@"
if [ `uname -s` = 'FreeBSD' ]; then
	export LDFLAGS=-L/usr/local/lib ;
	export CPPFLAGS=-I/usr/local/include;
fi

./configure ${CONFIGFLAGS} ${1+"$@"}

