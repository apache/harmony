/* Copyright 2003, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*****************/
/* Header files. */
/*****************/

#if defined(WIN64)
#if !defined(_WIN32_WINNT)
#define _WIN32_WINNT 0x0500
#endif /* _WIN32_WINNT */
#endif /* WIN64 */

#include <stdio.h>
#include <stdlib.h>
#include <windows.h>

#undef _CRTIMP
#define _CRTIMP __declspec(dllexport)	/* this is required so that we can redefine signal */
#define JSIGDLLSPEC __declspec(dllexport)

/* Tooling workaround - use JNIEXPORT, as parser ignores this on C prototypes */
#define JNIEXPORT JSIGDLLSPEC

#include "jsig.h"

/*********************/
/* Local Definitions */
/*********************/

typedef void (*sigfpe_handler_t) (int, int);
typedef sig_handler_t (*signalfunc_t) (int, sig_handler_t);

/* saved non-primary signal handlers */
static sig_handler_t *saved_signal_handler = 0;

/* Array of bits indicating whether or not the a primary signal handler is installed */
static long primary_sigs;
#define MAX_SIGNAL (sizeof(primary_sigs)*8 - 1)

#define SET_PRIMARY_HANDLER(sig) primary_sigs = primary_sigs | (1 << sig);
#define CLEAR_PRIMARY_HANDLER(sig) primary_sigs  = primary_sigs & (~(1 << sig));
#define PRIMARY_HANDLER_INSTALLED(sig) (primary_sigs & (1 << sig))

/* Macros for locking */
static CRITICAL_SECTION jsig_lock;
static CRITICAL_SECTION *jsig_lock_p = 0;

#if defined(WIN64)
#define SIGNAL_LOCK_INIT { InitializeCriticalSectionAndSpinCount(&jsig_lock,4000); jsig_lock_p = &jsig_lock; }
#else
#define SIGNAL_LOCK_INIT { InitializeCriticalSection(&jsig_lock); jsig_lock_p = &jsig_lock; }
#endif

#define SIGNAL_LOCK { if (jsig_lock_p == 0) SIGNAL_LOCK_INIT \
                      EnterCriticalSection(jsig_lock_p);     \
                    }

#define SIGNAL_UNLOCK LeaveCriticalSection(jsig_lock_p);

/**************************/
/* PRIVATE JSIG FUNCTIONS */
/**************************/

/*********************************************************************/
/* Function Name: jsig_init()                                        */
/*                                                                   */
/* Description:   Initialises the jsig signal handler list. It       */
/*                assumes that it is called within SIGNAL_LOCK       */
/*                                                                   */
/* Parameters:    None                                               */
/*                                                                   */
/* Returns:       None                                               */
/*********************************************************************/
static void
jsig_init ()
{
  if (saved_signal_handler == 0)
    {
      saved_signal_handler =
	(sig_handler_t *) malloc ((MAX_SIGNAL + 1) *
				  (size_t) sizeof (sig_handler_t *));
      if (saved_signal_handler == 0)
	{
	  fprintf (stderr, "libjsig unable to allocate memory\n");
	  abort ();
	}
      memset (saved_signal_handler, 0,
	      (MAX_SIGNAL + 1) * (size_t) sizeof (sig_handler_t *));
    }

  primary_sigs = 0;
}

#if !defined(IBM_JSIG_CRUNTIME_DEFAULT)
#if defined(IBM_BUILD_TYPE_dev)
#define IBM_JSIG_CRUNTIME_DEFAULT "MSVCRTD.DLL"
#else
#define IBM_JSIG_CRUNTIME_DEFAULT "MSVCRT.DLL"
#endif /*IBM_BUILD_TYPE_dev */
#endif

/*********************************************************************/
/* Function Name: real_signal()                                      */
/*                                                                   */
/* Description:   Calls the real signal                              */
/*                                                                   */
/* Parameters:    sig - Signal number                                */
/*                act - sigaction for signal handler to be installed */
/*                                                                   */
/* Returns:       return code from OS sigaction function             */
/*********************************************************************/
static sig_handler_t
real_signal (int sig, sig_handler_t act)
{
  static signalfunc_t real_signal = 0;
  if (real_signal == 0)
    {
      void *libhandle;
      char *libname;

      /* If environment variable IBM_JSIG_CRUNTIME is defined then use */
      /* that for the system library name, otherwise use default       */
      libname = getenv ("IBM_JSIG_CRUNTIME");
      if (libname == 0)
	{
	  libname = IBM_JSIG_CRUNTIME_DEFAULT;
	}

      /* Open the system library, if this fails then abort */
      libhandle = LoadLibrary (libname);
      if (libhandle == NULL)
	{
	  long errcode = GetLastError ();
	  fprintf (stderr, "libjsig - library %s not found - error %ld\n",
		   libname, errcode);
	  abort ();
	}

      /* Get the system sigaction function from the system library */
      /* If this fails then abort                                  */
      real_signal = (signalfunc_t) GetProcAddress (libhandle, "signal");
      if (real_signal == 0)
	{
	  long errcode = GetLastError ();
	  fprintf (stderr, "libjsig - unable to find sigaction - error %ld\n",
		   errcode);
	  abort ();
	}
    }

  /* Call the system sigaction function */
  return (*real_signal) (sig, act);
}

/*************************/
/* PUBLIC JSIG FUNCTIONS */
/*************************/

/*********************************************************************/
/* Function Name: jsig_handler()                                     */
/*                                                                   */
/* Description:   jsig's signal handler, responsible for chaining    */
/*                the installed signal handlers for the passed       */
/*                signal.                                            */
/*                Note that this can be called by the JVM signal     */
/*                is explicitly chain to the other application       */
/*                signal handlers                                    */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:       Value indicating whether default OS action is      */
/*                      required by application or whether it has    */
/*                      handled (or ignored) the signal              */
/*                      However, this is DISGARDED on Windows        */
/*********************************************************************/
JNIEXPORT int
jsig_handler (int sig, void *siginfo, void *uc)
{
  /* Note: rc set to zero, but not used by Windows, return type of int is for consistency with Unix */
  int rc = 0;
  sig_handler_t act;

  /* jsig not initialized, so do nothing */
  if (0 == saved_signal_handler)
    {
      return rc;
    }

  act = saved_signal_handler[sig];

  /* Reset the signal handler  */
  memset (&saved_signal_handler[sig], 0, sizeof (sig_handler_t));

  /* Call the required signal handler */
  if (act != 0 && act != SIG_IGN && act != SIG_DFL)
    {
      if (sig == SIGFPE)
	{
	  sigfpe_handler_t fpe_act = (sigfpe_handler_t) act;
	  union
	  {
	    void *p;
	    int i;
	  } info;
	  info.p = siginfo;
	  fpe_act (sig, info.i);
	}
      else
	{
	  act (sig);
	}
    }
  return rc;
}

/*********************************************************************/
/* Function Name: jsig_primary_signal()                              */
/*                                                                   */
/* Description:   Installs the primary signal handler for the        */
/*                specified signal.                                  */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:       0 if successful, -1 if an error occurred           */
/*********************************************************************/
JNIEXPORT sig_handler_t
jsig_primary_signal (int sig, sig_handler_t act)
{
  sig_handler_t oldact;
  BOOL primaryPreviouslyInstalled;

  if (sig < 0 || sig > MAX_SIGNAL)
    {
      return SIG_ERR;
    }

  SIGNAL_LOCK
    /* Initialise jsig if this is the first call */
    if (saved_signal_handler == 0)
    jsig_init ();

  primaryPreviouslyInstalled = PRIMARY_HANDLER_INSTALLED (sig);

  /* If the request is to de-install the primary handler then install  */
  /* any saved handler, otherwise install the specified signal handler */
  /* and save the previous handler                                     */
  if (act == SIG_DFL || act == SIG_IGN)
    {
      oldact = real_signal (sig, saved_signal_handler[sig]);
      if (oldact != SIG_ERR)
	{
	  memset (&saved_signal_handler[sig], 0, sizeof (sig_handler_t));
	  CLEAR_PRIMARY_HANDLER (sig);
	}
    }
  else
    {
      oldact = real_signal (sig, act);
      if (oldact != SIG_ERR && !primaryPreviouslyInstalled)
	{
	  saved_signal_handler[sig] = oldact;
	  SET_PRIMARY_HANDLER (sig);
	}
    }

  /* If a primary handler was not previously installed then clear */
  /* the return handler                                           */
  if (!(primaryPreviouslyInstalled || oldact == SIG_DFL || oldact == SIG_IGN))
    {
      oldact = 0;
    }

  SIGNAL_UNLOCK return oldact;
}

/*********************************************************************/
/* Function Name: signal()                                           */
/*                                                                   */
/* Description:   Interposed signal system function.                 */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:                                                          */
/*********************************************************************/
JNIEXPORT sig_handler_t
signal (int sig, sig_handler_t act)
{
  sig_handler_t oldact;

  if (sig < 0 || sig > MAX_SIGNAL)
    {
      return SIG_ERR;
    }

  SIGNAL_LOCK
    /* Initialise jsig if this is the first call */
    if (saved_signal_handler == 0)
    jsig_init ();

  /* If primary signal handler installed then just save the passed */
  /* signal info and return the previously saved signal info       */
  /* Otherwise just call the real signal function                  */
  if (PRIMARY_HANDLER_INSTALLED (sig))
    {
      oldact = saved_signal_handler[sig];
      saved_signal_handler[sig] = act;
    }
  else
    {
      oldact = real_signal (sig, act);
    }

  SIGNAL_UNLOCK return oldact;
}
