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

#include <windows.h>
#include "hycomp.h"
extern U_8 *lockFixupBegin;
extern U_8 *lockFixupEnd;
void
fixupLocks386 (void)
{

  SYSTEM_INFO aSysInfo;

  GetSystemInfo (&aSysInfo);
  if (aSysInfo.dwNumberOfProcessors == 1)
    {
      U_8 **fixup;
      DWORD oldProtect, ignored;
      U_8 *min = (U_8 *) - 1, *max = NULL;

      for (fixup = &lockFixupBegin; fixup < &lockFixupEnd; fixup++)
        {
          if (*fixup < min)
            min = *fixup;
          if (*fixup > max)
            max = *fixup;
        }
      if (VirtualProtect
          (min, max - min, PAGE_EXECUTE_READWRITE, &oldProtect))
        {
          for (fixup = &lockFixupBegin; fixup < &lockFixupEnd; fixup++)
            {
              **fixup = 0x90;   /* 0x90 == nop */
            }
          VirtualProtect (min, max - min, oldProtect, &ignored);
        }

    }
}
