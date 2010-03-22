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
#include "hycomp.h"
#include "hyport.h"
#include "hycunit.h"
#include <stdlib.h>
#include <string.h>

#include <stdio.h>
#define ERROR_STRING "Argh!"

int test_hyerror_set_last_error(struct HyPortLibrary *hyportLibrary);
int test_hyerror_set_last_error_with_message(struct HyPortLibrary *hyportLibrary);
int test_hyerror_last_error_message(struct HyPortLibrary *hyportLibrary);
int test_hyerror_last_error_number(struct HyPortLibrary *hyportLibrary);
int test_hyerror_startup(struct HyPortLibrary *hyportLibrary);
int test_hyerror_shutdown(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;

  printf("hyerror:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");
  
  Hytest_init(&hyportLibrary, "Portlib.Hyerror");
  Hytest_func(&hyportLibrary, test_hyerror_last_error_message, "hyerror_last_error_message");
  Hytest_func(&hyportLibrary, test_hyerror_last_error_number, "hyerror_last_error_number");
  Hytest_func(&hyportLibrary, test_hyerror_set_last_error, "hyerror_set_last_error");
  Hytest_func(&hyportLibrary, test_hyerror_set_last_error_with_message, "hyerror_set_last_error_with_message");
  Hytest_func(&hyportLibrary, test_hyerror_startup, "hyerror_startup");
  Hytest_func(&hyportLibrary, test_hyerror_shutdown, "hyerror_shutdown");
  ret = Hytest_close_and_output(&hyportLibrary);
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  
  printf("  portlib shutdown\n");
  return ret;
}

int test_hyerror_set_last_error(struct HyPortLibrary *hyportLibrary)
{
  I_32 num;
  const char *err;
  hyportLibrary->error_set_last_error(hyportLibrary,
                                     2, HYPORT_ERROR_NOTFOUND);
  num = hyportLibrary->error_last_error_number(hyportLibrary);
  err = hyportLibrary->error_last_error_message(hyportLibrary);
  printf("  num = %d, err = %s\n", num, err);

  if (num != HYPORT_ERROR_NOTFOUND) {
    Hytest_setErrMsg(hyportLibrary, "hyerror_last_error_number Output should be [%d] not [%d] (%s)\n",
            HYPORT_ERROR_NOTFOUND,num,HY_GET_CALLSITE());
    return -1;
  }

  if (strncmp(err, "", sizeof("")) == 0) {
    Hytest_setErrMsg(hyportLibrary, "hyerror_last_error_message was empty(%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyerror_set_last_error_with_message(struct HyPortLibrary *hyportLibrary)
{
  const char *err;
  hyportLibrary->error_set_last_error_with_message(hyportLibrary,
                                                  HYPORT_ERROR_NOTFOUND,
                                                  ERROR_STRING);
  err = hyportLibrary->error_last_error_message(hyportLibrary);
  printf("  err = %s\n", err);

  if (strncmp(err, ERROR_STRING, sizeof(ERROR_STRING)) != 0) {
    Hytest_setErrMsg(hyportLibrary, "hyerror_last_error_message Output should be [%s] not [%s] (%s)\n", ERROR_STRING,
            err,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyerror_last_error_message(struct HyPortLibrary *hyportLibrary)
{
  const char *err;
  err = hyportLibrary->error_last_error_message(hyportLibrary);
  printf("  err = %s\n", err);
  if (strncmp(err, "", sizeof("")) != 0) {
    Hytest_setErrMsg(hyportLibrary,
            "hyerror_last_error_message Output should be empty not [%s] (%s)\n", err,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyerror_last_error_number(struct HyPortLibrary *hyportLibrary)
{
  hyportLibrary->error_last_error_number(hyportLibrary);
  return 0;
}

int test_hyerror_startup(struct HyPortLibrary *hyportLibrary)
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
    hyportLibrary2.error_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "error startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.error_shutdown(&hyportLibrary2);
  return 0;
}

int test_hyerror_shutdown(struct HyPortLibrary *hyportLibrary)
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
    hyportLibrary2.error_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "time startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.error_shutdown(&hyportLibrary2);
  return 0;
}
