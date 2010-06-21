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

#include "VMMagic.h"
#include <string.h>

namespace Jitrino {

bool VMMagicUtils::isVMMagicClass(const char* kname) {
    static const unsigned magicPackageLen = sizeof(VMMAGIC_UNBOXED_PACKAGE_NAME)-1;
    bool res = false;
    if (*kname=='L') {
        res = !strncmp(kname+1, VMMAGIC_UNBOXED_PACKAGE_NAME, magicPackageLen);
    } else {
        res = !strncmp(kname, VMMAGIC_UNBOXED_PACKAGE_NAME, magicPackageLen);
    }
    return res;
}

}//namespace Jitirno
