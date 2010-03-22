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

/**
 * @author Vitaly A. Provodin
 */
#include <string.h>

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

using namespace jdwp;
using namespace VirtualMachine;

//-----------------------------------------------------------------------------
//VersionHandler---------------------------------------------------------------

void
VirtualMachine::VersionHandler::Execute(JNIEnv *jni) throw(AgentException)
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
    sprintf(description, pattern,
        (javaVersion == 0) ? unknown : javaVersion,
        (javaVmName == 0) ? unknown : javaVmName,
        (javaVmInfo == 0) ? unknown : javaVmInfo,
        (javaVmVersion == 0) ? unknown : javaVmVersion);

    JDWP_TRACE_DATA("Version: send: "
        << ", description=" << JDWP_CHECK_NULL(description)
        << ", jdwpMajor=" << JDWP_VERSION_MAJOR
        << ", jdwpMinor=" << JDWP_VERSION_MINOR
        << ", vmVersion=" << JDWP_CHECK_NULL(javaVersion)
        << ", vmVersion=" << JDWP_CHECK_NULL(javaVmName)

    );

    m_cmdParser->reply.WriteString(description);
    m_cmdParser->reply.WriteInt(JDWP_VERSION_MAJOR);
    m_cmdParser->reply.WriteInt(JDWP_VERSION_MINOR);
    m_cmdParser->reply.WriteString(javaVersion);
    m_cmdParser->reply.WriteString(javaVmName);
}

//-----------------------------------------------------------------------------
//ClassesBySignatureHandler----------------------------------------------------

void
VirtualMachine::ClassesBySignatureHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    const char *signature = m_cmdParser->command.ReadString();
    JDWP_TRACE_DATA("ClassesBySignature: received: "
        << "signature=" << JDWP_CHECK_NULL(signature));

    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetLoadedClasses(&classCount, &classes));
    JvmtiAutoFree dobj(classes);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

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
    JDWP_TRACE_DATA("ClassesBySignature: classes=" << count);
    
    int notIncludedClasses = 0;
    for (i = 0; i < count; i++)
    {
        jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(classes[i]);

        jint status;
        JVMTI_TRACE(err, jvmti->GetClassStatus(classes[i], &status));
        if (err != JVMTI_ERROR_NONE)
            throw AgentException(err);

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
        m_cmdParser->reply.WriteByte(refTypeTag);
        m_cmdParser->reply.WriteReferenceTypeID(jni, classes[i]);
        m_cmdParser->reply.WriteInt(status);
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {    
            jvmtiError err;
            char* signature = 0;
            JVMTI_TRACE(err, jvmti->GetClassSignature(classes[i], &signature, 0));
            JvmtiAutoFree afcs(signature);
            JDWP_TRACE_DATA("ClassesBySignature: class#=" << i
                << ", refTypeID=" << classes[i]
                << ", status=" << status
                << ", signature=" << JDWP_CHECK_NULL(signature));
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
}

bool
VirtualMachine::ClassesBySignatureHandler::IsSignatureMatch(jclass klass,
                                                            const char *signature)
                                                            throw(AgentException)
{
    char* sign = 0;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(klass, &sign, 0));
    JvmtiAutoFree dobj(sign);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    return strcmp(signature, sign) == 0;
}

//-----------------------------------------------------------------------------
//AllClassesHandler------------------------------------------------------------

void
VirtualMachine::AllClassesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jint classCount = 0;
    jclass* classes = 0;

    jvmtiEnv* jvmti = AgentBase::GetJvmtiEnv();
    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetLoadedClasses(&classCount, &classes));

    JvmtiAutoFree dobj(classes);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("AllClasses: classes=" << classCount);
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
}

//-----------------------------------------------------------------------------

int
VirtualMachine::AllClassesHandler::Compose41Class(JNIEnv *jni, jvmtiEnv* jvmti,
        jclass klass) throw (AgentException)
{
    jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(klass);

    char* signature = 0;

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetClassSignature(klass, &signature, 0));

    JvmtiAutoFree dobj(signature);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    jint status;
    JVMTI_TRACE(err, jvmti->GetClassStatus(klass, &status));
    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    // According to JVMTI spec ClassStatus flag for arrays and primitive classes must be zero
    if (status == JVMTI_CLASS_STATUS_ARRAY || status == JVMTI_CLASS_STATUS_PRIMITIVE) {
        status = 0;
    } else if ( (status & JVMTI_CLASS_STATUS_PREPARED) == 0 ) {
        // Given class is not prepared - don't return such class
        return 1;
    }

    m_cmdParser->reply.WriteByte(refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, klass);
    m_cmdParser->reply.WriteString(signature);
    m_cmdParser->reply.WriteInt(status);

    return 0;
}

//-----------------------------------------------------------------------------
//AllThreadHandler-------------------------------------------------------------

void
VirtualMachine::AllThreadsHandler::Execute(JNIEnv *jni)
    throw(AgentException)
{
    jint totalThreadsCount, threadsCount;
    jthread* threads = 0;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetAllThreads(&totalThreadsCount, &threads));

    JvmtiAutoFree dobj(threads);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    threadsCount = 0;
    ThreadManager& thrdMgr = GetThreadManager();

    JDWP_TRACE_DATA("AllThreads: threads=" << totalThreadsCount);

    int i;
    for (i = 0; i < totalThreadsCount; i++)
    {

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
            JDWP_TRACE_DATA("AllThreads: thread#=" << i 
                << ", name=" << JDWP_CHECK_NULL(info.name)
                << ", isAgent=" << (thrdMgr.IsAgentThread(jni, threads[i])));
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
}

//-----------------------------------------------------------------------------
//TopLevelThreadGroupsHandler--------------------------------------------------

void
VirtualMachine::TopLevelThreadGroupsHandler::Execute(JNIEnv *jni)
    throw(AgentException)
{
    jint groupCount;
    jthreadGroup* groups = 0;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetTopThreadGroups(&groupCount, &groups));

    JvmtiAutoFree dobj(groups);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("TopLevelThreadGroup: send: groupCount=" << groupCount);
    m_cmdParser->reply.WriteInt(groupCount);
    for (jint i = 0; i < groupCount; i++) {

#ifndef NDEBUG    
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadGroupInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadGroupInfo(groups[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
        
            JDWP_TRACE_DATA("TopLevelThreadGroup: send: group#" << i 
                << ", groupID=" << groups[i]
                << ", name=" << JDWP_CHECK_NULL(info.name));
        }
#endif

        m_cmdParser->reply.WriteThreadGroupID(jni, groups[i]);
    }
}

//-----------------------------------------------------------------------------
//DisposeHandler---------------------------------------------------------------

void
VirtualMachine::DisposeHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_DATA("Dispose: write reply");
    m_cmdParser->WriteReply(jni);
    JDWP_TRACE_DATA("Dispose: reset agent");
    GetPacketDispatcher().Reset(jni);
}

//-----------------------------------------------------------------------------
//IDSizesHandler---------------------------------------------------------------

void
VirtualMachine::IDSizesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    m_cmdParser->reply.WriteInt(FIELD_ID_SIZE);
    m_cmdParser->reply.WriteInt(METHOD_ID_SIZE);
    m_cmdParser->reply.WriteInt(OBJECT_ID_SIZE);
    m_cmdParser->reply.WriteInt(REFERENCE_TYPE_ID_SIZE);
    m_cmdParser->reply.WriteInt(FRAME_ID_SIZE);
}

//-----------------------------------------------------------------------------
//SuspendHandler---------------------------------------------------------------

void
VirtualMachine::SuspendHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_DATA("Suspend: suspendAll");
    GetThreadManager().SuspendAll(jni);
}

//-----------------------------------------------------------------------------
//ResumedHandler---------------------------------------------------------------

void
VirtualMachine::ResumeHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_DATA("Resume: resumeAll");
    GetThreadManager().ResumeAll(jni);
}

//-----------------------------------------------------------------------------
//ExitHandler------------------------------------------------------------------

void
VirtualMachine::ExitHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jint exitCode = m_cmdParser->command.ReadInt();
    JDWP_TRACE_DATA("Exit: received: exitCode=" << exitCode);

    JDWP_TRACE_DATA("Exit: write reply");
    m_cmdParser->WriteReply(jni);

    JDWP_TRACE_DATA("Exit: reset agent");
    GetTransportManager().Reset();
    
    JDWP_TRACE_DATA("Exit: terminate process");
    exit(static_cast<int>(exitCode));

/*
    // another variant is to call System.exit()
    ClassManager &clsMgr = AgentBase::GetClassManager();
    jclass klass = clsMgr.GetSystemClass();
    jmethodID methodID = jni->GetStaticMethodID(klass, "exit", "(I)V");
    clsMgr.CheckOnException(jni);

    jni->CallStaticVoidMethod(klass, methodID, exitCode);
    clsMgr.CheckOnException(jni);
*/
}

//-----------------------------------------------------------------------------
//CreateStringHandler----------------------------------------------------------

void
VirtualMachine::CreateStringHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    const char *utf = m_cmdParser->command.ReadString();
    JDWP_TRACE_DATA("CreateString: received: string=" << JDWP_CHECK_NULL(utf));
    jstring str = jni->NewStringUTF(utf);

    JDWP_TRACE_DATA("CreateString: send: objectID=" << str);
    m_cmdParser->reply.WriteObjectID(jni, str);
}

//-----------------------------------------------------------------------------
//CapabilitiesHandler----------------------------------------------------------

inline static jboolean IsAdded(int value)
{
    return (value == 1) ? JNI_TRUE : JNI_FALSE;
}

void
VirtualMachine::CapabilitiesHandler::Execute(JNIEnv *jni)
    throw(AgentException)
{
    jdwpCapabilities caps = GetCapabilities();

    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldModification));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canWatchFieldAccess));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetBytecodes));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetSyntheticAttribute));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetOwnedMonitorInfo));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetCurrentContendedMonitor));
    m_cmdParser->reply.WriteBoolean(IsAdded(caps.canGetMonitorInfo));
}

//-----------------------------------------------------------------------------
//ClassPathsHandler------------------------------------------------------------

void
VirtualMachine::ClassPathsHandler::WritePathStrings(char *str,
        char pathSeparator) throw(AgentException)
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

void
VirtualMachine::ClassPathsHandler::Execute(JNIEnv *jni) throw(AgentException)
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

    JDWP_TRACE_DATA("ClassPaths: baseDir="
        << JDWP_CHECK_NULL(baseDir));
    JDWP_TRACE_DATA("ClassPaths: pathSeparatorString="
        << JDWP_CHECK_NULL(pathSeparatorString));
    JDWP_TRACE_DATA("ClassPaths: classPaths="
        << JDWP_CHECK_NULL(classPaths));
    JDWP_TRACE_DATA("ClassPaths: bootClassPaths="
        << JDWP_CHECK_NULL(bootClassPaths));

    m_cmdParser->reply.WriteString(baseDir);
    WritePathStrings(classPaths, pathSeparator);
    WritePathStrings(bootClassPaths, pathSeparator);
}

//-----------------------------------------------------------------------------
//DisposeObjectsHandler--------------------------------------------------------

void
VirtualMachine::DisposeObjectsHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jint refCount;
    ObjectID objectID;
    jint objCount = m_cmdParser->command.ReadInt();
    JDWP_TRACE_DATA("DisposeObjects: dispose: objects=" << objCount);
    for (jint i = 0; i < objCount; i++)
    {
        objectID = m_cmdParser->command.ReadRawObjectID();
        refCount = m_cmdParser->command.ReadInt();
        GetObjectManager().DisposeObject(jni, objectID, refCount);
        JDWP_TRACE_DATA("DisposeObjects: object#=" << i 
            << ", objectID=" << objectID);
    }
}

//-----------------------------------------------------------------------------
//HoldEventsHandler------------------------------------------------------------

void
VirtualMachine::HoldEventsHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_DATA("HoldEvents: hold events");
    GetEventDispatcher().HoldEvents();
}

//-----------------------------------------------------------------------------
//ReleaseEventsHandler---------------------------------------------------------

void
VirtualMachine::ReleaseEventsHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_DATA("ReleaseEvents: release events");
    GetEventDispatcher().ReleaseEvents();
}

//-----------------------------------------------------------------------------
//CapabilitiesNewHandler-------------------------------------------------------

void
VirtualMachine::CapabilitiesNewHandler::Execute(JNIEnv *jni)
    throw(AgentException)
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

    for (int i = 0; i < 17; i++)
        m_cmdParser->reply.WriteBoolean(JNI_FALSE);
}

//-----------------------------------------------------------------------------
//RedefineClassesHandler-------------------------------------------------------

class DAgentAutoFree {
public:
    DAgentAutoFree(unsigned char **ptr, size_t count) throw() : m_ptr(ptr) {m_count = count;}
    ~DAgentAutoFree() throw()
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

void
VirtualMachine::RedefineClassesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    MemoryManager &mm = GetMemoryManager();

    jint classCount = m_cmdParser->command.ReadInt();
    JDWP_TRACE_DATA("RedefineClasses: received: classCount=" << classCount);

    jvmtiClassDefinition *classDefs =
        reinterpret_cast<jvmtiClassDefinition *>(mm.Allocate(sizeof(jvmtiClassDefinition)*classCount JDWP_FILE_LINE));
    AgentAutoFree dobjDefs(classDefs JDWP_FILE_LINE);

    {//to regulate an order of destructor jobs
        jint i, j;

        unsigned char **bytes =
            reinterpret_cast<unsigned char **>(mm.Allocate(sizeof(unsigned char *)*classCount JDWP_FILE_LINE));
        for (i = 0; i < classCount; i++) bytes[i] = 0;

        DAgentAutoFree dobjDefs(bytes, classCount);

        JDWP_TRACE_DATA("RedefineClasses: classes" << classCount);
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
                JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(classDefs[i].klass, &signature, 0));
                JvmtiAutoFree afcs(signature);
                JDWP_TRACE_DATA("RedefineClasses: class#=" << i
                    << ", refTypeID=" << classDefs[i].klass
                    << ", class_byte_count=" << classDefs[i].class_byte_count 
                    << ", signature=" << JDWP_CHECK_NULL(signature));
            }
#endif
        }

        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->RedefineClasses(classCount, classDefs));

        if (err != JVMTI_ERROR_NONE)
            throw AgentException(err);
    }
}

//-----------------------------------------------------------------------------
//SetDefaultStratumHandler-----------------------------------------------------

void
VirtualMachine::SetDefaultStratumHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    // Note. The SetDefaultStratum handler is not implemented
    // here because JDWP specs are not clear about this command.
    JDWP_TRACE_DATA("SetDefaultStratumHandler: not implemented");
    throw AgentException(JDWP_ERROR_NOT_IMPLEMENTED);
}

//-----------------------------------------------------------------------------
//AllClassesWithGenericHandler-------------------------------------------------

int
VirtualMachine::AllClassesWithGenericHandler::Compose41Class(JNIEnv *jni_env,
            jvmtiEnv* jvmti, jclass klass) throw (AgentException)
{
    jdwpTypeTag refTypeTag = GetClassManager().GetJdwpTypeTag(klass);

    char* signature = 0;
    char* generic = 0;

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetClassSignature(klass, &signature, &generic));

    JvmtiAutoFree dobjs(signature);
    JvmtiAutoFree dobjg(generic);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    jint status;
    JVMTI_TRACE(err, jvmti->GetClassStatus(klass, &status));
    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    // According to JVMTI spec ClassStatus flag for arrays and primitive classes must be zero
    if (status == JVMTI_CLASS_STATUS_ARRAY || status == JVMTI_CLASS_STATUS_PRIMITIVE) {
        status = 0;
    } else if ( (status & JVMTI_CLASS_STATUS_PREPARED) == 0 ) {
        // Given class is not prepared - don't return such class
        return 1;
    }

    m_cmdParser->reply.WriteByte(refTypeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni_env, klass);
    m_cmdParser->reply.WriteString(signature);

    if (generic != 0)
        m_cmdParser->reply.WriteString(generic);
    else
        m_cmdParser->reply.WriteString("");

    m_cmdParser->reply.WriteInt(status);
    JDWP_TRACE_DATA("AllClassesWithGeneric: typeTag=" << refTypeTag << ", refTypeID="
         << klass << ", signature=" << JDWP_CHECK_NULL(signature) << ", generic=" 
         << JDWP_CHECK_NULL(generic) << ", status=" << status);

    return 0;
}

//-----------------------------------------------------------------------------
