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

#pragma map(sysTranslate, "SYSXLATE")
#include <builtins.h>

char* 
sysTranslate(const char *source, int length, char *trtable, char* xlate_buf) { 
   int i = 0; 

   memcpy(xlate_buf, source, length);  /* copy source to target */

#if __COMPILER_VER >= 0x41050000
   /* If the compiler level supports the fast builtin for translation, use it 
      on the first n*256 bytes of the string
    */
   for (; length > 255; i += 256, length -=256) { 
       __tr(xlate_buf+i, trtable, 255); 
          
   }
#endif
   
   for (; length > 0; i++, length--) {      /* translate */
      xlate_buf[i] = trtable[source[i]]; 
   }

   xlate_buf[i] = '\0';                /* null terminate */
   return xlate_buf; 
}
