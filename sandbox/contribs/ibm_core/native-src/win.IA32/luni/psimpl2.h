/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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

#if !defined(psimpl2_h)
#define psimpl2_h
void JNICALL
Java_java_net_PlainSocketImpl2_connectStreamWithTimeoutSocketImpl2 (JNIEnv *
								    env,
								    jclass
								    thisClz,
								    jobject
								    fileDescriptor,
								    jint
								    remotePort,
								    jint
								    timeout,
								    jint
								    trafficClass,
								    jobject
								    inetAddress);
void JNICALL Java_java_net_PlainSocketImpl2_socketBindImpl2 (JNIEnv * env,
							     jclass thisClz,
							     jobject
							     fileDescriptor,
							     jint localPort,
							     jobject
							     inetAddress);
void JNICALL Java_java_net_PlainSocketImpl2_createStreamSocketImpl2 (JNIEnv *
								     env,
								     jclass
								     thisClz,
								     jobject
								     thisObjFD,
								     jboolean
								     preferIPv4Stack);
void JNICALL Java_java_net_PlainSocketImpl2_connectStreamSocketImpl2 (JNIEnv *
								      env,
								      jclass
								      thisClz,
								      jobject
								      fileDescriptor,
								      jint
								      remotePort,
								      jint
								      trafficClass,
								      jobject
								      inetAddress);
jint JNICALL Java_java_net_PlainSocketImpl2_sendDatagramImpl2 (JNIEnv * env,
							       jclass thisClz,
							       jobject
							       fileDescriptor,
							       jbyteArray
							       data,
							       jint offset,
							       jint msgLength,
							       jint
							       targetPort,
							       jobject
							       inetAddress);
#endif /* psimpl2_h */
