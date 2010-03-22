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

#include <stdlib.h>
#include "instrument.h"
#include "vmi.h"
#include "exceptions.h"



void throw_exception(JNIEnv * env,jvmtiError err){
    switch (err) {
    case JVMTI_ERROR_MUST_POSSESS_CAPABILITY:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "The environment does not possess the capability can_redefine_classes.");
        return;
    case JVMTI_ERROR_NULL_POINTER:
        throwNewExceptionByName(env, "java/lang/NullPointerException",
                                "One of class_bytes is NULL.");
        return;
    case JVMTI_ERROR_UNMODIFIABLE_CLASS:
        throwNewExceptionByName(env, "java/lang/instrument/UnmodifiableClassException",
                                "An element of class_definitions cannot be modified.");
        return;
    case JVMTI_ERROR_INVALID_CLASS:
        throwNewExceptionByName(env, "java/lang/ClassNotFoundException",
                                "An element of class_definitions is not a valid class.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_VERSION:
        throwNewExceptionByName(env, "java/lang/UnsupportedClassVersionError",
                                "A new class file has a version number not supported by this VM.");
        return;
    case JVMTI_ERROR_INVALID_CLASS_FORMAT:
        throwNewExceptionByName(env, "java/lang/ClassFormatError",
                                "A new class file is malformed.");
        return;
    case JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION:
        throwNewExceptionByName(env, "java/lang/ClassCircularityError",
                                "The new class file definitions would lead to a circular definition.");
        return;
    case JVMTI_ERROR_FAILS_VERIFICATION:
        throwNewExceptionByName(env, "java/lang/ClassFormatError",
                                "The class bytes fail verification.");
        return;
    case JVMTI_ERROR_NAMES_DONT_MATCH:
        throwNewExceptionByName(env, "java/lang/NoClassDefFoundError",
                                "The class name defined in a new class file is different from the name in the old class object.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A new class file requires adding a method.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A new class version changes a field.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A direct superclass is different for a new class version, or the set of directly implemented interfaces is different.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A new class version does not declare a method declared in the old class version.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A new class version has different modifiers.");
        return;
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED:
        throwNewExceptionByName(env, "java/lang/UnsupportedOperationException",
                                "A method in the new class version has different modifiers than its counterpart in the old class version.");
        return;
    default:
        throwNewExceptionByName(env, "java/lang/InternalError",
                                "Unknown error during redefinition.");
    }
}

void cleanup(JNIEnv* env, jvmtiClassDefinition *class_definitions, int filled_class_definitions){
    PORT_ACCESS_FROM_ENV (env);
    int i;
    for(i = 0;i<filled_class_definitions;i++){
        hymem_free_memory((jbyte *)class_definitions[i].class_bytes);
    }
    hymem_free_memory(class_definitions);
    return;
}

/*
 * This file contains native methods implementation for org/apache/harmony/instrument/internal/InstrumentationImpl
 */

jobjectArray extract_elements(JNIEnv *env, jvmtiEnv *jvmti, jint count, const jclass* classes_ptr){
    jclass klass;
    jobjectArray classes;
    int index;
    jvmtiError err;

    //get the class of "java.lang.Class" in java language
    klass= (*env)->FindClass(env, "java/lang/Class");
    if(NULL == klass){
        return NULL;
    }

    //initiate the object array to return, fill in all elements with the same value
    classes = (*env)->NewObjectArray(env, count, klass, NULL);
    if(NULL == classes){
        return NULL;
    }

    //fill in the object array with right values
    for(index=0; index<count; index++){
        (*env)->SetObjectArrayElement(env, classes, index, classes_ptr[index]);
    }

    err = (*jvmti)->Deallocate(jvmti,(unsigned char *)classes_ptr);
    check_jvmti_error(env, err, "Cannot deallocate memory.");

    return classes;
}

/*
 * Class:     Java_org_apache_harmony_instrument_internal_InstrumentationImpl
 * Method:    getAllLoadedClasses
 * Signature: ()[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_instrument_internal_InstrumentationImpl_getAllLoadedClasses
  (JNIEnv* env, jobject objThis){
    jvmtiEnv* jvmti=gdata->jvmti;
    jint count=0;
    jclass* classes_ptr=NULL;
    jobjectArray classes; //the object array to return

    jvmtiError err = (*jvmti)->GetLoadedClasses(jvmti, &count, &classes_ptr);
    check_jvmti_error(env, err, "Cannot get loaded classes.");

    classes = extract_elements(env, jvmti, count, classes_ptr);

    return classes;
}


/*
 * Class:     Java_org_apache_harmony_instrument_internal_InstrumentationImpl
 * Method:    getInitiatedClasses
 * Signature: (Ljava/lang/ClassLoader;)[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_instrument_internal_InstrumentationImpl_getInitiatedClasses
  (JNIEnv * env, jobject objThis, jobject loader){
    jvmtiEnv* jvmti=gdata->jvmti;
    jint count=0;
    jclass* classes_ptr=NULL;
    jobjectArray classes;

    jvmtiError err = (*jvmti)->GetClassLoaderClasses(jvmti, loader, &count, &classes_ptr);
    check_jvmti_error(env, err, "Cannot get loaded classes for this classloader.");

    classes = extract_elements(env, jvmti, count, classes_ptr);

    return classes;
}

/*
 * Class:     Java_org_apache_harmony_instrument_internal_InstrumentationImpl
 * Method:    getObjectSize_native
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_instrument_internal_InstrumentationImpl_getObjectSize_1native
  (JNIEnv * env, jobject objThis, jobject objToSize){
    jvmtiEnv *jvmti=gdata->jvmti;
    jlong size=0l;
    jvmtiError err=(*jvmti)->GetObjectSize(jvmti, objToSize, &size);
    check_jvmti_error(env, err, "Cannot get object size.");
    return size;
}

/*
 * Class:     Java_org_apache_harmony_instrument_internal_InstrumentationImpl
 * Method:    redefineClasses_native
 * Signature: ([Ljava/lang/instrument/ClassDefinition;)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_instrument_internal_InstrumentationImpl_redefineClasses_1native
  (JNIEnv * env, jobject objThis, jobjectArray javaClassDefArr){
    PORT_ACCESS_FROM_ENV (env);
    jvmtiEnv* jvmti=gdata->jvmti;
    int err;
    int index;
    jmethodID method_get_class;
    jmethodID method_get_data;
    jsize length;
    jvmtiClassDefinition *class_definitions;
    jclass clz;
    jmethodID method_clear;

    //locate the java methods needed by class definition data extraction
    jclass class_ClassDefinition=(*env)->FindClass(env, "java/lang/instrument/ClassDefinition");
    if(NULL == class_ClassDefinition){
        return;
    }

    method_get_data=(*env)->GetMethodID(env, class_ClassDefinition, "getDefinitionClassFile", "()[B");
    if(NULL == method_get_data){
        return;
    }

    method_get_class=(*env)->GetMethodID(env, class_ClassDefinition, "getDefinitionClass", "()Ljava/lang/Class;");
    if(NULL == method_get_class){
        return;
    }

    //allocate memory for native jvmtiClassDefinition structs to hold class redefinition data
    length=(*env)->GetArrayLength(env, javaClassDefArr);
    class_definitions=(jvmtiClassDefinition*) hymem_allocate_memory(sizeof(jvmtiClassDefinition)*length);
    if(NULL == class_definitions){
        return;
    }

    //extract class definition data from java array into native array
    for(index=0; index<length; index++){
        int class_byte_count;
        jobject obj_ClassDefinition=(*env)->GetObjectArrayElement(env, javaClassDefArr, index);
        jbyteArray jclass_bytes;
        jbyte* class_bytes;
        jclass klass=(jclass)(*env)->CallObjectMethod(env, obj_ClassDefinition, method_get_class);
        if (NULL == klass){
            cleanup(env, class_definitions, index);
            return;
        }
        jclass_bytes =(jbyteArray)(*env)->CallObjectMethod(env, obj_ClassDefinition, method_get_data);
        class_byte_count = (*env)->GetArrayLength(env, jclass_bytes);
        class_bytes = (jbyte *)hymem_allocate_memory(sizeof(jbyte)*class_byte_count);
        if(NULL == class_bytes){
            cleanup(env, class_definitions, index);
            return;
        }
        (*env)->GetByteArrayRegion(env,jclass_bytes,0,class_byte_count,class_bytes);

        //construct a jvmtiClassDefinition element
        class_definitions[index].klass=klass;
        class_definitions[index].class_bytes=(unsigned char*)class_bytes;
        class_definitions[index].class_byte_count=class_byte_count;
    }

    //perform redefinition
    err=(*jvmti)->RedefineClasses(jvmti, length, class_definitions);

    if (JVMTI_ERROR_NONE!=err){
        clz= (*env)->FindClass(env, "org/apache/harmony/instrument/internal/InstrumentationImpl");
        method_clear=(*env)->GetMethodID(env, clz, "clear", "()V");
        (*env)->CallVoidMethod(env,objThis,method_clear);
        throw_exception(env,err);
    }
    //free memory
    cleanup(env, class_definitions, length);
    return;
}

void check_jvmti_error(JNIEnv *env, jvmtiError error, const char *msg){
    if(error != JVMTI_ERROR_NONE){
        (*env)->FatalError(env,msg);
    }
    return;
}
