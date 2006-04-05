#!/bin/sh
#
#!
# @file ./dist-doc.sh
#
# @brief Distribute Boot JVM documentation package.
#
# Make @e sure to have performed the final build by running
# @link ./Makefile make veryclean@endlink followed by
# @link ./Makefile make all@endlink (both in the top level
# directory).  This will guarantee that everything compiles
# clean and may be installed and run on all platforms of
# this CPU type.
#
# Use @link ./dist-src.sh dist-src.sh@endlink to distribute
# the source package.
#
# Use @link ./dist-bin.sh dist-bin.sh@endlink to distribute
# the binary package.
#
# @see @link support/dist-common.sh support/dist-common.sh@endlink
#
# @attention  Make @e sure that all Eclipse project files are in
#             the "open" state when creating a distribution.
#             This will ensure immediate access to them by
#             Eclipse users without having to change anything.
#
#
# @todo  HARMONY-6-dist-doc.sh-1 A Windows .BAT version of this
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
# (Use  #! and #/ with dox-filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
###################################################################
#
# Script setup.
#
. support/echotest.sh

. support/dist-common.sh

MSG80="This script must NOT be interrupted.  Last chance to stop it..."
$echon "$PGMNAME:  $MSG80" $echoc
sleep 5
echo ""
echo ""

# Suppress attempts to interrupt
trap "" 1 2 3 15

###################################################################
#
# Clean up everything and rebuild it.
#
DistChkReleaseLevel

DistChkTarget

make veryclean

DistPrep

DistDocPrep

DistTargetBuild dox

echo ""
echo "$PGMNAME: Setting target directory permissions"
umask 022
chmod 0755 `find . -type d -print`

# Use same target as source distribution for original docs,
# that is, they will _not_ be changed.
if test -d doc.ORIG
then
    chmod -R +w doc.ORIG
    rm -rf doc.ORIG
fi

mv doc doc.ORIG

echo ""
echo "$PGMNAME: Setting target file permissions"

# Time stamp all files together
cd doc.ORIG
TMPTIMESTAMPFILE=${TMPDIR:-/tmp}/tmp.$PGMNAME.$$
rm -f $TMPTIMESTAMPFILE
touch $TMPTIMESTAMPFILE
for f in `find . -type f -print`
do
    chmod +w $f
    touch -r $TMPTIMESTAMPFILE $f
    chmod -w $f
done
rm -f $TMPTIMESTAMPFILE
cd ..

echo ""
echo "$PGMNAME: Creating distribution file '../$DISTDOCTAR'"

cd ..
ln -s bootJVM $TARGET_HOME

rm -f $DISTDOCTAR
tar cf $DISTDOCTAR $TARGET_HOME/doc.ORIG
rm $TARGET_HOME

if test ! -r $DISTDOCTAR
then
    echo ""
    echo "$PGMNAME: Cannot locate '../$DISTDOCTAR'."
    echo "$PGMNAME: Directory `cd ..; pwd` is probably not writable."
    echo "$PGMNAME: Please make it writable and try again."
    exit 4
fi

echo ""
echo \
   "$PGMNAME: Compressing distribution file into '../$DISTDOCTAR.gz'"
rm -f $DISTDOCTAR.gz
gzip $DISTDOCTAR
if test ! -r $DISTDOCTAR.gz
then
    echo ""
    echo "$PGMNAME: Cannot compress into '$DISTDOCTAR.gz'"
    exit 5
fi

chmod 0444 $DISTDOCTAR.gz
cd bootJVM

chmod -R +w doc.ORIG
mv doc.ORIG doc

DistDocUnPrep

DistUnPrep

echo ""
echo "$PGMNAME: Documentation distribution tar file created:"
echo ""
ls -l ../$DISTDOCTAR.gz
echo ""

###################################################################
#
# Done.
#
echo ""
echo "$PGMNAME: Documentation distribution complete"

#
# EOF
