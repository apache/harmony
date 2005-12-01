/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
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

#include "hyport.h"
#include "hythread.h"
#include <signal.h>
#include <sys/ucontext.h>
#include <unistd.h>
#include <setjmp.h>
#include <semaphore.h>

#include <jsig.h>

#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

#define MAX_PORTLIB_SIGNAL_TYPES  8
#define MAX_UNIX_SIGNAL_TYPES  _NSIG

typedef void (*linux386_sigaction) (int, siginfo_t *, void *);

/* Store the previous signal handlers, we need to restore them when we're done */
static struct
{
  struct sigaction action;
  U_32 restore;
} oldActions[MAX_UNIX_SIGNAL_TYPES];

/* record the signals for which we have registered a master handler
 * access to this must be protected with by masterHandlerMonitor */
static U_32 signalsWithMasterHandlers;
static U_32 shutDownASynchReporter;

static U_32 attachedPortLibraries;

typedef struct HyLinux386AsyncHandlerRecord
{
  HyPortLibrary *portLib;
  hysig_handler_fn handler;
  void *handler_arg;
  U_32 flags;
  struct HyLinux386AsyncHandlerRecord *next;
} HyLinux386AsyncHandlerRecord;

/* holds the options set by hysig_set_options */
static U_32 signalOptions;

static HyLinux386AsyncHandlerRecord *asyncHandlerList;

static sem_t wakeUpASynchReporter;
static sem_t sigQuitPendingSem;
static sem_t sigAbrtPendingSem;
static sem_t sigTermPendingSem;
static sem_t sigReconfigPendingSem;

static hythread_monitor_t asyncMonitor;
static hythread_monitor_t masterHandlerMonitor;
static hythread_monitor_t asyncReporterShutdownMonitor;
static U_32 asyncThreadCount;
static U_32 attachedPortLibraries;

typedef struct HyLinux386SignalInfo
{
  U_32 portLibrarySignalType;
  void *handlerAddress;
  void *handlerAddress2;
  struct sigcontext *sigContext;
  siginfo_t *sigInfo;
  Dl_info dl_info;
} HyLinux386SignalInfo;

struct HySignalHandlerRecord
{
  struct HySignalHandlerRecord *previous;
  struct HyPortLibrary *portLibrary;
  hysig_handler_fn handler;
  void *handler_arg;
  sigjmp_buf returnBuf;
  U_32 flags;
};

typedef struct HyCurrentSignal
{
  int signal;
  siginfo_t *sigInfo;
  void *contextInfo;
} HyCurrentSignal;

/* key to get the end of the synchronous handler records */
static hythread_tls_key_t tlsKey;

/* key to get the current synchronous signal */
static hythread_tls_key_t tlsKeyCurrentSignal;

struct
{
  U_32 portLibSignalNo;
  U_32 unixSignalNo;
} signalMap[] =
{
  {
  HYPORT_SIG_FLAG_SIGSEGV, SIGSEGV},
  {
  HYPORT_SIG_FLAG_SIGBUS, SIGBUS},
  {
  HYPORT_SIG_FLAG_SIGILL, SIGILL},
  {
  HYPORT_SIG_FLAG_SIGFPE, SIGFPE},
  {
  HYPORT_SIG_FLAG_SIGTRAP, SIGTRAP},
  {
  HYPORT_SIG_FLAG_SIGQUIT, SIGQUIT},
  {
  HYPORT_SIG_FLAG_SIGABRT, SIGABRT},
  {
  HYPORT_SIG_FLAG_SIGTERM, SIGTERM}
};

/* the number of xmm registers */
#define NXMMREGS 8

#define CDEV_CURRENT_FUNCTION _prototypes_private

static U_32 destroySignalTools (HyPortLibrary * portLibrary);

static U_32 infoForFPR (struct HyPortLibrary *portLibrary,
                        struct HyLinux386SignalInfo *info, I_32 index,
                        const char **name, void **value);

static void masterSynchSignalHandler (int signal, siginfo_t * sigInfo,
                                      void *contextInfo);

static U_32 infoForGPR (struct HyPortLibrary *portLibrary,
                        struct HyLinux386SignalInfo *info, I_32 index,
                        const char **name, void **value);

static void removeAsyncHandlers (HyPortLibrary * portLibrary);

static U_32 mapPortLibSignalToUnix (U_32 portLibSignal);

static U_32 mapUnixSignalToPortLib (U_32 signalNo, siginfo_t * sigInfo);

static U_32 infoForSignal (struct HyPortLibrary *portLibrary,
                           struct HyLinux386SignalInfo *info, I_32 index,
                           const char **name, void **value);

static void fillInLinux386SignalInfo (struct HyPortLibrary *portLibrary,
                                      U_32 portLibSignalType,
                                      hysig_handler_fn handler,
                                      siginfo_t * signalInfo,
                                      void *contextInfo,
                                      struct HyLinux386SignalInfo *hyinfo);

static U_32 infoForModule (struct HyPortLibrary *portLibrary,
                           struct HyLinux386SignalInfo *info, I_32 index,
                           const char **name, void **value);

static I_32 registerMasterHandlers (HyPortLibrary * portLibrary, U_32 flags,
                                    U_32 allowedSubsetOfFlags);

static U_32 countInfoInCategory (struct HyPortLibrary *portLibrary,
                                 void *info, U_32 category);

static void VMCALL sig_full_shutdown (struct HyPortLibrary *portLibrary);

static U_32 initializeSignalTools (HyPortLibrary * portLibrary);

static void masterASynchSignalHandler (int signal, siginfo_t * sigInfo,
                                       void *contextInfo);

static int HYTHREAD_PROC asynchSignalReporter (void *userData);

static U_32 infoForControl (struct HyPortLibrary *portLibrary,
                            struct HyLinux386SignalInfo *info, I_32 index,
                            const char **name, void **value);

static U_32 registerSignalHandlerWithOS (HyPortLibrary * portLibrary,
                                         U_32 portLibrarySignalNo,
                                         linux386_sigaction handler);

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_can_protect
I_32 VMCALL
hysig_can_protect (struct HyPortLibrary *portLibrary, U_32 flags)
{
  U_32 supportedFlags =
    HYPORT_SIG_FLAG_MAY_RETURN | HYPORT_SIG_FLAG_MAY_CONTINUE_EXECUTION;

  if (0 == (signalOptions & HYPORT_SIG_OPTIONS_REDUCED_SIGNALS))
    {
      supportedFlags |=
        HYPORT_SIG_FLAG_SIGALLSYNC | HYPORT_SIG_FLAG_SIGQUIT |
        HYPORT_SIG_FLAG_SIGABRT | HYPORT_SIG_FLAG_SIGTERM;
    }

  if ((flags & supportedFlags) == flags)
    {
      return 1;
    }
  else
    {
      return 0;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_info
U_32 VMCALL
hysig_info (struct HyPortLibrary * portLibrary, void *info, U_32 category,
            I_32 index, const char **name, void **value)
{
  *name = "";

  switch (category)
    {
    case HYPORT_SIG_SIGNAL:
      return infoForSignal (portLibrary, info, index, name, value);
    case HYPORT_SIG_GPR:
      return infoForGPR (portLibrary, info, index, name, value);
    case HYPORT_SIG_CONTROL:
      return infoForControl (portLibrary, info, index, name, value);
    case HYPORT_SIG_MODULE:
      return infoForModule (portLibrary, info, index, name, value);
    case HYPORT_SIG_FPR:
      return infoForFPR (portLibrary, info, index, name, value);
    case HYPORT_SIG_OTHER:

    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_info_count
U_32 VMCALL
hysig_info_count (struct HyPortLibrary * portLibrary, void *info,
                  U_32 category)
{
  return countInfoInCategory (portLibrary, info, category);

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_protect
/**
 * We register the master signal handlers here to deal with -Xrs
 */
I_32 VMCALL
hysig_protect (struct HyPortLibrary * portLibrary, hysig_protected_fn fn,
               void *fn_arg, hysig_handler_fn handler, void *handler_arg,
               U_32 flags, UDATA * result)
{

  struct HySignalHandlerRecord thisRecord;
  hythread_t thisThread;
  U_32 rc;
  U_32 allowHandlers;

  hythread_monitor_enter (masterHandlerMonitor);
  if (HYPORT_SIG_OPTIONS_REDUCED_SIGNALS & signalOptions)
    {
      /* -Xrs was set, we can't protect against any signals, do not install the master handler */
      allowHandlers = 0;
    }
  else
    {
      allowHandlers = 1;
      rc =
        registerMasterHandlers (portLibrary, flags,
                                HYPORT_SIG_FLAG_SIGALLSYNC);
    }
  hythread_monitor_exit (masterHandlerMonitor);

  if (allowHandlers)
    {

      if (rc)
        {
          return HYPORT_SIG_ERROR;
        }

      thisThread = hythread_self ();

      thisRecord.previous = hythread_tls_get (thisThread, tlsKey);
      thisRecord.portLibrary = portLibrary;
      thisRecord.handler = handler;
      thisRecord.handler_arg = handler_arg;
      thisRecord.flags = flags;

      if (flags & HYPORT_SIG_FLAG_MAY_RETURN)
        {

          /* setjmp/longjmp does not clear the mask setup by the OS when it delivers the signal. Use sigsetjmp/siglongjmp(buf, 1) instead */
          if (sigsetjmp (thisRecord.returnBuf, 1))
            {

              /* the handler had long jumped back here -- reset the signal handler stack and return */
              hythread_tls_set (thisThread, tlsKey, thisRecord.previous);
              *result = 0;
              /* Trc_PRT_signal_hysignal_sigProtect_Exit_exception(); */
              return HYPORT_SIG_EXCEPTION_OCCURRED;
            }
        }

      if (hythread_tls_set (thisThread, tlsKey, &thisRecord))
        {
          /* Trc_PRT_signal_hysignal_sigProtect_Exit2_error(); */
          return HYPORT_SIG_ERROR;
        }

    }                           /* end if allowHandlers */

  *result = fn (portLibrary, fn_arg);

  /* if the first hythread_tls_set succeeded, then this one will always succeed */
  hythread_tls_set (thisThread, tlsKey, thisRecord.previous);

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_set_async_signal_handler
U_32 VMCALL
hysig_set_async_signal_handler (struct HyPortLibrary * portLibrary,
                                hysig_handler_fn handler, void *handler_arg,
                                U_32 flags)
{

  U_32 rc = 0;
  U_32 signalsRegisteredWithOS = 0;
  HyLinux386AsyncHandlerRecord *cursor;
  HyLinux386AsyncHandlerRecord **previousLink;

  hythread_monitor_enter (masterHandlerMonitor);
  if (HYPORT_SIG_OPTIONS_REDUCED_SIGNALS & signalOptions)
    {
      /* -Xrs was set, we can't protect against any signals, do not install any handlers */
      rc = -1;
    }
  else
    {
      rc =
        registerMasterHandlers (portLibrary, flags,
                                HYPORT_SIG_FLAG_SIGALLASYNC);
    }
  hythread_monitor_exit (masterHandlerMonitor);

  if (rc)
    {
      return HYPORT_SIG_ERROR;
    }

  hythread_monitor_enter (asyncMonitor);

  /* wait until no signals are being reported */
  while (asyncThreadCount > 0)
    {
      hythread_monitor_wait (asyncMonitor);
    }

  /* is this handler already registered? */
  previousLink = &asyncHandlerList;
  cursor = asyncHandlerList;

  while (cursor)
    {
      if ((cursor->portLib == portLibrary) && (cursor->handler == handler)
          && (cursor->handler_arg == handler_arg))
        {
          if (flags == 0)
            {
              /* remove this handler record */
              *previousLink = cursor->next;
              portLibrary->mem_free_memory (portLibrary, cursor);

            }
          else
            {
              /* update the listener with the new flags */
              cursor->flags = flags;
            }
          break;
        }
      previousLink = &cursor->next;
      cursor = cursor->next;
    }

  if (cursor == NULL)
    {
      /* cursor will only be NULL if we failed to find it in the list */
      if (flags != 0)
        {
          HyLinux386AsyncHandlerRecord *record =
            portLibrary->mem_allocate_memory (portLibrary, sizeof (*record));

          if (record == NULL)
            {
              rc = 1;
            }
          else
            {
              record->portLib = portLibrary;
              record->handler = handler;
              record->handler_arg = handler_arg;
              record->flags = flags;
              record->next = NULL;

              /* add the new record to the end of the list */
              *previousLink = record;
            }
        }
    }

  hythread_monitor_exit (asyncMonitor);

  return rc;

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_shutdown
/*
 * The full shutdown routine "sig_full_shutdown" overwrites this once we've completed startup 
 */
void VMCALL
hysig_shutdown (struct HyPortLibrary *portLibrary)
{
  return;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_startup
/**
 * Start up the signal handling component of the port library
 *
 * Note: none of the master handlers are registered with the OS until the first call to either of hysig_protect or hysig_set_async_signal_handler
 * Note: the async reporting thread is not started up until the first call to hysig_set_asyc_signal_handler 
*/

I_32 VMCALL
hysig_startup (struct HyPortLibrary * portLibrary)
{

  I_32 result = 0;

  hythread_monitor_t globalMonitor = hythread_global_monitor ();
  U_32 index;

  hythread_monitor_enter (globalMonitor);
  if (attachedPortLibraries++ == 0)
    {

      /* initialize the old actions */
      for (index = 0; index < MAX_UNIX_SIGNAL_TYPES; index++)
        {
          oldActions[index].restore = 0;
        }

      result = initializeSignalTools (portLibrary);

    }
  hythread_monitor_exit (globalMonitor);

  if (result == 0)
    {
      /* we have successfully started up the signal portion, install the full shutdown routine */
      portLibrary->sig_shutdown = sig_full_shutdown;
    }

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION countInfoInCategory

static U_32
countInfoInCategory (struct HyPortLibrary *portLibrary, void *info,
                     U_32 category)
{
  void *value;
  const char *name;
  U_32 count = 0;

  while (portLibrary->
         sig_info (portLibrary, info, category, count, &name,
                   &value) != HYPORT_SIG_VALUE_UNDEFINED)
    {
      count++;
    }

  return count;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForSignal

static U_32
infoForSignal (struct HyPortLibrary *portLibrary,
               struct HyLinux386SignalInfo *info, I_32 index,
               const char **name, void **value)
{
  *name = "";

  switch (index)
    {

    case HYPORT_SIG_SIGNAL_TYPE:
    case 0:
      *name = "HyGeneric_Signal_Number";
      *value = &info->portLibrarySignalType;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_PLATFORM_SIGNAL_TYPE:
    case 1:
      *name = "Signal_Number";
      *value = &info->sigInfo->si_signo;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_ERROR_VALUE:
    case 2:
      *name = "Error_Value";
      *value = &info->sigInfo->si_errno;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_CODE:
    case 3:
      *name = "Signal_Code";
      *value = &info->sigInfo->si_code;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_HANDLER:
    case 4:
      *name = "Handler1";
      *value = &info->handlerAddress;
      return HYPORT_SIG_VALUE_ADDRESS;

    case 5:
      *name = "Handler2";
      *value = &info->handlerAddress2;
      return HYPORT_SIG_VALUE_ADDRESS;

    case HYPORT_SIG_SIGNAL_INACCESSIBLE_ADDRESS:
    case 6:
      /* si_code > 0 indicates that the signal was generated by the kernel */
      if (info->sigInfo->si_code > 0)
        {
          if ((info->sigInfo->si_signo == SIGBUS)
              || (info->sigInfo->si_signo == SIGSEGV))
            {
              *name = "InaccessibleAddress";
              *value = &info->sigInfo->si_addr;
              return HYPORT_SIG_VALUE_ADDRESS;
            }
        }
      return HYPORT_SIG_VALUE_UNDEFINED;

    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForFPR

static U_32
infoForFPR (struct HyPortLibrary *portLibrary,
            struct HyLinux386SignalInfo *info, I_32 index, const char **name,
            void **value)
{
  static const char *n_xmm[] = {
    "xmm0",
    "xmm1",
    "xmm2",
    "xmm3",
    "xmm4",
    "xmm5",
    "xmm6",
    "xmm7"
  };

  switch (index)
    {
    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForGPR

static U_32
infoForGPR (struct HyPortLibrary *portLibrary,
            struct HyLinux386SignalInfo *info, I_32 index, const char **name,
            void **value)
{
  *name = "";

  switch (index)
    {
    case HYPORT_SIG_GPR_X86_EDI:
    case 0:
      *name = "EDI";
      *value = &info->sigContext->edi;
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_GPR_X86_ESI:
    case 1:
      *name = "ESI";
      *value = &info->sigContext->esi;
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_GPR_X86_EAX:
    case 2:
      *name = "EAX";
      *value = &info->sigContext->eax;
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_GPR_X86_EBX:
    case 3:
      *name = "EBX";
      *value = &info->sigContext->ebx;
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_GPR_X86_ECX:
    case 4:
      *name = "ECX";
      *value = &info->sigContext->ecx;
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_GPR_X86_EDX:
    case 5:
      *name = "EDX";
      *value = &info->sigContext->edx;
      return HYPORT_SIG_VALUE_ADDRESS;
    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForControl

static U_32
infoForControl (struct HyPortLibrary *portLibrary,
                struct HyLinux386SignalInfo *info, I_32 index,
                const char **name, void **value)
{
  *name = "";
  U_8 *eip;

  switch (index)
    {
    case HYPORT_SIG_CONTROL_PC:
    case 0:
      *name = "EIP";
      *value = (void *) &(info->sigContext->eip);
      return HYPORT_SIG_VALUE_ADDRESS;
    case 1:
      *name = "ES";
      *value = (void *) &(info->sigContext->es);
      return HYPORT_SIG_VALUE_ADDRESS;
    case 2:
      *name = "DS";
      *value = (void *) &(info->sigContext->ds);
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_CONTROL_SP:
    case 3:
      *name = "ESP";
      *value = (void *) &(info->sigContext->esp);
      return HYPORT_SIG_VALUE_ADDRESS;
    case 4:
      *name = "EFlags";
      *value = (void *) &(info->sigContext->eflags);
      return HYPORT_SIG_VALUE_ADDRESS;
    case 5:
      *name = "CS";
      *value = (void *) &(info->sigContext->cs);
      return HYPORT_SIG_VALUE_ADDRESS;
    case 6:
      *name = "SS";
      *value = (void *) &(info->sigContext->ss);
      return HYPORT_SIG_VALUE_ADDRESS;
    case HYPORT_SIG_CONTROL_BP:
    case 7:
      *name = "EBP";
      *value = &info->sigContext->ebp;
      return HYPORT_SIG_VALUE_ADDRESS;
    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForModule

static U_32
infoForModule (struct HyPortLibrary *portLibrary,
               struct HyLinux386SignalInfo *info, I_32 index,
               const char **name, void **value)
{
  void *address;
  Dl_info *dl_info = &(info->dl_info);
  *name = "";

  address = (void *) info->sigContext->eip;
  int dl_result = dladdr ((void *) info->sigContext->eip, dl_info);

  switch (index)
    {
    case HYPORT_SIG_MODULE_NAME:
    case 0:
      *name = "Module";
      if (dl_result)
        {
          *value = (void *) (dl_info->dli_fname);
          return HYPORT_SIG_VALUE_STRING;
        }
      return HYPORT_SIG_VALUE_UNDEFINED;
    case 1:
      *name = "Module_base_address";
      if (dl_result)
        {
          *value = (void *) &(dl_info->dli_fbase);
          return HYPORT_SIG_VALUE_ADDRESS;
        }
      return HYPORT_SIG_VALUE_UNDEFINED;
    case 2:
      *name = "Symbol";
      if (dl_result)
        {
          if (dl_info->dli_sname != NULL)
            {
              *value = (void *) (dl_info->dli_sname);
              return HYPORT_SIG_VALUE_STRING;
            }
        }
      return HYPORT_SIG_VALUE_UNDEFINED;
    case 3:
      *name = "Symbol_address";
      if (dl_result)
        {
          *value = (void *) &(dl_info->dli_saddr);
          return HYPORT_SIG_VALUE_ADDRESS;
        }
      return HYPORT_SIG_VALUE_UNDEFINED;
    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION masterExceptionHandler

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION fillInLinux386SignalInfo

static void
fillInLinux386SignalInfo (struct HyPortLibrary *portLibrary,
                          U_32 portLibSignalType, hysig_handler_fn handler,
                          siginfo_t * signalInfo, void *contextInfo,
                          struct HyLinux386SignalInfo *hyinfo)
{
  struct sigcontext *sigContext;
  ucontext_t *uContext;

  uContext = (ucontext_t *) contextInfo;
  sigContext = (struct sigcontext *) &uContext->uc_mcontext;
  memset (hyinfo, 0, sizeof (*hyinfo));

  hyinfo->portLibrarySignalType = portLibSignalType;
  hyinfo->handlerAddress = (void *) handler;
  hyinfo->handlerAddress2 = (void *) masterSynchSignalHandler;
  hyinfo->sigContext = sigContext;
  hyinfo->sigInfo = signalInfo;
  /* module info is filled on demand */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION asynchSignalReporter

/**
 * Reports the asynchronous signal to all listeners.
 */
static int HYTHREAD_PROC
asynchSignalReporter (void *userData)
{
  U_32 type;
  HyLinux386AsyncHandlerRecord *cursor;
  U_32 result = FALSE;
  U_32 asyncSignalFlag = 0;

  /* Trc_PRT_signal_hysignal_asynchSignalReporter_Entry(signal); */

  /* Need an exit condition... */
  for (;;)
    {
      sem_wait (&wakeUpASynchReporter);

      /* we get woken up if there is a signal pending or it is time to shutdown */
      if (shutDownASynchReporter)
        {
          break;
        }

      /* determine which signal we've being woken up for */
      if (!sem_trywait (&sigQuitPendingSem))
        {
          asyncSignalFlag = HYPORT_SIG_FLAG_SIGQUIT;
        }
      else if (!sem_trywait (&sigAbrtPendingSem))
        {
          asyncSignalFlag = HYPORT_SIG_FLAG_SIGABRT;
        }
      else if (!sem_trywait (&sigTermPendingSem))
        {
          asyncSignalFlag = HYPORT_SIG_FLAG_SIGTERM;
        }

      /* report this signal to all of it's handlers */

      /* incrementing the asyncThreadCount will prevent the list from being modified while we use it */
      hythread_monitor_enter (asyncMonitor);
      asyncThreadCount++;
      hythread_monitor_exit (asyncMonitor);

      cursor = asyncHandlerList;
      while (cursor)
        {
          if (cursor->flags & asyncSignalFlag)
            {
              cursor->handler (cursor->portLib, asyncSignalFlag, NULL,
                               cursor->handler_arg);
              result = TRUE;
            }
          cursor = cursor->next;
        }

      hythread_monitor_enter (asyncMonitor);
      if (--asyncThreadCount == 0)
        {
          hythread_monitor_notify_all (asyncMonitor);
        }
      hythread_monitor_exit (asyncMonitor);

      if (!(signalOptions & HYPORT_SIG_OPTIONS_JSIG_NO_CHAIN))
        {
          jsig_handler (mapPortLibSignalToUnix (asyncSignalFlag), NULL, NULL);
        }

      /* reset the signal store */
      asyncSignalFlag = 0;
    }

  hythread_monitor_enter (asyncReporterShutdownMonitor);
  shutDownASynchReporter = 0;
  hythread_monitor_notify (asyncReporterShutdownMonitor);

  hythread_exit (asyncReporterShutdownMonitor);

  /* unreachable */
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION masterSynchSignalHandler

/**
 * This signal handler is specific to synchronous signals.
 * It will call all of the user's handlers that were registered with the vm using hysig_protect, 
 * upon receiving a signal they listen for.
 *
 */
static void
masterSynchSignalHandler (int signal, siginfo_t * sigInfo, void *contextInfo)
{
  U_32 portLibType;
  struct HySignalHandlerRecord *thisRecord;
  struct HyCurrentSignal currentSignal;
  struct HyCurrentSignal *previousSignal;
  hythread_t thisThread = hythread_self ();

  thisRecord = NULL;

  /* Trc_PRT_signal_hysignal_masterHandler_Entry(signal, sigInfo, contextInfo); */
  portLibType = mapUnixSignalToPortLib (signal, sigInfo);

  /* record this signal in tls so that jsig_handler can be called if any of the handlers decide we should be shutting down */
  currentSignal.signal = signal;
  currentSignal.sigInfo = sigInfo;
  currentSignal.contextInfo = contextInfo;

  previousSignal = hythread_tls_get (thisThread, tlsKeyCurrentSignal);

  hythread_tls_set (thisThread, tlsKeyCurrentSignal, &currentSignal);

  /* walk the stack of registered handlers from top to bottom searching for one which handles this type of exception */
  thisRecord = hythread_tls_get (thisThread, tlsKey);

  while (thisRecord)
    {
      if (thisRecord->flags & portLibType)
        {
          struct HyLinux386SignalInfo hyinfo;
          U_32 result;

          /* found a suitable handler */
          /* what signal type do we want to pass on here? port or platform based ? */
          fillInLinux386SignalInfo (thisRecord->portLibrary, portLibType,
                                    thisRecord->handler, sigInfo, contextInfo,
                                    &hyinfo);

          /* remove the handler we are about to invoke, now, in case the handler crashes */
          hythread_tls_set (thisThread, tlsKey, thisRecord->previous);

          result =
            thisRecord->handler (thisRecord->portLibrary, portLibType,
                                 &hyinfo, thisRecord->handler_arg);

          /* The only case in which we don't want the previous handler back on top is if it just returned HYPORT_SIG_EXCEPTION_RETURN
           *              In this case we will remove it from the top after executing the siglongjmp */
          hythread_tls_set (thisThread, tlsKey, thisRecord);

          if (result == HYPORT_SIG_EXCEPTION_CONTINUE_SEARCH)
            {
              /* continue looping */
            }
          else if (result == HYPORT_SIG_EXCEPTION_CONTINUE_EXECUTION)
            {
              hythread_tls_set (thisThread, tlsKeyCurrentSignal,
                                previousSignal);
              return;
            }
          else                  /* if (result == HYPORT_SIG_EXCEPTION_RETURN) */
            {
              hythread_tls_set (thisThread, tlsKeyCurrentSignal,
                                previousSignal);
              siglongjmp (thisRecord->returnBuf, 0);              
            }
        }

      thisRecord = thisRecord->previous;
    }

  if (!(signalOptions & HYPORT_SIG_OPTIONS_JSIG_NO_CHAIN))
    {
      int rc = jsig_handler (signal, (void *) sigInfo, contextInfo);
      if ((JSIG_RC_DEFAULT_ACTION_REQUIRED == rc)
          && (SI_USER != sigInfo->si_code))
        {
          abort ();
        }
    }

  /* if we got this far there weren't any handlers on the stack that knew what to with this signal
   * default action is to abort */
  abort ();

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION masterASynchSignalHandler

/** 
 * Determines the signal received and notifies the asynch signal reporter
 *
 * One semaphore is used to notify the asynchronous signal reporter that it is time to act.
 * Each expected aynch signal type has an associated semaphore which is used to count the number of "pending" signals.
 *
 */
static void
masterASynchSignalHandler (int signal, siginfo_t * sigInfo, void *contextInfo)
{
  U_32 portLibSignalType;
  U_32 index = -1;

  portLibSignalType = mapUnixSignalToPortLib (signal, sigInfo);

  switch (portLibSignalType)
    {
    case HYPORT_SIG_FLAG_SIGQUIT:
      sem_post (&sigQuitPendingSem);
      break;
    case HYPORT_SIG_FLAG_SIGABRT:
      sem_post (&sigAbrtPendingSem);
      break;
    case HYPORT_SIG_FLAG_SIGTERM:
      sem_post (&sigTermPendingSem);
      break;
    case HYPORT_SIG_FLAG_SIGRECONFIG:
      sem_post (&sigReconfigPendingSem);
      break;
    }

  /* Trc_PRT_signal_hysignal_AsynchSignalHandler_Entry(signal, sigInfo, contextInfo); */
  sem_post (&wakeUpASynchReporter);

  return;

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION registerSignalHandlerWithOS

/**
 * Register the signal handler with the OS, generally used to register the master signal handlers 
 * Not to be confused with hysig_protect, which registers the user's handler with the port library.
 * 
 * Calls to this function must be synchronized using "syncMonitor".
 * 
 * The use of this function forces the flags SA_RESTART | SA_SIGINFO | SA_NODEFER to be set for the new signal action
 * 
 * The old action for the signal handler is stored in oldActions. These must be restored before the portlibrary is shut down.
 *
 * @return 0 upon success, non-zero otherwise.
 */
static U_32
registerSignalHandlerWithOS (HyPortLibrary * portLibrary,
                             U_32 portLibrarySignalNo,
                             linux386_sigaction handler)
{
  struct sigaction newAction;
  U_32 unixSignalNo;

  unixSignalNo = mapPortLibSignalToUnix (portLibrarySignalNo);

  /* do not block any signals */
  if (sigemptyset (&newAction.sa_mask))
    {
      return -1;
    }

  /* automatically restart system calls that get interrupted by any signal */
  newAction.sa_flags = SA_RESTART;

  /* setting to SA_SIGINFO will result in "void (*sa_sigaction) (int, siginfo_t *, void *)" to be used, and not "__sighandler_t sa_handler". Both are members of struct sigaction. */
  /* using the former allows us to access more than just the signal number */
  newAction.sa_flags |= SA_SIGINFO;

  /*  SA_NODEFER prevents the current signal from being masked in the handler */
  newAction.sa_flags |= SA_NODEFER;

  /* the master exception handler is... */
  newAction.sa_sigaction = handler;

  /* now that we've set up the sigaction struct the way we want it, register the handler with the OS */
  if (HYJSIG_SIGACTION
      (unixSignalNo, &newAction, &oldActions[unixSignalNo].action))
    {
      return -1;
    }
  else
    {
      oldActions[unixSignalNo].restore = 1;
    }

  /* we've successfully registered the master handler for this, record it! */
  signalsWithMasterHandlers |= portLibrarySignalNo;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION mapUnixSignalToPortLib

 /**
 * The linux signal number is converted to the corresponding port library 
 * signal number.
 *
 * Some signals have subtypes which are detailed in the siginfo_t structure.
 */
static U_32
mapUnixSignalToPortLib (U_32 signalNo, siginfo_t * sigInfo)
{
  U_32 index;

  for (index = 0; index < sizeof (signalMap) / sizeof (signalMap[0]); index++)
    {
      if (signalMap[index].unixSignalNo == signalNo)
        {

          if (signalNo == SIGFPE)
            {

              /* Linux 2.4 kernel bug: 64-bit platforms or in 0.x30000 into si_code */
              switch (sigInfo->si_code & 0xf)
                {
                case FPE_FLTDIV:
                  return HYPORT_SIG_FLAG_SIGFPE_DIV_BY_ZERO;
                case FPE_INTDIV:
                  return HYPORT_SIG_FLAG_SIGFPE_INT_DIV_BY_ZERO;
                case FPE_INTOVF:
                  return HYPORT_SIG_FLAG_SIGFPE_INT_OVERFLOW;
                default:
                  return HYPORT_SIG_FLAG_SIGFPE;
                }
            }

          return signalMap[index].portLibSignalNo;
        }
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION mapPortLibSignalToUnix

/**
 * The defined port library signal is converted to the corresponding unix 
 * signal number.
 *
 * Note that FPE signal codes (subtypes) all map to the same signal number and are not included 
 */
static U_32
mapPortLibSignalToUnix (U_32 portLibSignal)
{
  U_32 index;

  /* mask out subtypes */
  portLibSignal &=
    HYPORT_SIG_FLAG_SIGALLSYNC | HYPORT_SIG_FLAG_SIGQUIT |
    HYPORT_SIG_FLAG_SIGABRT | HYPORT_SIG_FLAG_SIGTERM;

  for (index = 0; index < sizeof (signalMap) / sizeof (signalMap[0]); index++)
    {

      if (signalMap[index].portLibSignalNo == portLibSignal)
        {
          return signalMap[index].unixSignalNo;
        }

    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION registerMasterHandlers

/**
 * Registers the master handler for the signals in flags that don't have one
 * yet. We can not handle both synch and asynch signals in one call

 * Calls to this function must be synchronized using syncMonitor
 *
 * 
 * @param[in] flags the flags that we want signals for
 * @param[in] allowedSubsetOfFlags must be one of
 * HYPORT_SIG_FLAG_SIGALLSYNC, or HYPORT_SIG_FLAG_SIGALLASYNC
 * 
 * 
 * @return	0 upon success; HYPORT_SIG_ERROR otherwise.
 *			Possible failure scenarios include attempting to register a handler for
 *			a signal that is not included in the allowedSubsetOfFlags  
*/
static I_32
registerMasterHandlers (HyPortLibrary * portLibrary, U_32 flags,
                        U_32 allowedSubsetOfFlags)
{
  U_32 flagsSignalsOnly, flagsWithoutHandlers;
  linux386_sigaction handler;

  if (allowedSubsetOfFlags == HYPORT_SIG_FLAG_SIGALLSYNC)
    {
      handler = masterSynchSignalHandler;
    }
  else if (allowedSubsetOfFlags == HYPORT_SIG_FLAG_SIGALLASYNC)
    {
      handler = masterASynchSignalHandler;
    }
  else
    {
      return HYPORT_SIG_ERROR;
    }

  flagsSignalsOnly = flags & allowedSubsetOfFlags;
  flagsWithoutHandlers = flagsSignalsOnly & (~signalsWithMasterHandlers);

  if (flagsWithoutHandlers)
    {
      /* we be registerin' some handlers */
      U_32 portSignalType = 0;

      /* portSignalType starts off at 4 as it is the smallest synch
       * signal. In the case that we are registering an asynch
       * signal, flagsWithoutHandlers already has masked off the
       * synchronous signals (eg. "4") so we are not at risk of registering a handler
       * for an incorrect sigal       
       */
      for (portSignalType = 4; portSignalType < allowedSubsetOfFlags;
           portSignalType = portSignalType << 1)
        {
          /* iterate through all the  signals and register the master handler for those that don't have one yet */

          if (flagsWithoutHandlers & portSignalType)
            {
              /* we need a master handler for this (portSignalType's) signal */

              if (registerSignalHandlerWithOS
                  (portLibrary, portSignalType, handler))
                {
                  return HYPORT_SIG_ERROR;
                }
            }
        }
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION initializeSignalTools

static U_32
initializeSignalTools (HyPortLibrary * portLibrary)
{

  /* use this to record the end of the list of signal infos */
  if (hythread_tls_alloc (&tlsKey))
    {
      return -1;
    }

  /* use this to record the last signal that occured such that we can call jsig_handler in hyexit_shutdown_and_exit */
  if (hythread_tls_alloc (&tlsKeyCurrentSignal))
    {
      return -1;
    }

  if (hythread_monitor_init_with_name
      (&masterHandlerMonitor, 0, "portLibrary_hysig_sync_monitor"))
    {
      return -1;
    }

  if (hythread_monitor_init_with_name
      (&asyncReporterShutdownMonitor, 0,
       "portLibrary_hysig_asynch_reporter_shutdown_monitor"))
    {
      return -1;
    }

  if (hythread_monitor_init_with_name
      (&asyncMonitor, 0, "portLibrary_hysig_async_monitor"))
    {
      return -1;
    }

  /* The asynchronous signal reporter will wait on this semaphore  */
  if (sem_init (&wakeUpASynchReporter, 0, 0))
    {
      return -1;
    }

  /* The asynchronous signal reporter keeps track of the number of pending singals with these... */
  if (sem_init (&sigQuitPendingSem, 0, 0))
    {
      return -1;
    }
  if (sem_init (&sigAbrtPendingSem, 0, 0))
    {
      return -1;
    }
  if (sem_init (&sigTermPendingSem, 0, 0))
    {
      return -1;
    }
  if (sem_init (&sigReconfigPendingSem, 0, 0))
    {
      return -1;
    }

  if (hythread_create
      (NULL, 0, HYTHREAD_PRIORITY_MIN, 0, &asynchSignalReporter, NULL))
    {
      return -1;
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION destroySignalTools

static U_32
destroySignalTools (HyPortLibrary * portLibrary)
{
  hythread_tls_free (tlsKey);
  hythread_monitor_destroy (masterHandlerMonitor);
  hythread_monitor_destroy (asyncReporterShutdownMonitor);
  hythread_monitor_destroy (asyncMonitor);
  sem_destroy (&wakeUpASynchReporter);
  sem_destroy (&sigQuitPendingSem);
  sem_destroy (&sigAbrtPendingSem);
  sem_destroy (&sigTermPendingSem);
  sem_destroy (&sigReconfigPendingSem);
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_set_options
I_32 VMCALL
hysig_set_options (struct HyPortLibrary * portLibrary, U_32 options)
{

  if (HYPORT_SIG_OPTIONS_REDUCED_SIGNALS & options)
    {
      /* check that we haven't already set any master handlers */

      hythread_monitor_enter (masterHandlerMonitor);
      if (signalsWithMasterHandlers)
        {
          return -1;
        }
      hythread_monitor_exit (masterHandlerMonitor);

    }

  signalOptions |= options;
  return 0;

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_get_options
/* these options should always be 0 */
U_32 VMCALL
hysig_get_options (struct HyPortLibrary * portLibrary)
{

  return signalOptions;

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION sig_full_shutdown

static void VMCALL
sig_full_shutdown (struct HyPortLibrary *portLibrary)
{
  hythread_monitor_t globalMonitor = hythread_global_monitor ();
  U_32 index;

  hythread_monitor_enter (globalMonitor);
  if (--attachedPortLibraries == 0)
    {

      /* register the old actions we overwrote with our own */
      for (index = 0; index < MAX_UNIX_SIGNAL_TYPES; index++)
        {
          if (oldActions[index].restore)
            {
              HYJSIG_SIGACTION (index, &oldActions[index].action, NULL);
            }
        }

      removeAsyncHandlers (portLibrary);

      /* shut down the asynch reporter thread */
      hythread_monitor_enter (asyncReporterShutdownMonitor);
      shutDownASynchReporter = 1;

      sem_post (&wakeUpASynchReporter);

      while (shutDownASynchReporter)
        {
          hythread_monitor_wait (asyncReporterShutdownMonitor);
        }
      hythread_monitor_exit (asyncReporterShutdownMonitor);

      /* destroy all of the remaining monitors */
      destroySignalTools (portLibrary);

    }                           /* (signalStartupCount) */
  hythread_monitor_exit (globalMonitor);

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION removeAsyncHandlers

static void
removeAsyncHandlers (HyPortLibrary * portLibrary)
{
  /* clean up the list of async handlers */
  HyLinux386AsyncHandlerRecord *cursor;
  HyLinux386AsyncHandlerRecord **previousLink;

  hythread_monitor_enter (asyncMonitor);

  /* wait until no signals are being reported */
  while (asyncThreadCount > 0)
    {
      hythread_monitor_wait (asyncMonitor);
    }

  previousLink = &asyncHandlerList;
  cursor = asyncHandlerList;
  while (cursor)
    {
      if (cursor->portLib == portLibrary)
        {
          *previousLink = cursor->next;
          portLibrary->mem_free_memory (portLibrary, cursor);
          cursor = *previousLink;
        }
      else
        {
          previousLink = &cursor->next;
          cursor = cursor->next;
        }
    }

  hythread_monitor_exit (asyncMonitor);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_chain_at_shutdown_and_exit

/*  @internal 
 *
 * hyexit_shutdown_and_exit needs to call this to ensure the signal is chained to jsig (the application
 *  handler in the case when the shutdown is due to a fatal signal.
 * 
 * @return  
*/
void VMCALL
hysig_chain_at_shutdown_and_exit (struct HyPortLibrary *portLibrary)
{

  HyCurrentSignal *currentSignal =
    hythread_tls_get (hythread_self (), tlsKeyCurrentSignal);

  if (currentSignal != NULL)
    {
      /* we are shutting down due to a signal, forward it to the application handlers */
      if (!(signalOptions & HYPORT_SIG_OPTIONS_JSIG_NO_CHAIN))
        {
          jsig_handler (currentSignal->signal, currentSignal->sigInfo,
                        currentSignal->contextInfo);
        }
    }

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION

#undef CDEV_CURRENT_FUNCTION
