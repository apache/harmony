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

/**
 * @author Alexander V. Astapchuk
 */
#if defined( _WINDOWS)
#include <windows.h>
BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
                     )
{
    return TRUE;
}


    #include "nixemu.h"
#else
    #include <unistd.h>
    #include <pwd.h>
    #include <grp.h>
#endif /* ifdef _WINDOWS */

#include <stdlib.h>
#include <assert.h>

#include "vmi.h"
#include "jni.h"

jfieldID jf_uid = NULL;
jfieldID jf_username = NULL;
jfieldID jf_gid = NULL;
jfieldID jf_groupname = NULL;

jfieldID jf_groups = NULL;
jfieldID jf_groupsNames = NULL;

jclass jclassString = NULL;

JNIEXPORT void JNICALL
Java_org_apache_harmony_auth_module_UnixSystem_load
  (JNIEnv * jenv, jobject thiz)
{
    PORT_ACCESS_FROM_ENV(jenv);
    uid_t uid;
    gid_t gid;
    struct passwd * pp;
    struct group * pg;
    int gcount;

    if( NULL == jf_uid ) {
        jclass klass = (*jenv)->GetObjectClass (jenv, thiz);
        if( NULL == klass ) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not obtain object's Class");
            return;
        }

        if( NULL == (jf_uid = (*jenv)->GetFieldID (jenv, klass, "uid", "J"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"uid\" of type long");
            return;
        }
        if( NULL == (jf_username = (*jenv)->GetFieldID (jenv, klass, "username", "Ljava/lang/String;"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"username\" of type String");
            return;
        }
        if( NULL == (jf_gid = (*jenv)->GetFieldID (jenv, klass, "gid", "J"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"gid\" of type long");
            return;
        }
        if( NULL == (jf_groupname = (*jenv)->GetFieldID (jenv, klass, "groupname", "Ljava/lang/String;"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"groupname\" of type String");
            return;
        }

        if( NULL == (jf_groups = (*jenv)->GetFieldID (jenv, klass, "groups", "[J"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"groups\" of type long[]");
            return;
        }
        if( NULL == (jf_groupsNames = (*jenv)->GetFieldID (jenv, klass, "groupsNames", "[Ljava/lang/String;"))) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"groupsNames\" of type String[]");
            return;
        }
        if( NULL == (jclassString = (*jenv)->FindClass (jenv, "java/lang/String")) ) {
            jclass klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
            assert(klassErr);
            (*jenv)->ThrowNew (jenv, klassErr, "Could not find class java/lang/String");
            return;
        }
        jclassString = (jclass)(*jenv)->NewGlobalRef (jenv, jclassString);
    }

    uid = getuid();
    (*jenv)->SetLongField (jenv, thiz, jf_uid, (jlong)uid);
    gid = getgid();
    (*jenv)->SetLongField (jenv, thiz, jf_gid, (jlong)gid);

    pp = getpwuid(uid);
    (*jenv)->SetObjectField (jenv, thiz, jf_username, (*jenv)->NewStringUTF (jenv, pp->pw_name));

    pg = getgrgid(gid);
    (*jenv)->SetObjectField (jenv, thiz, jf_groupname, (*jenv)->NewStringUTF (jenv, pg->gr_name));

    gcount = getgroups(0, NULL);
    
    if( 0 != gcount ) {

        gid_t * gids;
        jlongArray jgs;
        jlong * jgs_raw;
        jobjectArray jgsnames;
        int i;
        int gcount_temp;

        gids = (gid_t*)hymem_allocate_memory(gcount*sizeof(gid_t));

        /* capture return code to fix compiler warning */
        gcount_temp = getgroups(gcount, gids);
        
        jgs = (*jenv)->NewLongArray (jenv, gcount);
        jgs_raw = (*jenv)->GetLongArrayElements (jenv, jgs, NULL);
        jgsnames = (*jenv)->NewObjectArray (jenv, gcount, jclassString, NULL);
        for(i=0; i<gcount; i++ ) {
            struct group * g = getgrgid(gids[i]);
            jgs_raw[i] = g->gr_gid;
            (*jenv)->SetObjectArrayElement (jenv, jgsnames, i, (*jenv)->NewStringUTF (jenv, g->gr_name));
        }
        (*jenv)->ReleaseLongArrayElements (jenv, jgs, jgs_raw, 0); /* here: 0='update java array with the passed values' */
        (*jenv)->SetObjectField (jenv, thiz, jf_groups, jgs);
        (*jenv)->SetObjectField (jenv, thiz, jf_groupsNames, jgsnames);

        hymem_free_memory(gids);

    };
}
