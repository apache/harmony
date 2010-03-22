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
#include "CommandDispatcher.h"
#include "PacketParser.h"
#include "TransportManager.h"

#include "CommandHandler.h"

#include "EventRequest.h"
#include "Method.h"
#include "ObjectReference.h"
#include "ReferenceType.h"
#include "ThreadGroupReference.h"
#include "ThreadReference.h"
#include "ThreadGroupReference.h"
#include "ArrayReference.h"
#include "EventRequest.h"
#include "ClassType.h"
#include "StringReference.h"
#include "VirtualMachine.h"
#include "ArrayType.h"
#include "ClassLoaderReference.h"
#include "ClassObjectReference.h"
#include "StackFrame.h"

using namespace jdwp;

//-----------------------------------------------------------------------------
void removeSynchronousHandler(CommandHandler *handler)
{
//    JDWP_TRACE_ENTRY("removeSynchronousHandler(" << handler << ')');

    if (handler == 0)
        return;

    if (handler->IsSynchronous())
    {
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_CMD)) {
            jdwpCommandSet cmdSet = handler->GetCommandParser()->command.GetCommandSet();
            jdwpCommand cmdKind = handler->GetCommandParser()->command.GetCommand();
            JDWP_TRACE_CMD("Remove handler: "
                << CommandDispatcher::GetCommandSetName(cmdSet) << "/"
                << CommandDispatcher::GetCommandName(cmdSet, cmdKind)
                << "[" << cmdSet << "/" << cmdKind << "]");
        }
#endif
        delete handler;
    }
}
//-----------------------------------------------------------------------------

void CommandDispatcher::ExecCommand(JNIEnv* jni, CommandParser *cmdParser)
    throw (AgentException)
{
    JDWP_TRACE_ENTRY("ExecCommand(" << jni << ',' << cmdParser << ')');

    CommandHandler *handler = 0;
    bool isSynchronous = false;
    jdwpError err = JDWP_ERROR_NONE;

    jdwpCommandSet cmdSet = cmdParser->command.GetCommandSet();
    jdwpCommand cmdKind = cmdParser->command.GetCommand();

    try
    {
        if (IsDead())
            throw AgentException(JDWP_ERROR_VM_DEAD);

        JDWP_TRACE_CMD("Create handler: "
            << GetCommandSetName(cmdSet) << "/"
            << GetCommandName(cmdSet, cmdKind)
            << "[" << cmdSet << "/" << cmdKind << "]");
        handler = CreateCommandHandler(cmdSet, cmdKind);
        isSynchronous = handler->IsSynchronous();

        handler->Run(jni, cmdParser);
    }
    catch (const TransportException &e)
    {
        if (isSynchronous) {
            removeSynchronousHandler(handler);
        }
        throw e;
    }
    catch (const AgentException &e)
    {
        if (isSynchronous) {
            removeSynchronousHandler(handler);
        }
        err = e.ErrCode();
        cmdParser->reply.SetError(err);
    }
    
    if (isSynchronous) {
        removeSynchronousHandler(handler);
    }

    if (err != JDWP_ERROR_NONE)
        cmdParser->WriteReply(jni);

    //when command is executed asynchronously,
    //memory is released by AsyncCommandHandler::StartExecution()
}

//-----------------------------------------------------------------------------

CommandHandler*
CommandDispatcher::CreateCommandHandler(jdwpCommandSet cmdSet, jdwpCommand cmdKind)
    throw (NotImplementedException, OutOfMemoryException)
{
    JDWP_TRACE_ENTRY("CreateCommandHandler(" << cmdSet << ',' << cmdKind << ')');

    switch (cmdSet)
    {
    //JDWP_COMMAND_SET_VIRTUAL_MACHINE-------------------------------------
    case JDWP_COMMAND_SET_VIRTUAL_MACHINE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_VM_VERSION :
            return new VirtualMachine::VersionHandler();

        case JDWP_COMMAND_VM_CLASSES_BY_SIGNATURE:
            return new VirtualMachine::ClassesBySignatureHandler();

        case JDWP_COMMAND_VM_ALL_CLASSES :
            return new VirtualMachine::AllClassesHandler();

        case JDWP_COMMAND_VM_ALL_THREADS:
            return new VirtualMachine::AllThreadsHandler();

        case JDWP_COMMAND_VM_TOP_LEVEL_THREAD_GROUPS:
            return new VirtualMachine::TopLevelThreadGroupsHandler();

        case JDWP_COMMAND_VM_DISPOSE:
            return new VirtualMachine::DisposeHandler();
        
        case JDWP_COMMAND_VM_ID_SIZES:
            return new VirtualMachine::IDSizesHandler();

        case JDWP_COMMAND_VM_SUSPEND:
            return new VirtualMachine::SuspendHandler();

        case JDWP_COMMAND_VM_RESUME:
            return new VirtualMachine::ResumeHandler();

        case JDWP_COMMAND_VM_EXIT:
            return new VirtualMachine::ExitHandler();

        case JDWP_COMMAND_VM_CREATE_STRING:
            return new VirtualMachine::CreateStringHandler();

        case JDWP_COMMAND_VM_CAPABILITIES:
            return new VirtualMachine::CapabilitiesHandler();

        case JDWP_COMMAND_VM_CLASS_PATHS:
            return new VirtualMachine::ClassPathsHandler();

        case JDWP_COMMAND_VM_DISPOSE_OBJECTS:
            return new VirtualMachine::DisposeObjectsHandler();

        case JDWP_COMMAND_VM_HOLD_EVENTS:
            return new VirtualMachine::HoldEventsHandler();

        case JDWP_COMMAND_VM_RELEASE_EVENTS:
            return new VirtualMachine::ReleaseEventsHandler();

        case JDWP_COMMAND_VM_CAPABILITIES_NEW:
            return new VirtualMachine::CapabilitiesNewHandler();

        case JDWP_COMMAND_VM_REDEFINE_CLASSES:
            return new VirtualMachine::RedefineClassesHandler();

        case JDWP_COMMAND_VM_SET_DEFAULT_STRATUM:
            return new VirtualMachine::SetDefaultStratumHandler();

        case JDWP_COMMAND_VM_ALL_CLASSES_WITH_GENERIC:
            return new VirtualMachine::AllClassesWithGenericHandler();

        }//JDWP_COMMAND_SET_VIRTUAL_MACHINE
        break;

    //JDWP_COMMAND_SET_REFERENCE_TYPE--------------------------------------
    case JDWP_COMMAND_SET_REFERENCE_TYPE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_RT_SIGNATURE:
            return new ReferenceType::SignatureHandler();

        case JDWP_COMMAND_RT_CLASS_LOADER:
            return new ReferenceType::ClassLoaderHandler();

        case JDWP_COMMAND_RT_MODIFIERS:
            return new ReferenceType::ModifiersHandler();

        case JDWP_COMMAND_RT_FIELDS:
            return new ReferenceType::FieldsHandler();

        case JDWP_COMMAND_RT_METHODS:
            return new ReferenceType::MethodsHandler();

        case JDWP_COMMAND_RT_GET_VALUES:
            return new ReferenceType::GetValuesHandler();

        case JDWP_COMMAND_RT_SOURCE_FILE:
            return new ReferenceType::SourceFileHandler();

        case JDWP_COMMAND_RT_NESTED_TYPES:
            return new ReferenceType::NestedTypesHandler();

        case JDWP_COMMAND_RT_STATUS:
            return new ReferenceType::StatusHandler();

        case JDWP_COMMAND_RT_INTERFACES:
            return new ReferenceType::InterfacesHandler();

        case JDWP_COMMAND_RT_CLASS_OBJECT:
            return new ReferenceType::ClassObjectHandler();

        case JDWP_COMMAND_RT_SOURCE_DEBUG_EXTENSION:
            return new ReferenceType::SourceDebugExtensionHandler();

        case JDWP_COMMAND_RT_SIGNATURE_WITH_GENERIC:
            return new ReferenceType::SignatureWithGenericHandler();

        case JDWP_COMMAND_RT_FIELDS_WITH_GENERIC:
            return new ReferenceType::FieldsWithGenericHandler();

        case JDWP_COMMAND_RT_METHODS_WITH_GENERIC:
            return new ReferenceType::MethodsWithGenericHandler();
        }//JDWP_COMMAND_SET_REFERENCE_TYPE
        break;

    //JDWP_COMMAND_SET_METHOD----------------------------------------------
    case JDWP_COMMAND_SET_METHOD:
        switch (cmdKind)
        {
        case JDWP_COMMAND_M_LINE_TABLE:
            return new Method::LineTableHandler();
        case JDWP_COMMAND_M_VARIABLE_TABLE:
            return new Method::VariableTableHandler();
        case JDWP_COMMAND_M_BYTECODES:
            return new Method::BytecodesHandler();
        case JDWP_COMMAND_M_OBSOLETE:
            return new Method::IsObsoleteHandler();
        case JDWP_COMMAND_M_VARIABLE_TABLE_WITH_GENERIC:
            return new Method::VariableTableWithGenericHandler();
        }
        break;

    //JDWP_COMMAND_SET_OBJECT_REFERENCE------------------------------------
    case JDWP_COMMAND_SET_OBJECT_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_OR_REFERENCE_TYPE:
            return new ObjectReference::ReferenceTypeHandler();

        case JDWP_COMMAND_OR_GET_VALUES:
            return new ObjectReference::GetValuesHandler();

        case JDWP_COMMAND_OR_SET_VALUES:
            return new ObjectReference::SetValuesHandler();

        case JDWP_COMMAND_OR_MONITOR_INFO:
            return new ObjectReference::MonitorInfoHandler();

        case JDWP_COMMAND_OR_INVOKE_METHOD:
            return new ObjectReference::InvokeMethodHandler();

        case JDWP_COMMAND_OR_DISABLE_COLLECTION:
            return new ObjectReference::DisableCollectionHandler();

        case JDWP_COMMAND_OR_ENABLE_COLLECTION:
            return new ObjectReference::EnableCollectionHandler();

        case JDWP_COMMAND_OR_IS_COLLECTED:
            return new ObjectReference::IsCollectedHandler();

        }
        break;

    //JDWP_COMMAND_SET_THREAD_REFERENCE------------------------------------
    case JDWP_COMMAND_SET_THREAD_REFERENCE:
        switch(cmdKind)
        {
        case JDWP_COMMAND_TR_NAME:
            return new ThreadReference::NameHandler();

        case JDWP_COMMAND_TR_SUSPEND:
            return new ThreadReference::SuspendHandler();

        case JDWP_COMMAND_TR_RESUME:
            return new ThreadReference::ResumeHandler();

        case JDWP_COMMAND_TR_STATUS:
            return new ThreadReference::StatusHandler();

        case JDWP_COMMAND_TR_THREAD_GROUP:
            return new ThreadReference::ThreadGroupHandler();

        case JDWP_COMMAND_TR_FRAMES:
            return new ThreadReference::FramesHandler();

        case JDWP_COMMAND_TR_FRAME_COUNT:
            return new ThreadReference::FrameCountHandler();

        case JDWP_COMMAND_TR_OWNED_MONITORS:
            return new ThreadReference::OwnedMonitorsHandler();

        case JDWP_COMMAND_TR_CURRENT_CONTENDED_MONITOR:
            return new ThreadReference::CurrentContendedMonitorHandler();

        case JDWP_COMMAND_TR_STOP:
            return new ThreadReference::StopHandler();

        case JDWP_COMMAND_TR_INTERRUPT:
            return new ThreadReference::InterruptHandler();

        case JDWP_COMMAND_TR_SUSPEND_COUNT:
            return new ThreadReference::SuspendCountHandler();

        }//JDWP_COMMAND_SET_THREAD_REFERENCE
        break;

    case JDWP_COMMAND_SET_THREAD_GROUP_REFERENCE:
        switch (cmdKind)
        {

        case JDWP_COMMAND_TGR_NAME:
            return new ThreadGroupReference::NameHandler();

        case JDWP_COMMAND_TGR_PARENT:
            return new ThreadGroupReference::ParentHandler();

        case JDWP_COMMAND_TGR_CHILDREN:
            return new ThreadGroupReference::ChildrenHandler();

        }//JDWP_COMMAND_SET_THREAD_GROUP_REFERENCE
        break;

    case JDWP_COMMAND_SET_ARRAY_REFERENCE:
        switch (cmdKind)
        {

        case JDWP_COMMAND_AR_LENGTH:
            return new ArrayReference::LengthHandler();

        case JDWP_COMMAND_AR_GET_VALUES:
            return new ArrayReference::GetValuesHandler();

        case JDWP_COMMAND_AR_SET_VALUES:
            return new ArrayReference::SetValuesHandler();
        }
        break;

    //JDWP_COMMAND_SET_EVENT_REQUEST---------------------------------------
    case JDWP_COMMAND_SET_EVENT_REQUEST:
        switch(cmdKind)
        {

        case JDWP_COMMAND_ER_SET:
            return new EventRequest::SetHandler();

        case JDWP_COMMAND_ER_CLEAR:
            return new EventRequest::ClearHandler();

        case JDWP_COMMAND_ER_CLEAR_ALL_BREAKPOINTS:
            return new EventRequest::ClearAllBreakpointsHandler();

        }//JDWP_COMMAND_SET_EVENT_REQUEST
        break;

    //JDWP_COMMAND_SET_CLASS_TYPE--------------------------------------
    case JDWP_COMMAND_SET_CLASS_TYPE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_CT_SUPERCLASS:
            return new ClassType::SuperClassHandler();

        case JDWP_COMMAND_CT_SET_VALUES:
            return new ClassType::SetValuesHandler();
        case JDWP_COMMAND_CT_INVOKE_METHOD:
            return new ClassType::InvokeMethodHandler();

        case JDWP_COMMAND_CT_NEW_INSTANCE:
            return new ClassType::NewInstanceHandler();

        }//JDWP_COMMAND_SET_CLASS_TYPE
        break;

    //JDWP_COMMAND_SET_STRING_REFERENCE--------------------------------------
    case JDWP_COMMAND_SET_STRING_REFERENCE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_SR_VALUE:
            return new StringReference::ValueHandler();

        }//JDWP_COMMAND_SR_VALUE
        break;

    //JDWP_COMMAND_SET_ARRAY_TYPE--------------------------------------
    case JDWP_COMMAND_SET_ARRAY_TYPE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_AT_NEW_INSTANCE:
            return new ArrayType::NewInstanceHandler();

        }
        break;

    //JDWP_COMMAND_SET_CLASS_LOADER_REFERENCE--------------------------------------
    case JDWP_COMMAND_SET_CLASS_LOADER_REFERENCE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_CLR_VISIBLE_CLASSES:
            return new ClassLoaderReference::VisibleClassesHandler();

        }
        break;

    //JDWP_COMMAND_SET_STACK_FRAME--------------------------------------
    case JDWP_COMMAND_SET_STACK_FRAME:
        switch(cmdKind)
        {

        case JDWP_COMMAND_SF_GET_VALUES:
            return new StackFrame::GetValuesHandler();

        case JDWP_COMMAND_SF_SET_VALUES:
            return new StackFrame::SetValuesHandler();

        case JDWP_COMMAND_SF_THIS_OBJECT:
            return new StackFrame::ThisObjectHandler();

        case JDWP_COMMAND_SF_POP_FRAME:
            return new StackFrame::PopFramesHandler();

        }
        break;

    //JDWP_COMMAND_SET_CLASS_OBJECT_REFERENCE--------------------------------------
    case JDWP_COMMAND_SET_CLASS_OBJECT_REFERENCE:
        switch(cmdKind)
        {

        case JDWP_COMMAND_COR_REFLECTED_TYPE:
            return new ClassObjectReference::ReflectedTypeHandler();

        }
        break;

    }//cmdSet

    JDWP_ERROR("command not implemented "
                        << GetCommandSetName(cmdSet) << "/"
                        << GetCommandName(cmdSet, cmdKind)
                        << "[" << cmdSet << "/" << cmdKind << "]");

    throw NotImplementedException();

    // never reached
    return 0;
}

//-----------------------------------------------------------------------------

const char*
CommandDispatcher::GetCommandSetName(jdwpCommandSet cmdSet)
{
    switch (cmdSet)
    {
    case JDWP_COMMAND_SET_VIRTUAL_MACHINE:
        return "VIRTUAL_MACHINE";
    case JDWP_COMMAND_SET_REFERENCE_TYPE:
        return "REFERENCE_TYPE";
    case JDWP_COMMAND_SET_CLASS_TYPE:
        return "CLASS_TYPE";
    case JDWP_COMMAND_SET_ARRAY_TYPE:
        return "ARRAY_TYPE";
    case JDWP_COMMAND_SET_INTERFACE_TYPE:
        return "INTERFACE_TYPE";
    case JDWP_COMMAND_SET_METHOD:
        return "METHOD";
    case JDWP_COMMAND_SET_FIELD:
        return "FIELD";
    case JDWP_COMMAND_SET_OBJECT_REFERENCE:
        return "OBJECT_REFERENCE";
    case JDWP_COMMAND_SET_STRING_REFERENCE:
        return "STRING_REFERENCE";
    case JDWP_COMMAND_SET_THREAD_REFERENCE:
        return "THREAD_REFERENCE";
    case JDWP_COMMAND_SET_THREAD_GROUP_REFERENCE:
        return "THREAD_GROUP_REFERENCE";
    case JDWP_COMMAND_SET_ARRAY_REFERENCE:
        return "ARRAY_REFERENCE";
    case JDWP_COMMAND_SET_CLASS_LOADER_REFERENCE:
        return "CLASS_LOADER_REFERENCE";
    case JDWP_COMMAND_SET_EVENT_REQUEST:
        return "EVENT_REQUEST";
    case JDWP_COMMAND_SET_STACK_FRAME:
        return "STACK_FRAME";
    case JDWP_COMMAND_SET_CLASS_OBJECT_REFERENCE:
        return "CLASS_OBJECT_REFERENCE";
    case JDWP_COMMAND_SET_EVENT:
        return "EVENT";
    }//cmdSet

    return "***UNKNOWN COMMAND_SET***";
}

//-----------------------------------------------------------------------------

const char*
CommandDispatcher::GetCommandName(jdwpCommandSet cmdSet, jdwpCommand cmdKind)
{
    switch (cmdSet)
    {

    case JDWP_COMMAND_SET_VIRTUAL_MACHINE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_VM_VERSION:
            return "VERSION";
        case JDWP_COMMAND_VM_CLASSES_BY_SIGNATURE:
            return "CLASSES_BY_SIGNATURE";
        case JDWP_COMMAND_VM_ALL_CLASSES:
            return "ALL_CLASSES";
        case JDWP_COMMAND_VM_ALL_THREADS:
            return "ALL_THREADS";
        case JDWP_COMMAND_VM_TOP_LEVEL_THREAD_GROUPS:
            return "TOP_LEVEL_THREAD_GROUPS";
        case JDWP_COMMAND_VM_DISPOSE:
            return "DISPOSE";
        case JDWP_COMMAND_VM_ID_SIZES:
            return "ID_SIZES";
        case JDWP_COMMAND_VM_SUSPEND:
            return "SUSPEND";
        case JDWP_COMMAND_VM_RESUME:
            return "RESUME";
        case JDWP_COMMAND_VM_EXIT:
            return "EXIT";
        case JDWP_COMMAND_VM_CREATE_STRING:
            return "CREATE_STRING";
        case JDWP_COMMAND_VM_CAPABILITIES:
            return "CAPABILITIES";
        case JDWP_COMMAND_VM_CLASS_PATHS:
            return "CLASS_PATHS";
        case JDWP_COMMAND_VM_DISPOSE_OBJECTS:
            return "DISPOSE_OBJECTS";
        case JDWP_COMMAND_VM_HOLD_EVENTS:
            return "HOLD_EVENTS";
        case JDWP_COMMAND_VM_RELEASE_EVENTS:
            return "RELEASE_EVENTS";
        case JDWP_COMMAND_VM_CAPABILITIES_NEW:
            return "CAPABILITIES_NEW";
        case JDWP_COMMAND_VM_REDEFINE_CLASSES:
            return "REDEFINE_CLASSES";
        case JDWP_COMMAND_VM_SET_DEFAULT_STRATUM:
            return "SET_DEFAULT_STRATUM";
        case JDWP_COMMAND_VM_ALL_CLASSES_WITH_GENERIC:
            return "ALL_CLASSES_WITH_GENERIC";
        }
        break;

    case JDWP_COMMAND_SET_REFERENCE_TYPE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_RT_SIGNATURE:
            return "SIGNATURE";
        case JDWP_COMMAND_RT_CLASS_LOADER:
            return "CLASS_LOADER";
        case JDWP_COMMAND_RT_MODIFIERS:
            return "MODIFIERS";
        case JDWP_COMMAND_RT_FIELDS:
            return "FIELDS";
        case JDWP_COMMAND_RT_METHODS:
            return "METHODS";
        case JDWP_COMMAND_RT_GET_VALUES:
            return "GET_VALUES";
        case JDWP_COMMAND_RT_SOURCE_FILE:
            return "SOURCE_FILE";
        case JDWP_COMMAND_RT_NESTED_TYPES:
            return "NESTED_TYPES";
        case JDWP_COMMAND_RT_STATUS:
            return "STATUS";
        case JDWP_COMMAND_RT_INTERFACES:
            return "INTERFACES";
        case JDWP_COMMAND_RT_CLASS_OBJECT:
            return "CLASS_OBJECT";
        case JDWP_COMMAND_RT_SOURCE_DEBUG_EXTENSION:
            return "SOURCE_DEBUG_EXTENSION";
        case JDWP_COMMAND_RT_SIGNATURE_WITH_GENERIC:
            return "SIGNATURE_WITH_GENERIC";
        case JDWP_COMMAND_RT_FIELDS_WITH_GENERIC:
            return "FIELDS_WITH_GENERIC";
        case JDWP_COMMAND_RT_METHODS_WITH_GENERIC:
            return "METHODS_WITH_GENERIC";
        }
        break;

    case JDWP_COMMAND_SET_CLASS_TYPE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_CT_SUPERCLASS:
            return "SUPERCLASS";
        case JDWP_COMMAND_CT_SET_VALUES:
            return "SET_VALUES";
        case JDWP_COMMAND_CT_INVOKE_METHOD:
            return "INVOKE_METHOD";
        case JDWP_COMMAND_CT_NEW_INSTANCE:
            return "NEW_INSTANCE";
        }
        break;

    case JDWP_COMMAND_SET_ARRAY_TYPE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_AT_NEW_INSTANCE:
            return "NEW_INSTANCE";
        }
        break;

    case JDWP_COMMAND_SET_INTERFACE_TYPE:
        break;

    case JDWP_COMMAND_SET_METHOD:
        switch (cmdKind)
        {
        case JDWP_COMMAND_M_LINE_TABLE:
            return "LINE_TABLE";
        case JDWP_COMMAND_M_VARIABLE_TABLE:
            return "VARIABLE_TABLE";
        case JDWP_COMMAND_M_BYTECODES:
            return "BYTECODES";
        case JDWP_COMMAND_M_OBSOLETE:
            return "OBSOLETE";
        case JDWP_COMMAND_M_VARIABLE_TABLE_WITH_GENERIC:
            return "VARIABLE_TABLE_WITH_GENERIC";
        }
        break;

    case JDWP_COMMAND_SET_FIELD:
        break;

    case JDWP_COMMAND_SET_OBJECT_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_OR_REFERENCE_TYPE:
            return "REFERENCE_TYPE";
        case JDWP_COMMAND_OR_GET_VALUES:
            return "GET_VALUES";
        case JDWP_COMMAND_OR_SET_VALUES:
            return "SET_VALUES";
        case JDWP_COMMAND_OR_MONITOR_INFO:
            return "MONITOR_INFO";
        case JDWP_COMMAND_OR_INVOKE_METHOD:
            return "INVOKE_METHOD";
        case JDWP_COMMAND_OR_DISABLE_COLLECTION:
            return "DISABLE_COLLECTION";
        case JDWP_COMMAND_OR_ENABLE_COLLECTION:
            return "ENABLE_COLLECTION";
        case JDWP_COMMAND_OR_IS_COLLECTED:
            return "IS_COLLECTED";
        }
        break;

    case JDWP_COMMAND_SET_STRING_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_SR_VALUE:
            return "VALUE";
        }
        break;

    case JDWP_COMMAND_SET_THREAD_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_TR_NAME:
            return "NAME";
        case JDWP_COMMAND_TR_SUSPEND:
            return "SUSPEND";
        case JDWP_COMMAND_TR_RESUME:
            return "RESUME";
        case JDWP_COMMAND_TR_STATUS:
            return "STATUS";
        case JDWP_COMMAND_TR_THREAD_GROUP:
            return "THREAD_GROUP";
        case JDWP_COMMAND_TR_FRAMES:
            return "FRAMES";
        case JDWP_COMMAND_TR_FRAME_COUNT:
            return "FRAME_COUNT";
        case JDWP_COMMAND_TR_OWNED_MONITORS:
            return "OWNED_MONITORS";
        case JDWP_COMMAND_TR_CURRENT_CONTENDED_MONITOR:
            return "CURRENT_CONTENDED_MONITOR";
        case JDWP_COMMAND_TR_STOP:
            return "STOP";
        case JDWP_COMMAND_TR_INTERRUPT:
            return "INTERRUPT";
        case JDWP_COMMAND_TR_SUSPEND_COUNT:
            return "SUSPEND_COUNT";
        }
        break;

    case JDWP_COMMAND_SET_THREAD_GROUP_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_TGR_NAME:
            return "NAME";
        case JDWP_COMMAND_TGR_PARENT:
            return "PARENT";
        case JDWP_COMMAND_TGR_CHILDREN:
            return "CHILDREN";
        }
        break;

    case JDWP_COMMAND_SET_ARRAY_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_AR_LENGTH:
            return "LENGTH";
        case JDWP_COMMAND_AR_GET_VALUES:
            return "GET_VALUES";
        case JDWP_COMMAND_AR_SET_VALUES:
            return "SET_VALUES";
        }
        break;

    case JDWP_COMMAND_SET_CLASS_LOADER_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_CLR_VISIBLE_CLASSES:
            return "VISIBLE_CLASSES";
        }
        break;

    case JDWP_COMMAND_SET_EVENT_REQUEST:
        switch (cmdKind)
        {
        case JDWP_COMMAND_ER_SET:
            return "SET";
        case JDWP_COMMAND_ER_CLEAR:
            return "CLEAR";
        case JDWP_COMMAND_ER_CLEAR_ALL_BREAKPOINTS:
            return "CLEAR_ALL_BREAKPOINTS";
        }
        break;

    case JDWP_COMMAND_SET_STACK_FRAME:
        switch (cmdKind)
        {
        case JDWP_COMMAND_SF_GET_VALUES:
            return "GET_VALUES";
        case JDWP_COMMAND_SF_SET_VALUES:
            return "SET_VALUES";
        case JDWP_COMMAND_SF_THIS_OBJECT:
            return "THIS_OBJECT";
        case JDWP_COMMAND_SF_POP_FRAME:
            return "POP_FRAME";
        }
        break;

    case JDWP_COMMAND_SET_CLASS_OBJECT_REFERENCE:
        switch (cmdKind)
        {
        case JDWP_COMMAND_COR_REFLECTED_TYPE:
            return "REFLECTED_TYPE";
        }
        break;

    case JDWP_COMMAND_SET_EVENT:
        switch (cmdKind)
        {
        case JDWP_COMMAND_E_COMPOSITE:
            return "COMPOSITE";
        }
        break;
    }//cmdSet

    return "***UNKNOWN COMMAND***";
}

//-----------------------------------------------------------------------------
