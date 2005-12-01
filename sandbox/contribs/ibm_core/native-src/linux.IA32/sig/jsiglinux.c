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

#if defined(LINUX)
#define _GNU_SOURCE 1
#endif

#include <stdio.h>
#include <signal.h>
#include <string.h>
#include <dlfcn.h>

#include "jsigunix.h"

/**************************/
/* PRIVATE JSIG FUNCTIONS */
/**************************/

/*********************************************************************/
/* Function Name: real_sigaction()                                   */
/*                                                                   */
/* Description:   Calls the real sigaction.                          */
/*                                                                   */
/* Parameters:    sig - Signal number                                */
/*                act - sigaction for signal handler to be installed */
/*                oact - sigaction for previously installed handler  */
/*                                                                   */
/* Returns:       return code from sigaction function                */
/*********************************************************************/
int
real_sigaction (int sig, const struct sigaction *act, struct sigaction *oact)
{
  extern int jsig_sigaction_isdefault (void);
  static sigactionfunc_t real_sigaction_fn = 0;
  if (real_sigaction_fn == 0)
    {
      if (jsig_sigaction_isdefault ())
        {
          real_sigaction_fn =
            (sigactionfunc_t) dlsym (RTLD_NEXT, "sigaction");
          if (real_sigaction_fn == 0)
            {
              real_sigaction_fn =
                (sigactionfunc_t) dlsym (RTLD_DEFAULT, "sigaction");
            }
        }
      else
        {
          real_sigaction_fn =
            (sigactionfunc_t) dlsym (RTLD_DEFAULT, "sigaction");
          if (real_sigaction_fn == 0)
            {
              real_sigaction_fn =
                (sigactionfunc_t) dlsym (RTLD_NEXT, "sigaction");
            }
        }
      if (real_sigaction_fn == 0)
        {
          fprintf (stderr, "libjsig unable to find sigaction - %s\n",
                   dlerror ());
          abort ();
        }
    }
  return (*real_sigaction_fn) (sig, act, oact);
}

/*********************************************************************/
/* Function Name: real_sigprocmask()                                 */
/*                                                                   */
/* Description:   Calls the real sigprocmask. This just calls        */
/*                sigprocmask for all platforms except on AIX        */
/*                where sigthreadmask is called                      */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:       return code from sigprocmask function              */
/*********************************************************************/
int
real_sigprocmask (int option, const sigset_t * new_set, sigset_t * old_set)
{
  return sigprocmask (option, new_set, old_set);
}

/*************************/
/* PUBLIC JSIG FUNCTIONS */
/*************************/

/*********************************************************************/
/* Function Name: signal()                                           */
/*                                                                   */
/* Description:   Interposed signal system function.                 */
/*                Note this is implemented with sigaction            */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:                                                          */
/*********************************************************************/
sig_handler_t
signal (int sig, sig_handler_t disp)
{
  return bsd_signal (sig, disp);
}

/*********************************************************************/
/* Function Name: ssignal()                                          */
/*                                                                   */
/* Description:   Interposed signal system function.                 */
/*                Note this is implemented with sigaction            */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:                                                          */
/*********************************************************************/
sig_handler_t
ssignal (int sig, sig_handler_t disp)
{
  return bsd_signal (sig, disp);
}

/*********************************************************************/
/* Function Name: __sysv_signal()                                    */
/*                                                                   */
/* Description:   Interposed signal system function.                 */
/*                Note this is implemented with sigaction            */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:                                                          */
/*********************************************************************/
sig_handler_t
__sysv_signal (int sig, sig_handler_t disp)
{
  return sysv_signal (sig, disp);
}

/*********************************************************************/
/* Function Name: sigvec()                                           */
/*                                                                   */
/* Description:   Interposed signal system function.                 */
/*                Note this is implemented with sigaction            */
/*                                                                   */
/* Parameters:                                                       */
/*                                                                   */
/* Returns:                                                          */
/*********************************************************************/
int
sigvec (int sig, const struct sigvec *invec, struct sigvec *outvec)
{
  int i;
  int status;
  struct sigaction act;
  struct sigaction oact;

  memset (&act, 0, sizeof (struct sigaction));
  if (invec)
    {
      sigemptyset (&act.sa_mask);
      for (i = 0; i < sizeof (invec->sv_mask) * 8; i++)
        {
          if (invec->sv_mask & (1 << i))
            {
              sigaddset (&act.sa_mask, i + 1);
            }
        }
      act.sa_handler = invec->sv_handler;
      if (invec->sv_flags & SV_ONSTACK)
        act.sa_flags |= SA_ONSTACK;
      if (!(invec->sv_flags & SV_INTERRUPT))
        act.sa_flags |= SA_RESTART;
      if (invec->sv_flags & SV_RESETHAND)
        act.sa_flags |= SA_RESETHAND;
    }

  memset (&oact, 0, sizeof (struct sigaction));
  if (invec)
    {
      status = sigaction (sig, &act, &oact);
    }
  else
    {
      status = sigaction (sig, 0, &oact);
    }

  if (status == 0 && outvec)
    {
      sigemptyset (&act.sa_mask);
      for (i = 0; i < sizeof (int) * 8; i++)
        {
          if (sigismember (&oact.sa_mask, i + 1))
            {
              outvec->sv_mask |= (1 << i);
            }
        }
      outvec->sv_handler = oact.sa_handler;
      outvec->sv_flags = 0;
      if (oact.sa_flags & SA_ONSTACK)
        outvec->sv_flags |= SV_ONSTACK;
      if (!(oact.sa_flags & SA_RESTART))
        outvec->sv_flags |= SV_INTERRUPT;
      if (oact.sa_flags & SA_RESETHAND)
        outvec->sv_flags |= SV_RESETHAND;
    }

  return status;
}

/***************/
/* End of File */
/***************/
