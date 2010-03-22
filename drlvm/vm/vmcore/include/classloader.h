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
 * @author Pavel Pervov, Pavel Rebriy
 */  
#ifndef _CLASSLOADER_H_
#define _CLASSLOADER_H_

#include <sstream>
#include <apr_pools.h>

#include "vm_core_types.h"
#include "object_layout.h"
#include "String_Pool.h"
#include "Class.h"
#include "open/vm.h"
#include "lock_manager.h"
#include "environment.h"
#include "natives_support.h"
#include "hashtable.h"
#include "loggerstring.h"
#include "jarfile_support.h"
#include "type.h"
#include "exceptions.h"
#include "vm_log.h"

class ClassTable : public MapEx<const String*, Class* > {};

/*
Concurrent class loading in user class loaders:
1)  resolution & direct defineClass call
    a.  if resolution happens first and finishes successfully,
        java/lang/LinkageError must be thrown for defineClass call
    b.  if defineClass starts first and succeeds, results for class loading in resolution
        should be discarded and already constructed class should be returned
2)  resolution & resolution
    a.  first come, first served; second will get constructed class
3)  direct defineClass call & direct defineClass call
    a.  first come, first served; java/lang/LinkageError must be thrown on the second thread.

ClassCircularityError can be only discovered on the thread, which defines class.
*/

struct ClassLoader
{
    struct LoadingClass
    {
        struct WaitingThread {
            VM_thread* m_waitingThread;
            WaitingThread* m_next;
        };
#ifdef _DEBUG
        // debugging
        const String* m_name;
#endif
        apr_pool_t* m_threadsPool;
        // threads waiting for this class
        WaitingThread* m_waitingThreads;
        // this event is signaled when class loader finishes
        // (successfully or unsuccessfully) loading this class
        int m_loadWaitFlag;
        // thread which owns class definition
        VM_thread* m_defineOwner;
        // thread which first started loading this class
        VM_thread* m_initiatingThread;

        LoadingClass() :
#ifdef _DEBUG
            m_name(NULL),
#endif
            m_threadsPool(NULL),
            m_waitingThreads(NULL),
            m_loadWaitFlag(0),
            m_defineOwner(NULL),
            m_initiatingThread(NULL) {}
        LoadingClass(const LoadingClass& lc) {
#ifdef _DEBUG
            m_name = lc.m_name;
#endif
            m_threadsPool = lc.m_threadsPool;
            m_waitingThreads = lc.m_waitingThreads;
            m_loadWaitFlag = lc.m_loadWaitFlag;
            m_defineOwner = lc.m_defineOwner;
            m_initiatingThread = lc.m_initiatingThread;
        }
        LoadingClass& operator=(const LoadingClass& lc) {
#ifdef _DEBUG
            m_name = lc.m_name;
#endif
            m_threadsPool = lc.m_threadsPool;
            m_waitingThreads = lc.m_waitingThreads;
            m_loadWaitFlag = lc.m_loadWaitFlag;
            m_defineOwner = lc.m_defineOwner;
            m_initiatingThread = lc.m_initiatingThread;

            return *this;
        }
        ~LoadingClass() {
            if(m_threadsPool != NULL) {
                apr_pool_destroy(m_threadsPool);
                m_threadsPool = NULL;
            }
            m_loadWaitFlag = 0;
        }

        void WaitLoading() {
            while(!m_loadWaitFlag) {
                hythread_yield();
            }
        }
        void SignalLoading() {
            m_loadWaitFlag = 1;
        }
        bool IsDefiner(VM_thread* thread) { return m_defineOwner == thread; }
        bool HasDefiner() { return m_defineOwner != NULL; }
        void EnqueueInitiator(VM_thread* new_definer, ClassLoader* cl, const String* clsname);
        void ChangeDefinerAndInitiator(VM_thread* new_definer, ClassLoader* cl, const String* clsname);
        bool IsInitiator(VM_thread* thread) { return m_initiatingThread == thread; }
        void SetInitiator(VM_thread* thread) { m_initiatingThread = thread; }
        void UpdateInitiator(VM_thread* thread) { m_initiatingThread = thread; }
        // this operation should be synchronized
        bool AlreadyWaiting(VM_thread* thread);
        // this operation should be synchronized
        void AddWaitingThread(VM_thread* thread, ClassLoader* cl, const String* clsname);
        // this operation should be synchronized
        void RemoveWaitingThread(VM_thread* thread, ClassLoader* cl, const String* clsname);
        bool HasWaitingThreads() { return (m_waitingThreads != NULL); }
           
    };

public:
    friend LoggerString& operator << (LoggerString& log, LoadingClass& lc);

private:
    class LoadingClasses : public MapEx<const String*, LoadingClass > {};
    class ReportedClasses : public MapEx<const String*, Class* > {};

    class JavaTypes : public MapEx<const String*, TypeDesc* > {};

    friend class GlobalClassLoaderIterator;
public:
    ClassLoader() : m_loader(NULL), m_parent(NULL), m_name(NULL), m_package_table(NULL), 
        m_loadedClasses(NULL), m_loadingClasses(NULL), m_reportedClasses(NULL),
        m_javaTypes(NULL), m_nativeLibraries(NULL), m_verifyData(NULL), m_markBit(false)
    {
        apr_pool_create(&pool, 0);
    }

    virtual ~ClassLoader();

    void ClassClearInternals(Class*); // clean internals when class is destroyed

    virtual bool Initialize( ManagedObject* loader = NULL );

    Class* LookupClass(const String* name) { 
        LMAutoUnlock aulock(&m_lock);
        Class** klass = m_loadedClasses->Lookup(name);
        if(klass == NULL)
            klass = m_initiatedClasses->Lookup(name);
        return klass?*klass:NULL;
    }
    void RemoveFromReported(const String* name){
        assert(!hythread_is_suspend_enabled());
        if(m_reportedClasses->Lookup(name)) {
            m_reportedClasses->Remove(name);
        }
    }
    bool InsertClass(Class* clss);
    void InsertInitiatedClass(Class* clss);
    Class* AllocateAndReportInstance(const Global_Env* env, Class* klass);
    Class* NewClass(const Global_Env* env, const String* name);
    Package* ProvidePackage(Global_Env* env, const String *class_name, const char *jar);
    Class* DefineClass(Global_Env* env, const char* class_name,
        U_8* bytecode, unsigned offset, unsigned length, const String** res_name = NULL);
    Class* LoadClass( Global_Env* UNREF env, const String* UNREF name);
    Class* LoadVerifyAndPrepareClass( Global_Env* env, const String* name);
    virtual void ReportException(const char* exn_name, std::stringstream& message_stream);
    virtual void ReportFailedClass(Class* klass, const char* exnclass, std::stringstream& exnmsg);
    void ReportFailedClass(Class* klass, const jthrowable exn);
    virtual void ReportFailedClass(const char* name, const char* exnclass, std::stringstream& exnmsg);
    void LoadNativeLibrary( const char *name );
    GenericFunctionPointer LookupNative(Method*);
    void SetVerifyData( void *data ) { m_verifyData = data; }
    void* GetVerifyData( void ) { return m_verifyData; }
    void Lock() { m_lock._lock(); }
    void Unlock() { m_lock._unlock(); }
    void LockTypesCache() { m_types_cache_lock._lock(); }
    void UnlockTypesCache() { m_types_cache_lock._unlock(); }
    static void LockLoadersTable() { m_tableLock._lock(); }
    static void UnlockLoadersTable() { m_tableLock._unlock(); }
protected:
    virtual Class* DoLoadClass( Global_Env* UNREF env, const String* UNREF name) = 0;
    Class* StartLoadingClass(Global_Env* env, const String* className);
    void RemoveLoadingClass(const String* className, LoadingClass* loading);
    void SuccessLoadingClass(const String* className);
    void FailedLoadingClass(const String* className);

public:
    bool IsBootstrap() { return m_loader == NULL; }
    void Mark() { m_markBit = true; }
    bool isMarked() { return m_markBit; }
    ManagedObject* GetLoader() { return m_loader; }
    ManagedObject** GetLoaderHandle() { return &m_loader; }
    const String* GetName() { return m_name; }
    ClassLoader* GetParent() { return m_parent; }
    Package_Table* getPackageTable() { return m_package_table; }
    ClassTable* GetLoadedClasses() { return m_loadedClasses; }
    ClassTable* GetInitiatedClasses() { return m_initiatedClasses; }
    LoadingClasses* GetLoadingClasses() { return m_loadingClasses; }
    ReportedClasses* GetReportedClasses() { return m_reportedClasses; }
    JavaTypes* GetJavaTypes() { return m_javaTypes; }

    // ClassLoaders collection interface and data
    // ppervov: I think we need separate class for this entity
    static ClassLoader* FindByObject( ManagedObject* loader );
    // ppervov: NOTE: LookupLoader has side effect of adding 'loader' to the collection
    VMEXPORT static ClassLoader* LookupLoader( ManagedObject* loader );
    static void UnloadClassLoader( ClassLoader* loader);
    static void gc_enumerate();
    static void ClearMarkBits();
    static void StartUnloading();
    static unsigned GetClassLoaderNumber() { return m_nextEntry; }
    static ClassLoader** GetClassLoaderTable() { return m_table; }
    static void DeleteClassLoaderTable(){
        STD_FREE(m_table);
        m_table = NULL;
    }

    void NotifyUnloading();

    inline void* Alloc(size_t size) {
        assert(pool);
        Lock();
        void* ptr = apr_palloc(pool, size);
        Unlock();
        return ptr;
    }

    PoolManager* GetCodePool(){
        return CodeMemoryManager;
    }

    inline void* CodeAlloc(size_t size, size_t alignment, Code_Allocation_Action action) {
        return CodeMemoryManager->alloc(size, alignment, action);
    }
    inline void* VTableAlloc(size_t size, size_t alignment, Code_Allocation_Action action) {
        return VM_Global_State::loader_env->VTableMemoryManager->alloc(size, alignment, action);        
    }

private:
    static Lock_Manager m_tableLock;
    static unsigned m_capacity;
    static unsigned m_nextEntry;
    static ClassLoader** m_table;
    static unsigned m_unloadedBytes;
    static ClassLoader* AddClassLoader( ManagedObject* loader );
    static void ReallocateTable( unsigned int new_capacity );

protected:
    // data
    ManagedObject* m_loader;
    ClassLoader* m_parent;
    const String* m_name; 
    Package_Table* m_package_table;
    ClassTable* m_loadedClasses;
    ClassTable* m_initiatedClasses;
    LoadingClasses* m_loadingClasses;
    ReportedClasses* m_reportedClasses;
    JavaTypes* m_javaTypes;
    NativeLibraryList m_nativeLibraries;
    Lock_Manager m_lock;
    Lock_Manager m_types_cache_lock;
    bool m_markBit;
    void* m_verifyData;
    apr_pool_t* pool;
    PoolManager *CodeMemoryManager;

    // methods
    Class* WaitDefinition(Global_Env* env, const String* className);
    Class* SetupAsArray(Global_Env* env, const String* klass);

private:
    void FieldClearInternals(Class*); // clean Field internals in Class
}; // class ClassLoader

inline LoggerString& operator <<(LoggerString& log, ClassLoader::LoadingClass& lc)
{
#ifdef _DEBUG
    log_printf("%s", lc.m_name->bytes);
#endif
    log_printf(" thread: %p %p", lc.m_defineOwner, lc.m_initiatingThread);
    return log;
}

#define REPORT_FAILED_CLASS_CLASS(loader, klass, exnname, exnmsg)   \
    {                                                               \
        std::stringstream ss;                                       \
        ss << exnmsg;                                               \
        loader->ReportFailedClass(klass, exnname, ss);              \
    }

#define REPORT_FAILED_CLASS_CLASS_EXN(loader, klass, exnhandle) \
    {                                                           \
        loader->ReportFailedClass(klass, exnhandle);            \
    }

#define REPORT_FAILED_CLASS_NAME(loader, name, exnname, exnmsg) \
    {                                                           \
        std::stringstream ss;                                   \
        ss << exnmsg;                                           \
        loader->ReportFailedClass(name, exnname, ss);           \
    }


class BootstrapClassLoader : public ClassLoader
{
public:
    struct BCPElement {
        bool m_isJarFile;
        const String* m_path;
        JarFile* m_jar;
        BCPElement* m_next;
    };
    struct BCPElements {
        BCPElements() : m_first(NULL), m_last(NULL) {}
        BCPElement *m_first;
        BCPElement *m_last;
    };

    BootstrapClassLoader(Global_Env* env);
    virtual ~BootstrapClassLoader();
    virtual bool Initialize( ManagedObject* loader = NULL );
    // reloading error reporting in bootstrap class loader
    virtual void ReportException(const char* exn_name, std::stringstream& message_stream);
    virtual void ReportFailedClass(Class* klass, const char* exnclass, std::stringstream& exnmsg) {
        if(! m_env->IsReadyForExceptions()) {
            ReportAndExit(exnclass, exnmsg);
        }
        ClassLoader::ReportFailedClass(klass, exnclass, exnmsg);
    }
    virtual void ReportFailedClass(const char* name, const char* exnclass, std::stringstream& exnmsg) {
        if(! m_env->IsReadyForExceptions()) {
            ReportAndExit(exnclass, exnmsg);
        }
        ClassLoader::ReportFailedClass(name, exnclass, exnmsg);
    }

    // primitive types are introduced for caching purpose
    TypeDesc* get_primitive_type(Kind k){
        assert (k <= K_LAST_PRIMITIVE ); // primitive types are limited by K_LAST_PRIMITIVE bound
        return primitive_types[k];
    }

protected:
    virtual Class* DoLoadClass(Global_Env* env, const String* name);

private:
    void ReportAndExit(const char* exnclass, std::stringstream& exnmsg);
    Class* LoadFromFile(const String* className);
    Class* LoadFromClassFile(const String* dir_name, const char* class_name_in_fs,
        const String* class_name, bool* not_found);
    Class* LoadFromJarFile( JarFile* jar_file,
        const char* class_name_in_jar, const String* class_name, bool* not_found);
    void SetClasspathFromString(char* prop_string, apr_pool_t *tmp_pool);
    void SetClasspathFromJarFile(JarFile *jar, apr_pool_t *tmp_pool);
    void SetBCPElement(const char *path, apr_pool_t *tmp_pool);

    BCPElements m_BCPElements;
    Global_Env* m_env;
    // primitive types array, K_LAST_PRIMITIVE - upper bound of primitive types
    TypeDesc* primitive_types[K_LAST_PRIMITIVE + 1]; 
}; // class BootstrapClassLoader

class UserDefinedClassLoader : public ClassLoader
{
public:
    UserDefinedClassLoader() {}
protected:
    virtual Class* DoLoadClass(Global_Env* env, const String* name);
}; // class UserDefinedClassLoader

/**
 * Function looks for method in native libraries of class loader.
 *
 * @param method - searching native method structure
 *
 * @return Pointer to found native function.
 *
 * @note Function raises <code>UnsatisfiedLinkError</code> with method name
 *       in exception message if specified method is not found.
 */
VMEXPORT GenericFunctionPointer
classloader_find_native(const Method_Handle method);

#endif // _CLASSLOADER_H_
