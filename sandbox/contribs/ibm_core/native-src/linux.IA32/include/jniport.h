/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if !defined(jniport_h)
#define jniport_h

#if defined(WIN32)||(defined(_WIN32))
#define JNIEXPORT __declspec(dllexport)
#define JNICALL __stdcall

typedef signed char jbyte;
typedef int jint;
typedef __int64 jlong;

#else

#define JNIEXPORT
typedef signed char jbyte;
typedef long long jlong;
typedef int jint;

#endif /* WIN32 */

#if !defined(JNICALL)
#define JNICALL
#endif

#if !defined(JNIEXPORT)
#define JNIEXPORT
#endif

#endif /* jniport_h */
