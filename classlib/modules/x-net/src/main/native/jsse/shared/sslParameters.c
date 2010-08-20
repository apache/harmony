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

#include "cipherList.h"

int getCipherSpecList(JNIEnv *env, SSL *ssl, const char *protocol, jstring **jciphers, char *openSSLNames[], char *specNames[], int mappedNamesCount) {
    int i, matched, ret, count;
    STACK_OF(SSL_CIPHER) *ciphers;

    ret = SSL_set_cipher_list(ssl, protocol);
    if (ret<=0) {
       ERR_print_errors_fp(stderr);
       // TODO: Throw exception here and return error value
    }

    ciphers = SSL_get_ciphers(ssl);
    count = sk_num(&ciphers->stack);
    *jciphers = malloc(sizeof(jstring)*count);

    matched = 0;
    for (i=0; i<count; i++)
    {
        const char *cipherName = SSL_CIPHER_get_name(sk_value(&ciphers->stack, i));
        int j;
        for (j=0; j<mappedNamesCount; j++) {
            if (!strcmp(cipherName, openSSLNames[j])) {
                (*jciphers)[matched] = (*env)->NewStringUTF(env, specNames[j]);                
                matched++;
                break;
            }
        }
    }

    return matched;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_xnet_provider_jsse_SSLParameters_initialiseDefaults
  (JNIEnv *env, jclass clazz)
{
    SSL_CTX *context;
    SSL *ssl;
    int i, ret, ssl2matched, ssl3matched, tlsmatched;
    jclass stringClass;
    jobjectArray stringArray;
    jstring *ssl2jciphers, *ssl3jciphers, *tlsjciphers;

    SSL_library_init();
    SSL_load_error_strings();
    OpenSSL_add_all_algorithms();

    context = SSL_CTX_new(SSLv23_method());

    ret = SSL_CTX_set_cipher_list(context, "SSLv2:SSLv3:TLSv1");
    if (ret<=0) {
       ERR_print_errors_fp(stderr);
       // TODO: throw exception here and return
    }

    ssl = SSL_new(context);
    
    // TODO: check for exception return
    ssl2matched = getCipherSpecList(env, ssl, "SSLv2", &ssl2jciphers, SSLv2_openSSLNames, SSLv2_SpecNames, SSLv2_CIPHER_COUNT);
    ssl3matched = getCipherSpecList(env, ssl, "SSLv3", &ssl3jciphers, SSLv3_openSSLNames, SSLv3_SpecNames, SSLv3_CIPHER_COUNT);
    tlsmatched = getCipherSpecList(env, ssl, "TLSv1", &tlsjciphers, TLSv1_openSSLNames, TLSv1_SpecNames, TLSv1_CIPHER_COUNT);

    stringClass = (*env)->FindClass(env, "java/lang/String");
    stringArray = (*env)->NewObjectArray(env, ssl2matched + ssl3matched + tlsmatched, stringClass, NULL);
    for (i=0; i<tlsmatched; i++)
    {
        (*env)->SetObjectArrayElement(env, stringArray, i, tlsjciphers[i]);
        (*env)->DeleteLocalRef(env, tlsjciphers[i]);
    }
    for (i=0; i<ssl3matched; i++)
    {
        (*env)->SetObjectArrayElement(env, stringArray, i + tlsmatched, ssl3jciphers[i]);
        (*env)->DeleteLocalRef(env, ssl3jciphers[i]);
    }
    for (i=0; i<ssl2matched; i++)
    {
        (*env)->SetObjectArrayElement(env, stringArray, i + ssl3matched + tlsmatched, ssl2jciphers[i]);
        (*env)->DeleteLocalRef(env, ssl2jciphers[i]);
    }
    
    free(ssl2jciphers);
    free(ssl3jciphers);
    free(tlsjciphers);
    SSL_free(ssl);
    SSL_CTX_free(context);

    // Initialise our global RNG functions to call into RNGHandler
    initialiseRandMethod(env);

    // Return the array of default cipher suites
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

    context = SSL_CTX_new(SSLv23_method());

    ret = SSL_CTX_set_cipher_list(context, "SSLv2:SSLv3:TLSv1");
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

char* findOpenSSLName(const char *cipher) {
    int i;
    if (strstr(cipher, "TLS_")) {
        // This is a TLS cipher name
        for (i=0; i<TLSv1_CIPHER_COUNT; i++) {
            if (!strcmp(cipher, TLSv1_SpecNames[i])) {
                return TLSv1_openSSLNames[i];
            }
        }
    } else if (strstr(cipher, "SSL_CK")) {
        // This is an SSLv2 cipher name
        for (i=0; i<SSLv2_CIPHER_COUNT; i++) {
            if (!strcmp(cipher, SSLv2_SpecNames[i])) {
                return SSLv2_openSSLNames[i];
            }
        }
    } else {
        // This is an SSLv3 cipher name
        for (i=0; i<SSLv3_CIPHER_COUNT; i++) {
            if (!strcmp(cipher, SSLv3_SpecNames[i])) {
                return SSLv3_openSSLNames[i];
            }
        }
    }  

    return NULL;  
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

        char *openSSLName = findOpenSSLName(cipher);
        if (openSSLName) {
            strcat(cipherList, openSSLName);
            if (i != count-1) {
                strcat(cipherList, ":");
            }
        }

        (*env)->ReleaseStringUTFChars(env, jcipher, cipher);
    }

    // Set the new cipher list in the context and SSL, if specified
    SSL_CTX_set_cipher_list(jlong2addr(SSL_CTX, context), cipherList);
    if (jssl) {
        SSL_set_cipher_list(jlong2addr(SSL, jssl), cipherList);
    }
}
