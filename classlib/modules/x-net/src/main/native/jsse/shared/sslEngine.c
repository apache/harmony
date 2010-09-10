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

#include "sslEngine.h"

#include <stdio.h>
#include "jni.h"
#include "hysock.h"
#include "openssl/bio.h"
#include "openssl/ssl.h"
#include "openssl/err.h"

static jobject handshake_need_wrap, handshake_need_unwrap, handshake_finished, handshake_not_handshaking;
static jobject engine_buffer_overflow, engine_buffer_underflow, engine_closed, engine_ok;
static int initialised = 0;

void init(JNIEnv *env) {
    jclass class;
    jfieldID fieldID;
    if (initialised == 1) {
      return;
    }
    // initialise handshake status enum
    class = (*env)->FindClass(env, "javax/net/ssl/SSLEngineResult$HandshakeStatus");
    fieldID = (*env)->GetStaticFieldID(env, class, "NEED_WRAP", "Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;");
    handshake_need_wrap = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "NEED_UNWRAP", "Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;");
    handshake_need_unwrap = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "FINISHED", "Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;");
    handshake_finished = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "NOT_HANDSHAKING", "Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;");
    handshake_not_handshaking = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    
    // initialise engine status enum
    class = (*env)->FindClass(env, "javax/net/ssl/SSLEngineResult$Status");
    fieldID = (*env)->GetStaticFieldID(env, class, "BUFFER_OVERFLOW", "Ljavax/net/ssl/SSLEngineResult$Status;");
    engine_buffer_overflow = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "BUFFER_UNDERFLOW", "Ljavax/net/ssl/SSLEngineResult$Status;");
    engine_buffer_underflow = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "CLOSED", "Ljavax/net/ssl/SSLEngineResult$Status;");
    engine_closed = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    fieldID = (*env)->GetStaticFieldID(env, class, "OK", "Ljavax/net/ssl/SSLEngineResult$Status;");
    engine_ok = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, class, fieldID));
    
    initialised = 1;
}

jobject getHandshakeStatus(JNIEnv *env, int state) {
    jclass exception;
    switch(state) {
    case SSL_ERROR_NONE:
      return handshake_not_handshaking;
    case SSL_ERROR_WANT_READ:
      return handshake_need_unwrap;
    case SSL_ERROR_WANT_WRITE:
      return handshake_need_wrap;
    default:
      exception = (*env)->FindClass(env, "javax/net/ssl/SSLHandshakeException");
      (*env)->ThrowNew(env, exception, ERR_reason_error_string(ERR_get_error()));
    }
    return NULL;
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_initImpl
  (JNIEnv *env, jclass clazz, jlong context) {
    init(env);
    return addr2jlong(SSL_new(jlong2addr(SSL_CTX, context)));
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_acceptImpl
  (JNIEnv *env, jclass clazz, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);
    BIO *server, *server_out;
    int ret;
    size_t bufsiz = 256;

    // create a bio pair
    BIO_new_bio_pair(&server, bufsiz, &server_out, bufsiz);
    
    SSL_set_bio(ssl, server, server);

    // Put our SSL into connect state
    SSL_set_accept_state(ssl);

    // Start the client handshake
    ret = SSL_do_handshake(ssl);

    return getHandshakeStatus(env, SSL_get_error(ssl, ret));
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_connectImpl
  (JNIEnv *env, jclass clazz, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);
    BIO *client, *client_out;
    int ret;
    
    size_t bufsize = 256;  // need to fix

    // create a bio pair
    BIO_new_bio_pair(&client, bufsize, &client_out, bufsize);
    
    SSL_set_bio(ssl, client, client);

    // Put our SSL into connect state
    SSL_set_connect_state(ssl);

    // Start the client handshake
    ret = SSL_do_handshake(ssl);

    return getHandshakeStatus(env, SSL_get_error(ssl, ret));
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_wrapImpl
  (JNIEnv *env, jclass clazz, jlong jssl, jbyteArray src, int src_len, 
  jbyteArray dst, int dst_len) {
    SSL *ssl = jlong2addr(SSL, jssl);
    int write_result, read_result;
    jobject handshake_state, engine_state, result;
    jclass result_class;
    jmethodID result_constructor;

    // write input data
    jbyte *buffer = (jbyte*) malloc(src_len * sizeof(jbyte*)); 
    (*env)->GetByteArrayRegion(env, src, 0, src_len, buffer);

    write_result = SSL_write(ssl, (const void *)buffer, (int)src_len);
    fprintf(stderr, "SSL_write, result:%d \n", write_result);
    if (write_result > 0) {
        // must not be handshaking as we've written bytes
        handshake_state = handshake_not_handshaking;
        if (write_result < src_len) {
            engine_state = engine_buffer_overflow;
        } else {
            engine_state = engine_ok;
        }
    } if (write_result == 0) {
        handshake_state = handshake_not_handshaking;
        engine_state = engine_closed;
    } else {
        handshake_state = getHandshakeStatus(env, SSL_get_error(ssl, write_result));
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        engine_state = engine_ok; // is this correct?
        write_result = 0;
    }
    free(buffer);
    
    
    // read output data
    buffer = (jbyte*) malloc(dst_len * sizeof(jbyte*));

    read_result = SSL_read(ssl, (void *)buffer, (int)dst_len);
    if (read_result > 0) {
      (*env)->SetByteArrayRegion(env, dst, 0, read_result, buffer);
    }
    if (read_result == -1) {
        // The socket has been closed
        jclass exception = (*env)->FindClass(env, "java/net/SocketException");
        (*env)->ThrowNew(env, exception, "Connection was reset");
        return NULL;
    }
    if (read_result < 0) {
      fprintf(stderr, "Read returned:%d \n", read_result);
      read_result = 0;
    }
    free(buffer);
    
    // construct return object
    result_class = (*env)->FindClass(env, "javax/net/ssl/SSLEngineResult");
    result_constructor = (*env)->GetMethodID(env, result_class, "<init>", 
        "(Ljavax/net/ssl/SSLEngineResult$Status;Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;II)V");
    result = (*env)->NewObject(env, result_class, result_constructor,
        engine_state, handshake_state, write_result, read_result);
    return result;
}
