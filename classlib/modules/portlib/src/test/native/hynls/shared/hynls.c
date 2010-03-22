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
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "hyport.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "hycunit.h"

int test_hynls_get_language(struct HyPortLibrary *hyportLibrary);
int test_hynls_get_region(struct HyPortLibrary *hyportLibrary);
int test_hynls_get_variant(struct HyPortLibrary *hyportLibrary);
int test_hynls_lookup_message(struct HyPortLibrary *hyportLibrary);
int test_hynls_printf(struct HyPortLibrary *hyportLibrary);
int test_hynls_vprintf(struct HyPortLibrary *hyportLibrary);
int test_hynls_set_catalog(struct HyPortLibrary *hyportLibrary);
int test_hynls_set_locale(struct HyPortLibrary *hyportLibrary);
int test_hynls_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hynls_startup(struct HyPortLibrary *hyportLibrary);
void forVprintfTest(struct HyPortLibrary *hyportLibrary,UDATA flags,U_32 module_name,U_32 message_num,char *format,...);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;

  printf("hynls:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");
  
  Hytest_init(&hyportLibrary, "Portlib.Hynls");
  Hytest_func(&hyportLibrary, test_hynls_get_language, "hynls_get_language");
  Hytest_func(&hyportLibrary, test_hynls_get_region, "hynls_get_region");
  Hytest_func(&hyportLibrary, test_hynls_get_variant, "hynls_get_variant");
  Hytest_func(&hyportLibrary, test_hynls_lookup_message, "hynls_lookup_message");
  Hytest_func(&hyportLibrary, test_hynls_printf, "hynls_printf");
  Hytest_func(&hyportLibrary, test_hynls_vprintf, "hynls_vprintf");
  Hytest_func(&hyportLibrary, test_hynls_set_catalog, "hynls_set_catalog");
  Hytest_func(&hyportLibrary, test_hynls_set_locale, "hynls_set_locale");
  Hytest_func(&hyportLibrary, test_hynls_startup, "hynls_startup");
  Hytest_func(&hyportLibrary, test_hynls_shutdown, "hynls_shutdown");
  ret = Hytest_close_and_output(&hyportLibrary);
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  
  printf("  portlib shutdown\n");
  return ret;
}

int test_hynls_get_language(struct HyPortLibrary *hyportLibrary)
{
  const char * language;
  language = hyportLibrary->nls_get_language(hyportLibrary);
  printf("language:\t%s\n",language);
  if(!language){
    Hytest_setErrMsg(hyportLibrary,
            "hynls_get_language Output should be [%s] not null (%s)\n",language ,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hynls_get_region(struct HyPortLibrary *hyportLibrary)
{
  const char * region;
  region = hyportLibrary->nls_get_region(hyportLibrary);
  printf("region:\t%s\n",region);
  if(!region){
    Hytest_setErrMsg(hyportLibrary,
            "hynls_get_region Output should be [%s] not null (%s)\n",region ,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hynls_get_variant(struct HyPortLibrary *hyportLibrary)
{
  const char * variant;
  variant = hyportLibrary->nls_get_variant(hyportLibrary);
  printf("variant:\t%s\n",variant);
  if(!variant){
    Hytest_setErrMsg(hyportLibrary,
            "hynls_get_variant Output should be [%s] not null (%s)\n",variant ,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hynls_lookup_message(struct HyPortLibrary *hyportLibrary)
{
  const char * message;
  message = hyportLibrary->nls_lookup_message(hyportLibrary,HYNLS_INFO,0,0,"Not Found");
  printf("message:\t%s\n",message);
  if(!message){
    Hytest_setErrMsg(hyportLibrary,
            "hynls_lookup_message Output should be [%s] not null (%s)\n",message ,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hynls_printf(struct HyPortLibrary *hyportLibrary)
{
  hyportLibrary->nls_printf(hyportLibrary,HYNLS_INFO,0,0,"Not Found");
  return 0;
}
int test_hynls_vprintf(struct HyPortLibrary *hyportLibrary)
{
  forVprintfTest(hyportLibrary,HYNLS_INFO,0,0,"","Not Found");
  return 0;
}

int test_hynls_set_catalog(struct HyPortLibrary *hyportLibrary)
{
  char *currentPath = NULL;
  char * endPathPtr = NULL;
  
  hyportLibrary->sysinfo_get_executable_name (hyportLibrary, NULL, &currentPath);
  endPathPtr = strrchr (currentPath, DIR_SEPARATOR);
  endPathPtr[1] = '\0';
  
  hyportLibrary->nls_set_catalog (hyportLibrary, (const char**) &currentPath,
                   1, "harmony", "properties");
  return 0;
}

int test_hynls_set_locale(struct HyPortLibrary *hyportLibrary)
{
  const char *lang = "en";
  const char *region = "US";
  const char *variant = "Test";
  const char *langGet;
  const char *regionGet;
  const char *variantGet;
  
  hyportLibrary->nls_set_locale (hyportLibrary, lang, region, variant);
  langGet = hyportLibrary->nls_get_language(hyportLibrary);
  regionGet = hyportLibrary->nls_get_region(hyportLibrary);
  variantGet = hyportLibrary->nls_get_variant(hyportLibrary);
  
  if(strcmp(lang,langGet)){
    Hytest_setErrMsg(hyportLibrary,
            "langGet Output should be [%s] not [%s] (%s)\n", lang, langGet,HY_GET_CALLSITE());
    return -1;
  }
  
  if(strcmp(region,regionGet)){
    Hytest_setErrMsg(hyportLibrary,
            "langGet Output should be [%s] not [%s] (%s)\n", region, regionGet,HY_GET_CALLSITE());
    return -1;
  }
  
  if(strcmp(variant,variantGet)){
    Hytest_setErrMsg(hyportLibrary,
            "langGet Output should be [%s] not [%s] (%s)\n", variant, variantGet,HY_GET_CALLSITE());
    return -1;
  }
  
  return 0;
}

int test_hynls_shutdown(struct HyPortLibrary *hyportLibrary)
{ 
  HyPortLibrary hyportLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 rc;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return -1;
  }
  rc =
    hyportLibrary2.nls_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "nls startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.nls_shutdown (&hyportLibrary2);
  return 0;
}

int test_hynls_startup(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary hyportLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 rc;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return -1;
  }
  rc =
    hyportLibrary2.nls_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "nls startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

void forVprintfTest(struct HyPortLibrary *hyportLibrary,UDATA flags,U_32 module_name,U_32 message_num,char *format,...)
{
  va_list args;
  va_start (args, format);
  hyportLibrary->nls_vprintf (hyportLibrary, flags,
               module_name, message_num, args);
  va_end (args);
}

