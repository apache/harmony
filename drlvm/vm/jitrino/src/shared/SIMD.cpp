/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, ersion 2.0
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
 * @author Intel, Buqi Cheng$
 *
 */

#include <string.h>
#include "SIMD.h"

namespace Jitrino {

bool
SIMDUtils::isSIMDClass(const char* kname)
{
  return !strncmp (kname + (*kname == 'L'), SIMD_HELPER_PACKAGE_NAME,
                   sizeof (SIMD_HELPER_PACKAGE_NAME) - 1);
}

bool
SIMDUtils::matchType (const char* candidate, const char* typeName)
{
  if (candidate[0] == 'L')
    {
      size_t typeLen = strlen (typeName);
      size_t candLen = strlen (candidate);
      return typeLen + 2 == candLen && !strncmp (typeName, candidate + 1, typeLen);
    }

  return !strcmp (typeName, candidate);
}

Type*
SIMDUtils::convertSIMDType2HIR (TypeManager &tm, const char *name)
{
  if (matchType (name, "com/intel/jvi/I8vec16"))
    return tm.getVectorType (tm.getInt8Type (), 16);
  else if (matchType (name, "com/intel/jvi/I16vec8"))
    return tm.getVectorType (tm.getInt16Type (), 8);
  else if (matchType (name, "com/intel/jvi/I32vec4"))
    return tm.getVectorType (tm.getInt32Type (), 4);
  else if (matchType (name, "com/intel/jvi/I64vec2"))
    return tm.getVectorType (tm.getInt64Type (), 2);
  else if (matchType(name, "com/intel/jvi/F32vec4"))
    return tm.getVectorType (tm.getSingleType (), 4);
  else if (matchType (name, "com/intel/jvi/F64vec2"))
    return tm.getVectorType (tm.getDoubleType (), 2);

  assert (0);

  return NULL;
}

Type*
SIMDUtils::convertSIMDType2HIR (TypeManager &tm, Type *type)
{
  const char *name = type->getName ();
  assert (isSIMDClass (name));

  return convertSIMDType2HIR (tm, name);
}

}//namespace Jitirno
