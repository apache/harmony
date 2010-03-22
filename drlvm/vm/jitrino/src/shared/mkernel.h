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

/**
 * @file
 * @brief 'micro-kernel' - contains kernel-like utilities - synchronization, 
          system info, etc.
 */

#if !defined(__MKERNEL_H_INCLUDED__)
#define __MKERNEL_H_INCLUDED__

#ifdef _WIN32
    // windows.h include introduces min/max defines, which conflicts
    // with STL's min/max used in many other places. This define prevents
    // them to appear.
    #define NOMINMAX 1
    // windows.h enforces its own aligment, which may conflict with 
    // currently used default one.
    // Note: EVERY inclusion of windows.h in Jitrino must be surrounded
    // with \#pragma pack(push/pop)
    #pragma pack(push)
    // <winsock2.h> must be included before <windows.h> to avoid 
    // compile-time errors with winsock.h. For more details, see:
    //www.microsoft.com/msdownload/platformsdk/ ..
    // ..     sdkupdate/2600.2180.7/contents.htm
        #include <winsock2.h>
        #include <windows.h>
        #if _MSC_VER < 1500
            #define vsnprintf _vsnprintf
        #endif
    #pragma pack(pop)
    // ... well, just to be absolutely sure...
    #undef min
    #undef max
#else
    #include <pthread.h>
    #include <semaphore.h>
#endif
#include <assert.h>

#if defined(FREEBSD)
#define PTHREAD_MUTEX_RECURSIVE_NP PTHREAD_MUTEX_RECURSIVE
#endif

namespace Jitrino { 

/**
 * @brief Exclusive lock.
 *
 * Class Mutex represents an exclusive lock. Interface is obvious.
 * 
 * There are no 'try_lock' or 'is_locked' methods, this absence is 
 * intentional.
 *
 * @note On both Linux and Windows platforms, the mutex object can be 
 *       recursively taken by the same thread (and, surely, must be released
 *       exactly the same number of times).
 *
 * @note On Linux platform, Mutex must be unlocked before destruction (that
 *       is, the Mutex object goes out of scope) - this is requirement of 
 *       LinuxThreads implementation.
 *
 * @note Can be used only to synchronize inside an application. Cannot be 
 *       used (and was not intended to do so) for inter-process 
 *       synchronization.
 *
 * @note On Windows platform the underlying OS object is not Mutex, but 
 *       CriticalSection instead, as it's a bit lighter and faster.
 *
 * @see AutoUnlock
 */
class Mutex {
#ifdef PLATFORM_POSIX 
    //
    // *nix implementation
    //
public:
    /**
     * @brief Frees resources associated with the mutex.
     */
    Mutex()
    {
        pthread_mutexattr_t attrs;
        pthread_mutexattr_init(&attrs);
        pthread_mutexattr_settype(&attrs, PTHREAD_MUTEX_RECURSIVE_NP);
        pthread_mutex_init(&m_handle, &attrs);
        pthread_mutexattr_destroy(&attrs);
    }
    /**
     * @brief Destructs the object.
     */
    ~Mutex()            { pthread_mutex_destroy(&m_handle); }
    
    /**
     * @brief Acquires a lock.
     */
    void lock(void)     { pthread_mutex_lock(&m_handle);    }
    
    /**
     * @brief Releases a lock.
     */
    void unlock(void)   { pthread_mutex_unlock(&m_handle);  }
private:
    pthread_mutex_t m_handle;
    
#else   // not PLATFORM_POSIX

    //
    // Win* implementation
    //
public:
    Mutex()             { InitializeCriticalSection(&m_cs); }
    ~Mutex()            { DeleteCriticalSection(&m_cs);     }
    void lock(void)     { EnterCriticalSection(&m_cs);      }
    void unlock(void)   { LeaveCriticalSection(&m_cs);      }
private:
    CRITICAL_SECTION    m_cs;

#endif  // ~ifdef PLATFORM_POSIX
    /**
     * @brief Disallows copying.
     */
    Mutex(const Mutex&);
    /**
     * @brief Disallows copying.
     */
    const Mutex& operator=(const Mutex&);

public:
    static Mutex appGlobal;

};


#define _LINE_VAR_CAT( name, line ) name##line
#define _LINE_VAR( name, line ) _LINE_VAR_CAT( name, line )
#define LINE_VAR(name) _LINE_VAR( name, __LINE__ )

#define SYNC_FIRST(Expression) \
    static bool LINE_VAR(first) = true;\
    bool LINE_VAR(locked) = false;\
    if (LINE_VAR(first)) {\
        Mutex::appGlobal.lock();\
        LINE_VAR(locked) = true;\
    }\
    Expression;\
    LINE_VAR(first) = false;\
    if (LINE_VAR(locked)) {\
        Mutex::appGlobal.unlock();\
    }\



/**
 * @brief Automatically unlocks a Mutex when AutoLock object goes out of 
 * scope.
 *
 * Class AutoUnlock is an utility class to handy acquire and [automatically]
 * release Mutex lock.
 *
 * A trivial C++ trick, which, I believe, is used everywhere with Mutexes - 
 * holds the lock in the ctor and releases lock in dtor - when AutoUnlock
 * object goes out of scope. 
 *
 * Safely accepts NULL-s - and performs nothing in this case.
 */
class AutoUnlock {
public:
    /**
     * @brief Locks the mutex.
     */
    AutoUnlock(Mutex& m) : m_mutex(&m)
    {
        m_mutex->lock();
    }
    
    /**
     * @brief Locks the mutex, or does nothing if \c pm is \b NULL.
     */
    AutoUnlock(Mutex * pm) : m_mutex(pm)
    {
        if(m_mutex) {
            m_mutex->lock();
        }
    }
    
    /**
     * @brief Forces mutex (if any) to be unlocked. 
     * 
     * When called, clears internal pointer to a mutex object, so the 
     * following sequential calls to forceUnlock and destructor are noops.
     */    
    void forceUnlock(void) 
    {
        if(m_mutex) {
            m_mutex->unlock();
            m_mutex = NULL;
        }
    }
    
    /**
     * @brief Unlocks mutex (if any).
     */
    ~AutoUnlock()
    {
        if(m_mutex) {
            m_mutex->unlock();
        }
    }
private:
    AutoUnlock(const AutoUnlock&);
    AutoUnlock& operator=(const AutoUnlock&);
    Mutex * m_mutex;
};


/**
 * @brief Class Runtime provides an info about environment the application
 *        is currently running.
 */
class Runtime {
public:
    /**
     * @brief Returns number of CPUs available in the system.
     *
     * In theory, may be 0, if underlying system does not provide the info.
     *
     * Does not even try to distinguish HyperThreading and 'phisical CPU', so
     * a CPU with HT enabled is seen as 2 CPUs.
     */
    static unsigned cpus(void)      { return num_cpus;      }
    
    /**
     * @brief Tests whether we're running in single-CPU machine.
     */
    static bool     is_one_cpu(void)     { return cpus() == 1;   }
    
    /**
     * @brief Tests whether we're running in multi-CPU box.
     */
    static bool     is_smp(void)    { return cpus() > 1;    }
private:
    /**
     * @brief Number of CPUs.
     *
     * Initialized once at startup and never changes.
     */
    static const unsigned num_cpus;
    
    /**
     * @brief Initializes \link #num_cpus number of CPUs\endlink.
     */
    static unsigned init_num_cpus(void);
    
    /** 
     * @brief Disallows creation, interface is only through static functions.
     */
    Runtime();
    
    /**
     * @brief Disallows copying.
     */
    Runtime(const Runtime&);
    
    /**
     * @brief Disallows copying.
     */
    Runtime& operator=(const Runtime&);
};

/**
 * @brief Key used to identify data in TLS.
 */
typedef unsigned long TlsKey;

/**
 * @brief Basic thread local storage (TLS) functionality.
 *
 * The functionality is made similar to the POSIX one - when a TLS key 
 * allocated, user may specify a function (<i>release function</i>) which 
 * will be called on thread's death to release the data stored in TLS.
 * 
 * With POSIX threads this functionality is by design. On Windows, this 
 * functionality is emulated by the Tls class.
 *
 * The emulation's behavior was made as much close to the one specified by
 * POSIX, though some differences still exist.
 *
 * - the release function is called only if data currently stored in TLS 
 *  is not NULL
 * - NULL value is associated with TLS key before calling release function
 * - (the difference) if release function re-associates a data with the TLS 
 *  key, this data is ignored (in other words, release function is called 
 *  no more than once for each thread)
 * 
 * @note Emulation on Windows requires external support - currently special
 * method Tls::threadEnd is called on DLL_THREAD_DETACH. As our code lives 
 * in dll it's enough for all our needs. If the code (and especially the 
 * release function behaviour) need to be reused for a threads in process, 
 * the Tls::threadEnd() must be called at the end of thread function.
 */
class Tls {
public:
    /**
     * @brief Allocates a key for TLS.
     *
     * Initial value associated with each key in NULL.
     *
     * @param free_func - a function will be called when thread is about to
     * finish, so it can free up the data stored in TLS. See Tls class 
     * comments.
     */
    static TlsKey allocKey(void (*free_func) (void *) = NULL);
    /**
     * @brief Releases the TlsKey.
     */
    static void releaseKey(TlsKey key);
#ifdef _WIN32
    /**
     * @brief Special-purpose method - used to emulate POSIX-like behavior 
     * with release function that may free up a data stored in TLS.
     *
     * Must be called when thread is about to finish.
     */
    static void threadDetach(void);
    /**
     * @brief Special-purpose method - used to cleanup dynamically 
     * allocated map.
     *
     * Must be called when thread is about to finish.
     */
    static void processDetach(void);
#endif
    /**
     * @brief Puts \c data into TLS.
     */
    static void put(TlsKey key, void* data);
    /**
     * @brief Gets \c data from TLS.
     *
     * If there were no preceding put() operations in the current thread, 
     * NULL returned.
     */
    static void* get(TlsKey key);
};

/**
 * A handy wrapper to organize TLS storage, with auto-<i>magic</i> release
 * of allocated data.
 * 
 * TlsStorage is used to manage a thread-local copy of data that exists 
 * in a <i>single copy per each thread</i>. Here it differs from TlsList 
 * and TlsStack that organize a thread local stack storage of several 
 * instances of a data.
 * 
 * The TlsStore presumes that a data stored in it is dynamically allocated
 * using <code>global operator new</code>. Upon a thread termination, a
 * non-<b>NULL</b> data is deallocated using <code>global operator 
 * delete</code>.
 * 
 *
 * Usage example:
 * <pre>
 * <code>
 * static TlsStore<SomeStruct> globalStore;
 *
 * foo()
 * {
 *      SomeStruct* data = globalStore.get();
 *      if (data == NULL) {
 *          data = new SomeStru();
 *          data->init();
 *          globalStore.put(data);
 *      }
 *      
 * }
 * </code>
 * </pre>
 */
template <class T> struct TlsStore {
    /**
     * @brief Allocates a TlsKey to be used for TLS manipulations.
     */
    TlsStore()
    {
        m_key = Tls::allocKey(free);
    }
    
    /**
     * @brief Releases TlsKey.
     */
    ~TlsStore()
    {
        Tls::releaseKey(m_key);
    }
    
    /**
     * @brief Returns the stored data or NULL if there were no data stored.
     */
    T* get(void) const
    {
        return (T*)Tls::get(m_key);
    }
    /**
     * @brief Stores the data into TLS, \b NULL-s are ok.
     * 
     * If there were a data stored before, the previous data is freed.
     */
    void put(T* pt) const
    {
        T* prev = get();
        if (prev != NULL) {
            free(prev);
        }
        Tls::put(m_key, pt);
    }
    /**
     * Releases the data (calls delete)
     * 
     * The method is invoked upon a thread termination to deallocate 
     * data stored in TLS.
     */
    static void free(void* p)
    {
        delete (T*)p;
    }
private:
    TlsKey m_key;
};


/**
 * @brief Provides basic functionality to organize stack of infos in TLS.
 *
 * <code>
 * <pre>
 *  TlsKey globalKey = Tls::allocKey(NULL);
 *  foo()
 *  {
 *      SomeStru fooLocalData;
 *      TlsList::push(globalKey, &fooLocalData);
 *      boo();
 *      TlsList::pop(globalKey);
 *  }
 *
 *  boo()
 *  {
 *      SomeStru* data = (SomeStru*)TlsList::get(globalKey);
 *      // do something
 *  }
 * </pre>
 * </code>
 *
 * \c foo() may be called in the same thread several times, and every time
 * the latest data will be available for \c boo().
 * 
 * @note Number of \c pop()-s must be strictly balanced with the number of 
 * push()-es, or a memory leak occurs.
 *
 * @note TlsList really stores its special data into TLS, so never mix 
 * calls of Tls::put()/Tls::get() with TlsList's methods with the same 
 * key.
 * 
 * Normally, the TlsList may be used to store a pointer to general data,
 * while for a particular data structures the template class TlsStack may 
 * be more convinient.
 *
 * @see TlsStack
 */
class TlsList {
public:
public:
    /**
     * @brief Pushes the \c data onto TLS stack.
     */
    static void  push(TlsKey key, void* data);
    /**
     * @brief Pops out the \c data from TLS stack.
     */
    static void* pop(TlsKey key);
    /**
     * @brief Returns top data item from TLS stack and leaves it on stack.
     */
    static void* get(TlsKey key);
private:
    /**
     * Helper structure to organize linked list of datas in TLS.
     */
    struct ListItem
    {
        ListItem(ListItem* _prev, void* _data)
        {
            prev = _prev;
            data = _data;
#ifdef _DEBUG            
            magic = MAGIC;
#endif
        }
    
#ifdef _DEBUG
        /**
         * @brief A signature, used to verify data integrity.
         */
        unsigned    magic;
        static const unsigned MAGIC = 0x4C495354; // 'LIST' in hex
        ~ListItem()
        {
            assert(magic == MAGIC);
        }
#endif
        void * data;
        ListItem* prev;
    };
};

/**
 * @brief Handy wrapper for TlsList.
 *
 * Use as following:
 *
 * <code>
 *  TlsStack<SomeStruct> globalSomeStructStack; // must be static global
 * </code>
 *
 * @note Note the declaration in the example. It's the type itself, and not 
 * the pointer - methods os TlsStack do \b not copy data. Instead only 
 * pointer to the specified data is stored.
 *
 * The result is that the stored data must be have it's scope not less than
 * appropriate push/pop sequence.
 */
template<class AType> class TlsStack {
public:
    /**
     * @brief Allocates a TlsKey to be used for TLS manipulations.
     */
    TlsStack(void)
    {
        m_key = Tls::allocKey();
    }
    /**
     * @brief Releases TlsKey.
     */
    ~TlsStack()
    {
        Tls::releaseKey(m_key);
    }
    /**
     * @brief Pushes the \c item onto TLS stack.
     */
    void push(AType& item)
    {
        push(&item);
    }
    /**
     * @brief Pushes the \c pitem onto TLS stack.
     */
    void push(AType* pitem)
    {
        TlsList::push(m_key, pitem);
    }
    /**
     * @brief Returns top data item from TLS stack and leaves it on stack.
     */
    AType* get(void)
    {
        return (AType*)TlsList::get(m_key);
    }
    /**
     * @brief Pops out the an item from TLS stack.
     */
    AType* pop(void)
    {
        return (AType*)TlsList::pop(m_key);
    }
private:
    TlsKey m_key;
};

/**
    Set of utility methods to get features the current CPU supports.
    The functionality of this class is rather scarce, but it covers all needs we have in JIT today
*/
class CPUID {
    CPUID(){}
public:
#if defined(_IA32_) || defined(_EM64T_)
    /** SSE2 is an extension of the IA-32 architecture, since 2000. */
    static bool isSSE2Supported();
#endif
};

}; // ~namespace Jitrino

#endif  // ~ifndef __MKERNEL_H_INCLUDED__
