#!/bin/sh
#!
# @file ./dox.sh
#
# @brief Create project documentation with @b Doxygen.
#
# Generate @b Doxygen documentation from source code containing
# appropriate documentation tags.  Customize the output to
# to eliminate unwanted warnings.
#
# @note Using GENERATE_RTF=YES seems to cause the spurious
#       message, "QGDict::hashAsciiKey: Invalid null key"
#       on otherwise working and valid documentation.
#       The reasons are not yet known, and it may be that
#       the directive file needs some different settings.
#
# @todo HARMONY-6-dox.sh-1 Identify reason for spurious message on
#       RTF output as described above.
#
# @todo HARMONY-6-dox.sh-2 A proper solution to old NetScape 4.7X
#       processing the HTML tag <b>&lt;pre class="fragment"&gt;</b>
#       syntax needs to be found and implemented so that
#       <b>&lt;code&gt;</b> and <b>@@verbatim</b> fragments, etc.,
#       may be displayed with proper framing @e and with proper
#       newlines and white space.  For the interim solution, see
#       the end of this script.  The problem will probably be
#       solved by adjusting the <b>doxygen.css</b> (the default
#       @b .css file) or supplying one that is customized for
#       the project or perhaps just for old NetScape browsers.
#       The scripts @link ./doxpatch.sh doxpatch.sh@endlink and
#       @link ./doxunpatch.sh doxunpatch.sh@endlink that support
#       the interim solution might also be used in support of the
#       proper solution if an "original" and a "patched" version of
#       the style sheet exists in the HTML output directory.
#       The alternative is to have an original and patched version
#       of the HTML pages, which might be preferable, depending on
#       the actual visual artifacts.  In this case, a patch/unpatch
#       script might either not be such a good idea or would be a bit
#       more complex to implement.  In any event, the correct approach
#       is to find the HTML problem and fix it, probably in the
#       style sheet.
#
# @todo HARMONY-6-dox.sh-3 Perhaps the above to-do item should be
#       generalized for the old NetScape 4.7X browser since is
#       seems to have some overall problems with fonts in the
#       default <b>doxygen.css</b> style sheet.  Perhaps someone with
#       CSS experience could contribute one or more style sheets
#       for use with different types of browsers so the HTML
#       documents look the same on all of them.
#
# @todo HARMONY-6-dox.sh-4 Consider the creation of a <b>.css</b>
#       file that is customized especially for this project.
#
# @todo HARMONY-6-dox.sh-1 A Windows .BAT version of this
#       script needs to be written
#
# @bug HARMONY-6-dox.sh-1001 If the token
#      sequence "@b @c word1 word2..." is used, two words
#      get bolded instead of one (Doxygen 1.4.4).  Likewise
#      with "@b @p word1 word2..."  By reversing the sequence,
#      only one word gets bolded.  Is this a "bug" or a "feature"?
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
########################################################################
#
# Script setup
#
. commondox.sh
. config/config_build_steps.sh

########################################################################
#
# Verify that Doxygen program exists
#
HERE=`pwd`
if test -n "$TMPDIR"
then
    if test -d $TMPDIR/.
    then
        cd $TMPDIR
    else
        echo "$0:  'TMPDIR' env var is not a directory: $TMPDIR"
        exit 1
    fi
else
    if test -d /tmp
    then
        cd /tmp
    else
        echo "$0:  Need either TMPDIR directory (env var) or '/tmp'"
        exit 2
    fi
fi

# Text Doxygen program itself
doxygen -g tmp.$$.Doxyfile > /dev/null
rc=$?
rm -f tmp.$$.Doxyfile

if test $rc -ne 0
then
    echo "doxygen:  File not found" 1>&2
    exit 3
fi

cd $HERE

########################################################################
#
# Extract some basic values from top-level configuration header file.
# Specifically, CONFIG_WORDWIDTH{32,64}, ARCH_BIG_ENDIAN,
# ARCH_LITTLE_ENDIAN, ARCH_ODD4_ADDRESS_SIGSEGV.
#
CONFIG_HEADER=config/config.h
#
UNABLE2RESOLVE="unable to resolve link to \\\`"
DOC4UNKNOWN="documentation for unknown define"
#
WORDWIDTH_FILTER=
grep WORDWIDTH32 $CONFIG_HEADER > /dev/null
rc1=$?
grep WORDWIDTH64 $CONFIG_HEADER > /dev/null
rc2=$?
if test 0 -eq $rc1 -a 0 -eq $rc2
then
    : # Something wierd in configuration, don't filter it.
else
    if test 1 -eq $rc1 -a 1 -eq $rc2
    then
        : # Something wierd in configuration, nothing to filter.
    else
        if test 0 -eq $rc1
        then
            WORDWIDTH_FILTER="${UNABLE2RESOLVE}#CONFIG_WORDWIDTH64"
        else
            WORDWIDTH_FILTER="${UNABLE2RESOLVE}#CONFIG_WORDWIDTH32"
        fi
    fi
fi

#
ENDIAN_LITTLE="$DOC4UNKNOWN ARCH_LITTLE_ENDIAN|${UNABLE2RESOLVE}#ARCH_LITTLE_ENDIAN"
ENDIAN_BIG="$DOC4UNKNOWN ARCH_BIG_ENDIAN|${UNABLE2RESOLVE}#ARCH_BIG_ENDIAN"
ENDIAN_FILTER="${ENDIAN_LITTLE}|${ENDIAN_BIG}"

########################################################################
#
# Warnings about preprocessor symbols that are present only when
# 'config/config.h' or 'jvm/src/arch.h' generate them.
#
ODD1="$DOC4UNKNOWN ARCH_ODD2_ADDRESS_SIGSEGV"
ODD2="${UNABLE2RESOLVE}#ARCH_ODD2_ADDRESS_SIGSEGV"
ODD3="$DOC4UNKNOWN ARCH_ODD4_ADDRESS_SIGSEGV"
ODD4="${UNABLE2RESOLVE}#ARCH_ODD4_ADDRESS_SIGSEGV"
ODD_FILTER="$ODD1|$ODD2|$ODD3|$ODD4"

GC1="${UNABLE2RESOLVE}#CONFIG_GC_TYPE_STUB"
GC2="${UNABLE2RESOLVE}#CONFIG_GC_TYPE_OTHER"
GC_FILTER="$GC1|$GC2"

HEAP1="${UNABLE2RESOLVE}#CONFIG_HEAP_TYPE_SIMPLE"
HEAP2="${UNABLE2RESOLVE}#CONFIG_HEAP_TYPE_BIMODAL"
HEAP3="${UNABLE2RESOLVE}#CONFIG_HEAP_TYPE_OTHER"
HEAPDF="${UNABLE2RESOLVE}#HEAP_DATA_FREE"
HEAPDG="${UNABLE2RESOLVE}#HEAP_DATA_GET"
HEAPMF="${UNABLE2RESOLVE}#HEAP_METHOD_GET"
HEAPMG="${UNABLE2RESOLVE}#HEAP_METHOD_FREE"
HEAPSF="${UNABLE2RESOLVE}#HEAP_STACK_FREE"
HEAPSG="${UNABLE2RESOLVE}#HEAP_STACK_GET"

HEAP_FILTER1="$HEAP1|$HEAP2|$HEAP3"
HEAP_FILTER2="$HEAPDF|$HEAPDG|$HEAPMF|$HEAPMG|$HEAPSF|$HEAPSG"

HEAP_FILTER="$HEAP_FILTER1|$HEAP_FILTER2"


CPP_FILTER="$ODD_FILTER|$GC_FILTER|$HEAP_FILTER"

########################################################################
#
# Mysterious warnings that I dont want to take the time to figure out
# for whatever reason.
#
# MYSTERY1 comes and goes with @c directives in 'classfile.h' and shows
# up in some source files that include it.
#

MYSTERY1="Warning: Illegal command \\\xrefitem as the argument of a \\\c command"

MYSTERY_FILTER="$MYSTERY1"

########################################################################
#
# Unnecessary warnings about specific files.
#

SPECIFICM1="Warning: file jvm/src/main.c already documented. Skipping documentation."

SPECIFIC_FILTER="$SPECIFICM1"

########################################################################
#
# Unnecessary warnings about shell scripts when INPUT_FILTER= has
# been commented out so as to not properly parse them, yet to
# speed up run time when working with just the source code, not
# with shell scripts also.
#

SHELL1="Warning: Found ';' while parsing initializer list! \(Doxygen could be confused by a macro call without semicolon\)"

SHELL_FILTER="$SHELL1"

########################################################################
#
# Clean up _all_ former output.  This is important to clear out stale
# results during development and when changing the Doxygen 
# configuration file.
#
# With the exception of effects of changing the configuration file,
# this cleanup step really _could_ be done under the covers by Doxygen
# by virtue ofthe fact that new output files overwrite old output files,
# but is done here anyway for the sake of being thorough and only
# generating _exactly_ what the current contents of the configuration
# file say should be generated:
#
if test -z "$SUPPRESS_DOXYGEN_VERYCLEAN"
then
    #
    # Typically only invoked the _very_ first time through.
    #
    if test ! -d $OUTPUT_DIRECTORY
    then
        echo ""
        echo "$0: Set environment variable SUPPRESS_DOXYGEN_VERYCLEAN"
        echo "    to be non-null in order to not remove all of the"
        echo "    documentation output files every time and thus run"
        echo "    faster at the expense of file roster precision due"
        echo "    to file add/delete or Doxygen directive file changes."
        echo ""
    fi

    $RMALL_CMD
fi

########################################################################
#
# Construct output directory, clean up its subdirectories for results
#
if test ! -d $OUTPUT_DIRECTORY
then
    mkdir $OUTPUT_DIRECTORY
fi

for d in $HTML_OUTPUT $LATEX_OUTPUT $RTF_OUTPUT $MAN_OUTPUT $XML_OUTPUT
do
    if test ! -d $OUTPUT_DIRECTORY/$d
    then
        mkdir $OUTPUT_DIRECTORY/$d
    fi
done

########################################################################
#
# Construct complete filter list-- DO NOT wrap this filter definition!
#
DOXYGEN_FILTER1="$WORDWIDTH_FILTER|$ENDIAN_FILTER|$CPP_FILTER"
DOXYGEN_FILTER2="$SPECIFIC_FILTER|$MYSTERY_FILTER|$SHELL_FILTER"
DOXYGEN_FILTER="$DOXYGEN_FILTER1|$DOXYGEN_FILTER2"

########################################################################
#
# Make it happen.  If so configured, adjust <pre class="fragment"> HTML
# tags for NetScape 4.7x so that they effectively ignore the tag.  If
# 4.7x processes them as they stand, there will be no newline characters
# displayed and all white space is condensed into one character in
# these fragments.
#

SED_IN_PATTERN="^PRE.fragment"
SED_OUT_PATTERN="PRE.fragment_PATCHED_BY_DOX_SH"

if test "YES" != "$CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X"
then
    doxygen bootjvm.dox 2>&1 | egrep -v "${DOXYGEN_FILTER}"
else
    # Only meaningful if "GENERATE_HTML=YES" in directive file

    if test -d $OUTPUT_DIRECTORY/$HTML_OUTPUT
    then
        rm -f $CSS_FILE_ORIG
        rm -f $CSS_FILE_PATCHED

        rm -f $CSS_FILE

        doxygen bootjvm.dox 2>&1 | egrep -v "${DOXYGEN_FILTER}"

        mv $CSS_FILE $CSS_FILE_ORIG

        sed "s/$SED_IN_PATTERN/$SED_OUT_PATTERN/" $CSS_FILE_ORIG > \
                                                       $CSS_FILE_PATCHED

        rm -f $CSS_FILE
        ln -s $CSS_FILE_PATCHED_NAME $CSS_FILE
    fi
fi

########################################################################
#
# EOF
