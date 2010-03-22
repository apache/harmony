#! /bin/bash

#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# @author: Valentin Al. Sitnick, Petr Ivanov
#

# STEP 5: set all environment variables needed for test building

echo
echo Test building

if [ ! -d $VTSSUITE_ROOT/bin/classes ]; then
    mkdir -p $VTSSUITE_ROOT/bin/classes
fi

if [ ! -d $VTSSUITE_ROOT/bin/lib ]; then
    mkdir -p $VTSSUITE_ROOT/bin/lib
fi

JAVAC_OPTS=" -g -source 1.4 -target 1.4 "

if [ $system = "Linux" ]; then
    CXX=g++
    CXXOPTIONS="-shared -fPIC"
    TARGET_OS=LINUX
    TARGET_OS_1=PLATFORM_POSIX
    TARGET_OS_2=POSIX

    JAVAC_OPTS=$JAVAC_OPTS" -classpath $VTSSUITE_ROOT/bin/classes "
    JAVAC_OPTS=$JAVAC_OPTS" -d $VTSSUITE_ROOT/bin/classes "
else
    JAVAC_OPTS=$JAVAC_OPTS" -classpath `cygpath -w $VTSSUITE_ROOT/bin/classes`"
    JAVAC_OPTS=$JAVAC_OPTS" -d `cygpath -w $VTSSUITE_ROOT/bin/classes` "

    source ms_envs.sh
fi

# STEP 6.1: compile special java files
#echo "Compiling special java-files... "

#if [ $system = "Linux" ]; then
#    $REF_JAVA_HOME/bin/javac $JAVAC_OPTS $VTSSUITE_ROOT/share/*.java
#else

#    $REF_JAVA_HOME/bin/javac.exe $JAVAC_OPTS `cygpath -w $VTSSUITE_ROOT/share/*.java`
#fi

# STEP 6.2: build test (compile java file(s) and creade native
#           lib (dll/so) for each test)
for tst in $testname; do

    echo " "

    ${FIND} $VTSSUITE_ROOT -type d -name $tst | while read line;
    do
        if [ $system = "Linux" ]; then               ####Linux
            if [ -e "$line/${tst}.java" ] ; then
                echo "Compiling file ${tst}.java"
                $REF_JAVA_HOME/bin/javac $JAVAC_OPTS $line/${tst}.java
            else
                echo "Java file for this test is not found."
            fi

            if [ -e "$line/${tst}.cpp" ] ; then
                echo "Compiling file ${tst}.cpp ---> $C_COMPILER"

                $C_COMPILER -O1 -g -Wall \
                    -I$VTSSUITE_ROOT/share \
                    -I$TST_JAVA_INCLUDE \
                    -D$TARGET_OS -D$TARGET_OS_1 -D$TARGET_OS_2 $CXXOPTIONS \
                    -o $VTSSUITE_ROOT/bin/lib/lib${tst}.so \
                    $VTSSUITE_ROOT/share/*.cpp $line/*.cpp
            fi
        else                                       ####Windows
            if [ -e "$line/${tst}.java" ] ; then
                echo "Compiling file ${tst}.java"
                $REF_JAVA_HOME/bin/javac.exe $JAVAC_OPTS \
                    `cygpath -w $line/${tst}.java`
            else
                echo "Java file for this test is not found."
            fi

            if [ -e "$line/${tst}.cpp" ] ; then

                echo "Compiling file ${tst}.cpp ---> $C_COMPILER "

                $C_COMPILER /nologo /W3 \
                    /I`cygpath -w $VTSSUITE_ROOT/share` \
                    /I`cygpath -w $TST_JAVA_INCLUDE` \
                    /Fo`cygpath -w $VTSSUITE_ROOT/bin/lib/` \
                    /Fe`cygpath -w $VTSSUITE_ROOT/bin/lib/${tst}` \
                    /LDd /Zi `cygpath -w $line/*.cpp` \
                    `cygpath -w \
                        $VTSSUITE_ROOT/share/events.cpp` \
                    `cygpath -w \
                        $VTSSUITE_ROOT/share/utils.cpp`
            fi
        fi
    done
done
