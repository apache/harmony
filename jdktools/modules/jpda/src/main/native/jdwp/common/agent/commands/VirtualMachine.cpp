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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
#include "jvmti.h"
#include "jdwp.h"

#include "VirtualMachine.h"
#include "PacketParser.h"
#include "PacketDispatcher.h"
#include "ThreadManager.h"
#include "ClassManager.h"
#include "ObjectManager.h"
#include "EventDispatcher.h"
#include "TransportManager.h"
#include "CallBacks.h"
#include "ExceptionManager.h"


#include "vmi.h"
#include "hyport.h"

#include <string.h>

using namespace jdwp;
using namespace VirtualMachine;
using namespace CallBacks;

//-----------------------------------------------------------------------------
//VersionHandler---------------------------------------------------------------

int
VirtualMachine::VersionHandler::Execute(JNIEnv *jni) 
{
    ClassManager &clsMgr = AgentBase::GetClassManager();

    char* javaVmVersion = clsMgr.GetProperty(jni, "java.vm.version");
    AgentAutoFree dobj_javaVmVersion(javaVmVersion JDWP_FILE_LINE);

    char *javaVersion = clsMgr.GetProperty(jni, "java.version");
    AgentAutoFree dobj_javaVersion(javaVersion JDWP_FILE_LINE);

    char *javaVmName = clsMgr.GetProperty(jni, "java.vm.name");
    AgentAutoFree dobj_javaVmName(javaVmName JDWP_FILE_LINE);

    char *javaVmInfo = clsMgr.GetProperty(jni, "java.vm.info");
    AgentAutoFree dobj_javaVmInfo(javaVmInfo JDWP_FILE_LINE);

    const char pattern[] = "JVM version %s (%s, %s, %s)";
    const char unknown[] = "?";
    size_t descriptionSize = sizeof(pattern) +
        strlen((javaVersion == 0) ? unknown : javaVersion) +
        strlen((javaVmName == 0) ? unknown : javaVmName) +
        strlen((javaVmInfo == 0) ? unknown : javaVmInfo) +
        strlen((javaVmVersion == 0) ? unknown : javaVmVersion);
    char *description = reinterpret_cast<char*>
        (AgentBase::GetMemoryManager().Allocate(descriptionSize JDWP_FILE_LINE));
    AgentAutoFree dobj_description(description JDWP_FILE_LINE);

    PORT_ACCESS_FROM_ENV(jni);
    hystr_printf(privatePortLibrary, description, (U_32)descriptionSize, pattern, (javaVersion == 0) ? unknown : javaVersion,
        (javaVmName == 0) ? unknown : javaVmName, (javaVmInfo == 0) ? unknown : javaVmInfo, (javaVmVersion == 0) ? unknown : javaVmVersion);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Version: send: description=%s, jdwpMajor=%d, jdwpMinor=%d, vmVersion=%s, vmName=%s",
                    JDWP_CHECK_NULL(description), JDWP_VERSION_MAJOR, JDWP_VERSION_MINOR,
                    JDWP_CHECK_NULL(javaVersion), JDWP_CHECK_NULL(javaVmName)));

    m_cmdParser->reply.WriteString(description);
    m_cmdParser->reply.WriteInt(JDWP_VERSION_MAJOR);
    m_cmdParser->reply.WriteInt(JDWP_VERSION_MINOR);
    m_cmdParser->reply.WriteString(javaVersion);
    m_cmdParser->reply.WriteString(javaVmName);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ClassesBySignatureHandler----------------------------------------------------

int
VirtualMachine::ClassesBySignatureHandler::Execute(JNIEnv *jni) 
{
    const char *signature = m_cmdParser->command.ReadString();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassesBySignature: received: signature=%s", JDWP_CHECK_NULL(signature)));

    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    AgentBase::GetJniEnv()->PushLocalFrame(100);

    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetLoadedClasses(&classCount, &classes));
    JvmtiAutoFree dobj(classes);

    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
        JDWP_SET_EXCEPTION(e);
        return err;
    }

    int i;
    int count = 0;
    for (i = 0; i < classCount; i++)
    {
        if (IsSignatureMatch(classes[i], signature)) {
            classes[count] = classes[i];
            count++;
        } 
    }

    size_t classCountPos = m_cmdParser->reply.GetPosition();
    m_cmdParser->reply.WriteInt(count);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassesBySignature: classes=%d", count));
    
    int notIncludedClasses = 0;
    for (i = count - 1; i >= 0; i --)
    {
        jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(classes[i]);

        jint status;
        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassStatus(classes[i], &status));
        if (err != JVMTI_ERROR_NONE){
            AgentException e(err);
            JDWP_SET_EXCEPTION(e);
            return err;
        }
        if (status == JVMTI_CLASS_STATUS_ARRAY) {
           status = 0;
        } else {
            if ( status == JVMTI_CLASS_STATUS_PRIMITIVE ) {
                status = 0;
            } else {
                if ( (status & JVMTI_CLASS_STATUS_PREPARED) == 0 ) {
                    // Given class is not prepared - don't return such class
                    notIncludedClasses++;
                    continue;
                }
            }
        }
        m_cmdParser->reply.WriteByte((jbyte)refTypeTag);
        m_cmdParser->reply.WriteReferenceTypeID(jni, classes[i]);
        m_cmdParser->reply.WriteInt(status);


#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {    
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(classes[i], &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassesBySignature: class#=%d, refTypeID=%p, status=%d, signature=%s", 
                            i, classes[i], status, JDWP_CHECK_NULL(signature)));
        }
#endif
    }

    if ( notIncludedClasses != 0 ) {
        size_t currentPos = m_cmdParser->reply.GetPosition();
        jint currentLength = m_cmdParser->reply.GetLength();
        m_cmdParser->reply.SetPosition(classCountPos);
        m_cmdParser->reply.WriteInt(count - notIncludedClasses);
        m_cmdParser->reply.SetPosition(currentPos);
        m_cmdParser->reply.SetLength(currentLength);
    }

    AgentBase::GetJniEnv()->PopLocalFrame(NULL);

    return JDWP_ERROR_NONE;
}

bool
VirtualMachine::ClassesBySignatureHandler::IsSignatureMatch(jclass klass,
                                                            const char *signature)
{
    char* sign = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(klass, &sign, 0));
    JvmtiAutoFree dobj(sign);

    if (err != JVMTI_ERROR_NONE){
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "GetClassSignature failed with error %d on signature %s", err, signature));
        return false;
    }

    return strcmp(signature, sign) == 0;
}

//-----------------------------------------------------------------------------
//AllClassesHandler------------------------------------------------------------

int
VirtualMachine::AllClassesHandler::Execute(JNIEnv *jni) 
{
    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();
    jvmtiError err;

    AgentBase::GetJniEnv()->PushLocalFrame(100);
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetLoadedClasses(&classCount, &classes));

    JvmtiAutoFree dobj(classes);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
        JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "AllClasses: classes=%d", classCount));
    size_t classCountPos = m_cmdParser->reply.GetPosition();
    m_cmdParser->reply.WriteInt(classCount);

    // don't trace signatures of all classes
    int notIncludedClasses = 0;
    for (int i = 0; i < classCount; i++) {
        notIncludedClasses += Compose41Class(jni, jvmti, classes[i]);
    }

    if (notIncludedClasses > 0) {
        size_t currentPos = m_cmdParser->reply.GetPosition();
        jint currentLength = m_cmdParser->reply.GetLength();
        m_cmdParser->reply.SetPosition(classCountPos);
        m_cmdParser->reply.WriteInt(classCount - notIncludedClasses);
        m_cmdParser->reply.SetPosition(currentPos);
        m_cmdParser->reply.SetLength(currentLength);
    }

    AgentBase::GetJniEnv()->PopLocalFrame(NULL);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
VirtualMachine::AllClassesHandler::Compose41Class(JNIEnv *jni, jvmtiEnv* jvmti,
        jclass klass) 
{
    jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(klass);

    char* signature = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(klass, &signature, 0));

    JvmtiAutoFree dobj(signature);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    jint status;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassStatus(klass, &status));
    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    // According to JVMTI spec ClassStatus flag for arrays and primitive classes must be zero
    if (status == JVMTI_CLASS_STATUS_ARRAY || status == JVMTI_CLASS_STATUS_PRIMITIVE) {
        status = 0;
    } else if ( (status & JVMTI_CLASS_STATUS_PREPARED) == 0 ) {
        // Given class is not prepared - don't return such class
        return 1;
    }

    m_cmdParser->reply.WriteByte((jbyte)refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, klass);
    m_cmdParser->reply.WriteString(signature);
    m_cmdParser->reply.WriteInt(status);

    return 0;
}

//-----------------------------------------------------------------------------
//AllThreadHandler-------------------------------------------------------------

int
VirtualMachine::AllThreadsHandler::Execute(JNIEnv *jni)
{
    jint totalThreadsCount, threadsCount;
    jthread* threads = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetAllThreads(&totalThreadsCount, &threads));

    JvmtiAutoFree dobj(threads);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    threadsCount = 0;
    ThreadManager& thrdMgr = GetThreadManager();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "AllThreads: threads=%d", totalThreadsCount));

    int i;
    for (i = 0; i < totalThreadsCount; i++)
    {

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "AllThreads: thread#=%d, name=%s, isAgent=%s", 
                            i, JDWP_CHECK_NULL(info.name), (thrdMgr.IsAgentThread(jni, threads[i])?"TRUE":"FALSE")));
        }
#endif

        // don't report internal agent threads
        if ( !thrdMgr.IsAgentThread(jni, threads[i]) ) {
            threads[threadsCount] = threads[i];
            threadsCount++;
        }
    }

    m_cmdParser->reply.WriteInt(threadsCount);
    for (i = 0; i < threadsCount; i++)
    {
        m_cmdParser->reply.WriteThreadID(jni, threads[i]);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//TopLevelThreadGroupsHandler--------------------------------------------------

int
VirtualMachine::TopLevelThreadGroupsHandler::Execute(JNIEnv *jni)
{
    jint groupCount;
    jthreadGroup* groups = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetTopThreadGroups(&groupCount, &groups));

    JvmtiAutoFree dobj(groups);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "TopLevelThreadGroup: send: groupCount=%d", groupCount));
    m_cmdParser->reply.WriteInt(groupCount);
    for (jint i = 0; i < groupCount; i++) {

#ifndef NDEBUG    
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadGroupInfo info;
            info.name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadGroupInfo(groups[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
        
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "TopLevelThreadGroup: send: group#%d, groupID=%p, name=%s", 
                            i, groups[i], JDWP_CHECK_NULL(info.name)));
        }
#endif

        m_cmdParser->reply.WriteThreadGroupID(jni, groups[i]);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//DisposeHandler---------------------------------------------------------------

int
VirtualMachine::DisposeHandler::Execute(JNIEnv *jni) 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Dispose: write reply"));
    int ret = m_cmdParser->WriteReply(jni);
    JDWP_CHECK_RETURN(ret);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Dispose: reset agent"));
    GetPacketDispatcher().Reset(jni);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//IDSizesHandler---------------------------------------------------------------

int
VirtualMachine::IDSizesHandler::Execute(JNIEnv *jni) 
{
    m_cmdParser->reply.WriteInt(FIELD_ID_SIZE);
    m_cmdParser->reply.WriteInt(METHOD_ID_SIZE);
    m_cmdParser->reply.WriteInt(OBJECT_ID_SIZE);
    m_cmdParser->reply.WriteInt(REFERENCE_TYPE_ID_SIZE);
    m_cmdParser->reply.WriteInt(FRAME_ID_SIZE);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//SuspendHandler---------------------------------------------------------------

int
VirtualMachine::SuspendHandler::Execute(JNIEnv *jni) 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Suspend: suspendAll"));
    int ret = GetThreadManager().SuspendAll(jni);
    return ret;
}

//-----------------------------------------------------------------------------
//ResumedHandler---------------------------------------------------------------

int
VirtualMachine::ResumeHandler::Execute(JNIEnv *jni) 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Resume: resumeAll"));
    int ret = GetThreadManager().ResumeAll(jni);

    return ret;
}

//-----------------------------------------------------------------------------
//ExitHandler------------------------------------------------------------------

int
VirtualMachine::ExitHandler::Execute(JNIEnv *jni) 
{
    jint exitCode = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Exit: received: exitCode=%d", exitCode));

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Exit: write reply"));
    // No need to check return code here as we will exit the process immediately
    m_cmdParser->WriteReply(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Exit: reset agent"));
    // No need to check return code here as we will exit the process immediately
    GetTransportManager().Reset();
    
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Exit: terminate process"));
    exit(static_cast<int>(exitCode));

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//CreateStringHandler----------------------------------------------------------

int
VirtualMachine::CreateStringHandler::Execute(JNIEnv *jni) 
{
    const char *utf = m_cmdParser->command.ReadString();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "CreateString: received: string=%s", JDWP_CHECK_NULL(utf)));
    jstring str = jni->NewStringUTF(utf);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "CreateString: send: objectID=%p", str));
    m_cmdParser->reply.WriteObjectID(jni, str);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//CapabilitiesHandler----------------------------------------------------------

inline static jboolean IsAdded(int value)
{
    return (value == 1) ? JNI_TRUE : JNI_FALSE;
}

int
VirtualMachine::CapabilitiesHandler::Execute(JNIEnv *jni)
{
    jdwpCapabilities caps = GetCapabilities();

    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldModification));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldAccess));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetBytecodes));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetSyntheticAttribute));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetOwnedMonitorInfo));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetCurrentContendedMonitor));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetMonitorInfo));

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ClassPathsHandler------------------------------------------------------------

void
VirtualMachine::ClassPathsHandler::WritePathStrings(char *str,
        char pathSeparator)
{
    if (str == 0) {
        m_cmdParser->reply.WriteInt(1);
        m_cmdParser->reply.WriteString(str);
    } else {
        const size_t len = strlen(str);
        jint pathCount = 0;
        size_t i;

        for (i = 0; i < len; i++) {
            if (str[i] == pathSeparator) {
                pathCount++;
            }
        }
        pathCount++;

        m_cmdParser->reply.WriteInt(pathCount);

        char *path = str;
        for (i = 0; i < len; i++) {
            if (str[i] == pathSeparator) {
                str[i] = '\0';
                m_cmdParser->reply.WriteString(path);
                path = &str[i+1];
            }
        }
        m_cmdParser->reply.WriteString(path);
    }
}

int
VirtualMachine::ClassPathsHandler::Execute(JNIEnv *jni) 
{
    ClassManager &clsMgr = AgentBase::GetClassManager();

    char *baseDir = clsMgr.GetProperty(jni, "user.dir");
    AgentAutoFree dobj_baseDir(baseDir JDWP_FILE_LINE);

    char *classPaths = clsMgr.GetProperty(jni, "java.class.path");
    AgentAutoFree dobj_classPaths(classPaths JDWP_FILE_LINE);

    // try several alternatives for boot.class.path
    char *bootClassPaths = clsMgr.GetProperty(jni, "sun.boot.class.path");
    if (bootClassPaths == 0) {
        bootClassPaths = clsMgr.GetProperty(jni, "vm.boot.class.path");
    }
    if (bootClassPaths == 0) {
        bootClassPaths = clsMgr.GetProperty(jni, "org.apache.harmony.boot.class.path");
    }
    AgentAutoFree dobj_bootClassPaths(bootClassPaths JDWP_FILE_LINE);

    char *pathSeparatorString = clsMgr.GetProperty(jni, "path.separator");
    AgentAutoFree dobj_pathSeparatorString(pathSeparatorString JDWP_FILE_LINE);
    char pathSeparator =
        (pathSeparatorString == 0) ? ';' : pathSeparatorString[0];

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassPaths: baseDir=%s", JDWP_CHECK_NULL(baseDir)));
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassPaths: pathSeparatorString=%s", JDWP_CHECK_NULL(pathSeparatorString)));
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassPaths: classPaths=%s", JDWP_CHECK_NULL(classPaths)));
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClassPaths: bootClassPaths=%s", JDWP_CHECK_NULL(bootClassPaths)));

    m_cmdParser->reply.WriteString(baseDir);
    WritePathStrings(classPaths, pathSeparator);
    WritePathStrings(bootClassPaths, pathSeparator);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//DisposeObjectsHandler--------------------------------------------------------

int
VirtualMachine::DisposeObjectsHandler::Execute(JNIEnv *jni) 
{
    jint refCount;
    ObjectID objectID;
    jint objCount = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "DisposeObjects: dispose: objects=%d", objCount));
    for (jint i = 0; i < objCount; i++)
    {
        objectID = m_cmdParser->command.ReadRawObjectID();
        refCount = m_cmdParser->command.ReadInt();
        GetObjectManager().DisposeObject(jni, objectID, refCount);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "DisposeObjects: object#=%d, objectID=%p", i, objectID));
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//HoldEventsHandler------------------------------------------------------------

int
VirtualMachine::HoldEventsHandler::Execute(JNIEnv *jni) 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "HoldEvents: hold events"));
    GetEventDispatcher().HoldEvents();

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ReleaseEventsHandler---------------------------------------------------------

int
VirtualMachine::ReleaseEventsHandler::Execute(JNIEnv *jni) 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReleaseEvents: release events"));
    GetEventDispatcher().ReleaseEvents();

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//CapabilitiesNewHandler-------------------------------------------------------

int
VirtualMachine::CapabilitiesNewHandler::Execute(JNIEnv *jni)
{
    jdwpCapabilities caps = GetCapabilities();

    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldModification));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldAccess));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetBytecodes));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetSyntheticAttribute));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetOwnedMonitorInfo));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetCurrentContendedMonitor));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetMonitorInfo));
    
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canRedefineClasses));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canAddMethod));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canUnrestrictedlyRedefineClasses));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canPopFrames));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canUseInstanceFilters));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetSourceDebugExtension));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canRequestVMDeathEvent));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canSetDefaultStratum));

    // New capabilities for Java 6
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetInstanceInfo));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canRequestMonitorEvents));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetMonitorFrameInfo));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canUseSourceNameFilters));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetConstantPool ));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canForceEarlyReturn));

    for (int i = 0; i < 11; i++){
        m_cmdParser->reply.WriteBoolean(JNI_FALSE);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//RedefineClassesHandler-------------------------------------------------------

class DAgentAutoFree {
public:
    DAgentAutoFree(unsigned char **ptr, size_t count) : m_ptr(ptr) {m_count = count;}
    ~DAgentAutoFree()
    {
        MemoryManager &mm = AgentBase::GetMemoryManager();
        if (m_ptr != 0)
        {
            for (jint i = 0; i < static_cast<long long>(m_count); i++)
            {
                if (m_ptr[i] != 0)
                    mm.Free(m_ptr[i] JDWP_FILE_LINE);
            }

            mm.Free(m_ptr JDWP_FILE_LINE);
        }
    }
private:
    DAgentAutoFree(const DAgentAutoFree& other) : m_ptr(other.m_ptr) { }
    const DAgentAutoFree& operator=(const DAgentAutoFree& r) {return *this;}

    size_t m_count;
    unsigned char **m_ptr;
};

int
VirtualMachine::RedefineClassesHandler::Execute(JNIEnv *jni) 
{
    MemoryManager &mm = GetMemoryManager();

    jint classCount = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "RedefineClasses: received: classCount=%d", classCount));

    jvmtiClassDefinition *classDefs =
        reinterpret_cast<jvmtiClassDefinition *>(mm.Allocate(sizeof(jvmtiClassDefinition)*classCount JDWP_FILE_LINE));
    AgentAutoFree dobjDefs(classDefs JDWP_FILE_LINE);

    {//to regulate an order of destructor jobs
        jint i, j;

        unsigned char **bytes =
            reinterpret_cast<unsigned char **>(mm.Allocate(sizeof(unsigned char *)*classCount JDWP_FILE_LINE));
        for (i = 0; i < classCount; i++) bytes[i] = 0;

        DAgentAutoFree dobjDefs(bytes, classCount);

        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "RedefineClasses: classes=%d", classCount));
        for (i = 0; i < classCount; i++)
        {
            classDefs[i].klass = m_cmdParser->command.ReadReferenceTypeID(jni);
            classDefs[i].class_byte_count = m_cmdParser->command.ReadInt();
            bytes[i] =
                reinterpret_cast<unsigned char *>(mm.Allocate(sizeof(unsigned char) * classDefs[i].class_byte_count JDWP_FILE_LINE));
            for (j = 0; j < classDefs[i].class_byte_count; j++)
                bytes[i][j] = m_cmdParser->command.ReadByte();
            classDefs[i].class_bytes = bytes[i];
#ifndef NDEBUG
            if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {    
                jvmtiError err;
                char* signature = 0;
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(classDefs[i].klass, &signature, 0));
                JvmtiAutoFree afcs(signature);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "RedefineClasses: class#=%d, refTypeID=%p, class_byte_count=%d, signature=%s", 
                                i, classDefs[i].klass, classDefs[i].class_byte_count, JDWP_CHECK_NULL(signature)));
            }
#endif
        }

        jvmtiError err;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->RedefineClasses(classCount, classDefs));

        if (err != JVMTI_ERROR_NONE){
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//SetDefaultStratumHandler-----------------------------------------------------

int
VirtualMachine::SetDefaultStratumHandler::Execute(JNIEnv *jni)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_FUNC_FL, "SetDefaultStratumHandler(%p)", jni));

    char *stratum = m_cmdParser->command.ReadStringNoFree();
    AgentBase::SetDefaultStratum(stratum);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//AllClassesWithGenericHandler-------------------------------------------------

int
VirtualMachine::AllClassesWithGenericHandler::Compose41Class(JNIEnv *jni_env,
            jvmtiEnv* jvmti, jclass klass) 
{
    jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(klass);

    char* signature = 0;
    char* generic = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(klass, &signature, &generic));

    JvmtiAutoFree dobjs(signature);
    JvmtiAutoFree dobjg(generic);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    jint status;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassStatus(klass, &status));
    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    // According to JVMTI spec ClassStatus flag for arrays and primitive classes must be zero
    if (status == JVMTI_CLASS_STATUS_ARRAY || status == JVMTI_CLASS_STATUS_PRIMITIVE) {
        status = 0;
    } else if ( (status & JVMTI_CLASS_STATUS_PREPARED) == 0 ) {
        // Given class is not prepared - don't return such class
        return 1;
    }

    m_cmdParser->reply.WriteByte((jbyte)refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni_env, klass);
    m_cmdParser->reply.WriteString(signature);

    if (generic != 0)
        m_cmdParser->reply.WriteString(generic);
    else
        m_cmdParser->reply.WriteString("");

    m_cmdParser->reply.WriteInt(status);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "AllClassesWithGeneric: typeTag=%d, refTypeID=%p, signature=%s, generic=%s, status=%d", 
                    refTypeTag, klass, JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(generic), status));

    return 0;
}

// New command for Java 6
//-----------------------------------------------------------------------------
//InstanceCountsHandler-------------------------------------------------

int
VirtualMachine::InstanceCountsHandler::Execute(JNIEnv *jni) 
{
    jint refTypesCount = m_cmdParser->command.ReadInt();
    // Illegal argument
    if(refTypesCount < 0) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }

    m_cmdParser->reply.WriteInt(refTypesCount);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InstanceCounts: return the number of counts that follow:%d", refTypesCount));

    // Number of reference types equals zero
    if(0 == refTypesCount) {
        return JDWP_ERROR_NONE;
    }

    jclass jvmClass;
    jvmtiError err;
    // Tag is used to mark object which is reported in FollowReferences
    jlong tag_value = 0xffff;
    jlong tags[1] = {tag_value};
    jint reachableInstancesNum = 0;

    // Initial callbacks for FollowReferences
    // These callbacks will tag the expected objects which are reported in FollowReferences
    jvmtiHeapCallbacks hcbs;
    memset(&hcbs, 0, sizeof(hcbs));
    hcbs.heap_iteration_callback = NULL;
    hcbs.heap_reference_callback = &HeapReferenceCallback;
    hcbs.primitive_field_callback = NULL;
    hcbs.array_primitive_value_callback = NULL;
    hcbs.string_primitive_value_callback = NULL;

    for(int i = 0; i < refTypesCount;i++) {
         jvmClass = m_cmdParser->command.ReadReferenceTypeID(jni);
        // Can be: InternalErrorException, OutOfMemoryException,
        // JDWP_ERROR_INVALID_CLASS, JDWP_ERROR_INVALID_OBJECT
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            char* signature = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(jvmClass, &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InstanceCounts: received: refTypeID=%p, classSignature=%s", jvmClass, JDWP_CHECK_NULL(signature)));
        }
#endif
        
        //It initiates a traversal over the objects that are directly and indirectly reachable from the heap roots.
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->FollowReferences(0, jvmClass,  NULL,
             &hcbs, &tag_value));
        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_CLASS
            // JVMTI_ERROR_INVALID_OBJECT, JVMTI_ERROR_NULL_POINTER 
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        jobject *pResultObjects = 0;

        // Return the instances that have been marked expected tag_value tag.
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetObjectsWithTags(1, tags, &reachableInstancesNum,
            &pResultObjects, NULL));
		JvmtiAutoFree afResultObjects(pResultObjects);
  
        if (err != JVMTI_ERROR_NONE) {
            // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ILLEGAL_ARGUMENT 
            // JVMTI_ERROR_ILLEGAL_ARGUMENT, JVMTI_ERROR_NULL_POINTER  
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        m_cmdParser->reply.WriteLong(reachableInstancesNum);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "InstanceCounts: return the number of instances for the corresponding  reference type:%d",
                        reachableInstancesNum));
	   //Set objects tags back to 0 
       for(int i = 0; i < reachableInstancesNum; i++) {
          JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetTag(pResultObjects[i], 0));
          jni->DeleteLocalRef(pResultObjects[i]);
          if (err != JVMTI_ERROR_NONE) {
              AgentException e(err);
	          JDWP_SET_EXCEPTION(e);
              return err;
          }
        }
        // tag_value is changed to indicate instances of other types 
        tags[0] = ++tag_value;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
