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

#define MAX_MSG_LEN 10000

typedef struct Testcase 
{
  char *name;
  double time;
  int pass;
  char *msg;
  struct Testcase *next;
} Testcase;

struct Testcase *testcases = NULL;
char err_msg[MAX_MSG_LEN] = "\0";

void
Hytest_init(HyPortLibrary *portLibrary, char *module_name)
{
  PORT_ACCESS_FROM_PORT (portLibrary);
  while (testcases!=NULL) 
  {
    struct Testcase *t = testcases;
    testcases = testcases->next;
    hymem_free_memory(t->name);
    hymem_free_memory(t->msg);
    hymem_free_memory(t);
  }
  testcases = hymem_allocate_memory(sizeof(struct Testcase));
  testcases->name = hymem_allocate_memory(strlen(module_name)+1);
  strcpy(testcases->name, module_name);
  testcases->time = 0;
  testcases->pass = 1;
  testcases->msg = NULL;
  testcases->next = NULL;
  hyfile_printf(PORTLIB, HYPORT_TTY_OUT, "\t[hycunit] Now test module: %s\n", testcases->name);
}

int
Hytest_close_and_output(HyPortLibrary *portLibrary)
{
  char *buf = NULL;
  char *prefix = "TEST-";
  char *postfix = ".xml";
  char *module_name = NULL;
  int buflen;
  struct Testcase *final = NULL, *t = NULL;
  IDATA fd;
  double total_time = 0;
  int fails = 0;
  int totalcase = 0;
  PORT_ACCESS_FROM_PORT (portLibrary);
  
  while (testcases!=NULL)
  {
    t = testcases;
    totalcase ++;
    total_time += t->time;
    if (!t->pass) fails++;
    testcases = t->next;
    t->next = final;
    final = t;
  }
  totalcase -=1;
  if (final == NULL) return 0;
  module_name = hymem_allocate_memory(strlen(final->name)+1);
  strcpy(module_name, final->name);
  buflen = strlen(prefix) + strlen(module_name) + strlen(postfix) + 1;
  buf = hymem_allocate_memory(buflen);
  hystr_printf(portLibrary, buf, buflen, "%s%s%s", prefix, module_name, postfix); 
  fd = hyfile_open(buf, HyOpenCreate | HyOpenWrite | HyOpenTruncate, 0600);
  
  hyfile_printf(portLibrary, fd, "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
  hyfile_printf(portLibrary, fd, "<testsuite errors=\"%d\" failures=\"%d\" name=\"%s\" tests=\"%d\" time=\"%lf\">\n", 
		  0, fails, module_name, totalcase, total_time);
  do {
    t = final;
    final = final->next;
    if (t->name!=NULL) hymem_free_memory(t->name);
    if (t->msg!=NULL) hymem_free_memory(t->msg);
    hymem_free_memory(t);
    if (final != NULL)
    {

      hyfile_printf(portLibrary, fd, "<testcase classname=\"%s\" name=\"%s\" time=\"%lf\">", 
		  module_name, final->name, final->time);      
      if (!final->pass)
      {
	if (final->msg!=NULL)
          hyfile_printf(portLibrary, fd, "\n<failure message=\"%s\" type=\"native code failure\"></failure>\n",
			final->msg);
	else 
          hyfile_printf(portLibrary, fd, "\n<failure message=\"Failed\" type=\"native code failure\"></failure>\n");
      }
      hyfile_printf(portLibrary, fd, "</testcase>\n");
    }
  } while (final != NULL);

  hyfile_printf(portLibrary, fd, "</testsuite>\n");  
  hyfile_close(fd);
  
  if (module_name!=NULL) hymem_free_memory(module_name);
  if (strlen(err_msg) != 0) err_msg[0] = '\0';
  if (buf!=NULL) hymem_free_memory(buf);

  if (fails) return -1;
  return 0;
}

void 
Hytest_func(HyPortLibrary *portLibrary, int (*testfunc)(HyPortLibrary *), char *func_name)
{
  U_64 hires, hires2, delta;
  int res;
  struct Testcase *onetest;

  PORT_ACCESS_FROM_PORT (portLibrary);
  onetest = hymem_allocate_memory(sizeof(struct Testcase));
  onetest->name = hymem_allocate_memory(strlen(func_name)+1);
  strcpy(onetest->name, func_name);

  hires = hytime_hires_clock();
  res = testfunc(portLibrary);
  hires2 = hytime_hires_clock();
  delta = hytime_hires_delta(hires, hires2, HYPORT_TIME_DELTA_IN_MILLISECONDS);
  onetest->time = (double)delta/1000.0;
  hyfile_printf(PORTLIB, HYPORT_TTY_OUT, "\t[hycunit] Test run on: %s, ", func_name);
  onetest->msg = NULL;
  if (res == 0)
  {
    hyfile_printf(PORTLIB, HYPORT_TTY_OUT, "Passed :), ");
    onetest->pass = 1;
  }
  else
  {
    hyfile_printf(PORTLIB, HYPORT_TTY_OUT, "Failed :(, ");
    onetest->pass = 0;
    if (strlen(err_msg) >0) 
    {
      onetest->msg = hymem_allocate_memory(strlen(err_msg)+1); 
      strcpy(onetest->msg, err_msg);
      err_msg[0]='\0';
    }
  }
  hyfile_printf(PORTLIB, HYPORT_TTY_OUT, "Time elapsed: %lf sec\n", onetest->time);
  onetest->next = testcases;
  testcases = onetest;
}

void
Hytest_setErrMsg(HyPortLibrary *portLibrary, const char *format, ...)
{
  U_32 rc;
  va_list args;
  va_start (args, format);
  rc = portLibrary->str_vprintf (portLibrary, err_msg, MAX_MSG_LEN, format, args);
  va_end (args);
}

