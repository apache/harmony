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

#define CDEV_CURRENT_FUNCTION _comment_
/** 
 * @file
 * @ingroup Port
 * @brief Timer utilities
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>

#include "hyport.h"
#include "portpriv.h"
#include "hyportpg.h"

#define CDEV_CURRENT_FUNCTION _prototypes_private
void VMCALL shutdown_timer (void);
I_32 VMCALL init_timer (void);
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_msec_clock
/**
 * Query OS for timestamp.
 * Retrieve the current value of system clock and convert to milliseconds.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, time value in milliseconds on success.
 * @deprecated Use @ref hytime_hires_clock and @ref hytime_hires_delta
 */
UDATA VMCALL
hytime_msec_clock (struct HyPortLibrary *portLibrary)
{
  LARGE_INTEGER freq, i, multiplier;
  UDATA result;

  if (!QueryPerformanceFrequency (&freq))
    {
      return (UDATA) GetTickCount ();
    }

  multiplier.QuadPart = freq.QuadPart / 1000;

  QueryPerformanceCounter (&i);
  result = (UDATA) (i.QuadPart / multiplier.QuadPart);

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_usec_clock
/**
 * Query OS for timestamp.
 * Retrieve the current value of system clock and convert to microseconds.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, time value in microseconds on success.
 * @deprecated Use @ref hytime_hires_clock and @ref hytime_hires_delta
 */
UDATA VMCALL
hytime_usec_clock (struct HyPortLibrary * portLibrary)
{
  LARGE_INTEGER freq, i, multiplier;
  UDATA result;

  if (!QueryPerformanceFrequency (&freq))
    {
      return (UDATA) GetTickCount ();
    }

  multiplier.QuadPart = freq.QuadPart / 1000000;

  QueryPerformanceCounter (&i);
  result = (UDATA) (i.QuadPart / multiplier.QuadPart);

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_current_time_millis
/**
 * Query OS for timestamp.
 * Retrieve the current value of system clock and convert to milliseconds since
 * January 1st 1970.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, time value in milliseconds on success.
 */
I_64 VMCALL
hytime_current_time_millis (struct HyPortLibrary * portLibrary)
{
  /* returns in time the number of 100ns elapsed since January 1, 1601 */
  /* subtract 116444736000000000 = number of 100ns from jan 1, 1601 to jan 1, 1970 */
  /* multiply by 10000 to get number of milliseconds since Jan 1, 1970 */

  LONGLONG time;
  GetSystemTimeAsFileTime ((FILETIME *) & time);
  return (I_64) ((time - 116444736000000000) / 10000);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION init_timer
I_32 VMCALL
init_timer (void)
{
  /* on Win98 this forced the process to run with a higher
   * resolution clock. It made things like Thread.sleep() more
   * accurate. But the functions it calls are defined in WINMM.DLL,
   * which forces USER32.DLL, GDI.DLL and other modules to
   * be loaded, polluting the address space. By not loading WINMM.DLL
   * we increase the chances of having a large contiguous region
   * of virtual memory to use as the Java heap.
   */
#if 0
  TIMECAPS timecaps;
  timeGetDevCaps (&timecaps, sizeof (TIMECAPS));
  timeBeginPeriod (timecaps.wPeriodMin);
#endif

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION shutdown_timer
void VMCALL
shutdown_timer (void)
{
  /* see init_timer */
#if 0
  TIMECAPS timecaps;
  timeGetDevCaps (&timecaps, sizeof (TIMECAPS));
  timeEndPeriod (timecaps.wPeriodMin);
#endif
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_hires_clock
/**
 * Query OS for timestamp.
 * Retrieve the current value of the high-resolution performance counter.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, time value on success.
 */
U_64 VMCALL
hytime_hires_clock (struct HyPortLibrary *portLibrary)
{
  LARGE_INTEGER i;

  if (QueryPerformanceCounter (&i))
    {
      return (U_64) i.QuadPart;
    }
  else
    {
      return (U_64) GetTickCount ();
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_hires_frequency
/**
 * Query OS for clock frequency
 * Retrieves the frequency of the high-resolution performance counter.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, number of ticks per second on success.
 */
U_64 VMCALL
hytime_hires_frequency (struct HyPortLibrary * portLibrary)
{
  return PPG_time_hiresClockFrequency;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_hires_delta
/**
 * Calculate time difference between two hires clock timer values @ref hytime_hires_clock.
 *
 * Given a start and end time determine how much time elapsed.  Return the value as
 * requested by the required resolution
 *
 * @param[in] portLibrary The port library.
 * @param[in] startTime Timer value at start of timing interval
 * @param[in] endTime Timer value at end of timing interval
 * @param[in] requiredResolution Returned timer resolution as a fraction of a second.  For example: 
 *  \arg 1 to report elapsed time in seconds
 *  \arg 1,000 to report elapsed time in milliseconds
 *  \arg 1,000,000 to report elapsed time in microseconds
 *
 * @return 0 on failure, time difference on success.
 *
 * @note helper macros are available for commonly requested resolution
 *  \arg HYPORT_TIME_DELTA_IN_SECONDS return timer value in seconds.
 *  \arg HYPORT_TIME_DELTA_IN_MILLISECONDS return timer value in milliseconds.
 *  \arg HYPORT_TIME_DELTA_IN_MICROSECONDS return timer value in micoseconds.
 *  \arg HYPORT_TIME_DELTA_IN_NANOSECONDS return timer value in nanoseconds.
 */
U_64 VMCALL
hytime_hires_delta (struct HyPortLibrary * portLibrary, U_64 startTime,
		    U_64 endTime, UDATA requiredResolution)
{
  U_64 ticks;
  U_64 frequency = PPG_time_hiresClockFrequency;

  /* modular arithmetic saves us, answer is always ... */
  ticks = endTime - startTime;

  if (frequency == requiredResolution)
    {
      return ticks;
    }

  if (frequency < requiredResolution)
    {
      return (ticks * requiredResolution) / frequency;
    }

  return ticks / (frequency / requiredResolution);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hytime_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hytime_shutdown (struct HyPortLibrary *portLibrary)
{
  shutdown_timer ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hytime_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the time operations may be created here.  All resources created here should be destroyed
 * in @ref hytime_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_TIME
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hytime_startup (struct HyPortLibrary *portLibrary)
{
  LARGE_INTEGER freq;

  if (QueryPerformanceFrequency (&freq))
    {
      PPG_time_hiresClockFrequency = freq.QuadPart;
    }
  else
    {
      PPG_time_hiresClockFrequency = 1000;	/* GetTickCount() returns millis */
    }

  return init_timer ();
}

#undef CDEV_CURRENT_FUNCTION
