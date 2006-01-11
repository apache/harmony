#!/bin/sh
# $Id$

# Bail on errors
set -e

# Parse command line
CLASSPATH_HOME=/usr/local/classpath
for ARG in $@; do
  case $ARG in
    --with-classpath=*)
    	CLASSPATH_HOME=`echo $ARG | sed 's/--with-classpath=\(.*\)$/\1/g'`
    	;;
  esac
done

# Check directory
if [ ! -f etc/makedist.sh ]; then
    echo '*** Error: run me from the top level directory please'
    exit 1
fi

# Check classpath is there
GLIBJ="${CLASSPATH_HOME}/share/classpath/glibj.zip"
if [ ! -f "${GLIBJ}" ]; then
    echo "*** Error: build and install classpath first (${GLIBJ} not found)"
    exit 1
fi

# Remove generated files
rm -f java/jc.zip java/api.tgz

# Configure (so we have Makefiles)
sh autogen.sh ${1+"$@"}

# Create the distribution
make dist

# Clean up
make distclean

