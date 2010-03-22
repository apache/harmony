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
#include <stdio.h>

#include "hycomp.h"
#include "hyport.h"
#include "hythread.h"
#include "hycunit.h"

int test_hytime_current_time_millis(struct HyPortLibrary *hyportLibrary);
int test_hytime_msec_clock(struct HyPortLibrary *hyportLibrary);
int test_hytime_hires_clock(struct HyPortLibrary *hyportLibrary);
int test_hytime_hires_delta(struct HyPortLibrary *hyportLibrary);
int test_hytime_hires_frequency(struct HyPortLibrary *hyportLibrary);
int test_hytime_usec_clock(struct HyPortLibrary *hyportLibrary);
int test_hytime_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hytime_startup(struct HyPortLibrary *hyportLibrary);

#ifdef HY_NO_THR
HyThreadLibrary *privateThreadLibrary;
#endif

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;

  printf("hytime:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");

#ifdef HY_NO_THR
  privateThreadLibrary = hyportLibrary.port_get_thread_library(&hyportLibrary);
#endif

  Hytest_init(&hyportLibrary, "Portlib.Hytime");
  Hytest_func(&hyportLibrary, test_hytime_current_time_millis, "hytime_current_time_millis");
  Hytest_func(&hyportLibrary, test_hytime_msec_clock, "hytime_msec_clock");
  Hytest_func(&hyportLibrary, test_hytime_hires_clock, "hytime_hires_clock");
  Hytest_func(&hyportLibrary, test_hytime_hires_delta, "hytime_hires_delta");
  Hytest_func(&hyportLibrary, test_hytime_hires_frequency, "hytime_hires_frequency");
  Hytest_func(&hyportLibrary, test_hytime_usec_clock, "hytime_usec_clock");
  Hytest_func(&hyportLibrary, test_hytime_shutdown, "hytime_shutdown");
  Hytest_func(&hyportLibrary, test_hytime_startup, "hytime_startup");
  ret = Hytest_close_and_output(&hyportLibrary);
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  printf("  portlib shutdown\n");

  return ret;
}

int test_hytime_current_time_millis(struct HyPortLibrary *hyportLibrary)
{
  I_64 millis;
  millis = hyportLibrary->time_current_time_millis(hyportLibrary);
  printf("millis = %lld\n", millis);
  return 0;
}

int test_hytime_msec_clock(struct HyPortLibrary *hyportLibrary)
{
  UDATA msec;
  msec = hyportLibrary->time_msec_clock(hyportLibrary);
  printf("msec = %u\n", msec);
  return 0;
}

int test_hytime_hires_clock(struct HyPortLibrary *hyportLibrary)
{
  U_64 hires,hires2;
  hires = hyportLibrary->time_hires_clock(hyportLibrary);
  printf("hires = %llu\n", hires);
  hythread_sleep(1000);
  hires2 = hyportLibrary->time_hires_clock(hyportLibrary);
  printf("hires2 = %llu\n", hires2);
  return 0;
}

int test_hytime_hires_delta(struct HyPortLibrary *hyportLibrary)
{
  U_64 delta,hires,hires2,freq;
  hires = hyportLibrary->time_hires_clock(hyportLibrary);
  printf("hires = %llu\n", hires);
  freq = hyportLibrary->time_hires_frequency(hyportLibrary);
  printf("freq = %llu\n", freq);

  hythread_sleep(1000);

  hires2 = hyportLibrary->time_hires_clock(hyportLibrary);
  printf("hires2 = %llu\n", hires2);
  
  delta = hyportLibrary->time_hires_delta(hyportLibrary,
                                         hires, hires2,
                                         HYPORT_TIME_DELTA_IN_MICROSECONDS);
  printf("delta = %llu\n", delta);
  if (delta <= 0) {
    Hytest_setErrMsg(hyportLibrary, "hires_clock did not increment after 1s sleep\n");
    return -1;
  }
  return 0;
}

int test_hytime_hires_frequency(struct HyPortLibrary *hyportLibrary)
{
  U_64 freq;
  freq = hyportLibrary->time_hires_frequency(hyportLibrary);
  printf("freq = %llu\n", freq);
  return 0;
}

int test_hytime_usec_clock(struct HyPortLibrary *hyportLibrary)
{
  
  UDATA usec;
  usec = hyportLibrary->time_usec_clock(hyportLibrary);
  printf("usec = %u\n", usec);
  return 0;
}

int test_hytime_shutdown(struct HyPortLibrary *hyportLibrary)
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
    hyportLibrary2.time_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "time startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.time_shutdown(&hyportLibrary2);
  return 0;
}

int test_hytime_startup(struct HyPortLibrary *hyportLibrary)
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
    hyportLibrary2.time_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "time startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

