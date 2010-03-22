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
#ifndef _VM_LOG_H_
#define _VM_LOG_H_

/**
 * @file
 * VM-oriented convenience enhancements to logger.
 */

#include "loggerstring.h"
#include "jni.h"
#include "open/vm.h"
#include "open/vm_method_access.h"

/**
 * The convenience method for logging Class instances.
 */
inline LoggerString& operator<<(LoggerString& log, const Class_Handle clss) {
    if (clss) {
        log << class_get_name(clss); 
    } else {
        log << "<null class>";
    }
    return log;
}

/**
 * The convenience method for logging Method handles.
 */
inline LoggerString& operator<<(LoggerString& log, const Method_Handle m) {
    if (m) {
        log << method_get_class(m) << "."
        << method_get_name(m)
        << method_get_descriptor(m);
    } else {
        log << "<null method>";
    }

    return log;
}

/**
 * The convenience method for logging JNI method IDs.
 */
inline LoggerString& operator<<(LoggerString& log, const jmethodID m) {
    return log << reinterpret_cast<const Method_Handle>(m);
}


/**
 * The convenience method for logging jboolean values.
 */
inline LoggerString& operator<<(LoggerString& log, const jboolean b) {
    if (b == JNI_FALSE) {
        log << "false";
    } else if (b == JNI_TRUE) {
        log << "true";
    } else {
        log << "(jboolean) " << ((unsigned) b);
    }

    return log;
}

#endif // _VM_LOG_H_
