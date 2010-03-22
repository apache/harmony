/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Pavel N. Vyssotski
 */

/**
 * @file
 * jdwpTypes.h
 *
 */

#ifndef _JDWP_TYPES_H_
#define _JDWP_TYPES_H_

#include "jni.h"
#include "jdwp.h"

// Defined to parse bytes order for x86 platform
#define IS_BIG_ENDIAN_PLATFORM 1

namespace jdwp {

    typedef jlong FieldID;
    typedef jlong MethodID;
    typedef jlong ObjectID;
    typedef ObjectID ReferenceTypeID;
    typedef jlong FrameID;

    const size_t FIELD_ID_SIZE          = sizeof(FieldID);
    const size_t METHOD_ID_SIZE         = sizeof(MethodID);
    const size_t OBJECT_ID_SIZE         = sizeof(ObjectID);
    const size_t REFERENCE_TYPE_ID_SIZE = sizeof(ReferenceTypeID);
    const size_t FRAME_ID_SIZE          = sizeof(FrameID);

    typedef jint PacketID;
    typedef jint RequestID;

    /**
     * The structure containing the Java value with the associated JDWP tag.
     */
    struct jdwpTaggedValue {
        jdwpTag tag;
        jvalue value;
    };

    /**
     * The structure containing all JDWP capabilities.
     * One bit per capability.
     */
    struct jdwpCapabilities {
        unsigned int canWatchFieldModification : 1;
        unsigned int canWatchFieldAccess : 1;
        unsigned int canGetBytecodes : 1;
        unsigned int canGetSyntheticAttribute : 1;
        unsigned int canGetOwnedMonitorInfo : 1;
        unsigned int canGetCurrentContendedMonitor : 1;
        unsigned int canGetMonitorInfo : 1;
        unsigned int canRedefineClasses : 1;
        unsigned int canAddMethod : 1;
        unsigned int canUnrestrictedlyRedefineClasses : 1;
        unsigned int canPopFrames : 1;
        unsigned int canUseInstanceFilters : 1;
        unsigned int canGetSourceDebugExtension : 1;
        unsigned int canRequestVMDeathEvent : 1;
        unsigned int canSetDefaultStratum : 1;
        unsigned int : 17;
    };

    /**
     * The structure containing JDWP location information.
     */
    struct jdwpLocation {
        jdwpTypeTag typeTag;
        jclass classID;
        jmethodID methodID;
        jlocation loc;
    };

    /**
     * Representation of the null value for the jobject JNI type and types
     * derived from the jobject, jclass and so on.
     */
    #define JOBJECT_NULL 0

    /**
     * Access flags for class, field, and method
     * as defined in the JVM specification.
     */
    enum {
        ACC_PUBLIC       = 0x0001,
        ACC_PRIVATE      = 0x0002,
        ACC_PROTECTED    = 0x0004,
        ACC_STATIC       = 0x0008,
        ACC_FINAL        = 0x0010,
        ACC_SYNCHRONIZED = 0x0020,
        ACC_SUPER        = 0x0020,
        ACC_VOLATILE     = 0x0040,
        ACC_TRANSIENT    = 0x0080,
        ACC_VARARGS      = 0x0080,
        ACC_NATIVE       = 0x0100,
        ACC_INTERFACE    = 0x0200,
        ACC_ABSTRACT     = 0x0400,
        ACC_STRICT       = 0x0800,
        ACC_SYNTHETIC    = 0x1000,
        ACC_ANNOTATION   = 0x2000,
        ACC_ENUM         = 0x4000
    };

}

#endif // _JDWP_TYPES_H_
