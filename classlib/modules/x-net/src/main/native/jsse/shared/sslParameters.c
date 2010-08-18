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

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_getDefaultCipherSuites
  (JNIEnv *env, jclass clazz)
{
    SSL_CTX *context;
    SSL *ssl;
    int i, count;
    jclass stringClass;
    jobjectArray stringArray; 
    STACK_OF(SSL_CIPHER) *ciphers;

    SSL_library_init();
    SSL_load_error_strings();
    OpenSSL_add_all_algorithms();

    context = SSL_CTX_new(SSLv23_method());

    /*ret = SSL_CTX_set_cipher_list(context, "ALL");
    if (ret<=0) {
       ERR_print_errors_fp(stderr);
    }*/

    ssl = SSL_new(context);

    ciphers = SSL_get_ciphers(ssl);
    count = sk_num(&ciphers->stack);

    stringClass = (*env)->FindClass(env, "java/lang/String");
    stringArray = (*env)->NewObjectArray(env, count, stringClass, NULL);

    for (i=0; i<count; i++)
    {
        const char *cipherName = SSL_CIPHER_get_name(sk_value(&ciphers->stack, i));
        jstring jcipherName = (*env)->NewStringUTF(env, cipherName);
        (*env)->SetObjectArrayElement(env, stringArray, i, jcipherName);
        (*env)->DeleteLocalRef(env, jcipherName);
    }

    SSL_free(ssl);
    SSL_CTX_free(context);

    return stringArray;
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_initialiseContext
  (JNIEnv *env, jclass clazz, jbyteArray jtrustCerts, jbyteArray jkeyCert, jbyteArray jprivateKey)
{
    SSL_CTX *context;
    jint size;
    jint i;
    X509_STORE *certStore;
    X509 *x509cert;
    const unsigned char *temp;
    int ret;
    RAND_METHOD *randMethod;
    JavaVM *jvm;

    context = SSL_CTX_new(SSLv23_method());

    ret = SSL_CTX_set_cipher_list(context, "ALL");
    if (ret<=0) {
       ERR_print_errors_fp(stderr);
    }

    // Set client auth off by default
    SSL_CTX_set_verify(context, SSL_VERIFY_NONE, NULL);

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
            temp = (const unsigned char*)certBuffer;

            // Create an X509 from the ASN1 encoded certificate
            x509cert = d2i_X509(NULL, &temp, (int)byteSize);

            // Add the certificate to the existing ones in the SSL_CTX
            ret = X509_STORE_add_cert(certStore, x509cert);
            if (ret<=0) {
               ERR_print_errors_fp(stderr);
            }
            free(certBuffer);
        }
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
    
    return addr2jlong(context);
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_setEnabledProtocolsImpl
  (JNIEnv *env, jclass clazz, jlong context, jlong jssl, jint flags) 
{
    SSL_CTX *ctx = jlong2addr(SSL_CTX, context);
    SSL *ssl = jlong2addr(SSL, jssl);
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

    // If we have been passed an SSL pointer, set the options on that SSL too
    if (ssl) {
        SSL_clear_options(ssl, options);
        SSL_set_options(ssl, options ^ mask);
    }
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_setClientAuthImpl
  (JNIEnv *env, jclass clazz, jlong context, jlong jssl, jshort flag)
{
    SSL_CTX *ctx = jlong2addr(SSL_CTX, context);
    SSL *ssl = jlong2addr(SSL, jssl);
    int mode = 0;

    switch (flag) {
    case NO_CLIENT_AUTH:
        mode = SSL_VERIFY_NONE;
        break;
    case REQUEST_CLIENT_AUTH:
        mode = SSL_VERIFY_PEER;
        break;
    case REQUIRE_CLIENT_AUTH:
        mode = SSL_VERIFY_PEER | SSL_VERIFY_FAIL_IF_NO_PEER_CERT;
        break;
    default:
        // Should never happen
        return;
    }

    // Set the client authentication mode with a NULL callback
    SSL_CTX_set_verify(ctx, mode, NULL);

    // If we have been passed an SSL pointer, set the options on that SSL too
    if (ssl) {
        SSL_set_verify(ssl, mode, NULL);
    }
}

JNIEXPORT void JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_setEnabledCipherSuitesImpl
  (JNIEnv *env, jclass clazz, jlong context, jlong jssl, jobjectArray jenabledCiphers)
{
    jsize i;
    int size = 0;
    jsize count = (*env)->GetArrayLength(env, jenabledCiphers);
    char *cipherList;
    
    // Calculate the length of the cipher string for OpenSSL.
    // This is strlen(cipher) + 1 for the ':' separator or the final '/0'
    for (i=0; i<count; i++) {
        size += (*env)->GetStringUTFLength(env, (jstring)(*env)->GetObjectArrayElement(env, jenabledCiphers, i)) + 1;
    }

    // malloc the memory we need
    // TODO: go through the port library for this
    cipherList = malloc(size);
    memset(cipherList, 0, size);

    // Now strcat all our cipher names separated by colons, as required by OpenSSL
    for (i=0; i<count; i++) {
        jstring jcipher = (jstring)(*env)->GetObjectArrayElement(env, jenabledCiphers, i);
        const char *cipher = (*env)->GetStringUTFChars(env, jcipher, NULL);
        strcat(cipherList, cipher);
        if (i != count-1) {
            strcat(cipherList, ":");
        }
        (*env)->ReleaseStringUTFChars(env, jcipher, cipher);
    }

    // Set the new cipher list in the context and SSL, if specified
    SSL_CTX_set_cipher_list(jlong2addr(SSL_CTX, context), cipherList);
    if (jssl) {
        SSL_set_cipher_list(jlong2addr(SSL, jssl), cipherList);
    }
}
