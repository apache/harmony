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
/**
 * @author Andrey Yakushev
 */

/**
 * @file org_apache_harmony_lang_management_MemoryNotificationThread.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.MemoryNotificationThread class.
 */

#include <cxxlog.h>
#include "org_apache_harmony_lang_management_MemoryNotificationThread.h"

/* Native methods */

/*
 * Method: org.apache.harmony.lang.management.MemoryNotificationThread.processNotificationLoop(I)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_MemoryNotificationThread_processNotificationLoop(JNIEnv *, jobject,
                                                                                         jint)
{
    // TODO implement this method stub correctly
    TRACE2("management","processNotificationLoop stub invocation");
};

