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

typedef struct {
  BIO *bio;
  BIO *bio_io;
} _sslengine;

static jobject handshake_need_wrap, handshake_need_unwrap, handshake_finished, handshake_not_handshaking;
static jobject engine_buffer_overflow, engine_buffer_underflow, engine_closed, engine_ok;

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

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_initImpl
  (JNIEnv *env, jclass clazz) {
    jclass class;
    jfieldID fieldID;
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
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_initSSL
  (JNIEnv *env, jclass clazz, jlong context) {
    SSL *ssl;
    ssl = SSL_new(jlong2addr(SSL_CTX, context));
    return addr2jlong(ssl);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_initSSLEngine
  (JNIEnv *env, jclass clazz, jlong jssl) {
    _sslengine *sslengine;
    SSL *ssl = jlong2addr(SSL, jssl);
    BIO *bio, *bio_in, *bio_out;
    sslengine = malloc(sizeof(_sslengine));
    bio = BIO_new(BIO_f_ssl());
    BIO_set_ssl(bio, ssl, BIO_NOCLOSE);
    // create a bio pair
    BIO_new_bio_pair(&bio_in, 0, &bio_out, 0);
    BIO_get_ssl(bio, &ssl);
    SSL_set_bio(ssl, bio_in, bio_in);
    sslengine->bio = bio;
    sslengine->bio_io = bio_out;
    return addr2jlong(sslengine);
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_acceptImpl
  (JNIEnv *env, jclass clazz, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);
    int ret;
    
    // Put our SSL into accept state
    SSL_set_accept_state(ssl);
    // Start the client handshake
    ret = SSL_do_handshake(ssl);
    
    return handshake_need_unwrap;
    //return getHandshakeStatus(env, SSL_get_error(ssl, ret));
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_connectImpl
  (JNIEnv *env, jclass clazz, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);
    int ret;
    
    // Put our SSL into accept state
    SSL_set_connect_state(ssl);
    // Start the server handshake
    ret = SSL_do_handshake(ssl);
    
    return handshake_need_wrap;
    //return getHandshakeStatus(env, SSL_get_error(ssl, ret));
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_wrapImpl
  (JNIEnv *env, jclass clazz, jlong jsslengine, jlong src_address, int src_len, 
  jlong dst_address, int dst_len) {
    _sslengine *sslengine = jlong2addr(_sslengine, jsslengine);
    BIO *bio = sslengine->bio;
    BIO *bio_io = sslengine->bio_io;
    SSL *ssl = NULL;
    int write_result = 0, read_result = 0;
    jobject handshake_state = NULL, engine_state = NULL, result = NULL;
    jclass result_class;
    jmethodID result_constructor;
    jbyte *src_buffer = jlong2addr(jbyte, src_address);
    jbyte *dst_buffer = jlong2addr(jbyte, dst_address);

    BIO_get_ssl(bio, &ssl);

    fprintf(stderr, ">wrap 1: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    
    // write input data
    write_result = BIO_write(bio, (const void *)src_buffer, (int)src_len);
    fprintf(stderr, ">wrap BIO_write, result:%d \n", write_result);
    if (write_result > 0) {
        // wrote some data so must not be handshaking
        handshake_state = handshake_not_handshaking;
        if (write_result < src_len) {
            engine_state = engine_buffer_overflow;
        } else {
            engine_state = engine_ok;
        }
    } else {
        write_result = 0;
        handshake_state = handshake_need_unwrap;
        engine_state = engine_ok;
    }
    
    fprintf(stderr, ">wrap 2: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    
    // read output data
    read_result = BIO_read(bio_io, dst_buffer, dst_len);
    
    fprintf(stderr, ">wrap read result: %d\n", read_result);
    fprintf(stderr, ">wrap 3: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    fprintf(stderr, ">wrap bio pending: %d\n", BIO_ctrl_pending(bio));
    fprintf(stderr, ">wrap bio can write: %d\n", BIO_ctrl_get_write_guarantee(bio));
    fprintf(stderr, ">wrap bio read request: %d\n", BIO_ctrl_get_read_request(bio));
    fprintf(stderr, ">wrap IO pending: %d\n", BIO_ctrl_pending(bio_io));
    fprintf(stderr, ">wrap IO can write: %d\n", BIO_ctrl_get_write_guarantee(bio_io));
    fprintf(stderr, ">wrap IO read request: %d\n", BIO_ctrl_get_read_request(bio_io));

    if (read_result < 0) {
        // change state?
        read_result = 0;
    }
    
    // construct return object
    result_class = (*env)->FindClass(env, "javax/net/ssl/SSLEngineResult");
    result_constructor = (*env)->GetMethodID(env, result_class, "<init>", 
        "(Ljavax/net/ssl/SSLEngineResult$Status;Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;II)V");
    result = (*env)->NewObject(env, result_class, result_constructor,
        engine_state, handshake_state, write_result, read_result);
    return result;
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_unwrapImpl
  (JNIEnv *env, jclass clazz, jlong jsslengine, jlong src_address, int src_len, 
  jlong dst_address, int dst_len) {
    _sslengine *sslengine = jlong2addr(_sslengine, jsslengine);
    BIO *bio = sslengine->bio;
    BIO *bio_io = sslengine->bio_io;
    SSL *ssl = NULL;
    int write_result = 0, read_result = 0;
    jobject handshake_state = NULL, engine_state = NULL, result = NULL;
    jclass result_class;
    jmethodID result_constructor;
    jbyte *src_buffer = jlong2addr(jbyte, src_address);
    jbyte *dst_buffer = jlong2addr(jbyte, dst_address);

    BIO_get_ssl(bio, &ssl);

    fprintf(stderr, ">unwrap 1: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    
    // write input data
    //buffer = (jbyte*) malloc(src_len * sizeof(jbyte*));
    //(*env)->GetByteArrayRegion(env, src, 0, src_len, buffer);
    //write_result = BIO_write(bio_io, (const void *)buffer, (int)src_len);
    write_result = BIO_write(bio_io, (const void *)src_buffer, (int)src_len);
    fprintf(stderr, ">unwrap BIO_write, result:%d \n", write_result);
    if (write_result < 0) {
        // change state?
        write_result = 0;
    }
    
    //free(buffer);
    
    fprintf(stderr, ">unwrap 2: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    
    // read output data
    //buffer = (jbyte*) malloc(dst_len * sizeof(jbyte*));
    //read_result = BIO_read(bio, buffer, dst_len);
    read_result = BIO_read(bio, dst_buffer, dst_len);
    
    if (read_result > 0) {
        // wrote some data so must not be handshaking
        handshake_state = handshake_not_handshaking;
        if (read_result < src_len) {
            engine_state = engine_buffer_underflow;
        } else {
            engine_state = engine_ok;
        }
    } else {
        read_result = 0;
        handshake_state = handshake_need_wrap;
        engine_state = engine_ok;
    }
    
    fprintf(stderr, ">unwrap read result: %d\n", read_result);
    fprintf(stderr, ">unwrap 3: SSL in init? %d : %s\n", SSL_in_init(ssl), SSL_state_string_long(ssl));
    fprintf(stderr, ">unwrap bio pending: %d\n", BIO_ctrl_pending(bio));
    fprintf(stderr, ">unwrap bio can write: %d\n", BIO_ctrl_get_write_guarantee(bio));
    fprintf(stderr, ">unwrap bio read request: %d\n", BIO_ctrl_get_read_request(bio));
    fprintf(stderr, ">unwrap IO pending: %d\n", BIO_ctrl_pending(bio_io));
    fprintf(stderr, ">unwrap IO can write: %d\n", BIO_ctrl_get_write_guarantee(bio_io));
    fprintf(stderr, ">unwrap IO read request: %d\n", BIO_ctrl_get_read_request(bio_io));
    
    //if (read_result > 0) {
    //  (*env)->SetByteArrayRegion(env, dst, 0, read_result, buffer);
    //}
    //free(buffer);
    
    // construct return object
    result_class = (*env)->FindClass(env, "javax/net/ssl/SSLEngineResult");
    result_constructor = (*env)->GetMethodID(env, result_class, "<init>", 
        "(Ljavax/net/ssl/SSLEngineResult$Status;Ljavax/net/ssl/SSLEngineResult$HandshakeStatus;II)V");
    result = (*env)->NewObject(env, result_class, result_constructor,
        engine_state, handshake_state, write_result, read_result);
    return result;  
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_closeInboundImpl
  (JNIEnv *env, jclass clazz, jlong jsslengine) {
    _sslengine *sslengine = jlong2addr(_sslengine, jsslengine);
    BIO_shutdown_wr(sslengine->bio_io);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLEngineImpl_closeOutboundImpl
  (JNIEnv *env, jclass clazz, jlong jsslengine) {
    _sslengine *sslengine = jlong2addr(_sslengine, jsslengine);
    BIO_shutdown_wr(sslengine->bio);
}
