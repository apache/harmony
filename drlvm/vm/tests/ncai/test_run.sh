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

if [ $system = "Linux" ]; then
    export CLASSPATH="$VTSSUITE_ROOT/bin/classes"
#    cp_options="-cp $VTSSUITE_ROOT/bin/classes -Djava.library.path=$VTSSUITE_ROOT/bin/lib"
else
    export CLASSPATH=`cygpath -m $VTSSUITE_ROOT/bin/classes`
#    cp_options="-cp `cygpath -w $VTSSUITE_ROOT/bin/classes` -Djava.library.path=`cygpath -w $VTSSUITE_ROOT/bin/lib`"
fi

echo
echo "Test running..."

#CUSTOM_PROPS=-Djava.bla_bla_bla.property="bla_bla_bla_bla_bla"

for tst in $testname; do

    ${FIND} $VTSSUITE_ROOT/bin/classes -name "${tst}.class" | while read line;
    do
        AGENT="-agentlib:${tst}"
        CLASS="ncai.funcs.${tst}"

        echo $TST_JAVA $cp_options $AGENT $CLASS
        $TST_JAVA $cp_options $AGENT $CLASS

    done
done

