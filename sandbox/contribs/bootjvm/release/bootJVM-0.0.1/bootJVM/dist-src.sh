#!/bin/sh
#
#!
# @file ./dist-src.sh
#
# @brief Distribute Boot JVM source package, with and without \
# documentation package.
#
# Clean up all build targets so that only source remains.
# Then create pre-formatted documentation for installation
# by @b config.sh and bundle into a tar file.
#
# Use @link ./dist-bin.sh dist-bin.sh@endlink to distribute
# the binary package.
#
# Use @link ./dist-doc.sh dist-doc.sh@endlink to distribute
# the documentation package.
#
#
# @see @link ./common.sh ./common.sh@endlink
#
# @attention  Make @e sure that all Eclipse project files are in
#             the "open" state when creating a distribution.
#             This will ensure immediate access to them by
#             Eclipse users without having to change anything.
#
# @todo  HARMONY-6-dist-src.sh-1 A Windows .BAT version of this
#        script needs to be written
#
#
# @section Control
#
# \$URL$
#
# \$Id$
#
# Copyright 2005 The Apache Software Foundation
# or its licensors, as applicable.
#
# Licensed under the Apache License, Version 2.0 ("the License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied.
#
# See the License for the specific language governing permissions
# and limitations under the License.
#
# @version \$LastChangedRevision$
#
# @date \$LastChangedDate$
#
# @author \$LastChangedBy$
#
#         Original code contributed by Daniel Lydick on 09/28/2005.
#
# @section Reference
#
#/ /* 
# (Use  #! and #/ with dox_filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
###################################################################
#
# Script setup.
#
. echotest.sh

. common.sh

MSG80="This script must NOT be interrupted.  Last chance to stop it..."
$echon "$PGMNAME:  $MSG80" $echoc
sleep 5
echo ""
echo ""

# Suppress attempts to interrupt
trap "" 1 2 3 15

###################################################################
#
# Check for common Eclipse output directory at normal level.
#
# Notice that each JNI implmementation will have its own directory
# tree with its own scripts and its own 'bin' directory, while Eclipse
# generates the directory in question, which 'clean.sh jni' does not
# clean up.
#
if test -d jni/bin
then
    echo "$PGMNAME: Found 'jni/bin' directory.  This is an artifact"
    echo "$PGMNAME: of the Eclipse project in 'jni'.  Please remove"
    echo "$PGMNAME: this directory manually before proceeding."
    exit 2
fi

###################################################################
#
# Prepare for distribution, then strip out _all_ output,
# rebuild documentation in _all_ formats, archive it, and
# then delete it.  Package the result in final 'tar' file.
#
DistChkReleaseLevel

DistChkTarget

DistPrep

# For the 'vi' users on the team
TAGLIST=`find . -name tags -print`
if test -n "$TAGLIST"
then
    echo ""
    echo "$PGMNAME: Cleaning out tag files"
    rm -f $TAGLIST
fi

COREFILES=`find . -name core -type f -print`
if test -n "$COREFILES"
then
    echo ""
    echo "$PGMNAME: Removing 'core' files"

    rm -f $COREFILES
fi

echo ""
echo "$PGMNAME: Cleaning out all binaries"

./clean.sh all

DistDocPrep

echo ""
echo "$PGMNAME: Setting directory permissions"
umask 022
chmod 0755 `find . -type d -print | egrep -v "/\.svn"`

echo ""
echo "$PGMNAME: Setting source file permissions"
chmod 0644 `find . -type f -name \*.c -print | egrep -v "/\.svn"`
chmod 0644 `find . -type f -name \*.h -print | egrep -v "/\.svn"`
chmod 0644 `find . -type f -name \*.java -print | egrep -v "/\.svn"`
chmod 0755 `find . -type f -name \*.sh -print | egrep -v "/\.svn"`
chmod 0644 `find . -type f -name \*.dox -print | egrep -v "/\.svn"`
chmod 0644 [A-Z]*
chmod 0644 `find . -type f -name .\?\?\* -print | egrep -v "/\.svn"`

DistTargetBuild dox

# Time stamp all files together
TMPTIMESTAMPFILE=${TMPDIR:-/tmp}/tmp.$PGMNAME.$$
rm -f $TMPTIMESTAMPFILE
touch $TMPTIMESTAMPFILE

# Itemize all files and symbolic links, less SVN administrative areas
for f in `find . -type f -print | egrep -v "/\.svn"`
do
    # Any file that complains here is not listed in the known file types
    touch -r $TMPTIMESTAMPFILE $f
done
rm -f $TMPTIMESTAMPFILE

DistDocTar

DistDocUnPrep

DistConfigPrep

echo ""
echo "$PGMNAME: Creating distribution file '../$DISTSRCDOCTAR'"

cd ..
ln -s bootJVM $TARGET_HOME

rm -f $DISTSRCDOCTAR $DISTSRCDOCTAR.gz

# Itemize all files and symbolic links, less SVN administrative areas
SRCTARCFCMD=`(find bootJVM -type l -print; \
              find bootJVM -type f -print) |\
              egrep -v "/\.svn|$PREFMTDOCSTAR" | \
              sed "s/^bootJVM/$TARGET_HOME/" | \
              sort`

tar cf $DISTSRCDOCTAR $SRCTARCFCMD $TARGET_HOME/$PREFMTDOCSTAR.gz
if test ! -r $DISTSRCDOCTAR
then
    echo ""
    echo "$PGMNAME: Cannot locate '../$DISTSRCDOCTAR'."
    echo "$PGMNAME: Directory `cd ..; pwd` is probably not writable."
    echo "$PGMNAME: Please make it writable and try again."
    exit 5
fi

echo ""
echo \
   "$PGMNAME: Compressing distribution file into '../$DISTSRCDOCTAR.gz'"
gzip $DISTSRCDOCTAR
if test ! -r $DISTSRCDOCTAR.gz
then
    echo ""
    echo "$PGMNAME: Cannot compress into '$DISTSRCDOCTAR.gz'"
    exit 6
fi

chmod 0444 $DISTSRCDOCTAR.gz

echo ""
echo "$PGMNAME: Creating distribution file '../$DISTSRCTAR'"

rm -f $DISTSRCTAR $DISTSRCTAR.gz

#
# Temporarily remove documentation package, create distribution file,
# and restore it.
#
rm -f $TARGET_HOME/$PREFMTDOCSTAR.gz

# Itemize all files and symbolic links, less SVN administrative areas
tar cf $DISTSRCTAR $SRCTARCFCMD
cat $DISTSRCDOCTAR.gz | gunzip | tar xf - $TARGET_HOME/$PREFMTDOCSTAR.gz

if test ! -r $DISTSRCTAR
then
    echo ""
    echo "$PGMNAME: Cannot locate '../$DISTSRCTAR'."
    echo "$PGMNAME: Directory `cd ..; pwd` is probably not writable."
    echo "$PGMNAME: Please make it writable and try again."
    exit 7
fi

echo ""
echo \
   "$PGMNAME: Compressing distribution file into '../$DISTSRCTAR.gz'"
rm -f $DISTSRCTAR.gz
gzip $DISTSRCTAR
if test ! -r $DISTSRCTAR.gz
then
    echo ""
    echo "$PGMNAME: Cannot compress into '$DISTSRCTAR.gz'"
    exit 8
fi

chmod 0444 $DISTSRCTAR.gz

rm $TARGET_HOME
cd bootJVM

DistConfigUnPrep

DistUnPrep

echo ""
echo "$PGMNAME: Source distribution tar files created:"
echo ""
ls -l ../$DISTSRCTAR.gz
ls -l ../$DISTSRCDOCTAR.gz
echo ""

###################################################################
#
# Done.
#
echo ""
echo "$PGMNAME: Source distribution complete"

#
# EOF
