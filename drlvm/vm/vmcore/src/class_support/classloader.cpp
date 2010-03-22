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
 * @author Pavel Pervov
 */  

#define LOG_DOMAIN LOG_CLASS_INFO
#include "cxxlog.h"
#include "vm_log.h"

#include <sstream>

#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "jit_import_rt.h"
#include "open/vm_properties.h"
#include "classloader.h"
#include "object_layout.h"
#include "String_Pool.h"
//#include "open/vm.h"
#include "exceptions.h"
#include "properties.h"
#include "vm_strings.h"
#include "nogc.h"
#include "bytereader.h"
#include "Package.h"
#include "jvmti_internal.h"
#include "ini.h"
#include "open/gc.h"
#include "open/hythread_ext.h"
#include "open/vm_util.h"
#include "suspend_checker.h"
#include "verifier.h"
#include "classpath_const.h"

#include "port_filepath.h"
#include <apr_file_io.h>
#include <apr_file_info.h>
#include <apr_mmap.h>

#include "jarfile_util.h"
#include "jni_utils.h"
#include "mem_alloc.h"

#include "port_sysencoding.h"

unsigned ClassLoader::m_capacity = 0;
unsigned ClassLoader::m_unloadedBytes = 0;
unsigned ClassLoader::m_nextEntry = 0;
ClassLoader** ClassLoader::m_table = NULL;
Lock_Manager ClassLoader::m_tableLock;

// external declaration; definition is in Class_File_Loader.cpp
const String* class_extract_name(Global_Env* env,
                                 U_8* buffer,
                                 unsigned offset, unsigned length);


bool ClassLoader::Initialize( ManagedObject* loader )
{
    m_loader = loader;
    if(m_loader != NULL) {
        m_name = m_loader->vt()->clss->get_name();
    }
    if(!(m_package_table = new Package_Table())){
        return false;
    }
    m_loadedClasses = new ClassTable();
    if(!m_loadedClasses) return false;
    m_initiatedClasses = new ClassTable();
    if(!m_initiatedClasses) return false;
    m_loadingClasses = new LoadingClasses();
    if(!m_loadingClasses) return false;
    m_reportedClasses = new ReportedClasses();
    if(!m_reportedClasses) return false;
    m_javaTypes = new JavaTypes();
    if(!m_javaTypes) return false;

    Global_Env *env = VM_Global_State::loader_env;
    assert(env);

    assert(env->bootstrap_code_pool_size);
    assert(env->user_code_pool_size);
    size_t code_pool_size = IsBootstrap()
        ? env->bootstrap_code_pool_size
        : env->user_code_pool_size;

    CodeMemoryManager = new PoolManager(code_pool_size, env->use_large_pages, true/*is_code*/);
    if(!CodeMemoryManager) return false;

    return true;
}

void ClassLoader::NotifyUnloading() 
{
    Global_Env *env = VM_Global_State::loader_env;
    env->em_interface->ClassloaderUnloadingCallback((Class_Loader_Handle)this);

    ClassTable::iterator it;
    ClassTable* LoadedClasses = GetLoadedClasses();
    for (it = LoadedClasses->begin(); it != LoadedClasses->end(); it++)
    {
        Class* c;
        c = it->second;
        assert(c);
        c->notify_unloading();
    }
}

ClassLoader::~ClassLoader() 
{
    Global_Env *env = VM_Global_State::loader_env;
    ClassTable::iterator it;
    ClassTable* LoadedClasses = GetLoadedClasses();
    for (it = LoadedClasses->begin(); it != LoadedClasses->end(); it++)
    {
        Class* c;
        c = it->second;
        assert(c);
        ClassClearInternals(c);
    }

    if (GetLoadedClasses())
    {
        VM_Global_State::loader_env->unloaded_class_count += GetLoadedClasses()->GetItemCount();
        delete GetLoadedClasses();
    }
    if (GetLoadingClasses())
        delete GetLoadingClasses();
    if (GetReportedClasses())
        delete GetReportedClasses();
    if (GetVerifyData())
        vf_release_verify_data(GetVerifyData());
    if (GetJavaTypes())
    {
        JavaTypes::iterator itjt;
        for (itjt = GetJavaTypes()->begin(); itjt != GetJavaTypes()->end(); itjt++)
        {
            TypeDesc* td = itjt->second;
            delete td;
        }
        delete GetJavaTypes();
    }
    if (m_package_table)
        delete m_package_table;
    
    for(NativeLibInfo* info = m_nativeLibraries; info;info = info->next ) {
        natives_unload_library(info->handle);        
    }

    delete CodeMemoryManager;
    CodeMemoryManager = NULL;

    apr_pool_destroy(pool);
}

void ClassLoader::LoadingClass::EnqueueInitiator(VM_thread* new_definer, ClassLoader* cl, const String* clsname) 
{
    if(!IsInitiator(new_definer)) {
        if(!IsInitiator(NULL)) { // no initiator, i.e. exitings
            AddWaitingThread(m_initiatingThread, cl, clsname);
            UpdateInitiator(new_definer);
        }
        RemoveWaitingThread(new_definer, cl, clsname);
    }
}

void ClassLoader::LoadingClass::ChangeDefinerAndInitiator(VM_thread* new_definer, ClassLoader* cl, const String* clsname) 
{
    TRACE2("classloader.collisions", cl << " " << new_definer << " CDAI " << m_initiatingThread << " " << clsname->bytes);
    assert(m_defineOwner == NULL);
    m_defineOwner = new_definer;
    EnqueueInitiator(new_definer, cl, clsname);
}

bool ClassLoader::LoadingClass::AlreadyWaiting(VM_thread* thread) 
{
    for(WaitingThread* wt = m_waitingThreads; wt; wt = wt->m_next) {
        if(wt->m_waitingThread == thread) {
            return true;
        }
    }
    return false;
}

void ClassLoader::LoadingClass::AddWaitingThread(VM_thread* thread, ClassLoader* cl, const String* clsname) 
{
    TRACE2("classloader.collisions", cl << " AWT " << thread << " " << clsname->bytes);
    if(!m_threadsPool)
        apr_pool_create(&m_threadsPool, 0);

    WaitingThread* wt =
        (WaitingThread*)apr_palloc(m_threadsPool, sizeof(WaitingThread));
    wt->m_waitingThread = thread;
    wt->m_next = m_waitingThreads;
    m_waitingThreads = wt;
}

void ClassLoader::LoadingClass::RemoveWaitingThread(VM_thread* thread, ClassLoader* cl, const String* clsname) 
{
    TRACE2("classloader.collisions", cl << " RWT " << thread << " " << clsname->bytes);
    // find in waiting threads
    WaitingThread* prev;
    for(WaitingThread* wt = prev = m_waitingThreads; wt; prev = wt, wt = wt->m_next) {
        if(wt->m_waitingThread == thread) {
            // need to remove this element
            if(prev == wt) {
                m_waitingThreads = m_waitingThreads->m_next;
                break;
            }
            prev->m_next = wt->m_next;
            break;
        }
    }
}

Class* ClassLoader::NewClass(const Global_Env* env, const String* name)
{
    Class *clss = NULL;
    TRACE2("classloader.newclass", "allocating Class for \"" << name->bytes << "\"" );

    if(env->InBootstrap() && name == env->JavaLangClass_String) {
        assert(env->JavaLangClass_Class == NULL);
    }

    clss = (Class*)Alloc(sizeof(Class));
    // ppervov: FIXME: should check that class is successfully allocated
    assert(clss);

    clss->init_internals(env, name, this);

    if(clss->get_name())
        clss = AllocateAndReportInstance(env, clss);

    return clss;
}

Class* ClassLoader::DefineClass(Global_Env* env, const char* class_name,
                                U_8* bytecode, unsigned offset, unsigned length,
                                const String** res_name)
{
    assert(!exn_raised());
    const String *className;
    
    TRACE2("classloader.defineclass", "Defining class " << (NULL != class_name ? class_name : "NULL") << " with loader " << this);
    if(class_name) {
        className = env->string_pool.lookup(class_name);
    } else {
        className = class_extract_name(env, bytecode, offset, length);
        if(className == NULL) {
            FailedLoadingClass(className);
            exn_raise_by_name("java/lang/ClassFormatError",
                "class name could not be extracted from provided class data", NULL);
            TRACE("No class name, DefineClass failed with CFE");
            return NULL;
        }
    }
    if (LookupClass(className) != NULL) {
        static const char* mess_templ = "Illegal attempt to redefine class : %s";
        size_t mess_size = strlen(mess_templ) + className->len + 1;
        char* err_mess = (char*)STD_ALLOCA(mess_size);
        sprintf(err_mess, mess_templ, className->bytes);
        exn_raise_object(exn_create("java/lang/LinkageError", err_mess));
        TRACE(err_mess);
        return NULL;
    }
    
    if(res_name) {
        *res_name = className;
    }

    Class* clss = NULL;
    if((clss = WaitDefinition(env, className)) != NULL || exn_raised())
        return clss;

    U_8 *redef_buf = NULL;
    int redef_buflen = 0;
    if(jvmti_should_report_event(JVMTI_EVENT_CLASS_FILE_LOAD_HOOK)) {
        jvmti_send_class_file_load_hook_event(env, this, class_name,
            length, bytecode + offset,
            &redef_buflen, &redef_buf);
    }
    if(NULL != redef_buf)
    {
        bytecode = redef_buf;
        offset = 0;
        length = redef_buflen;
    }

    clss = NewClass(env, className);
    if(!clss) {
        FailedLoadingClass(className);
        return NULL;
    }

    /*
     *  Create a Class File Stream object
     */
    ByteReader cfs(bytecode, offset, length);

    /* 
     * now parse and verify the class data
     */
    if(!clss->parse(env, cfs)) {
        if (NULL != redef_buf)
            _deallocate(redef_buf);
        FailedLoadingClass(className);
        TRACE("Parsing classdata failed: " << className->bytes);
        return NULL;
    }
    if (NULL != redef_buf)
        _deallocate(redef_buf);

    // XXX: should be removed
    // Special case: ClassLoader.defineClass() call with null classname value.
    // Calling AllocateAndReportInstance() after class_parse() which has
    // determined real class name.
    // Note: in ordinary case AddToReported() is called from
    // NewClass()->InitClassFields().
    if( className == NULL ) {
        clss = AllocateAndReportInstance(env, clss);
        if (clss == NULL) {
            FailedLoadingClass(className);
            return NULL;
        }
    }
    // XXX

    if(!clss->load_ancestors(env)) {
        FailedLoadingClass(className);
        return NULL;
    }

    if (!InsertClass(clss)){
        FailedLoadingClass(className);
        return NULL;
    }

    SuccessLoadingClass(className);

    if(this != env->bootstrap_class_loader || !env->InBootstrap())
    {
        if(jvmti_should_report_event(JVMTI_EVENT_CLASS_LOAD)) {
            jvmti_send_class_load_event(env, clss);
        }
    }
    ++(env->total_loaded_class_count);

    return clss;
}


Package* ClassLoader::ProvidePackage(Global_Env* env, const String* class_name,
                                     const char* jar)
{
    const char* clss = class_name->bytes;
    const char* sep = strrchr(clss, '/');
    const String* package_name;
    if (!sep) {
        // this must be the default package...
        package_name = env->string_pool.lookup("");
    } else {
        package_name = env->string_pool.lookup(clss,
            static_cast<unsigned>(sep - clss));
    }
    Lock();
    Package *package = m_package_table->lookup(package_name);
    if (package == NULL) {
        // create a new package
        void* p = apr_palloc(pool, sizeof(Package));
        const char* jar_url = jar ?
            apr_pstrcat(pool, "jar:file:", jar, "!/", NULL) : NULL;
        package = new (p) Package(package_name, jar_url);
        m_package_table->Insert(package);
    }
    Unlock();

    return package;
}

Class* ClassLoader::LoadVerifyAndPrepareClass(Global_Env* env, const String* name)
{
    assert(!exn_raised());
    assert(hythread_is_suspend_enabled());

    Class* clss = LoadClass(env, name);
    if(!clss) return NULL;
    if(!clss->verify(env)) return NULL;
    if(!clss->prepare(env)) return NULL;

    return clss;
}


void ClassLoader::ReportFailedClass(Class* klass, const jthrowable exn)
{
    if(exn_raised()) {
        assert(exn == NULL);
        return;
    }
    exn_raise_object(exn);
}


void ClassLoader::ReportFailedClass(Class* klass, const char* exnclass, std::stringstream& exnmsg)
{
    assert(!exn_raised());
    jthrowable exn = exn_create(exnclass, exnmsg.str().c_str());

    if(exn_raised()) {
        assert(exn == NULL);
        return;
    }
    exn_raise_object(exn);
}


void ClassLoader::ReportFailedClass(const char* klass, const char* exnclass,
                                    std::stringstream& exnmsg)
{
    const String* klassName = VM_Global_State::loader_env->string_pool.lookup(klass);

    assert(!exn_raised());

    jthrowable exn = exn_create(exnclass, exnmsg.str().c_str());
    // ppervov: FIXME: should throw OOME

    if(!exn_raised()) {
        assert(exn != NULL);
        exn_raise_object(exn);
    } else {
        assert(exn == NULL);
    }
}


void ClassLoader::RemoveLoadingClass(const String* className, LoadingClass* loading) 
{
    assert(loading);
    loading->EnqueueInitiator(get_thread_ptr(), this, className);
    loading->UpdateInitiator(NULL);
    if(!loading->HasWaitingThreads()) {
        TRACE2("classloader.collisions", this << " " << get_thread_ptr() << " R " << className->bytes);
        m_loadingClasses->Remove(className);
    }
}


void ClassLoader::SuccessLoadingClass(const String* className) 
{
    LMAutoUnlock aulock( &m_lock );
    LoadingClass* lc = m_loadingClasses->Lookup(className);
    if (lc) {
        lc->SignalLoading();
        RemoveLoadingClass(className, lc);
    }
}

ClassLoader* ClassLoader::FindByObject(ManagedObject* loader)
{
    LMAutoUnlock aulock(&(ClassLoader::m_tableLock));
    ClassLoader* cl;
    for(unsigned i = 0; i < m_nextEntry; i++)
    {
        cl = m_table[i];
        if( loader == cl->m_loader ) return cl;
    }
    return NULL;
}


ClassLoader* ClassLoader::LookupLoader( ManagedObject* loader )
{
    LMAutoUnlock aulock(&(ClassLoader::m_tableLock));
    if( !loader ) return NULL;
    ClassLoader *cl = FindByObject( loader );
    if( cl )
        return cl;
    else
        return AddClassLoader( loader );
}


void ClassLoader::UnloadClassLoader( ClassLoader* loader )
{
    LMAutoUnlock aulock( &(ClassLoader::m_tableLock) );
    unsigned i;
    for(i = 0; i < m_nextEntry; i++)
    {
        ClassLoader* cl = m_table[i];
        if( loader == cl) break;
    }
    if (i == m_nextEntry) return;
    ClassLoader* cl = m_table[i];
    --m_nextEntry;
    for (; i < m_nextEntry; i++)
        m_table[i] = m_table[i+1];
    delete cl; // !!! Must use MM here
}


void ClassLoader::gc_enumerate()
{
    TRACE2("enumeration", "enumerating classes");
    LMAutoUnlock aulock( &(ClassLoader::m_tableLock) );
    
    Boolean do_class_unloading = gc_supports_class_unloading();
    for(unsigned int i = 0; i < m_nextEntry; i++) {
        //assert (m_table[i]->m_loader);
        if (m_table[i]->m_markBit) {
            vm_enumerate_root_reference((void**)(&(m_table[i]->m_loader)), FALSE);
        }
        else {
            if(do_class_unloading)
               vm_enumerate_weak_root_reference((void**)(&(m_table[i]->m_loader)), FALSE);
            else
               vm_enumerate_root_reference((void**)(&(m_table[i]->m_loader)), FALSE);
        }
    }
}


void ClassLoader::ClearMarkBits()
{
    TRACE2("classloader.unloading.clear", "Clearing mark bits");
    assert(!hythread_is_suspend_enabled());
    LMAutoUnlock aulock( &(ClassLoader::m_tableLock) );
    ClassTable::iterator cti;
    for(unsigned i = 0; i < m_nextEntry; i++) {
        TRACE2("classloader.unloading.debug", "  Clearing mark bits in classloader "
            << m_table[i] << " (" << m_table[i]->m_loader << " : "
            << m_table[i]->m_loader->vt_unsafe()->clss->get_name()->bytes << ") and its classes");
        m_table[i]->m_markBit = 0;
        Class*   c;
        ReportedClasses::iterator itc;
        ReportedClasses* RepClasses = m_table[i]->GetReportedClasses();
        for (itc = RepClasses->begin(); itc != RepClasses->end(); itc++)
        {
            c = itc->second;
            if (c && c->get_vtable()) c->get_vtable()->vtmark = 0;
        }
        ClassTable* ct = m_table[i]->GetLoadedClasses();
        ClassTable::iterator it;
        for (it = ct->begin(); it != ct->end(); it++)
        {
            c = it->second;
            if (c && c->get_vtable()) c->get_vtable()->vtmark = 0;
        }
     }
    TRACE2("classloader.unloading.clear", "Finished clearing mark bits");
}

void ClassLoader::StartUnloading()
{
    TRACE2("classloader.unloading.marking", "Finished marking loaders");
    TRACE2("classloader.unloading.do", "Start checking loaders ready to be unloaded");
    LMAutoUnlock aulock( &(ClassLoader::m_tableLock) );
    vector<ClassLoader*> unloadinglist;
    unsigned i;
    for(i = 0; i < m_nextEntry; i++) {
        if(m_table[i]->m_loader) {
           TRACE2("classloader.unloading.debug", "  Skipping live classloader "
                << m_table[i] << " (" << m_table[i]->m_loader << " : "
                << ((VTable*)(*(unsigned**)(m_table[i]->m_loader)))->clss->get_name()->bytes << ")");
            continue;
        }
        TRACE2("classloader.unloading.stats", "  Unloading classloader "
            << m_table[i] << " (" << m_table[i] << ")");
#ifdef _DEBUG // check that all j.l.Classes inside j.l.ClassLoader are also unloaded
        ClassTable* ct = m_table[i]->GetLoadedClasses();
        ClassTable::iterator it;
        for (it = ct->begin(); it != ct->end(); it++)
        {
            Class* c = it->second;
            if (*c->get_class_handle())
            {
                LDIE(72, "FAILED on unloading classloader: \n{0} live j.l.Class of unloaded class is detected: {1}" <<
                    (void*)m_table[i] << c->get_name()->bytes);
            }
         }
#endif
        unloadinglist.push_back(m_table[i]);
    }

    vector<ClassLoader*>::reverse_iterator it;
    for (it = unloadinglist.rbegin(); it != unloadinglist.rend(); it++)
    {
        (*it)->NotifyUnloading();
    }

    // safely remove classloaders from m_table
    // iterate in reverse order to unload child loaders first
    for (it = unloadinglist.rbegin(); it != unloadinglist.rend(); it++)
    {
        UnloadClassLoader(*it);
    }
    
    TRACE2("classloader.unloading.do", "Finished checking loaders");
}


ClassLoader* ClassLoader::AddClassLoader( ManagedObject* loader )
{
    SuspendDisabledChecker sdc;

    ClassLoader* cl = new UserDefinedClassLoader();
    TRACE2("classloader.unloading.add", "Adding class loader "
        << cl << " (" << loader << " : "
        << loader->vt()->clss->get_name()->bytes << ")");
    cl->Initialize( loader );
    if( m_capacity <= m_nextEntry )
        ReallocateTable( m_capacity?(2*m_capacity):32 );
    m_table[m_nextEntry++] = cl;
    return cl;
}


void ClassLoader::ReallocateTable( unsigned int new_capacity )
{
    LMAutoUnlock aulock( &(ClassLoader::m_tableLock) );
    ClassLoader** new_table =
        (ClassLoader**) STD_MALLOC( new_capacity*sizeof(ClassLoader*) );
    if( new_table == NULL ) return;
    memcpy( new_table, m_table, m_nextEntry*sizeof(ClassLoader*) );
    assert(m_nextEntry <= m_capacity);
    assert(m_nextEntry < new_capacity);
    if( m_table ) {
        STD_FREE( m_table );
    }
    m_table = new_table;
    m_capacity = new_capacity;
}


Class* ClassLoader::StartLoadingClass(Global_Env* UNREF env, const String* className)
{
    TRACE2("classloader.loading", "StartLoading class " << className << " with loader " << this);
    Class** pklass = NULL;
    Class* klass = NULL;

    // try to find if class is already loaded
    VM_thread* cur_thread = get_thread_ptr();
    while(true)
    {
        LMAutoUnlock aulock(&m_lock);
        // check if the class has been already recorded as initiated by this loader
        pklass = m_initiatedClasses->Lookup(className);
        if (pklass) return *pklass;

        pklass = m_loadedClasses->Lookup(className);
        if(pklass)
        {
            klass = *pklass;
            // class has already been loaded
            if(klass->get_state() == ST_LoadingAncestors) {
                // there is a circularity in the class hierarchy
                aulock.ForceUnlock();
                REPORT_FAILED_CLASS_CLASS(this, klass, "java/lang/ClassCircularityError", klass->get_name()->bytes);
                return NULL;
            }
            return klass;
        }
        LoadingClass* loading = m_loadingClasses->Lookup(className);
        if(loading)
        {
            if(loading->IsInitiator(cur_thread) || loading->IsDefiner(cur_thread)) {
                // check if one thread can load one class recursively
                // at first sight, this is a circularity error condition
                FailedLoadingClass(className);
                aulock.ForceUnlock();
                REPORT_FAILED_CLASS_NAME(this, className->bytes,
                    "java/lang/ClassCircularityError", className->bytes);
                return NULL;
            }
            TRACE2("classloader.collisions",
                this << " Collision while loading class " << className->bytes);
            if(m_loader == NULL) {
                if(loading->AlreadyWaiting(cur_thread)) {
                    // we spinned one time already;
                    // loading in some other thread failed
                    // so, lets try to load once again in this thread
                    loading->SetInitiator(cur_thread);
                    loading->RemoveWaitingThread(cur_thread, this, className);
                    return NULL;
                }
                // avoid preliminary removal of LoadingClass
                loading->AddWaitingThread(cur_thread, this, className);
                // lazy wait event creation
                aulock.ForceUnlock();
                // only wait for class loading if we are in bootstrap class loader
                loading->WaitLoading();
                continue;
            } else {
                // if we are in a user class loader
                // register as a loading thread
                if(!loading->AlreadyWaiting(cur_thread)) {
                    loading->AddWaitingThread(cur_thread, this, className);
                } else {
                    // it is recursive resolution
                    assert(0 && "Recursive resolution happened!");
                }

                return NULL;
            }
        }
        LoadingClass lc;
#ifdef _DEBUG
        lc.m_name = (String*)className;
#endif
        lc.SetInitiator(cur_thread);
        TRACE2("classloader.collisions", this << " " << cur_thread << " I " << className->bytes);
        m_loadingClasses->Insert(className, lc);
        return NULL;
    }
}

void ClassLoader::FailedLoadingClass(const String* className)
{
    TRACE2("classloader", "Failed loading class " << className << " with loader " << this);
    tmn_suspend_disable();
    LMAutoUnlock aulock( &m_lock );

    LoadingClass* lc = m_loadingClasses->Lookup(className);
    if (lc) {
        lc->SignalLoading();
        RemoveLoadingClass(className, lc);
    }
    RemoveFromReported(className);
    aulock.ForceUnlock();
    tmn_suspend_enable();
}


Class* ClassLoader::WaitDefinition(Global_Env* env, const String* className)
{
    VM_thread* cur_thread = get_thread_ptr();
    LoadingClass* loading;
    Class* clss = NULL;
    m_lock._lock();
    do
    {
        loading = m_loadingClasses->Lookup(className);
        if(loading && m_loadedClasses->Lookup(className) == NULL) {
            TRACE2("classloader.collisions", this << " " << cur_thread << " DC " << className->bytes);
            if(!loading->HasDefiner()) {
                break;
            } else {
                // defining thread should not get here
                assert(!loading->IsDefiner(cur_thread));
                assert(loading->AlreadyWaiting(cur_thread));
                m_lock._unlock();
                TRACE2("classloader.collisions", this << " " << cur_thread << " WAITING " << className->bytes);
                // wait class loading
                loading->WaitLoading();
                m_lock._lock();
                // check if it is not direct defineClass call which happened to
                // compete in defining this class with some other thread
            }
        }

        // if we do not have class with this name already loaded
        if((clss = StartLoadingClass(env, className)) != NULL) {
            if(loading && loading->AlreadyWaiting(cur_thread)) {
                loading->RemoveWaitingThread(cur_thread, this, className);
                if(!loading->HasWaitingThreads()){
                    TRACE2("classloader.collisions", this << " " << cur_thread << " R " << className->bytes);
                    m_loadingClasses->Remove(className);
                }
                m_lock._unlock();
                TRACE2("classloader.collisions", this << " " << cur_thread << " ret " << className->bytes);
                return clss;
            }
            // now we will execute Java code, when creating exception
            // so, unlock class loader lock
            m_lock._unlock();
            // otherwise, we have a class, which was already successfully
            // created earlier, but someone has called ClassLoader.defineClass
            // second time for this class. Spec requires us to throw LinkageError.
            // We only have to report current error condition and do not need
            // to record error state in the class...
            std::stringstream ss;
            ss << "class " << clss->get_name()->bytes << " is defined second time";
            jthrowable exn = exn_create("java/lang/LinkageError", ss.str().c_str());
            exn_raise_object(exn);
            return NULL;
        }

        loading = m_loadingClasses->Lookup(className);
    } while(0);

    assert(loading != NULL);
    // mark ourselves as defining thread
    loading->ChangeDefinerAndInitiator(cur_thread, this, className);

    // unlock class loader and start defining class
    m_lock._unlock();

    return clss;
}


Class* ClassLoader::SetupAsArray(Global_Env* env, const String* classNameString)
{
    // check java/lang/Object is loaded
    assert(env->JavaLangObject_Class != NULL);
    assert(classNameString);
    const char* className = classNameString->bytes;
    // make sure it is array name
    assert(className && className[0] == '[');

    // count number of dimensions in requested array
    unsigned n_dimensions = 1;
    while(className[n_dimensions] == '[') n_dimensions++;

    bool isArrayOfPrimitives = false;
    const char* baseType = &className[n_dimensions];
    Class* baseClass;
    switch(*baseType)
    {
    case 'B':
    case 'C':
    case 'D':
    case 'F':
    case 'I':
    case 'J':
    case 'S':
    case 'Z':
        // for primitive type these must be no more symbols
        // in class name
        if ('\0' != *(baseType + 1))
        {
            FailedLoadingClass(classNameString);
            REPORT_FAILED_CLASS_NAME(this, classNameString->bytes, "java/lang/NoClassDefFoundError", classNameString->bytes);
            return NULL;
        }
        baseClass = class_get_class_of_primitive_type((VM_Data_Type)*baseType);
        isArrayOfPrimitives = true;
        break;
    case 'L':
        {
            unsigned nameLen;
            baseType++;
            for(nameLen = 0; baseType[nameLen] != ';' &&
                             baseType[nameLen] != 0; nameLen++);
            if(!baseType[nameLen]) {
                FailedLoadingClass(classNameString);
                REPORT_FAILED_CLASS_NAME(this, className,
                    VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
                    className);
                return NULL;
            }
            baseClass = LoadVerifyAndPrepareClass(env, env->string_pool.lookup(baseType, nameLen));
            if(baseClass == NULL) {
                FailedLoadingClass(classNameString);
                return NULL;
            }
            isArrayOfPrimitives = false;
        }
        break;

    default:
        FailedLoadingClass(classNameString);
        REPORT_FAILED_CLASS_NAME(this, className,
            VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
            className);
        return NULL;
    }
    ClassLoader* baseLoader = baseClass->get_class_loader();
    Class* elementClass = baseClass;
    if(n_dimensions > 1) {
        elementClass = baseLoader->LoadVerifyAndPrepareClass(env, env->string_pool.lookup(&className[1]));
        if(elementClass == NULL) {
            FailedLoadingClass(classNameString);
            return NULL;
        }
    }

    Class* klass;
    if(baseLoader != this) {
        // if base class was loaded with different loader
        // allow base loader to load the array
        klass = baseLoader->LoadVerifyAndPrepareClass(env, classNameString);
        if(klass) {
            SuccessLoadingClass(classNameString);
        } else {
            FailedLoadingClass(classNameString);
        }
        return klass;
    }

    // we should wait here for creating class
    if((klass = WaitDefinition(env, classNameString)) != NULL)
        return klass;

    // create class
    klass = NewClass(env, classNameString);
    if (!klass) {
        FailedLoadingClass(classNameString);
        return NULL;
    }

    // setup array-related fields
    klass->setup_as_array(env, n_dimensions, isArrayOfPrimitives,
        baseClass, elementClass);

    if(!klass->load_ancestors(env)) {
        FailedLoadingClass(classNameString);
        return NULL;
    }

    if (!InsertClass(klass)){
        FailedLoadingClass(classNameString);
        return NULL;
    }
    SuccessLoadingClass(classNameString);

    return klass;
} // ClassLoader::SetupAsArray


static void set_struct_Class_field_in_java_lang_Class(const Global_Env* env, ManagedObject** jlc, Class *clss)
{
    assert(managed_object_is_java_lang_class(*jlc));
    assert(env->vm_class_offset != 0);

    Class** vm_class_ptr = (Class **)(((U_8*)(*jlc)) + env->vm_class_offset);
    *vm_class_ptr = clss;
} // set_struct_Class_field_in_java_lang_Class


/** Adds Class* pointer to m_reportedClasses HashTable. 
*   clss->m_name must not be NULL.
*/
Class* ClassLoader::AllocateAndReportInstance(const Global_Env* env, Class* clss)
{
     ASSERT_RAISE_AREA;
    const String* name = clss->get_name();
    assert(name);

    if (env->InBootstrap()) {
        // Make sure we are bootstrapping initial VM classes
        assert((name == env->JavaLangObject_String)
             || (strcmp(name->bytes, "java/io/Serializable") == 0)
             || (name == env->JavaLangClass_String)
             || (strcmp(name->bytes, "java/lang/reflect/AnnotatedElement") == 0)
             || (strcmp(name->bytes, "java/lang/reflect/GenericDeclaration") == 0)
             || (strcmp(name->bytes, "java/lang/reflect/Type") == 0));
    } else {
        Class* root_class = env->JavaLangClass_Class;
        assert(root_class != NULL);

        tmn_suspend_disable(); // -----------------vvv
        ManagedObject* new_java_lang_Class = root_class->allocate_instance();
        if(new_java_lang_Class == NULL)
        {
            tmn_suspend_enable();
            // couldn't allocate java.lang.Class instance for this class
            // ppervov: TODO: throw OutOfMemoryError
            exn_raise_object(
                VM_Global_State::loader_env->java_lang_OutOfMemoryError);
            return NULL;
        }
        // add newly created java_lang_Class to reportable collection
        LMAutoUnlock aulock(&m_lock);
        ManagedObject** ch = (ManagedObject**)Alloc(sizeof(ManagedObject*));
        *ch = new_java_lang_Class;
        m_reportedClasses->Insert(name, clss);
        clss->set_class_handle(ch);

        aulock.ForceUnlock();
        TRACE("NewClass inserting class \"" << name->bytes
            << "\" with key " << name << " and object " << new_java_lang_Class);
        assert(new_java_lang_Class == *(clss->get_class_handle()));

        assert(env->vm_class_offset);
        set_struct_Class_field_in_java_lang_Class(env, clss->get_class_handle(), clss);
        tmn_suspend_enable(); // -----------------^^^
    }

    return clss;
}


void ClassLoader::ClassClearInternals(Class* clss)
{
    clss->clear_internals();
}


void ClassLoader::LoadNativeLibrary( const char *name )
{
    // Translate library path to full canonical path to ensure that
    // natives_support:search_library_list() will recognize all loaded libraries
    apr_pool_t* tmp_pool;
    apr_status_t stat = apr_pool_create(&tmp_pool, this->pool);
    // FIXME: process failure properly

    // $$$ GMJ - we don't want it to be where we're running from, but
    //    where everything else came from.  For now, let it be
    //    so that apr_dso_load() will do the right thing.  This is
    //    going to be weird, because we have native code in both
    //    / as well as /default...  FIXME
    //
    // const char* canoname = port_filepath_canonical(name, tmp_pool); 

    const char *canoname = name;
    
    // get library name from string pool
    Global_Env *env = VM_Global_State::loader_env;
    const String *lib_name = env->string_pool.lookup( canoname );

    // free temporary pool
    apr_pool_destroy(tmp_pool);

    // lock class loader
    LMAutoUnlock cl_lock( &m_lock );

    NativeLibInfo* info;

    // find throughout all the libraries in this class loader
    for( info = m_nativeLibraries;
         info;
         info = info->next )
    {
        if( info->name == lib_name ) {
            // found need library
            return;
        }
    }

    // load native library
    bool just_loaded;
    NativeLoadStatus status;
    p_TLS_vmthread->onload_caller = this;
    NativeLibraryHandle handle = natives_load_library(lib_name->bytes, &just_loaded, &status);
    p_TLS_vmthread->onload_caller = NULL;
    if( !handle || !just_loaded ) {
        // create error message
        char apr_error_message[1024];
        natives_describe_error(status, apr_error_message,
                sizeof(apr_error_message));

        std::stringstream UNREF message_stream;
        message_stream << "Failed loading library \"" << lib_name->bytes << "\": " 
                << apr_error_message;

        // trace
        TRACE2("classloader.native", "Loader (" << this << ") native library: "
            << message_stream.str().c_str());

        int converted_buffer_size =
            port_get_utf8_converted_system_message_length(apr_error_message);
        char *converted_error_message = (char *)STD_ALLOCA(converted_buffer_size);
        port_convert_system_error_message_to_utf8(converted_error_message,
            converted_buffer_size, apr_error_message);

        std::stringstream converted_message_stream;
        converted_message_stream << "Failed loading library \"" <<
            lib_name->bytes << "\": " << converted_error_message;

        // unlock class loader
        cl_lock.ForceUnlock();

        // report exception
        ReportException("java/lang/UnsatisfiedLinkError", converted_message_stream);
        return;
    }

    // trace
    TRACE2("classloader.native", "Loader (" << this
        << ") loaded native library: " << lib_name->bytes);

    // allocate memory
    info = (NativeLibInfo*)Alloc(sizeof(NativeLibInfo));

    // set native library
    info->name = lib_name;
    info->handle = handle;
    info->next = m_nativeLibraries;
    m_nativeLibraries = info;
    return;
}

GenericFunctionPointer ClassLoader::LookupNative(Method* method)
{
    // get class and class loader of a given method
    Class* klass = method->get_class();

    // get class name, method name and method descriptor
    const String* class_name = klass->get_name();
    const String* method_name = method->get_name();
    const String* method_desc = method->get_descriptor();

    // trace
    TRACE2("classloader.native", "Loader (" << this << ") find native function: "
        << class_name->bytes << "."
        << method_name->bytes << method_desc->bytes);

    // return if method is already registered
    if (NULL != method->get_registered_native_func()) {
        return (GenericFunctionPointer) method->get_registered_native_func();
    }
    
    // find throughout all the libraries 
    GenericFunctionPointer func = natives_lookup_method(m_nativeLibraries,
        class_name->bytes, method_name->bytes, method_desc->bytes);
    if(func) return func;

    // create error string "<class_name>.<method_name><method_descriptor>
    int clen = class_name->len;
    int mlen = method_name->len;
    int dlen = method_desc->len;
    int len = clen + 1 + mlen + dlen;
    char *error = (char*)STD_ALLOCA(len + 1);
    memcpy(error, class_name->bytes, clen);
    error[clen] = '.';
    memcpy(error + clen + 1, method_name->bytes, mlen);
    memcpy(error + clen + 1 + mlen, method_desc->bytes, dlen);
    error[len] = '\0';

    // trace
    TRACE2("classloader.native", "Loader (" << this << ") native function not found: "
        << class_name->bytes << "."
        << method_name->bytes << method_desc->bytes);

    // raise exception
    jthrowable exc_object = exn_create( "java/lang/UnsatisfiedLinkError", error);
    exn_raise_object(exc_object);

    return NULL;
}

void class_unloading_clear_mark_bits() {
    ClassLoader::ClearMarkBits();
}

void class_unloading_start() {
    ClassLoader::StartUnloading();
}

inline void
BootstrapClassLoader::SetClasspathFromString(char* bcp,
                                               apr_pool_t* tmp_pool)
{
    assert(bcp);

    // set bootclasspath elements
    char *path_name = strtok(bcp, PORT_PATH_SEPARATOR_STR);
    while (path_name)
    {
        SetBCPElement(path_name, tmp_pool);
        path_name = strtok(NULL, PORT_PATH_SEPARATOR_STR);
    }

    return;
} // BootstrapClassLoader::SetClasspathFromProperty

inline void BootstrapClassLoader::SetBCPElement(const char *path, apr_pool_t *tmp_pool)
{
    BCPElement* element;
    // check existence of a given path in bootstrap classpath
    const char* canoname = port_filepath_canonical(path, tmp_pool); 
    const String* new_path = m_env->string_pool.lookup(canoname);
    for( element = m_BCPElements.m_first; element; element = element->m_next ) {
        if( element->m_path == new_path ) {
            // found such path
            return;
        }
    }

    // check existence of a given path
    apr_finfo_t finfo;
    if(apr_stat(&finfo, new_path->bytes, APR_FINFO_SIZE, tmp_pool) != APR_SUCCESS) {
        // broken path to the file
        return;
    }

    element = (BCPElement*)apr_palloc(pool, sizeof(BCPElement) );
    element->m_path = new_path;
    element->m_next = NULL;
    element->m_isJarFile = file_is_archive(new_path->bytes);
    if(element->m_isJarFile) {
        JarEntryCache* jec = NULL;
        if(m_env->use_common_jar_cache
            &&m_BCPElements.m_last
            && m_BCPElements.m_last->m_isJarFile)
        {
            // if previous boot class path element is jar, reuse the same cache
            jec = m_BCPElements.m_last->m_jar->GetCache();
        }
        void* mem_JarFile = apr_palloc(pool, sizeof(JarFile));
        element->m_jar = new (mem_JarFile) JarFile(jec);

        if(element->m_jar && element->m_jar->Parse(new_path->bytes, m_env->map_bootsrtap_jars))
        {
            TRACE2("classloader.jar", "opened archive: " << new_path );
        }
    }

    // insert element into collection
    if( NULL == m_BCPElements.m_first ) {
        m_BCPElements.m_first = element;
    } else {
        m_BCPElements.m_last->m_next = element;
    }
    m_BCPElements.m_last = element;

    return;
} // BootstrapClassLoader::SetBCPElement

inline void BootstrapClassLoader::SetClasspathFromJarFile(JarFile *jar, apr_pool_t* tmp_pool)
{
    // get classpath from archive file
    const char* jar_classpath = archive_get_class_path(jar);
    if( !jar_classpath ) {
        // no classpath found
        return;
    }

    // copy a given classpath
    size_t len = strlen(jar_classpath) + 1;
    char* classpath = (char*)STD_ALLOCA(len);
    memcpy(classpath, jar_classpath, len);
    STD_FREE((void*)jar_classpath); //FIXME inconsistent MM
#ifdef PLATFORM_NT
    //on windows, we change the path to lower case
    strlwr(classpath);
#endif // PLATFORM_NT

    // get archive file directory
    const char* jar_name = jar->GetName();
    assert(jar_name);
    len = strlen(jar_name) + 1;
    char* dir_name = (char*)STD_ALLOCA(len);
    memcpy(dir_name, jar_name, len);
    char* end_dir = strrchr(dir_name, PORT_FILE_SEPARATOR);
    if( end_dir ) {
        // archive file name has file separator
        // find end of directory name
        while( *(end_dir - 1) == PORT_FILE_SEPARATOR ) {
            end_dir--;
        }
        *(end_dir + 1) = '\0';
#ifdef PLATFORM_NT
        //on windows, we change the path to lower case
        strlwr(dir_name);
#endif // PLATFORM_NT
    } else {
        // only file name without directory
        dir_name = NULL;
    }

    // combine classpath as a directory name + classpath
    // and set into bootclasspath collection
    char* path_name = strtok(classpath, " ");
    while( path_name != NULL ) {
        if( dir_name ) {
            // catenate directory name and classpath
            path_name = apr_pstrcat(tmp_pool, dir_name, path_name, NULL );
        }
        SetBCPElement(path_name, tmp_pool);
        path_name = strtok(NULL, " ");
    }

    return;
} // BootstrapClassLoader::SetClasspathFromJarFile

BootstrapClassLoader::BootstrapClassLoader(Global_Env* env) : m_env(env)
{
    // create array of primitive types
    primitive_types[0] = new TypeDesc(K_S1, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[1] = new TypeDesc(K_S2, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[2] = new TypeDesc(K_S4, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[3] = new TypeDesc(K_S8, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[4] = new TypeDesc(K_Sp, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[5] = new TypeDesc(K_U1, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[6] = new TypeDesc(K_U2, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[7] = new TypeDesc(K_U4, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[8] = new TypeDesc(K_U8, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[9] = new TypeDesc(K_Up, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[10] = new TypeDesc(K_F4, NULL, NULL, NULL,(ClassLoader*)this, NULL);
    primitive_types[11] = new TypeDesc(K_F8, NULL, NULL, NULL,(ClassLoader*)this, NULL);
    primitive_types[12] = new TypeDesc(K_Boolean, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[13] = new TypeDesc(K_Char, NULL, NULL, NULL, (ClassLoader*)this, NULL);
    primitive_types[14] = new TypeDesc(K_Void, NULL, NULL, NULL, (ClassLoader*)this, NULL);
}

BootstrapClassLoader::~BootstrapClassLoader()
{
    // destroy array of primitive types
    unsigned sz = sizeof(primitive_types) / sizeof(TypeDesc*);
    for (unsigned i = 0; i < sz; i++)
    {
        delete primitive_types[i];
        primitive_types[i] = NULL;
    }

    for(BCPElement* bcpe = m_BCPElements.m_first;
        bcpe != NULL; bcpe = bcpe->m_next)
    {
        if(bcpe->m_isJarFile) {
            bcpe->m_jar->~JarFile();
            bcpe->m_jar = NULL;
        }
    }
}


/**
* Part of bootstrap classpath composed with jars from components.
*/
static char* bootstrap_components_classpath(Global_Env *vm_env) {
    const char* PREFIX = "vm.component.classpath.";

    const char* kernel_dir_path = vm_env->JavaProperties()->get(O_A_H_VM_VMDIR);
    char* buf = NULL;
    size_t buf_size = 0, buf_len = 0;

    char** keys = vm_env->VmProperties()->get_keys_staring_with(PREFIX);
    unsigned i = 0;
    for (; keys[i]; ++i) 
    {
        const char* key = keys[i];
        const char* path = vm_env->VmProperties()->get(keys[i]);
        size_t path_len = strlen(path) + strlen(PORT_PATH_SEPARATOR_STR) + 1;
        // check if path must be extended
        bool no_dir = (strstr(path, PORT_FILE_SEPARATOR_STR) == NULL);
        if (no_dir) {
            path_len += strlen(kernel_dir_path) + 1;
        }
        if (buf_len + path_len > buf_size) {
            buf_size += path_len * 2;
            buf = (char*)STD_REALLOC(buf, buf_size);
            buf[buf_len] = '\0';
        }
        buf_len += path_len;
        if (no_dir) {
            strcat(buf, kernel_dir_path);
            strcat(buf, PORT_FILE_SEPARATOR_STR);
        }
        strcat(buf, path);
        strcat(buf, PORT_PATH_SEPARATOR_STR);
        vm_env->VmProperties()->destroy((char*)path);
    } 
    vm_env->VmProperties()->destroy(keys);
    vm_env->JavaProperties()->destroy(const_cast<char*>(kernel_dir_path));

    return buf;
}


static int calculate_subst_len(const char* value,
                             const char* subst, const int subst_len,
                             const char* substby, const int substby_len)
{
    int len = (int) strlen(value);
    for(; *value; value++) {
        if(*value != '%') {
            continue;
        }
        if(strstr(value, subst) == value) {
            len += substby_len - subst_len;
            value += subst_len - 1; // -1 to cover ++ in loop
            break;
        }
    }
    return len;
}


static void fillin_with_subst(char* bootClassPath, const char* path_elem,
                              const int max_len,
                              const char* subst, const int subst_len,
                              const char* substby, const int substby_len)
{
    int pos = (int) strlen(bootClassPath);
    for(; *path_elem; path_elem++) {
        assert(pos < max_len);
        if(*path_elem != '%') {
            // just copy a char
            bootClassPath[pos] = *path_elem;
            pos++;
            continue;
        }
        if (strstr(path_elem, subst) == path_elem) {
            // make substitution
            strcat(bootClassPath, substby);
            // -1 for next iteration of outer loop
            path_elem += subst_len - 1;
            pos += substby_len;
        }
    }
    bootClassPath[pos] = '\0';
}


static char* construct_kernel_BCP(const char* vm_dir, Properties& props)
{
    static const char* subst_item = "%LAUNCHER_HOME%/%VM_DIR%";
    static const int subst_len = 24;
    int subst_value_len = (int) strlen(vm_dir);

    char** keys = props.get_keys();
    char** cur_key = keys;
    int bcpVmLen = 0, bcpElemId;
    while(*cur_key) {
        char* value = props.get(*cur_key);
        if(sscanf(*cur_key, "bootclasspath.kernel.%d",
                &bcpElemId) == 1)
        {
            bcpVmLen +=
                calculate_subst_len(value, subst_item, subst_len, vm_dir, subst_value_len)
                + ((int) strlen(PORT_PATH_SEPARATOR_STR));
        }
        props.destroy(value);
        cur_key++;
    }

    char* bootClassPath = NULL;
    if(bcpVmLen != 0) {
        bootClassPath = (char*)STD_MALLOC(bcpVmLen);
        bootClassPath[0] = '\0';
        cur_key = keys;
        for(int l = 0; *cur_key; cur_key++)
        {
            char* value = props.get(*cur_key);
            if(sscanf(*cur_key, "bootclasspath.kernel.%d",
                    &bcpElemId) == 1)
            {
                if(l++ != 0) {
                    strcat(bootClassPath, PORT_PATH_SEPARATOR_STR);
                }
                // fill in with substitution
                fillin_with_subst(bootClassPath, value, bcpVmLen,
                    subst_item, subst_len, vm_dir, subst_value_len);
            }
            props.destroy(value);
        }
    }

    props.destroy(keys);

    return bootClassPath;
}


static void load_properties(const char* path, const char* name, Properties& props)
{
    apr_pool_t* tmp_pool;
    apr_pool_create(&tmp_pool, NULL);
    char* propfile_name =
        (char*)apr_palloc(tmp_pool, strlen(path) + strlen(name)
            + strlen("/.properties\0"));
    // the last strlen is for path separator, properties file extension, and terminating zero
    strcpy(propfile_name, path);
    strcat(propfile_name, PORT_FILE_SEPARATOR_STR);
    strcat(propfile_name, name);
    strcat(propfile_name, ".properties" );

    apr_file_t* propfile;
    apr_status_t status =
        apr_file_open(&propfile, propfile_name, APR_READ, APR_OS_DEFAULT, tmp_pool);
    assert(!status);

    apr_off_t size = 0;
    status = apr_file_seek(propfile, APR_END, &size);
    assert(!status);

    apr_mmap_t* mmap_pf;
    status = apr_mmap_create(&mmap_pf, propfile, 0, (apr_size_t)size,
        APR_MMAP_READ, tmp_pool);
    assert(!status);

    char* pf_data = (char*)mmap_pf->mm;

    int line_len = 0;
    char* line_start = pf_data;
    char* line_end;
    int key_len = 0, value_len = 0;
    char* key_holder = (char*)STD_MALLOC(key_len);
    char* value_holder = (char*)STD_MALLOC(value_len);
    for(int i = 0; i < size; i++, pf_data++) {
        if(*pf_data == 0xA || *pf_data == 0xD) {
            line_len = (int) (pf_data - line_start);
            if(line_len == 0) {
                line_start = pf_data + 1;
                continue;
            }
            if(*line_start != '#') {
                line_end = pf_data - 1;
                // process key=value
                char* delim = line_start;
                for(; delim <= line_end && *delim != '='; delim++);
                if(delim <= line_end) {
                    int cur_key_len = (int) (delim - line_start);
                    if(key_len < cur_key_len) {
                        key_len = cur_key_len + 1;
                        key_holder = (char*)STD_REALLOC(key_holder, key_len);
                    }
                    strncpy(key_holder, line_start, cur_key_len);
                    key_holder[cur_key_len] = '\0';
                    delim++; // move delimeter to point at the beginning of value
                    char* value_end = delim;
                    // limit value by either line_end of start of comment
                    for(; value_end <= line_end && *value_end != '#'; value_end++);
                    int cur_value_len = (int) (value_end - delim);
                    if(value_len < cur_value_len) {
                        value_len = cur_value_len + 1;
                        value_holder = (char*)STD_REALLOC(value_holder, value_len);
                    }
                    strncpy(value_holder, delim, cur_value_len);
                    value_holder[cur_value_len] = '\0';
                    props.set(key_holder, value_holder);
                } // else the line is malformed and we just skip it
            }
            // go to next line
            line_start = pf_data + 1;
        }
    }

    STD_FREE(key_holder);
    STD_FREE(value_holder);
    apr_mmap_delete(mmap_pf);
    apr_file_close(propfile);
    apr_pool_destroy(tmp_pool);
}

static char* get_kernel_path(Global_Env* env) {
    char *vmdir = env->JavaProperties()->get(O_A_H_VM_VMDIR);
    assert(vmdir);

    Properties hyvmprops;
    load_properties(vmdir, "harmonyvm", hyvmprops);

    char* kernel_bcp = construct_kernel_BCP(vmdir, hyvmprops);

    env->JavaProperties()->destroy(vmdir);

    return kernel_bcp;
}


bool BootstrapClassLoader::Initialize(ManagedObject* UNREF loader)
{
    // init common class loader internals
    ClassLoader::Initialize();

    // get list of natives libraries
    char *lib_list = m_env->VmProperties()->get("vm.other_natives_dlls");
             
    // separate natives libraries
    char *lib_name = strtok( lib_list, PORT_PATH_SEPARATOR_STR );

    while( lib_name != NULL )
    {
        // load native library
        LoadNativeLibrary( lib_name );

        // find next library
        lib_name = strtok( NULL, PORT_PATH_SEPARATOR_STR );
    }
    m_env->VmProperties()->destroy(lib_list);

    /*
     * At this point, LUNI is loaded, so we can use the boot class path
     * generated there to set vm.boot.class.path since we didn't do
     * it before.  We also need to add what is listed in harmonyvm.properties
     * DRLVM properties file.
     * NOTE: parse_arguments.cpp defers any override, append or prepend here,
     * storing everything as properties.
     */


    char* xbcp = m_env->VmProperties()->get(XBOOTCLASSPATH);
    char* prepend = m_env->VmProperties()->get(XBOOTCLASSPATH_P);
    char* append = m_env->VmProperties()->get(XBOOTCLASSPATH_A);

    size_t bcp_p_len = (prepend ? strlen(prepend) + strlen(PORT_PATH_SEPARATOR_STR) : 0);
    size_t bcp_a_len = (append ? strlen(PORT_PATH_SEPARATOR_STR) + strlen(append) : 0);
    char* vmboot = NULL;
    // If boot classpath is overridden, just use that value.
    if(xbcp) {
        vmboot = (char*)STD_MALLOC(strlen(xbcp) + bcp_a_len + bcp_p_len + 1);
        strcpy(vmboot + bcp_p_len, xbcp);
        m_env->VmProperties()->destroy(xbcp);
    }

    if(vmboot == NULL) { 
        // Not overridden, so lets build
        // First, helper jars
        char* comp_path = bootstrap_components_classpath(m_env);
        // Second, read in harmonyvm.properties
        char* kernel_bcp = get_kernel_path(m_env);
        // Third, what is provided by LUNI as class library classes
        char *luni_path = m_env->JavaProperties()->get(O_A_H_BOOT_CLASS_PATH);

        vmboot = (char*)STD_MALLOC(
            bcp_p_len
            + (comp_path ? strlen(comp_path) : 0)
            + strlen(kernel_bcp)
            + (luni_path ? strlen(PORT_PATH_SEPARATOR_STR) + strlen(luni_path) : 0)
            + bcp_a_len
            + 1);
        char* vmboot_start = vmboot + bcp_p_len;
        *vmboot = '\0'; // make allocated buffer look like a C-string
        *vmboot_start = '\0';

        if(comp_path != NULL) {
            strcpy(vmboot_start, comp_path);
            STD_FREE(comp_path);
        }
        strcat(vmboot_start, kernel_bcp);
        STD_FREE(kernel_bcp);

        if(luni_path != NULL) {
            strcat(vmboot_start, PORT_PATH_SEPARATOR_STR);
            strcat(vmboot_start, luni_path);
            m_env->JavaProperties()->destroy(luni_path);
        }
    }

    /*
     *  now if there a pre or post bootclasspath, add those
     */

    if (prepend || append) {
        if (prepend) {
            strcpy(vmboot, prepend);
            vmboot[bcp_p_len-1] = PORT_PATH_SEPARATOR;
            m_env->VmProperties()->destroy(prepend);
        }

        if (append) {
            strcat(vmboot, PORT_PATH_SEPARATOR_STR);
            strcat(vmboot, append);
            m_env->VmProperties()->destroy(append);
        }
    }

    // create temp pool for apr functions
    apr_pool_t *tmp_pool;
    apr_pool_create(&tmp_pool, NULL);

    // create a bootclasspath collection

    SetClasspathFromString(vmboot, tmp_pool);
    STD_FREE(vmboot);

    // get a classpath from archive files manifests and
    // set into boot class path collection
    for( BCPElement* element = m_BCPElements.m_first;
         element;
         element = element->m_next )
    {
        TRACE2("classloader.bootclasspath", "BCP: " << element->m_path->bytes );
        if( element->m_isJarFile ) {
            SetClasspathFromJarFile( element->m_jar, tmp_pool );
        }
    }

    // Here we have constructed boot class path without appended application class path
    size_t vmboot_len = 0;
    // Count new path length
    for(BCPElement* element = m_BCPElements.m_first;
        element;
        element = element->m_next)
    {
        vmboot_len += element->m_path->len + 1; // 1 is for either PATH_SEPARATOR or \0
    }
    vmboot = (char*)STD_MALLOC(vmboot_len);
    vmboot[0] = '\0';
    for(BCPElement* element = m_BCPElements.m_first;
        element;
        element = element->m_next)
    {
        if(element != m_BCPElements.m_first) {
            strcat(vmboot, PORT_PATH_SEPARATOR_STR);
        }
        strcat(vmboot, element->m_path->bytes);
    }
    /*
     *  set VM_BOOT_CLASS_PATH and SUN_BOOT_CLASS_PATH for any code 
     *  that needs it
     */
    m_env->VmProperties()->set(VM_BOOT_CLASS_PATH, vmboot);
    m_env->JavaProperties()->set(VM_BOOT_CLASS_PATH, vmboot);
    m_env->JavaProperties()->set(SUN_BOOT_CLASS_PATH, vmboot);
    
    // check if vm.bootclasspath.appendclasspath property is set to true
    if( TRUE == vm_property_get_boolean("vm.bootclasspath.appendclasspath", FALSE, VM_PROPERTIES) ) {
        // append classpath to bootclasspath
        char * cp = m_env->JavaProperties()->get("java.class.path");
        SetClasspathFromString(cp, tmp_pool);
        m_env->JavaProperties()->destroy(cp);
    }

    // get a classpath from archive files manifests and
    // set into boot class path collection
    for( BCPElement* element = m_BCPElements.m_first;
         element;
         element = element->m_next )
    {
        TRACE2("classloader.bootclasspath", "BCP: " << element->m_path->bytes );
        if( element->m_isJarFile ) {
            SetClasspathFromJarFile( element->m_jar, tmp_pool );
        }
    }

    // destroy temp pool
    apr_pool_destroy(tmp_pool);
    return true;
} // BootstrapClassLoader::Initialize

Class* ClassLoader::LoadClass(Global_Env* env, const String* className)
{
    Class* klass = DoLoadClass(env, className);

    // record class as initiated
    if (klass) {
        LMAutoUnlock aulock(&m_lock);

        // check if class has been already recorded as initiated by DefineClass()
        Class** pklass = m_initiatedClasses->Lookup(className);
        if (NULL == pklass) {
            m_initiatedClasses->Insert(className, klass);
        } else {
            assert(klass == *pklass);
        }
    }

    return klass;
} // ClassLoader::LoadClass

Class* BootstrapClassLoader::DoLoadClass(Global_Env* UNREF env,
                                       const String* className)
{
    assert(!exn_raised());
    assert(env == m_env);
    assert(!exn_raised());

    Class* klass = StartLoadingClass(m_env, className);

    if(klass != NULL) {
        // class is already loaded so return it
        return klass;
    }
    if(exn_raised()) {
        // this is most probably ClassCircularityError
        // detected in StartLoadingClass
        return NULL;
    }

    TRACE2("classloader.load", "Loader (" << this << ") loading class: " << className->bytes << "...");

    // Not in the class cache: load the class or, if an array class, create it
    if(className->bytes[0] == '[') {
        // array classes require special handling
        klass = SetupAsArray(m_env, className);
    } else {
        // load class from file
        klass = LoadFromFile(className);
    }
    return klass;
} // BootstrapClassLoader::DoLoadClass

Class* UserDefinedClassLoader::DoLoadClass(Global_Env* env, const String* className)
{
    ASSERT_RAISE_AREA;
    assert(m_loader != NULL);
    assert(!exn_raised());

    Class* klass = StartLoadingClass(env, className);

    if(klass != NULL) {
        return klass;
    }
    if(exn_raised()) {
        // this is most probably ClassCircularityError
        // detected in StartLoadingClass
        return NULL;
    }

    TRACE2("classloader.load", "Loader U (" << this << ") loading class: " << className->bytes << "...");

    char* cname = const_cast<char*>(className->bytes);
    if( cname[0] == '[' ) {
        klass = SetupAsArray(env, className);
        return klass;
    }

    // Replace '/' with '.'
    unsigned len = className->len + 1;
    char* name = (char*) STD_ALLOCA(len);
    char* tmp_name = name;
    memcpy(name, className->bytes, len);
    while (tmp_name = strchr(tmp_name, '/')) {
        *tmp_name = '.';
        ++tmp_name;
    }
    String* class_name_with_dots =
        VM_Global_State::loader_env->string_pool.lookup(name);

    // call the version that takes the resolve flag
    // some subclasses of ClassLoader do NOT overload the (Ljava/lang/String;) version of the method
    // they all define (Ljava/lang/String;Z) version because it is abstract
    // wgs's comment here:
    //  * (Ljava/lang/String;Z) is not abstract in current JDK version
    //  * Generally (Ljava/lang/String;) are overloaded
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();

    jvalue args[2];

  
    // Set the arg #1 first because calling vm_instantiate_cp_string_resolved
    // can cause GC and we would like to get away with code that doesn't
    // protect references from GC.
    ManagedObject* jstr;
    
    REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
        jstr = uncompress_compressed_reference(class_name_with_dots->intern.compressed_ref);
#endif // REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        jstr = class_name_with_dots->intern.raw_ref;
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
    REFS_RUNTIME_SWITCH_ENDIF

    if (jstr != NULL) {
        ObjectHandle h = oh_allocate_local_handle();
        h->object = jstr;
        args[1].l = h;
    } else {
        ObjectHandle h = oh_allocate_local_handle();
        h->object = vm_instantiate_cp_string_resolved(class_name_with_dots);
        args[1].l = h;
    }

    if (exn_raised()) {
        TRACE2("classloader", "OutOfMemoryError before loading class " << className->bytes);
        tmn_suspend_enable();
        FailedLoadingClass(className);
        return NULL;
    }

    ObjectHandle hl = oh_allocate_local_handle();
    hl->object = m_loader;
    args[0].l = hl;

    Method* method = NULL;
    method = class_lookup_method_recursive(m_loader->vt()->clss, env->LoadClass_String, env->LoadClassDescriptor_String);
    assert(method);

    jvalue res;
    vm_execute_java_method_array((jmethodID) method, &res, args);

    if(exn_raised()) {
        tmn_suspend_enable();

        jthrowable exn = exn_get();
        // translate ClassNotFoundException to NoClassDefFoundError (if any)
        Class* NotFoundExn_class = env->java_lang_ClassNotFoundException_Class;
        Class* exn_class = jobject_to_struct_Class(exn);

        INFO("Loading of " << className->bytes << " class failed due to "
            << exn_class->get_name()->bytes);

        while(exn_class && exn_class != NotFoundExn_class) {
            exn_class = exn_class->get_super_class();
        }

        if (exn_class == NotFoundExn_class) {
            TRACE("translating ClassNotFoundException to "
                    "NoClassDefFoundError for " << className->bytes);
            exn_clear();
            jthrowable new_exn = CreateNewThrowable(
                p_TLS_vmthread->jni_env,
                env->java_lang_NoClassDefFoundError_Class,
                className->bytes, exn);
            if(new_exn) {
                exn_raise_object(new_exn);
            } else {
                assert(exn_raised());
                TRACE("Failed to translate ClassNotFoundException "
                    "to NoClassDefFoundError for " << className->bytes);
            }
        }

        FailedLoadingClass(className);
        return NULL;
    }
    if(res.l == NULL)
    {
        // NOTE: error in user class loader
        //       class was not loaded but exception was not thrown
        tmn_suspend_enable();
        FailedLoadingClass(className);
        REPORT_FAILED_CLASS_NAME(this, className->bytes,
            VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
            className->bytes);
        return NULL;
    }
    ObjectHandle oh = (ObjectHandle) res.l;
    Class* clss = java_lang_Class_to_struct_Class(oh->object);
    tmn_suspend_enable();

    assert(clss->get_class_loader() != NULL);
    if(clss->get_class_loader() != this) {
        // if loading of this class was delegated to some other CL
        //      signal successful loading for our CL
        SuccessLoadingClass(className);
    } else
        INFO2("class", "[Loaded " << (NULL != className ? className->bytes : "NULL") <<
              " by " << (NULL != clss->get_class_loader()->GetName() ? clss->get_class_loader()->GetName()->bytes : "NULL")
               << "]");
    return clss;
} // UserDefinedClassLoader::DoLoadClass

static jmethodID getClassRegistryMethod() {
    Global_Env* env = VM_Global_State::loader_env;
    String* acr_func_name = env->string_pool.lookup("registerLoadedClass");
    String* acr_func_desc = env->string_pool.lookup("(Ljava/lang/Class;)V");
    Class* clss = env->LoadCoreClass("java/lang/ClassLoader");
    Method* m = clss->lookup_method(acr_func_name, acr_func_desc);
    assert(m);
    return (jmethodID)m;
}

bool ClassLoader::InsertClass(Class* clss) 
{
    if (!IsBootstrap()) // skip BS classes
    {
        static jmethodID registryMethod = getClassRegistryMethod();
        tmn_suspend_disable();
        jvalue args[2], res;

        // this parameter
        ObjectHandle hl = oh_allocate_local_handle();
        hl->object = m_loader;
        args[0].l = hl;

        // jlc parameter
        ObjectHandle chl = oh_allocate_local_handle();
        chl->object = *clss->get_class_handle();
        args[1].l = chl;

        vm_execute_java_method_array(registryMethod, &res, args);
        tmn_suspend_enable();

        if(exn_raised()) {
            return false;
        }
    }

    tmn_suspend_disable();
    LMAutoUnlock aulock(&m_lock);
    m_loadedClasses->Insert(clss->get_name(), clss);
    if (!IsBootstrap()){
        RemoveFromReported(clss->get_name());
    }
    
    m_initiatedClasses->Insert(clss->get_name(), clss);
    aulock.ForceUnlock();
    tmn_suspend_enable();
    return true;
}


void ClassLoader::InsertInitiatedClass(Class* clss)
{
    LMAutoUnlock aulock(&m_lock);
    if(!m_initiatedClasses->Lookup(clss->get_name())) {
        m_initiatedClasses->Insert(clss->get_name(), clss);
    }
}

void BootstrapClassLoader::ReportAndExit(const char* exnclass, std::stringstream& exnmsg) 
{
    DIE(("%s : %s", exnclass, exnmsg.str().c_str()));
}

Class* BootstrapClassLoader::LoadFromFile(const String* class_name)
{
    assert(!exn_raised());

    // the symptom of circularity (illegal) is that a null class name is passed,
    // so detect this immediately
    if(!class_name->bytes) {
        REPORT_FAILED_CLASS_NAME(this, class_name->bytes,
            "java/lang/ClassCircularityError", class_name->bytes);
        return NULL;
    }

    // first change class name from the internal fully qualified name
    // to an external file system name
    unsigned baselen = class_name->len + 7;  // includes '.class' plus extra '\0'

    // copy class name
    char* class_name_in_fs = (char*)STD_ALLOCA(baselen);
    memcpy(class_name_in_fs, class_name->bytes, class_name->len);

    // change fully qualified form to path name by replacing / with '\\' or vice versa.
    for(char* pointer = class_name_in_fs;
        pointer < class_name_in_fs + class_name->len;
        pointer++)
    {
        if((*pointer == '/') || (*pointer == '\\')) {
            *pointer = PORT_FILE_SEPARATOR;
        }
    }
    memcpy(class_name_in_fs + class_name->len, ".class", 7);

    // set class name in archive file
    char* class_name_in_jar;
    if( '/' == PORT_FILE_SEPARATOR ) {
        class_name_in_jar = class_name_in_fs;
    } else {
        class_name_in_jar = (char*)STD_ALLOCA(baselen);
        memcpy(class_name_in_jar, class_name->bytes, class_name->len);
        memcpy(class_name_in_jar + class_name->len, ".class", 7);
    }

    // find class in bootclasspath
    Class* clss = NULL;
    for( BCPElement* element = m_BCPElements.m_first;
         element;
         element = element->m_next )
    {
        bool not_found;
        if(element->m_isJarFile) {
            clss = LoadFromJarFile(element->m_jar, class_name_in_jar,
                class_name, &not_found);
        } else {
            clss = LoadFromClassFile(element->m_path, class_name_in_fs,
                class_name, &not_found);
        }

        // check if a given class is found
        if(!not_found) {
            p_TLS_vmthread->class_not_found = false;
            return clss;
        }
        assert(clss == NULL);
    }
    REPORT_FAILED_CLASS_NAME(this, class_name->bytes,
        VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
        class_name->bytes);
    FailedLoadingClass(class_name);
    return NULL;
} // BootstrapClassLoader::LoadFromFile


Class* BootstrapClassLoader::LoadFromClassFile(const String* dir_name,
    const char* class_name_in_fs, const String* class_name, bool* not_found)
{
    // create local temp pool
    apr_pool_t *local_pool;
    apr_pool_create(&local_pool, NULL);
    *not_found = false;

    // set full file name
    char* full_name = apr_pstrcat(local_pool, dir_name->bytes,
        PORT_FILE_SEPARATOR_STR, class_name_in_fs, NULL );

    // check file existence
    apr_finfo_t finfo;
    if(apr_stat(&finfo, full_name, APR_FINFO_SIZE, local_pool) != APR_SUCCESS) {
        // file does not exist
        *not_found = true;
        apr_pool_destroy(local_pool);
        return NULL;
    }

    // file exists, try to open it
    apr_file_t *file_handle;
    if(apr_file_open(&file_handle, full_name,
        APR_FOPEN_READ|APR_FOPEN_BINARY, 0, local_pool) != APR_SUCCESS)
    {
        // cannot open file
        *not_found = true;
        apr_pool_destroy(local_pool);
        return NULL;
    }

    // read file in buf
    size_t buf_len = (size_t)finfo.size;
    unsigned char* buf = (unsigned char*)STD_MALLOC(buf_len);
    // FIXME: check that memory was allocated
    apr_file_read(file_handle, buf, &buf_len);

    // close file
    apr_file_close(file_handle);

    // define class
    Class* clss = DefineClass(m_env, class_name->bytes, buf, 0,
        (unsigned)buf_len);
    if(clss) {
        clss->set_class_file_name(m_env->string_pool.lookup(full_name)->bytes);
    }
    STD_FREE(buf);
    apr_pool_destroy(local_pool);
    INFO2("class", "[Loaded " << (NULL != class_name ? class_name->bytes : "NULL") << " from " << dir_name->bytes << "]");
    return clss;
} // BootstrapClassLoader::LoadFromClassFile

Class* BootstrapClassLoader::LoadFromJarFile( JarFile* jar_file,
    const char* class_name_in_jar, const String* class_name, bool* not_found)
{
    assert(!exn_raised());

    if(jar_file->HasSharedCache()) {
        // class was looked up earlier
        *not_found = true;
        return NULL;
    }

    // find archive entry in archive file
    *not_found = false;
    const JarEntry *entry = jar_file->Lookup(class_name_in_jar);
    if(!entry) {
        // file was not found
        *not_found = true;
        return NULL;
    }

    // unpack entry
    unsigned size = entry->GetContentSize();
    unsigned char* buffer = (unsigned char*)STD_MALLOC(size);
    // FIXME: check that memory was allocated

    if(!entry->GetContent(buffer)) {
        // cannot unpack entry
        *not_found = true;
        STD_FREE(buffer);
        return NULL;
    }

    // create package information with jar source
    ProvidePackage(m_env, class_name, JarFile::GetJar(entry->GetJarIndex())->GetName());

    // define class
    Class *clss = DefineClass(m_env, class_name->bytes, buffer, 0, size, NULL);
    if(clss) {
        // set class file name
        clss->set_class_file_name(JarFile::GetJar(entry->GetJarIndex())->GetName());
    }

    STD_FREE(buffer);

    INFO2("class", "[Loaded " << (NULL != class_name ? class_name->bytes : "NULL") << " from " << JarFile::GetJar(entry->GetJarIndex())->GetName()  << "]");
    return clss;
} // BootstrapClassLoader::LoadFromJarFile

// Function looks for method in native libraries of class loader.
VMEXPORT GenericFunctionPointer
classloader_find_native(const Method_Handle method)
{
    assert(hythread_is_suspend_enabled());
    assert(!exn_raised());
    assert(method);
    return method->get_class()->get_class_loader()->LookupNative(method);
}

void ClassLoader::ReportException(const char* exn_name, std::stringstream& message_stream)
{
    // raise exception
    jthrowable exn = exn_create(exn_name, message_stream.str().c_str());
    if (exn)
        exn_raise_object(exn);
    else
        // Exception could have failed to be created, e.g. because of OOME
        assert(exn_raised());
}

void BootstrapClassLoader::ReportException(const char* exn_name, std::stringstream& message_stream)
{
    // if still can't create an exception, print error message and exit VM
    if (! m_env->IsReadyForExceptions())
    {
        ReportAndExit(exn_name, message_stream);
    }

    ClassLoader::ReportException(exn_name, message_stream);
}




