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
  * @brief Implementation of utilities declared in #mkernel.h.
  */
#include "mkernel.h"

#ifdef _WIN32
    #include <map>
    using std::map;
#else
    #include <unistd.h>
    #include <pthread.h>
#endif

namespace Jitrino {

Mutex Mutex::appGlobal;

#ifdef _WIN32
typedef void (*RELEASE_FPTR)(void *);
class FMAP : public map<TlsKey, RELEASE_FPTR>, public Mutex
{};



/**
 * Creates, initializes and caches the map to store RELEASE_FPTS functions.
 *
 * As the allocKey() and thus release function registration is done from 
 * few static initializers, then this separate getter function instead of 
 * static FMAP - to ensure no problems with static initializers.
 *
 * The dynamic allocation is for the same reason, to avoid static 
 * destructors conflict (e.g. if Tls::releaseKey is called after the ~FMAP).
 *
 */
static FMAP* get_fmap(void)
{
    static FMAP * fmap = NULL;
    if (fmap == NULL) {
        fmap = new FMAP();
    }
    return fmap;
}
#endif  // ~ifdef _WIN32

TlsKey Tls::allocKey(void (*free_func) (void *))
{
#ifdef _WIN32
    DWORD key = TlsAlloc();
    assert(key != TLS_OUT_OF_INDEXES);
    if (free_func != NULL) {
        FMAP& fmap = *get_fmap();
        fmap.lock();
            fmap[key] = free_func;
        fmap.unlock();
    }
#else
    pthread_key_t key;
    int res = pthread_key_create(&key, free_func);
    assert(!res);   res = res;
#endif
    return (TlsKey)key;
}

void Tls::releaseKey(TlsKey key)
{
#ifdef _WIN32
    BOOL res = TlsFree((DWORD)key);
    FMAP& fmap = *get_fmap();
    fmap.lock();
        if (fmap.find(key) != fmap.end()) {
            fmap.erase(key);
        }
    fmap.unlock();
    assert(res); res = res;
#else
    int res = pthread_key_delete(key);
    assert(!res);   res = res;
#endif
}
#ifdef _WIN32
void Tls::threadDetach(void)
{
    FMAP& fmap = *get_fmap();
    fmap.lock();
        for(FMAP::iterator i=fmap.begin(); i != fmap.end(); i++) {
            TlsKey key = i->first;
            void* data = Tls::get(key);
            if (data != NULL) {
                Tls::put(key, NULL);
                i->second(data);
            }
        }
    fmap.unlock();
}

void Tls::processDetach(void)
{
    FMAP* fmap = get_fmap();
    delete fmap;
}

#endif

void Tls::put(TlsKey key, void* pval)
{
#ifdef _WIN32
    BOOL res = TlsSetValue((DWORD)key, pval);
    assert(res); res = res;
#else
    int res = pthread_setspecific((pthread_key_t)key, pval);
    assert(!res);   res = res;
#endif
}

void* Tls::get(TlsKey key)
{
#ifdef _WIN32
    void * pval = TlsGetValue((DWORD)key);
    assert(pval != NULL || GetLastError() == NO_ERROR);
#else
    void * pval = pthread_getspecific((pthread_key_t)key);
#endif
    return pval;
}

void  TlsList::push(TlsKey key, void * pval)
{
    ListItem* top = (ListItem*)Tls::get(key);
    ListItem* thiz = new ListItem(top, pval);
    Tls::put(key, thiz);
}

void* TlsList::pop(TlsKey key)
{
    ListItem* top = (ListItem*)Tls::get(key);
    void * data = NULL;
    if (top != NULL) {
        data = top->data;
        Tls::put(key, top->prev);
        delete top;
    }
    else {
        // pop on empty stack
        assert(false);
    }
    return data;
}

void* TlsList::get(TlsKey key)
{
    ListItem* top = (ListItem*)Tls::get(key);
    // get() on empty stack - unexpected and wrong usage.
    assert(top != NULL);
#ifdef _DEBUG
    assert(top->magic == ListItem::MAGIC);
#endif
    return top->data;
}


const unsigned Runtime::num_cpus = Runtime::init_num_cpus();

unsigned Runtime::init_num_cpus(void)
{
#ifdef PLATFORM_POSIX
    int num = (int)sysconf(_SC_NPROCESSORS_ONLN);
    return num == -1 ? 0 : 1;
#else
    SYSTEM_INFO sinfo;
    GetSystemInfo(&sinfo);
    return sinfo.dwNumberOfProcessors;
#endif    
}

#if defined(_EM64T_)
bool CPUID::isSSE2Supported() {
    return true;
}
#elif defined(_IA32_) //older IA-32
bool CPUID::isSSE2Supported() {
    /*
     * cpuid instruction: 
     * - takes 0x1 on eax,
     * - returns standard features flags in edx, bit 26 is SSE2 flag
     * - clobbers ebx, ecx
     */
    unsigned int fflags =0;
#ifdef _WIN32
    __asm {
        mov    eax, 0x1
        cpuid
        mov    fflags, edx
    };
#elif defined (__linux__) || defined(FREEBSD)
    unsigned int stub;
    //ebx must be restored for -fPIC
     __asm__ __volatile__ (
            "push %%ebx; cpuid; mov %%ebx, %%edi; pop %%ebx" :
                "=a" (stub),
                "=D" (stub),
                "=c" (stub),
                "=d" (fflags) : "a" (0x1));
#else
#error "Need assembly code to query CPUID on this platform"
#endif
    bool res = ((fflags & (1<<26))!=0);
    return res;
}
#endif //older IA-32

}; // ~namespace Jitrino
