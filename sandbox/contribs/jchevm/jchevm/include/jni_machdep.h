
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id$
 */

#ifndef _JNI_MACHDEP_H_
#define _JNI_MACHDEP_H_

/*
 * Goal: define JNI C typedefs for Java primitive types.
 */

#if (defined (__ia64__) || defined (__alpha__) || defined (__i386__) || defined(__sparc__)) || defined(__ppc__)

typedef	unsigned char	jboolean;
typedef	signed char	jbyte;
typedef	unsigned short	jchar;
typedef	signed short	jshort;
typedef	signed int	jint;
typedef	long long	jlong;
typedef float		jfloat;
typedef double		jdouble;

#else
#error "Unsupported architecture"
#endif

#endif	/* _JNI_MACHDEP_H_ */
