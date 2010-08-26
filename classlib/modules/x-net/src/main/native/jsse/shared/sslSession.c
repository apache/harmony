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

#include "sslSession.h"
#include <stdio.h>
#include "jni.h"
#include "hysock.h"
#include "openssl/bio.h"
#include "openssl/ssl.h"
#include "openssl/err.h"

#include "cipherList.h"

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSessionImpl_initialiseSession
  (JNIEnv *env, jobject object, jlong jssl)
{
    SSL *ssl = jlong2addr(SSL, jssl);
    return addr2jlong(SSL_get_session(ssl));
}

char* getSpecName(const char *cipherName, char *openSSLNames[], char *specNames[], int mappedNamesCount) {
    int i;
    for (i=0; i<mappedNamesCount; i++) {
        if (!strcmp(cipherName, openSSLNames[i])) {
            return specNames[i];
        }
    }
    return NULL;
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSessionImpl_getCipherNameImpl
  (JNIEnv *env, jobject object, jlong jssl) {
    SSL *ssl = jlong2addr(SSL, jssl);
    const SSL_CIPHER *cipher;
    const char *cipherName = SSL_get_cipher(ssl);
    char *protocol = SSL_get_cipher_version(ssl);
    char *specName = NULL;
    char buf[256];

    if (!strcmp(protocol, "TLSv1/SSLv3")) {
        // We're in either TLS or SSLv3, now find the spec name
        specName = getSpecName(cipherName, getTLSv1OpenSSLNames(), getTLSv1SpecNames(), TLSv1_CIPHER_COUNT);
        if (!specName) {
            // Not in the TLS list, now search the SSL list
            // TODO: Lists are likely to be the same - can this case ever occur?
            specName = getSpecName(cipherName, getSSLv3OpenSSLNames(), getSSLv3SpecNames(), SSLv3_CIPHER_COUNT);
        }        
    } else {
        // SSLv2 case
        specName = getSpecName(cipherName, getSSLv2OpenSSLNames(), getSSLv2SpecNames(), SSLv2_CIPHER_COUNT);
    }

    cipher = SSL_get_current_cipher(ssl);
    return (*env)->NewStringUTF(env, specName);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLSessionImpl_getCreationTimeImpl
  (JNIEnv *env, jobject object, jlong jsession) {
    SSL_SESSION *session = jlong2addr(SSL_SESSION, jsession);

    return (jlong)SSL_SESSION_get_time(session)*1000;
}


