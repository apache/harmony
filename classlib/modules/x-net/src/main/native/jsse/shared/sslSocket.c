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

#include "sslSocket.h"
#include <stdio.h>
#include "jni.h"
#include "hysock.h"
#include "openssl/bio.h"
#include "openssl/ssl.h"
#include "openssl/err.h"
#include "errno.h"

jlong getFD(JNIEnv * env, jobject fd) {
    jclass descriptorCLS;
    jfieldID descriptorFID;
    hysocket_t hysocketP;

    //TODO add to cache
    descriptorCLS = (*env)->FindClass (env, "java/io/FileDescriptor");
    if (NULL == descriptorCLS){
        return 0;
    }

    descriptorFID = (*env)->GetFieldID (env, descriptorCLS, "descriptor", "J");
    if (NULL == descriptorFID){
        return 0;
    }

    hysocketP = (hysocket_t) ((IDATA)((*env)->GetLongField (env, fd, descriptorFID)));
    if (NULL == hysocketP) {
        return 0;
    }

#if defined(WIN32) || defined(WIN64)
    if (hysocketP->flags & SOCKET_IPV4_OPEN_MASK) {
        return (jlong)(hysocketP->ipv4);
    } else if (hysocketP->flags & SOCKET_IPV6_OPEN_MASK) {
        return (jlong)(hysocketP->ipv6);
    } else {
        return 0;
    }
#else 
    return (jlong)(hysocketP->sock);
#endif
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_initImpl
  (JNIEnv *env, jclass clazz, jlong context) {
    return addr2jlong(SSL_new(jlong2addr(SSL_CTX, context)));
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_sslAcceptImpl
  (JNIEnv *env, jclass clazz, jlong jssl, jobject fd) {
    jlong socket = getFD(env, fd);
    SSL *ssl = jlong2addr(SSL, jssl);
    BIO *bio;
    int ret;
 
    bio = BIO_new_socket((int)socket, BIO_NOCLOSE);
    SSL_set_bio(ssl, bio, bio);

    SSL_set_mode(ssl, SSL_MODE_AUTO_RETRY);

    // Put our SSL into accept state
    SSL_set_accept_state(ssl);

    // Start the server handshake
    ret = SSL_do_handshake(ssl);
    if (ret<=0) {
        jclass exception = (*env)->FindClass(env, "javax/net/ssl/SSLHandshakeException");
        (*env)->ThrowNew(env, exception, ERR_reason_error_string(ERR_get_error())); 
    }
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_sslConnectImpl
  (JNIEnv *env, jclass clazz, jlong jssl, jobject fd) {
    jlong socket = getFD(env, fd);
    SSL *ssl = jlong2addr(SSL, jssl);
    BIO *bio;
    int ret;

    bio = BIO_new_socket((int)socket, BIO_NOCLOSE);
    SSL_set_bio(ssl, bio, bio);
    
    SSL_set_mode(ssl, SSL_MODE_AUTO_RETRY);

    // Put our SSL into connect state
    SSL_set_connect_state(ssl);

    // Start the client handshake
    ret = SSL_do_handshake(ssl);
    if (ret<=0) {
        jclass exception = (*env)->FindClass(env, "javax/net/ssl/SSLHandshakeException");
        (*env)->ThrowNew(env, exception, ERR_reason_error_string(ERR_get_error()));
    }
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_writeAppDataImpl
  (JNIEnv *env, jclass clazz, jlong jssl, jbyteArray data, jint offset, jint len) {
    SSL *ssl = jlong2addr(SSL, jssl);
    int ret;

    jbyte *buffer = (jbyte*) malloc(len * sizeof(jbyte*)); 
    (*env)->GetByteArrayRegion(env, data, offset, len, buffer);

    ret = SSL_write(ssl, (const void *)buffer, (int)len);
    // Check len bytes were written to the socket and loop if not
    if (ret == -1) {
        // The socket has been closed
        jclass exception = (*env)->FindClass(env, "java/net/SocketException");
        (*env)->ThrowNew(env, exception, "Connection was reset");
    }

    free(buffer);
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_readAppDataImpl
  (JNIEnv *env, jclass clazz, jlong jssl, jbyteArray data, jint offset, jint len) {
    SSL *ssl = jlong2addr(SSL, jssl);
    int ret;

    jbyte *buffer = (jbyte*) malloc(len * sizeof(jbyte*));

    ret = SSL_read(ssl, (void *)buffer, (int)len);
    if (ret == -1) {
        // The socket has been closed
        jclass exception = (*env)->FindClass(env, "java/net/SocketException");
        (*env)->ThrowNew(env, exception, "Connection was reset");
        return -1;
    }
    
    (*env)->SetByteArrayRegion(env, data, offset, ret, buffer);
    free(buffer);
    return ret;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSocketImpl_closeImpl
  (JNIEnv *env, jclass clazz, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);

    // The SSLSocket is being closed, so shutdown and free our SSL
    SSL_shutdown(ssl);
    SSL_free(ssl);
}

