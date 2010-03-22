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

#------------------------------------------------------------------------------
#     Array with all possible group names.

group_name[1]="GetAllModules";
group_name[2]="GetModuleInfo";
group_name[3]="GetAllThreads";
group_name[4]="GetThreadHandle";
group_name[5]="GetThreadInfo";
group_name[6]="SuspendResumeThread";
group_name[7]="TerminateThread";
group_name[8]="ReadMemory";
group_name[9]="GetFrameCount";
group_name[10]="GetStackTrace";
group_name[11]="all";

#------------------------------------------------------------------------------
#     Array with tests devided by groups. Array index corresponds to index in
# group_name array.

#--------------------------------------

group[1]="  GetAllModules01
            GetAllModules02
            GetAllModules03
            GetAllModules01n
            GetAllModules02n
         "
#--------------------------------------

group[2]="  GetModuleInfo01
            GetModuleInfo01n
            GetModuleInfo02n
         "
#--------------------------------------

group[3]="  GetAllThreads01
            GetAllThreads02
            GetAllThreads01n
            GetAllThreads02n
         "
#--------------------------------------

group[4]="  GetThreadHandle01
            GetThreadHandle02
            GetThreadHandle01n
            GetThreadHandle02n
         "
#--------------------------------------


group[5]="  GetThreadInfo01
            GetThreadInfo02
            GetThreadInfo01n
            GetThreadInfo02n
         "
#--------------------------------------

group[6]="  ResumeThread01
            ResumeThread01n
            ResumeThread02n
            ResumeThread03n
            SuspendThread01
            SuspendThread02
            SuspendThread01n
            SuspendThread02n
            SuspendThread03n
         "
#--------------------------------------

group[7]="  TerminateThread01
            TerminateThread02
            TerminateThread01n
            TerminateThread02n
         "
#--------------------------------------

group[8]="  ReadMemory01
            ReadMemory02
            ReadMemory03
            ReadMemory04
         "
#--------------------------------------

group[9]="  GetFrameCount01
            GetFrameCount02
            GetFrameCount03
            GetFrameCount01n
            GetFrameCount02n
            GetFrameCount03n
            GetFrameCount04n
            GetFrameCount05n
            GetFrameCount06n
         "
#--------------------------------------

group[10]=" GetStackTrace01
            GetStackTrace02
            GetStackTrace03
            GetStackTrace04
            GetStackTrace01n
            GetStackTrace02n
            GetStackTrace03n
            GetStackTrace04n
            GetStackTrace05n
            GetStackTrace06n
          "
#--------------------------------------

group[11]="
          "
#--------------------------------------

# vim:ff=unix



