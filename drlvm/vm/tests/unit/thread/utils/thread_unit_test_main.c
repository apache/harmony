/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include <stdio.h>
#include <string.h>
#include "testframe.h"
#include "thread_unit_test_utils.h"

void setup(int argc, char *argv[]) {

    //log_set_level(2);
    log_debug("setup");

    test_java_thread_setup(argc, argv);
}

void teardown(void) {

    log_debug("teardown");

    test_java_thread_teardown(); 
}

int main(int argc, char *argv[]) {

    int res;
    res = default_main(argc, argv);
    //printf("RES = %i\nPress <Enter> for exit\n", res);
    //getchar();

    return res;    
}
