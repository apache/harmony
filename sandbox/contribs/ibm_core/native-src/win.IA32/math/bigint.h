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

#if !defined(bigint_h)
#define bigint_h

jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_subImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src1, jlongArray src2));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_divImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray topObject,
	    jlongArray bottomObject));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_mulImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src1, jlongArray src2));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_negImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_addImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src1, jlongArray src2));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_remImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray topObject,
	    jlongArray bottomObject));
jlongArray JNICALL Java_com_ibm_oti_util_math_BigInteger_shlImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src, jint shiftval));
jint JNICALL Java_com_ibm_oti_util_math_BigInteger_compImpl
PROTOTYPE ((JNIEnv * env, jclass cls, jlongArray src1, jlongArray src2));

#endif /* bigint_h */
