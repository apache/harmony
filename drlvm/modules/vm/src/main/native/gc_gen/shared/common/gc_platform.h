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
 * @author Xiao-Feng Li, 2006/10/05
 */

#ifndef _GC_PLATFORM_H_
#define _GC_PLATFORM_H_

#include "port_vmem.h"
#include "port_atomic.h"
#include "port_malloc.h"
#include "port_barriers.h"

#include <assert.h>

#if defined(__linux__) || defined(FREEBSD)
#include <ctype.h>
#endif

#ifndef MAP_ANONYMOUS
#define MAP_ANONYMOUS MAP_ANON
#endif

#include <apr_time.h>
#include <apr_atomic.h>

#include <open/hythread_ext.h>

extern char* large_page_hint;

#ifndef _DEBUG

//#define RELEASE_DEBUG
#ifdef RELEASE_DEBUG
#undef assert
#define assert(x) do{ if(!(x)) __asm{int 3}}while(0)
#endif

#endif //_DEBUG

#ifndef _IPF_
#define PREFETCH_SUPPORTED
#endif

#ifdef _WINDOWS_
#define FORCE_INLINE __forceinline   

#ifdef PREFETCH_SUPPORTED
#include <xmmintrin.h>
#define prefetchnta(pref_addr)	_mm_prefetch((char*)(pref_addr), _MM_HINT_NTA )
#endif /*ALLOC_PREFETCH*/

#elif defined (__linux__) || defined (FREEBSD)
#define FORCE_INLINE inline  __attribute__((always_inline))

#ifdef PREFETCH_SUPPORTED
#define prefetchnta(pref_addr)  __asm__ ("prefetchnta (%0)"::"r"(pref_addr))
#endif /*PREFETCH_SUPPORTED*/
#else 
#define FORCE_INLINE inline
#endif /* _WINDOWS_ */

#ifdef PREFETCH_SUPPORTED
#define PREFETCH prefetchnta
#else
#define PREFETCH(x) 
#endif

#ifdef PREFETCH_SUPPORTED
extern Boolean mark_prefetch;
#endif

#define ABS_DIFF(x, y) (((x)>(y))?((x)-(y)):((y)-(x)))
#define USEC_PER_SEC INT64_C(1000000)

#define VmThreadHandle  void*
#define VmEventHandle   hysem_t
#define THREAD_OK       TM_ERROR_NONE
#define THREAD_GROUP    hythread_group_t

extern THREAD_GROUP gc_thread_group;

inline THREAD_GROUP get_gc_thread_group () {
    if (!gc_thread_group) {
        IDATA UNUSED stat = hythread_group_create(&gc_thread_group);
        assert(stat == TM_ERROR_NONE);
    }
    return gc_thread_group;
}


inline int vm_wait_event(VmEventHandle event)
{   int stat = (int)hysem_wait(event);
    assert(stat == THREAD_OK); return stat;
}

inline int vm_set_event(VmEventHandle event)
{   int stat = (int)hysem_post(event);
    assert(stat == THREAD_OK); return stat;
}

inline int vm_reset_event(VmEventHandle event)
{   int stat = (int)hysem_set(event,0);
    assert(stat == THREAD_OK); return stat;
}

inline int vm_create_event(VmEventHandle* event)
{  return (int)hysem_create(event, 0, 1); }

inline void vm_thread_yield()
{  hythread_yield(); }

inline void* vm_thread_local()
{  return hythread_self();  }

inline int vm_create_thread(int (*func)(void*), void *data)
{ 
  hythread_t ret_thread = (hythread_t)STD_CALLOC(1,hythread_get_struct_size());
  assert(ret_thread);
  
  UDATA stacksize = 0;
  UDATA priority = 5;
  
  return (int)hythread_create_ex(ret_thread, get_gc_thread_group(), stacksize, priority, NULL,
                              (hythread_entrypoint_t)func, data);
}

inline int vm_thread_is_suspend_enable()
{
  return (int)hythread_is_suspend_enabled();
}

inline int vm_suspend_all_threads()
{
  int disable_count = hythread_reset_suspend_disable();
  hythread_suspend_all(NULL, NULL);
  hythread_suspend_disable();
  return disable_count;
}

inline int vm_suspend_all_threads( hythread_iterator_t *thread_iterator )
{
  int disable_count = hythread_reset_suspend_disable();
  hythread_suspend_all(thread_iterator, NULL);
  hythread_suspend_disable();
  return disable_count;
}
inline void vm_resume_all_threads(int disable_count)
{
  hythread_suspend_enable();
  hythread_resume_all(NULL);
  hythread_set_suspend_disable(disable_count);
}

inline void *atomic_casptr(volatile void **mem, void *with, const void *cmp) 
{  return apr_atomic_casptr(mem, with, cmp); }

inline POINTER_SIZE_INT atomic_casptrsz(volatile POINTER_SIZE_INT* mem,
                                        POINTER_SIZE_INT swap, 
                                        POINTER_SIZE_INT cmp)
{
  // we can't use apr_atomic_casptr, which can't work correctly in 64bit machine. 
#ifdef POINTER64
  return port_atomic_cas64(mem, swap, cmp);
#else
  return apr_atomic_cas32(mem, swap, cmp);
#endif
}

inline U_32 atomic_cas32(volatile unsigned int *mem,
                                           apr_uint32_t swap,
                                           apr_uint32_t cmp) 
{  return (U_32)apr_atomic_cas32(mem, swap, cmp); }

inline U_32 atomic_inc32(volatile unsigned int *mem)
{  return (U_32)apr_atomic_inc32(mem); }

inline U_32 atomic_dec32(volatile unsigned int  *mem)
{  return (U_32)apr_atomic_dec32(mem); }

inline U_32 atomic_add32(volatile unsigned int  *mem, unsigned int  val) 
{  return (U_32)apr_atomic_add32(mem, val); }

#ifndef _WINDOWS_
#include <sys/mman.h>
#endif

inline unsigned int vm_get_system_alloc_unit()
{
#ifdef _WINDOWS_  
  SYSTEM_INFO si;
  GetSystemInfo(&si);
  return si.dwAllocationGranularity;
#else 
  return port_vmem_page_sizes()[0];
#endif
}

inline void *vm_map_mem(void* start, POINTER_SIZE_INT size) 
{
  void* address;
#ifdef _WINDOWS_
  address = VirtualAlloc(start, size, MEM_RESERVE|MEM_COMMIT, PAGE_READWRITE);
#else 
  address = mmap(start, size, PROT_READ|PROT_WRITE, MAP_FIXED|MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
  if(address == MAP_FAILED) address = NULL;
    
#endif /* ifdef _WINDOWS_ else */

  return address;
}

inline Boolean vm_unmap_mem(void* start, POINTER_SIZE_INT size) 
{
  unsigned int result;
#ifdef _WINDOWS_
  result = VirtualFree(start, 0, MEM_RELEASE);
#else
  result = munmap(start, size);
  if(result == 0) result = TRUE;
  else result = FALSE;  
#endif /* ifdef _WINDOWS_ else */

  assert(result); /* expect that memory was really released */
  return result;
}

inline void *vm_alloc_mem(void* start, POINTER_SIZE_INT size) 
{
  void* address;
#ifdef _WINDOWS_
  address = VirtualAlloc(start, size, MEM_RESERVE|MEM_COMMIT, PAGE_READWRITE);
#else
  address = mmap(start, size, PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
  if(address == MAP_FAILED) address = NULL;
    
#endif /* ifdef _WINDOWS_ else */

  return address;
}

inline Boolean vm_free_mem(void* start, POINTER_SIZE_INT size) 
{
  return vm_unmap_mem(start, size);
}

inline void *vm_reserve_mem(void* start, POINTER_SIZE_INT size) 
{
  void* address;
#ifdef _WINDOWS_
  address = VirtualAlloc(start, size, MEM_RESERVE, PAGE_READWRITE);
#else
  if(start == 0)
    address = mmap(0, size, PROT_NONE, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
  else
    address = mmap(start, size, PROT_NONE, MAP_FIXED|MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
  
  if(address == MAP_FAILED) address = NULL;
    
#endif /* ifdef _WINDOWS_ else */

  return address;
}

inline Boolean vm_release_mem(void* start, POINTER_SIZE_INT size) 
{
  return vm_unmap_mem(start, size);
}

inline void *vm_commit_mem(void* start, POINTER_SIZE_INT size) 
{
  void* address;
#ifdef _WINDOWS_
  address = VirtualAlloc(start, size, MEM_COMMIT, PAGE_READWRITE);
#else
  int result = mprotect(start, size, PROT_READ|PROT_WRITE);
  if(result == 0) address = start;
  else address = NULL;  
#endif /* ifdef _WINDOWS_ else */

  return address;
}

inline Boolean vm_decommit_mem(void* start, POINTER_SIZE_INT size) 
{
  unsigned int result;
#ifdef _WINDOWS_
  result = VirtualFree(start, size, MEM_DECOMMIT);
#else
  result = mprotect(start, size, PROT_NONE);
  if(result == 0) result = TRUE;
  else result = FALSE;  
    
#endif /* ifdef _WINDOWS_ else */

  return result;
}

inline void mem_fence()
{
  port_rw_barrier(); 
}

inline int64 time_now() 
{  return apr_time_now(); }

inline void string_to_upper(char* s)
{
  while(*s){
    *s = toupper(*s);
    s++;
  }
}  

#ifdef PLATFORM_POSIX
#define max(x, y) (((x)>(y))?(x):(y))
#define min(x, y) (((x)<(y))?(x):(y))
#endif

typedef volatile unsigned int SpinLock;

enum Lock_State{
  FREE_LOCK,
  LOCKED
};

#define try_lock(x) (!atomic_cas32(&(x), LOCKED, FREE_LOCK))
#define lock(x) while( !try_lock(x)){ while( x==LOCKED ){ vm_thread_yield();}}
#define unlock(x) do{ x = FREE_LOCK;}while(0)

#endif //_GC_PLATFORM_H_
