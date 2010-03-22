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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef _VMMAGIC_JITRINO_H_
#define _VMMAGIC_JITRINO_H_
namespace Jitrino {

#define VMHELPER_PACKAGE_NAME           "org/apache/harmony/drlvm/"
#define VMMAGIC_UNBOXED_PACKAGE_NAME    "org/vmmagic/unboxed/"

#define VMHELPER_TYPE_NAME              "org/apache/harmony/drlvm/VMHelper"
#define PRAGMA_INLINE_TYPE_NAME         "org/vmmagic/pragma/Inline"

class VMMagicUtils {
public:
    // checks of class nams is vmmagic class
    // both typenames and descriptor names are supported
    static bool isVMMagicClass(const char* kname);
};

} //namespace Jitirno
#endif


