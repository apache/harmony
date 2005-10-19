#!/bin/sh
#
#!
# @file ./dist-doc.sh
#
# @brief Distribute Boot JVM documentation package.
#
# Make @e sure to have performed the final build by running
# @link ./clean.sh clean.sh all@endlink followed by
# @link ./build.sh build.sh all@endlink.  This will guarantee
# that everything compiles clean and may be installed and
# run on all platforms of this CPU type.
#
# Use @link ./dist-src.sh dist-src.sh@endlink to distribute
# the source package.
#
# Use @link ./dist-bin.sh dist-bin.sh@endlink to distribute
# the binary package.
#
# @see @link ./common.sh ./common.sh@endlink
#
# @attention  Make @e sure that all Eclipse project files are in
#             the "open" state when creating a distribution.
#             This will ensure immediate access to them by
#             Eclipse users without having to change anything.
#
#
# @todo A Windows .BAT version of this script needs to be written
#
#
# @section Control
#
# \$URL$ \$Id$
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
# Clean up everything and rebuild it.
#
./clean.sh all

DistPrep

DistDocPrep

DistTargetBuild dox

DistConfigPrep

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
touch TMPTIMESTAMPFILE
for f in `find . -type f -print`
do
    chmod +w $f
    touch -r TMPTIMESTAMPFILE $f
    chmod -w $f
done
rm -f $TMPTIMESTAMPFILE
cd ..

TARGET_HOME="harmony/bootJVM-$CONFIG_RELEASE_LEVEL"
cd ../..
mv harmony/bootJVM $TARGET_HOME

echo ""
echo "$PGMNAME: Creating distribution file '../../$DISTDOCTAR'"

rm -f $DISTDOCTAR
tar cf $DISTDOCTAR $TARGET_HOME/doc.ORIG
mv $TARGET_HOME harmony/bootJVM

if test ! -r $DISTDOCTAR
then
    echo ""
    echo "$PGMNAME: Directory `cd ../..; pwd` is not writable."
    echo "$PGMNAME: Please make it writable and try again."
    exit 4
fi

echo ""
echo \
   "$PGMNAME: Compressing distribution file into '../../$DISTDOCTAR.gz'"
rm -f $DISTDOCTAR.gz
gzip $DISTDOCTAR
if test ! -r $DISTDOCTAR.gz
then
    echo ""
    echo "$PGMNAME: Cannot compress into '$DISTDOCTAR.gz'"
    exit 5
fi

chmod 0444 $DISTDOCTAR.gz
cd harmony/bootJVM

chmod -R +w doc.ORIG
mv doc.ORIG doc

DistConfigUnPrep

DistDocUnPrep

DistUnPrep

echo ""
echo "$PGMNAME: Documentation distribution tar file created:"
echo ""
ls -l ../../$DISTDOCTAR.gz
echo ""

###################################################################
#
# Done.
#
echo ""
echo "$PGMNAME: Documentation distribution complete"

#
# EOF
