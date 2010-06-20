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
 * @file org_apache_harmony_lang_management_GarbageCollectorMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.GarbageCollectorMXBeanImpl class.
 */

#include <cxxlog.h>
#include <open/gc.h>
#include "org_apache_harmony_lang_management_GarbageCollectorMXBeanImpl.h"

/*
 * Method: org.apache.harmony.lang.management.GarbageCollectorMXBeanImpl.getCollectionCountImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_GarbageCollectorMXBeanImpl_getCollectionCountImpl(JNIEnv *, jobject)
{
    TRACE2("management","GarbageCollectorMXBeanImpl_getCollectionCountImpl invocation");
    return gc_get_collection_count();
};

/*
 * Method: org.apache.harmony.lang.management.GarbageCollectorMXBeanImpl.getCollectionTimeImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_GarbageCollectorMXBeanImpl_getCollectionTimeImpl(JNIEnv *, jobject)
{
    TRACE2("management","GarbageCollectorMXBeanImpl_getCollectionTimeImpl invocation");
    return gc_get_collection_time()/1000;
};


