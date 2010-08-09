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

#include "sslParameters.h"
#include <stdio.h>
#include "jni.h"
#include "hysock.h"
#include "openssl/bio.h"
#include "openssl/ssl.h"
#include "openssl/err.h"
#include "jsse_rand.h"

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_initialiseContext
  (JNIEnv *env, jclass clazz, jbyteArray jtrustCerts, jbyteArray jkeyCert, jbyteArray jprivateKey)
{
    SSL_CTX *context;
    jint size;
    jint i;
    X509_STORE *certStore;
    X509 *x509cert;
    unsigned char *temp;
    int ret;
    RAND_METHOD *randMethod;
    JavaVM *jvm;

    SSL_library_init();
    SSL_load_error_strings();
    OpenSSL_add_all_algorithms();
    context = SSL_CTX_new(SSLv23_method());

    ret = SSL_CTX_set_cipher_list(context, "ALL");
    if (ret<=0) {
       ERR_print_errors_fp(stderr);
    }

    // First initilise the trust certificates in our newly created context
    size = (*env)->GetArrayLength(env, jtrustCerts);
    if (size) {
        // Get the current trust certificate store and add any certs passed in
        certStore = SSL_CTX_get_cert_store(context);
        for (i=0; i<size; i++) {
            jbyteArray cert = (jbyteArray) (*env)->GetObjectArrayElement(env, jtrustCerts, i);
            jint byteSize = (*env)->GetArrayLength(env, cert);
            
            // malloc a buffer for the ASN1 encoded certificate
            jbyte *certBuffer = (jbyte*) malloc(byteSize * sizeof(jbyte*)); 
            (*env)->GetByteArrayRegion(env, cert, 0, byteSize, certBuffer);

            // Copy certBuffer as the d2i_X509 will increment it
            temp = (unsigned char*)certBuffer;

            // Create an X509 from the ASN1 encoded certificate
            x509cert = d2i_X509(NULL, &temp, (int)byteSize);

            // Add the certificate to the existing ones in the SSL_CTX
            ret = X509_STORE_add_cert(certStore, x509cert);
            if (ret<=0) {
               ERR_print_errors_fp(stderr);
            }
            free(certBuffer);
        }

        // Carry out peer cert verification
        // TODO: Is this the right setting?
        SSL_CTX_set_verify(context, SSL_VERIFY_PEER, NULL);
        SSL_CTX_set_verify_depth(context, 1);
    }

    if (jkeyCert != NULL) {
        jint byteSize = (*env)->GetArrayLength(env, jkeyCert);
            
        // malloc a buffer for the ASN1 encoded certificate
        jbyte *certBuffer = (jbyte*) malloc(byteSize * sizeof(jbyte*)); 
        (*env)->GetByteArrayRegion(env, jkeyCert, 0, byteSize, certBuffer);

        // Set the key cert passed in as the default for this context
        ret = SSL_CTX_use_certificate_ASN1(context, byteSize, (unsigned char *)certBuffer);
        if (ret<=0) {
           ERR_print_errors_fp(stderr);
        }
        free(certBuffer);
    }

    if (jprivateKey != NULL) {
        jint byteSize = (*env)->GetArrayLength(env, jprivateKey);
            
        // malloc a buffer for the ASN1 encoded certificate
        jbyte *certBuffer = (jbyte*) malloc(byteSize * sizeof(jbyte*)); 
        (*env)->GetByteArrayRegion(env, jprivateKey, 0, byteSize, certBuffer);

        // Set the private key passed in as the default for this context
        ret = SSL_CTX_use_PrivateKey_ASN1(EVP_PKEY_RSA, context, (unsigned char *)certBuffer, byteSize);
        if (ret<=0) {
           ERR_print_errors_fp(stderr);
        }

        SSL_CTX_check_private_key(context);
        if (ret<=0) {
           ERR_print_errors_fp(stderr);
        }
        free(certBuffer);
    }

    // TODO: Check for error return here
    (*env)->GetJavaVM(env, &jvm);
    randMethod = getRandMethod(jvm);
    RAND_set_rand_method(randMethod);
    
    return (jlong)context;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_setEnabledProtocolsImpl
  (JNIEnv *env, jclass clazz, jlong context, jint flags) 
{
    SSL_CTX *ctx = (SSL_CTX*)context;
    long options = 0;
    long mask = SSL_OP_NO_TLSv1 | SSL_OP_NO_SSLv3 | SSL_OP_NO_SSLv2;

    if (flags & PROTOCOL_TLSv1) {
        options |= SSL_OP_NO_TLSv1;
    }
    if (flags & PROTOCOL_SSLv3) {
        options |= SSL_OP_NO_SSLv3;
    }
    if (flags & PROTOCOL_SSLv2) {
        options |= SSL_OP_NO_SSLv2;
    }

    // Clearing the options enables the protocol, setting disables
    SSL_CTX_clear_options(ctx, options);
    SSL_CTX_set_options(ctx, options ^ mask);
}
