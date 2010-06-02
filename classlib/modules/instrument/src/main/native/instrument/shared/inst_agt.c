/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define USING_VMI

#include "instrument.h"
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#ifndef HY_ZIP_API
#include <zipsup.h>
#else /* HY_ZIP_API */
#include <vmizip.h>
#endif /* HY_ZIP_API */
#include <jni.h>
#include <vmi.h>

/*
 * This file implements a JVMTI agent to init Instrument instance, and handle class define/redefine events
 */

AgentList *tail = &list;
int gsupport_redefine = 0;
static JNIEnv *jnienv;

//call back function for ClassLoad event
void JNICALL callbackClassFileLoadHook(jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jclass class_being_redefined,
    jobject loader,
    const char* name,
    jobject protection_domain,
    jint class_data_len,
    const unsigned char* class_data,
    jint* new_class_data_len,
    unsigned char** new_class_data){

    jclass inst_class = *(gdata->inst_class);
    jbyteArray jnew_bytes = NULL;
    jbyteArray jold_bytes = (*jni_env)->NewByteArray(jni_env, class_data_len);
    jmethodID transform_method = *(gdata->transform_method);
    int name_len = strlen(name);
    jbyteArray jname_bytes = (*jni_env)->NewByteArray(jni_env, name_len);

    //construct java byteArray for old class data and class name
    (*jni_env)->SetByteArrayRegion(jni_env, jold_bytes, 0, class_data_len, (jbyte *)class_data);
    (*jni_env)->SetByteArrayRegion(jni_env, jname_bytes, 0, name_len, (jbyte *)name);

    //invoke transform method
    jnew_bytes = (jbyteArray)(*jni_env)->CallObjectMethod(jni_env, *(gdata->inst), transform_method, loader, jname_bytes, class_being_redefined, protection_domain, jold_bytes);

    //get transform result to native char array
    if(0 != jnew_bytes){
        *new_class_data_len = (*jni_env)->GetArrayLength(jni_env, jnew_bytes);
        (*jvmti_env)->Allocate(jvmti_env, *new_class_data_len, new_class_data);
        *new_class_data = (*jni_env)->GetPrimitiveArrayCritical(jni_env, jnew_bytes, JNI_FALSE);
        (*jni_env)->ReleasePrimitiveArrayCritical(jni_env, jnew_bytes, *new_class_data, 0);
    }
    return;
}

//call back function for VM init event
void JNICALL callbackVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread){
    jmethodID constructor;
    static jmethodID transform_method;
    static jmethodID premain_method;
    static jobject inst_obj;
    static jclass inst_class;
    jvmtiError err;
    AgentList *elem;

    PORT_ACCESS_FROM_ENV (env);
    inst_class = (*env)->FindClass(env, "org/apache/harmony/instrument/internal/InstrumentationImpl");
    if(NULL == inst_class){
        (*env)->FatalError(env,"class cannot find: org/apache/harmony/instrument/internal/InstrumentationImpl");
        return;
    }
    inst_class = (jclass)(*env)->NewGlobalRef(env, inst_class);
    gdata->inst_class = &inst_class;

    constructor = (*env)->GetMethodID(env, inst_class,"<init>", "(Z)V");
    if(NULL == constructor){
        (*env)->FatalError(env,"constructor cannot be found.");
        return;
    }

    inst_obj = (*env)->NewObject(env, inst_class, constructor, gsupport_redefine?JNI_TRUE:JNI_FALSE);
    if(NULL == inst_obj){
        (*env)->FatalError(env,"object cannot be inited.");
        return;
    }

    inst_obj = (*env)->NewGlobalRef(env, inst_obj);
    gdata->inst = &inst_obj;

    transform_method = (*env)->GetMethodID(env, inst_class, "transform", "(Ljava/lang/ClassLoader;[BLjava/lang/Class;Ljava/security/ProtectionDomain;[B)[B");
    if(NULL == transform_method){
        (*env)->FatalError(env,"transform method cannot find.");
        return;
    }
    gdata->transform_method = &transform_method;

    premain_method = (*env)->GetMethodID(env, inst_class, "executePremain", "([B[B)V");
    if(NULL == premain_method){
        (*env)->FatalError(env,"executePremain method cannot find.");
        return;
    }
    gdata->premain_method = &premain_method;
    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    check_jvmti_error(env, err, "Cannot set JVMTI ClassFileLoadHook event notification mode.");

    //parse command options and run premain class here
    if(tail == &list){
        return;
    }
    for(elem = list.next; elem != NULL; elem = list.next){
        char *agent_options = elem->option;
        char *class_name = elem->class_name;
        jbyteArray joptions=NULL, jclass_name;
        if(class_name){
            jclass_name = (*env)->NewByteArray(env, strlen(class_name));
            (*env)->SetByteArrayRegion(env, jclass_name, 0, strlen(class_name), (jbyte*)class_name);
        }else{
            goto DEALLOCATE;
        }
        if(agent_options){
            joptions = (*env)->NewByteArray(env, strlen(agent_options));
            (*env)->SetByteArrayRegion(env, joptions, 0, strlen(agent_options), (jbyte*)agent_options);
        }

        (*env)->CallObjectMethod(env, *(gdata->inst), *(gdata->premain_method), jclass_name, joptions);
    DEALLOCATE:
        list.next = elem->next;
        hymem_free_memory(elem->class_name);
        hymem_free_memory(elem->option);
        hymem_free_memory(elem);
    }
    tail = &list;
}

char* Read_Manifest(JavaVM *vm, JNIEnv *env,const char *jar_name){
    I_32 retval;
#ifndef HY_ZIP_API
    HyZipFile zipFile;
    HyZipEntry zipEntry;
#else
    VMIZipFile zipFile;
    VMIZipEntry zipEntry;
#endif
    char *result;
    int size = 0;
    char errorMessage[1024];

    /* Reach for the VM interface */
    VMI_ACCESS_FROM_JAVAVM(vm);
    PORT_ACCESS_FROM_JAVAVM(vm);

#ifdef HY_ZIP_API
    VMIZipFunctionTable *zipFuncs = (*VMI)->GetZipFunctions(VMI);

#endif /* HY_ZIP_API */
    /* open zip file */
#ifndef HY_ZIP_API
    retval = zip_openZipFile(privatePortLibrary, (char *)jar_name, &zipFile, NULL);
#else /* HY_ZIP_API */
    retval = zipFuncs->zip_openZipFile(VMI, (char *)jar_name, &zipFile, 0);
#endif /* HY_ZIP_API */
    if(retval){
        sprintf(errorMessage,"failed to open file:%s, %d\n", jar_name, retval);
        (*env)->FatalError(env, errorMessage);
        return NULL;
    }

    /* get manifest entry */
#ifndef HY_ZIP_API
    zip_initZipEntry(privatePortLibrary, &zipEntry);
    retval = zip_getZipEntry(privatePortLibrary, &zipFile, &zipEntry, "META-INF/MANIFEST.MF", TRUE);
#else /* HY_ZIP_API */
    zipFuncs->zip_initZipEntry(VMI, &zipEntry);
    retval = zipFuncs->zip_getZipEntry(VMI, &zipFile, &zipEntry, "META-INF/MANIFEST.MF", ZIP_FLAG_READ_DATA_POINTER);
#endif /* HY_ZIP_API */
    if (retval) {
#ifndef HY_ZIP_API
        zip_freeZipEntry(PORTLIB, &zipEntry);
#else /* HY_ZIP_API */
        zipFuncs->zip_freeZipEntry(VMI, &zipEntry);
#endif /* HY_ZIP_API */
        sprintf(errorMessage,"failed to get entry: %d\n", retval);
        (*env)->FatalError(env, errorMessage);
        return NULL;
    }

    /* read bytes */
    size = zipEntry.uncompressedSize;
    result = (char *)hymem_allocate_memory(size*sizeof(char) + 1);
#ifndef HY_ZIP_API
    retval = zip_getZipEntryData(privatePortLibrary, &zipFile, &zipEntry, (unsigned char*)result, size);
#else /* HY_ZIP_API */
    retval = zipFuncs->zip_getZipEntryData(VMI, &zipFile, &zipEntry, (unsigned char*)result, size);
#endif /* HY_ZIP_API */
    if(retval){
#ifndef HY_ZIP_API
        zip_freeZipEntry(PORTLIB, &zipEntry);
#else /* HY_ZIP_API */
        zipFuncs->zip_freeZipEntry(VMI, &zipEntry);
#endif /* HY_ZIP_API */
        sprintf(errorMessage,"failed to get bytes from zip entry, %d\n", zipEntry.extraFieldLength);
        (*env)->FatalError(env, errorMessage);
        return NULL;
    }

    result[size] = '\0';
    /* free resource */
#ifndef HY_ZIP_API
    zip_freeZipEntry(privatePortLibrary, &zipEntry);
    retval = zip_closeZipFile(privatePortLibrary, &zipFile);
#else /* HY_ZIP_API */
    zipFuncs->zip_freeZipEntry(VMI, &zipEntry);
    retval = zipFuncs->zip_closeZipFile(VMI, &zipFile);
#endif /* HY_ZIP_API */
    if (retval) {
        sprintf(errorMessage,"failed to close zip file: %s, %d\n", jar_name, retval);
        (*env)->FatalError(env, errorMessage);
        return NULL;
    }
    return result;
}

char* read_attribute(JavaVM *vm, char *manifest,char *lwrmanifest, const char * target){
    char *pos;
    char *end;
    char *value;
    char *tmp;
    int length;
    PORT_ACCESS_FROM_JAVAVM(vm);

    if(NULL == strstr(lwrmanifest,target)){
        return NULL;
    }

    pos = manifest+ (strstr(lwrmanifest,target) - lwrmanifest);
    pos += strlen(target)+2;//": "
    end = strchr(pos, '\n');

    while (end != NULL && *(end + 1) == ' ') {
        end = strchr(end + 1, '\n');
    }

    if(NULL == end){
        end = manifest + strlen(manifest);
    }

    length = end - pos;

    value = (char *)hymem_allocate_memory(sizeof(char)*(length+1));
    tmp = value;

    end = strchr(pos, '\n');
    while (end != NULL && *(end + 1) == ' ') {
        /* in windows, has '\r\n' in the end of line, omit '\r' */
        if (*(end - 1) == '\r') {
            strncpy(tmp, pos, end - 1 - pos);
            tmp += end - 1 - pos;
            pos = end + 2;
        } else {
            strncpy(tmp, pos, end - pos);
            tmp += end - pos;
            pos = end + 2;
        }
        end = strchr(end + 1, '\n');
    }

    if (NULL == end) {
        strcpy(tmp, pos);
    } else {
        /* in windows, has '\r\n' in the end of line, omit '\r' */
        if (*(end - 1) == '\r') {
            end--;
        }
        strncpy(tmp, pos, end - pos);
        *(tmp + (end - pos)) = '\0';
    }

    return value;
}

char* strlower(char * str){
    char *temp = str;
    while((*temp = tolower(*temp)))
        temp++;
    return str;
}

int str2bol(char *str){
    return 0 == strcmp("true", strlower(str));
}

jint Parse_Options(JavaVM *vm, JNIEnv *env, jvmtiEnv *jvmti,  const char *agent){
    PORT_ACCESS_FROM_JAVAVM(vm);
    VMI_ACCESS_FROM_JAVAVM(vm);

    AgentList *new_elem = (AgentList *)hymem_allocate_memory(sizeof(AgentList));
    char *agent_cpy = (char *)hymem_allocate_memory(sizeof(char)*(strlen(agent)+1));
    char *jar_name, *manifest;
    char *options = NULL;
    char *class_name, *bootclasspath, *str_support_redefine;
    char *bootclasspath_item;
    char *classpath;
    char *classpath_cpy;
    int support_redefine = 0;
    char *pos;
    char *lwrmanifest;

    strcpy(agent_cpy, agent);
    //parse jar name and options
    pos = strchr(agent_cpy, '=');
    if(pos>0){
        *pos++ = 0;
        options = (char *)hymem_allocate_memory(sizeof(char) * (strlen(pos)+1));
        strcpy(options, pos);
    }
    jar_name =agent_cpy;

    //read jar files, find manifest entry and read bytes
    //read attributes(premain class, support redefine, bootclasspath)
    manifest = Read_Manifest(vm,env, jar_name);
    lwrmanifest = (char *)hymem_allocate_memory(sizeof(char) * (strlen(manifest)+1));
    strcpy(lwrmanifest,manifest);
    strlower(lwrmanifest);

    //jar itself added to bootclasspath
    check_jvmti_error(env, (*jvmti)->GetSystemProperty(jvmti,"java.class.path",&classpath),"Failed to get classpath.");
    classpath_cpy = (char *)hymem_allocate_memory((sizeof(char)*(strlen(classpath)+strlen(jar_name)+2)));
    strcpy(classpath_cpy,classpath);
#if defined(WIN32) || defined(WIN64)
    strcat(classpath_cpy,";");
#else
    strcat(classpath_cpy,":");
#endif
    strcat(classpath_cpy,jar_name);
    check_jvmti_error(env, (*jvmti)->SetSystemProperty(jvmti, "java.class.path",classpath_cpy),"Failed to set classpath.");
    hymem_free_memory(classpath_cpy);
    hymem_free_memory(jar_name);

    //save options, save class name, add to agent list
    class_name = read_attribute(vm, manifest, lwrmanifest,"premain-class");
    if(NULL == class_name){
        hymem_free_memory(lwrmanifest);
        hymem_free_memory(manifest);
        (*env)->FatalError(env,"Cannot find Premain-Class attribute.");
    }
    new_elem->option = options;
    new_elem->class_name = class_name;
    new_elem->next = NULL;
    tail->next = new_elem;
    tail = new_elem;

    //calculate support redefine
    str_support_redefine = read_attribute(vm, manifest, lwrmanifest,"can-redefine-classes");
    if(NULL != str_support_redefine){
        support_redefine = str2bol(str_support_redefine);
        hymem_free_memory(str_support_redefine);
    }
    gsupport_redefine &= support_redefine;

    //add bootclasspath

    bootclasspath = read_attribute(vm, manifest, lwrmanifest,"boot-class-path");
    if (NULL != bootclasspath){
        
#if defined(WIN32) || defined(WIN64)
        // On Windows the agent jar path can have a mixture of forward and back slashes.
        // For ease, convert forward slashes to back slashes
        char *currentSlash = strchr(jar_name, '/');
        while (currentSlash) {
            *currentSlash = '\\';
            currentSlash = strchr(currentSlash, '/');
        }
#endif

        bootclasspath_item = strtok(bootclasspath, " ");
        while(NULL != bootclasspath_item){
            if ((bootclasspath_item[0] != DIR_SEPARATOR) && (strrchr(jar_name, DIR_SEPARATOR))) {
                // This is not an absolute path, so add this relative path to the path of the agent library
                int lastSeparatorOff = strrchr(jar_name, DIR_SEPARATOR) - jar_name + 1;
                int size = lastSeparatorOff + strlen(bootclasspath_item) + 1;
                char *jarPath = (char *)hymem_allocate_memory(size);
                
                memcpy(jarPath, jar_name, lastSeparatorOff);
                strcpy(jarPath + lastSeparatorOff, bootclasspath_item);
                check_jvmti_error(env, (*jvmti)->AddToBootstrapClassLoaderSearch(jvmti, jarPath),"Failed to add bootstrap classpath.");
                hymem_free_memory(jarPath);
            } else {
                // This is either an absolute path of jar_name has not path before the filename
                check_jvmti_error(env, (*jvmti)->AddToBootstrapClassLoaderSearch(jvmti, bootclasspath_item),"Failed to add bootstrap classpath.");
            }           

            bootclasspath_item = strtok(NULL, " ");
        }
        hymem_free_memory(bootclasspath);
    }
    hymem_free_memory(lwrmanifest);
    hymem_free_memory(manifest);
    return 0;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved){
    PORT_ACCESS_FROM_JAVAVM(vm);
    VMI_ACCESS_FROM_JAVAVM(vm);
    jvmtiError jvmti_err;
    JNIEnv *env = NULL;
    static jvmtiEnv *jvmti;
    jvmtiCapabilities updatecapabilities;
    jint err = (*vm)->GetEnv(vm, (void **)&jnienv, JNI_VERSION_1_2);
    if(JNI_OK != err){
        return err;
    }

    if(!gdata){
        jvmtiCapabilities capabilities;
        jvmtiError jvmti_err;
        jvmtiEventCallbacks callbacks;

        gdata = hymem_allocate_memory(sizeof(AgentData));

        //get jvmti environment
        err = (*vm)->GetEnv(vm, (void **)&jvmti, JVMTI_VERSION_1_0);
        if(JNI_OK != err){
            return err;
        }
        gdata->jvmti = jvmti;

        //get JVMTI potential capabilities
        jvmti_err = (*jvmti)->GetPotentialCapabilities(jvmti, &capabilities);
        check_jvmti_error(env, jvmti_err, "Cannot get JVMTI potential capabilities.");
        gsupport_redefine = (capabilities.can_redefine_classes == 1);

        //set events callback function
        (void)memset(&callbacks, 0, sizeof(callbacks));
        callbacks.ClassFileLoadHook = &callbackClassFileLoadHook;
        callbacks.VMInit = &callbackVMInit;
        jvmti_err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(jvmtiEventCallbacks));
        check_jvmti_error(env, jvmti_err, "Cannot set JVMTI event callback functions.");

        //enable classfileloadhook event
        jvmti_err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
        check_jvmti_error(env, jvmti_err, "Cannot set JVMTI VMInit event notification mode.");
    }

    err = Parse_Options(vm,jnienv, gdata->jvmti,options);

    //update capabilities JVMTI
    memset(&updatecapabilities, 0, sizeof(updatecapabilities));
    updatecapabilities.can_generate_all_class_hook_events = 1;
    updatecapabilities.can_redefine_classes = gsupport_redefine;
    //FIXME VM doesnot support the capbility right now.
    //capabilities.can_redefine_any_class = 1;
    jvmti_err = (*jvmti)->AddCapabilities(jvmti, &updatecapabilities);
    check_jvmti_error(env, jvmti_err, "Cannot add JVMTI capabilities.");

    return err;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm){
    PORT_ACCESS_FROM_JAVAVM(vm);
    VMI_ACCESS_FROM_JAVAVM(vm);
    //free the resource here
    if(gdata){
        jvmtiEnv *jvmti = gdata->jvmti;
        jvmtiError err = (*jvmti)->DisposeEnvironment(jvmti);
        if(err != JVMTI_ERROR_NONE)     {
            (*jnienv)->FatalError(jnienv,"Cannot dispose JVMTI environment.");
        }
        hymem_free_memory(gdata);
        gdata = NULL;
    }
    return;
}
