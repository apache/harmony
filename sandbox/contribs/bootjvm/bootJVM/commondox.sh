#!/bin/sh
#!
# @file ./commondox.sh
#
# @brief Common code for @link ./dox.sh dox.sh@endlink and
# @link ./undox.sh undox.sh@endlink.
#
# This script is common to @link ./dox.sh dox.sh@endlink and
# @link ./undox.sh undox.sh@endlink and is not designed to do
# anything on its own.
#
#
# @todo  A Windows .BAT version of this script needs to be written
#
#
# @section Control
#
# \$URL: https://svn.apache.org/path/name/commondox.sh $ \$Id: commondox.sh 0 09/28/2005 dlydick $
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
# @version \$LastChangedRevision: 0 $
#
# @date \$LastChangedDate: 09/28/2005 $
#
# @author \$LastChangedBy: dlydick $
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
########################################################################
#
# Doxygen configuration file.  Parse out keywords from this file.
#
DOXYFILE=bootjvm.dox

########################################################################
#
# Parse out the target output directory and its subdirectories for
# various output styles.  Use as path name $OUTPUT_DIRECTORY/$xxx_OUTPUT
#
CUT="cut -d= -f2 | sed 's/ //g'"

OUTPUT_DIRECTORY=`grep "^OUTPUT_DIRECTORY" $DOXYFILE | eval $CUT`

# Subdirectories
HTML_OUTPUT=`grep "^HTML_OUTPUT" $DOXYFILE | eval $CUT`
LATEX_OUTPUT=`grep "^LATEX_OUTPUT" $DOXYFILE | eval $CUT`
RTF_OUTPUT=`grep "^RTF_OUTPUT" $DOXYFILE | eval $CUT`
MAN_OUTPUT=`grep "^MAN_OUTPUT" $DOXYFILE | eval $CUT`
XML_OUTPUT=`grep "^XML_OUTPUT" $DOXYFILE | eval $CUT`

# Default values
if test -z "$OUTPUT_DIRECTORY"; then OUTPUT_DIRECTORY=doc; fi
if test -z "$HTML_OUTPUT";      then HTML_OUTPUT=html;     fi
if test -z "$LATEX_OUTPUT";     then LATEX_OUTPUT=latex;   fi
if test -z "$RTF_OUTPUT";       then RTF_OUTPUT=rtf;       fi
if test -z "$MAN_OUTPUT";       then MAN_OUTPUT=man;       fi
if test -z "$XML_OUTPUT";       then XML_OUTPUT=xml;       fi

########################################################################
#
# Remove output directory, clean up its subdirectories for results
#
# Each component is listed separately.  If there is _anything_ else
# in this area, then the final 'rmdir' will fail.  Such an additional
# item needs to be accounted for in these scripts and not just
# arbitrarily stored into this area, which should _alway_ remain
# _completely_ auto-generated.
#
RMALL_CMD="\
rm -rf $OUTPUT_DIRECTORY/$HTML_OUTPUT \
       $OUTPUT_DIRECTORY/$LATEX_OUTPUT \
       $OUTPUT_DIRECTORY/$RTF_OUTPUT \
       $OUTPUT_DIRECTORY/$MAN_OUTPUT \
       $OUTPUT_DIRECTORY/$XML_OUTPUT; \
rmdir  $OUTPUT_DIRECTORY"

########################################################################
#
# Default Doxygen cascadiing style sheet file, plus patched versions.
#
CSS_FILE_NAME=doxygen.css
CSS_FILE_ORIG_NAME=$CSS_FILE_NAME.ORIG
CSS_FILE_PATCHED_NAME=$CSS_FILE_NAME.PATCHED_BY_DOX_SH

CSS_FILE=$OUTPUT_DIRECTORY/$HTML_OUTPUT/$CSS_FILE_NAME
CSS_FILE_ORIG=$OUTPUT_DIRECTORY/$HTML_OUTPUT/$CSS_FILE_ORIG_NAME
CSS_FILE_PATCHED=$OUTPUT_DIRECTORY/$HTML_OUTPUT/$CSS_FILE_PATCHED_NAME

#
# EOF
