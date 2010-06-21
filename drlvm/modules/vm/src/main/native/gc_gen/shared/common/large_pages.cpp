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
 * @author Yu-Nan He, 2007/01/18
 */
#define LOG_DOMAIN "gc.base"
#include "gc_common.h"
char* large_page_hint = NULL;

#if defined (_WINDOWS_)
Boolean set_privilege(HANDLE process, LPCTSTR priv_name, Boolean is_enable)
{
  HANDLE token;
  TOKEN_PRIVILEGES tp;
  bool res = OpenProcessToken(process, TOKEN_ADJUST_PRIVILEGES, &token);
  if(!res){
    return FALSE;
  }
  
  tp.PrivilegeCount = 1;
  tp.Privileges[0].Attributes = is_enable ? SE_PRIVILEGE_ENABLED : 0;
  
  res = LookupPrivilegeValue( NULL, priv_name, &tp.Privileges[0].Luid);
  if(!res){
    CloseHandle(token);
    return FALSE;
  }
  
  if (AdjustTokenPrivileges( token, FALSE, &tp, 0, NULL, 0) == ERROR_NOT_ALL_ASSIGNED) {
    CloseHandle(token);
    return FALSE;
  }
  return TRUE;
}

Boolean obtain_lock_memory_priv()
{
  HANDLE process = GetCurrentProcess();
  return set_privilege(process, SE_LOCK_MEMORY_NAME, TRUE);
}

Boolean release_lock_memory_priv()
{
  HANDLE process = GetCurrentProcess();
  return set_privilege(process, SE_LOCK_MEMORY_NAME, FALSE);
}

void* alloc_large_pages(size_t size, const char* hint)
{
  void* alloc_addr = NULL;
  bool lock_memory_enable = obtain_lock_memory_priv();
  
  if(lock_memory_enable){
    alloc_addr = VirtualAlloc(NULL, size, MEM_RESERVE | MEM_COMMIT | MEM_LARGE_PAGES, PAGE_READWRITE);    
    release_lock_memory_priv();    
    if(alloc_addr == NULL){
      LWARN(49, "GC large_page: No required number of large pages found. Please reboot.....");
      return NULL;
    }else
      return alloc_addr;
  }else{
    LWARN(50, "GC large_page: Check that you have permissions:\nGC large_page: Control Panel->Administrative Tools->Local Security Settings->->User Rights Assignment->Lock pages in memory.\nGC large_page: Start VM as soon after reboot as possible, because large pages become fragmented and unusable after a while.\nGC large_page: Heap size should be multiple of large page size.");
    return NULL;
  }
}

#elif defined (__linux__)
#include <fcntl.h>
#include <unistd.h>

static size_t proc_huge_page_size   = 4 * MB;
static size_t proc_huge_pages_total = (size_t)-1;
static size_t proc_huge_pages_free  = 0;
static const char* str_HugePages_Total = "HugePages_Total:";
static const char* str_HugePages_Free  = "HugePages_Free:";
static const char* str_Hugepagesize    = "Hugepagesize:";


static const char* parse_value(const char* buf, int len, const char* name, int name_len, size_t* value){
  if (len < name_len) return NULL;
  if (strncmp(buf, name, name_len)) return NULL;
  buf += name_len;
  char* endpos;
  long int res = strtol(buf, &endpos, 10);
  if (endpos == buf) return NULL;
  *value = (size_t) res;
  return endpos;
}

static void parse_proc_meminfo(size_t required_size){
  FILE* f = fopen("/proc/meminfo", "r");
  if (f == NULL){
    LWARN(51, "GC large_page: Can't open /proc/meminfo");
    return;
  }

  size_t size = 128;
  char* buf = (char*) malloc(size);
  while (true){
    ssize_t len = getline(&buf, &size, f);
    if (len == -1) break;
    parse_value(buf, len, str_HugePages_Total, strlen(str_HugePages_Total), &proc_huge_pages_total);
    parse_value(buf, len, str_HugePages_Free, strlen(str_HugePages_Free), &proc_huge_pages_free);
    const char* end =parse_value(buf, len, str_Hugepagesize, strlen(str_Hugepagesize), &proc_huge_page_size);
    if (end && !strncmp(end, " kB", 3)) proc_huge_page_size *= KB;
  }
  if (buf) free(buf);
  
  if (proc_huge_pages_total == (size_t)-1){
    LWARN(52, "GC large_page: Large pages are not supported by kernel.\nGC large_page: CONFIG_HUGETLB_PAGE and CONFIG_HUGETLBFS needs to be enabled.");
  } else if (proc_huge_pages_total == 0){
    LWARN(53, "GC large_page: No large pages reserved,  Use the following command: echo num> /proc/sys/vm/nr_hugepages.\nGC large_page: Do it just after kernel boot before huge pages become fragmented.");
  } else {
    //compute required huge page number
    size_t required = (required_size+proc_huge_page_size-1)/proc_huge_page_size;
    if (proc_huge_pages_total < required) {
      LWARN(54, "GC large_page: required size exceeds total large page size.");
    } else if (proc_huge_pages_free < required) {
      LWARN(55, "GC large_page: required size exceeds free large page size.");
    }
  }
}

void* mmap_large_pages(size_t size, const char* path)
{
  const char* postfix = "/vm_heap";
  char* buf = (char*) malloc(strlen(path) + strlen(postfix) + 1);
  assert(buf);

  strcpy(buf, path);
  strcat(buf, postfix);

  int fd = open(buf, O_CREAT | O_RDWR, 0700);
  if (fd == -1){
    LWARN(56, "GC large_page: Can't open Mount hugetlbfs with: mount none /mnt/huge -t hugetlbfs.\nGC large_page: Check you have appropriate permissions to /mnt/huge.\nGC large_page: Use command line switch -Dgc.lp=/mnt/huge.");
    free(buf);
    return NULL;
  }
  unlink(buf);

  void* addr = mmap(0, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
  if (addr == MAP_FAILED){
    LWARN(57, "GC large_page: Map failed.");
    close(fd);
    free(buf);
    return NULL;
  }
  close(fd);
  free(buf);
  return addr;
}

void* alloc_large_pages(size_t size, const char* hint){
  parse_proc_meminfo(size);
  void* alloc_addr = mmap_large_pages(size, hint);
  if(alloc_addr == NULL){
    LWARN(58, "GC large_page: Large pages allocation failed.");
    return NULL;
  }
  return alloc_addr;
}
#elif defined(FREEBSD)
void* mmap_large_pages(size_t size, const char* path)
{
  return NULL;
}
void* alloc_large_pages(size_t size, const char* hint)
{
  return NULL;
}
#else
#error Need to define mmap_large_pages and alloc_large_pages
#endif
