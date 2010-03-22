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

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "hycomp.h"
#include "hyport.h"
#include "hycunit.h"

int test_hystr_printf_int(struct HyPortLibrary *hyportLibrary);
int test_hystr_printf_double(struct HyPortLibrary *hyportLibrary);
int test_hystr_printf (struct HyPortLibrary *hyportLibrary);
int test_hystr_vprintf (struct HyPortLibrary *hyportLibrary);
int test_hystr_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hystr_startup(struct HyPortLibrary *hyportLibrary);
U_32 forVprintfTest(struct HyPortLibrary *hyportLibrary,char *buf,U_32 buflen,char* format,...);

int main (int argc, char **argv, char **envp)
{
   HyPortLibrary hyportLibrary;
   HyPortLibraryVersion portLibraryVersion;
   int ret;

   printf("hystr:\n");

   HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
   if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
   {
      fprintf(stderr, "portlib init failed\n");
      return 1;
   }

   printf("  portlib initialized\n");

   Hytest_init(&hyportLibrary, "Portlib.Hystr");
   Hytest_func(&hyportLibrary, test_hystr_printf_int, "hystr_printf_int");
   Hytest_func(&hyportLibrary, test_hystr_printf_double, "hystr_printf_double");
   Hytest_func(&hyportLibrary, test_hystr_printf, "hystr_printf");
   Hytest_func(&hyportLibrary, test_hystr_vprintf, "hystr_vprintf");
   Hytest_func(&hyportLibrary, test_hystr_startup, "test_hystr_startup");
   Hytest_func(&hyportLibrary, test_hystr_shutdown, "hystr_shutdown");
   ret = Hytest_close_and_output(&hyportLibrary);
     
   if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
      fprintf(stderr, "portlib shutdown failed\n");
      return 1;
   }
   printf("  portlib shutdown\n");

   return ret;
}

int test_hystr_printf_int(struct HyPortLibrary *hyportLibrary)
{
   PORT_ACCESS_FROM_PORT(hyportLibrary);
   char buf[1000];
   int testcase[]={0, 1, -1, 1000, -2000, 999999};
   char *testout[]={"  0", "  1", " -1", "1000", "-2000", "999999"};
   int i;
   for (i=0; i<sizeof(testcase)/sizeof(int); i++)
   {
      hyportLibrary->str_printf(hyportLibrary, buf, 1000, "%3d", testcase[i]);
      if (strcmp(buf, testout[i])!=0)
      {
	 Hytest_setErrMsg(hyportLibrary, "Output should be [%s] not [%s] (%s)", testout[i], buf, HY_GET_CALLSITE());
	 return -1;
      }
   }
   return 0;
}

int test_hystr_printf_double(struct HyPortLibrary *hyportLibrary)
{
   PORT_ACCESS_FROM_PORT(hyportLibrary);
   char buf[1000];
   double testcase[]={0, 1, -1.090909, 1000.12345, -2000.999999};
   char *testout[]={" 0.000", " 1.000", "-1.091", "1000.123", "-2001.000"};
   int i;
   for (i=0; i<sizeof(testcase)/sizeof(double); i++)
   {
      hyportLibrary->str_printf(hyportLibrary, buf, 1000, "%6.3lf", testcase[i]);
      if (strcmp(buf, testout[i])!=0)
      {
	 Hytest_setErrMsg(hyportLibrary, "Output should be [%s] not [%s] (%s)", testout[i], buf, HY_GET_CALLSITE());
	 return -1;
      }
   }
   return 0;
}

int test_hystr_printf (struct HyPortLibrary *hyportLibrary)
{
   char buf[512];
   U_32 ret;
   ret = hyportLibrary->str_printf(hyportLibrary, buf, 10,"%d",-1);
   if(ret!=2)
   {
      Hytest_setErrMsg(hyportLibrary, "The character num Output should be [2] not [%d] (%s)\n", ret, HY_GET_CALLSITE());
      return -1;
   }
   if(strcmp(buf,"-1"))
   {
      Hytest_setErrMsg(hyportLibrary, "The buffer Output should be [-1] not [%s] (%s)\n", buf, HY_GET_CALLSITE());
      return -1;
   }

   ret = hyportLibrary->str_printf(hyportLibrary, buf, 10,"%09d %7.4d %7.2f",9,-2,3.1415926535);
   if(ret!=9)
   {
      Hytest_setErrMsg(hyportLibrary, "The character num Output should be [10] not [%d] (%s)\n", ret, HY_GET_CALLSITE());
      return -1;
   }
   if(strcmp(buf,"000000009"))
   {
      Hytest_setErrMsg(hyportLibrary, "The buffer Output should be [000000009   -0002    3.14] not [%s] (%s)\n", buf, HY_GET_CALLSITE());
      return -1;
   } 
   return 0;
}

int test_hystr_vprintf (struct HyPortLibrary *hyportLibrary)
{
   char buf[512];
   U_32 ret;
   ret = forVprintfTest(hyportLibrary, buf, 10,"%d",-1);
   if(ret!=2)
   {
      Hytest_setErrMsg(hyportLibrary, "The character num Output should be [2] not [%d] (%s)\n", ret, HY_GET_CALLSITE());
      return -1;
   }
   if(strcmp(buf,"-1"))
   {
      Hytest_setErrMsg(hyportLibrary, "The buffer Output should be [-1] not [%s] (%s)\n", buf, HY_GET_CALLSITE());
      return -1;
   }

   ret = forVprintfTest(hyportLibrary, buf, 10,"%09d %7.4d %7.2f",9,-2,3.1415926535);
   if(ret!=9)
   {
      Hytest_setErrMsg(hyportLibrary, "The character num Output should be [10] not [%d] (%s)\n", ret, HY_GET_CALLSITE());
      return -1;
   }
   if(strcmp(buf,"000000009"))
   {
      Hytest_setErrMsg(hyportLibrary, "The buffer Output should be [000000009   -0002    3.14] not [%s] (%s)\n", buf, HY_GET_CALLSITE());
      return -1;
   } 
   return 0;
}

int test_hystr_shutdown(struct HyPortLibrary *hyportLibrary)
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
      hyportLibrary2.str_startup (&hyportLibrary2);
   if (0 != rc)
   {
      Hytest_setErrMsg(hyportLibrary, "error startup failed: %s (%s)\n",
      hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
      return -1;
   }
   hyportLibrary2.str_shutdown(&hyportLibrary2);
   return 0;
}

int test_hystr_startup(struct HyPortLibrary *hyportLibrary)
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
     hyportLibrary2.str_startup (&hyportLibrary2);
   if (0 != rc)
   {
      Hytest_setErrMsg(hyportLibrary, "time startup failed: %s (%s)\n",
      hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
      return -1;
   }
   hyportLibrary2.str_shutdown(&hyportLibrary2);
   return 0;
}

U_32 forVprintfTest(struct HyPortLibrary *hyportLibrary,char *buf ,U_32 buflen,char* format,...)
{
   U_32 ret;
   va_list args;
   va_start (args, format);
   ret = hyportLibrary->str_vprintf(hyportLibrary, buf, buflen ,format, args);
   va_end (args);
   return ret;
}

