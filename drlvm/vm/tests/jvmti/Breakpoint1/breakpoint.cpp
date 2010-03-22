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
 * @author Pavel Rebriy
 */

#include "agent.h"

static jint lines[] = {50, 55};
static bool error = false;

static void
set_test_success( jvmtiEnv* jvmti_env,
                  JNIEnv* jni_env,
                  jmethodID method)
{
    CHECK_ERROR();

    DEBUG("Setting success == true... ");
    jclass klass;
    jvmtiError result = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method class");
        return;
    }
    jfieldID field = jni_env->GetStaticFieldID(klass, "status", "Z");
    if( !field ) {
        ERR_REPORT("get 'status' field");
        return;
    }
    jni_env->SetStaticBooleanField(klass, field, JNI_TRUE);
    DEBUG("done");
    return;
}

void JNICALL 
agent_callback_MethodEntry( jvmtiEnv* jvmti_env,
                            JNIEnv* jni_env,
                            jthread thread,
                            jmethodID method)
{
    CHECK_ERROR();

    char *name;
    char *descr;
    jvmtiError result = jvmti_env->GetMethodName(method, &name, &descr, NULL);
    if( result != JVMTI_ERROR_NONE
        || strcmp(name, "test") || strcmp(descr, "()V" ) )
    {
        return;
    }
    jclass klass;
    result = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method class");
        return;
    }
    char *class_name;
    result = jvmti_env->GetClassSignature(klass, &class_name, NULL);
    if(result != JVMTI_ERROR_NONE
        || strcmp(class_name, "LBreakpoint1/Breakpoint1;"))
    {
        ERR_REPORT("wrong MethodEntry callback");
        return;
    }
    DEBUG("MethodEntry callback is called for Breakpoint1.Breakpoint1.test()");

    DEBUG("Disabling MethodEntry event... ");
    result = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_METHOD_ENTRY, NULL);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("disable MethodEntry event");
        return;
    }
    DEBUG("done");

    DEBUG("Getting line number table...");
    jvmtiLineNumberEntry *table;
    jint number;
    result = jvmti_env->GetLineNumberTable( method, &number, &table );
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get line number table");
        return;
    }
    DEBUG("done");

    DEBUG("Setting breakpoints... ");
    for(int index = 0; index < 2; index++) {
        bool is_set = false;
        for(jint count = 0; count < number - 1; count++) {
            if(lines[index] >= table[count].line_number
                && lines[index] < table[count + 1].line_number )
            {
                is_set = true;
                DEBUG("Setting breakpoint on line " << lines[index] << "...");
                result = jvmti_env->SetBreakpoint(method, table[count].start_location);
                if(result != JVMTI_ERROR_NONE) {
                    ERR_REPORT("set breakpoint on line " << lines[index]);
                    return;
                }
                DEBUG("done");
                break;
            }
        }
        if(!is_set) {
            ERR_REPORT("set breakpoint on line " << lines[index]);
            return;
        }
    }
    DEBUG("Setting breakpoints... done");

    DEBUG("Deallocating line number table... ");
    result = jvmti_env->Deallocate((unsigned char*)table);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("Deallocate failed!");
        return;
    }
    DEBUG("done");

    return;
}

void JNICALL
agent_callback_Breakpoint( jvmtiEnv* jvmti_env,
                           JNIEnv* jni_env,
                           jthread thread,
                           jmethodID method,
                           jlocation location)
{
    CHECK_ERROR();

    char *name;
    char *descr;
    jvmtiError result = jvmti_env->GetMethodName(method, &name, &descr, NULL);
    if( result != JVMTI_ERROR_NONE
        || strcmp(name, "test") || strcmp(descr, "()V" ) )
    {
        return;
    }
    jclass klass;
    result = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method class");
        return;
    }
    char *class_name;
    result = jvmti_env->GetClassSignature(klass, &class_name, NULL);
    if(result != JVMTI_ERROR_NONE
        || strcmp(class_name, "LBreakpoint1/Breakpoint1;"))
    {
        ERR_REPORT("wrong Breakpoint callback");
        return;
    }
    DEBUG("Breakpoint occupied in function Breakpoint1.Breakpoint1.test()");

    DEBUG("Getting line number table...");
    jvmtiLineNumberEntry *table;
    jint number;
    result = jvmti_env->GetLineNumberTable( method, &number, &table );
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get line number table");
        return;
    }
    DEBUG("done");

    DEBUG("Checking breakpoints...");
    for(jint count = 0; count < number - 1; count++) {
        if(location >= table[count].start_location
            && location < table[count + 1].start_location)
        {
            DEBUG("Breakpoint on line " << table[count].line_number);
            static bool is_occupied = false;
            if( !is_occupied ) {
                if( lines[0] == table[count].line_number ) {
                    // the first breakpoint is occupied
                    is_occupied = true;
                    DEBUG("Clearing the first breakpoint...");
                    result = jvmti_env->ClearBreakpoint(method, location);
                    if(result != JVMTI_ERROR_NONE) {
                        ERR_REPORT("clear breakpoint");
                        return;
                    }
                    DEBUG("done");
                    break;
                } else {
                    ERR_REPORT("wrong breakpoint callback");
                    return;
                }
            } else {
                if( lines[1] == table[count].line_number ) {
                    // the second breakpoint is occupied
                    DEBUG("Disabling Breakpoint event... ");
                    result = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
                        JVMTI_EVENT_BREAKPOINT, NULL);
                    if(result != JVMTI_ERROR_NONE) {
                        ERR_REPORT("disable Breakpoint event");
                        return;
                    }
                    DEBUG("done");
                    set_test_success(jvmti_env, jni_env, method);
                    break;
                } else {
                    ERR_REPORT("wrong breakpoint callback");
                    return;
                }
            }
        }
    }

    DEBUG("Deallocating line number table... ");
    result = jvmti_env->Deallocate((unsigned char*)table);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("Deallocate failed!");
        return;
    }
    DEBUG("done");
    return;
}
