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

#ifndef _SIMD_JITRINO_H_
#define _SIMD_JITRINO_H_

#include "Opcode.h"

namespace Jitrino {

#ifndef VECTOR_WIDTH
#define VECTOR_WIDTH 16
#endif

#define SIMD_HELPER_PACKAGE_NAME  "com/intel/jvi"

class TypeManager;
class Type;

class SIMDUtils
{
public:
  // checks of class nams is vmmagic class
  // both typenames and descriptor names are supported
  static bool isSIMDClass(const char* kname);

  static Type* convertSIMDType2HIR (TypeManager &tm, const char *name);

  static Type* convertSIMDType2HIR (TypeManager &tm, Type *type);

private:
  static bool matchType (const char* candidate, const char* typeName);
};

} //namespace Jitirno
#endif
