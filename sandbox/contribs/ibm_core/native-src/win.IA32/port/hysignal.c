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

#include <windows.h>
#include <stdlib.h>
#include "hyport.h"
#include "hythread.h"
#include "hysignal.h"

typedef struct HyWin32AsyncHandlerRecord
{
  HyPortLibrary *portLib;
  hysig_handler_fn handler;
  void *handler_arg;
  U_32 flags;
  struct HyWin32AsyncHandlerRecord *next;
} HyWin32AsyncHandlerRecord;

static HyWin32AsyncHandlerRecord *asyncHandlerList;
static hythread_monitor_t asyncMonitor;
static U_32 asyncThreadCount;
static U_32 attachedPortLibraries;
/* holds the options set by hysig_set_options */
static U_32 signalOptions;

#define CDEV_CURRENT_FUNCTION _prototypes_private

static U_32 mapWin32ExceptionToPortlibType (U_32 exceptionCode);

static U_32 infoForGPR (struct HyPortLibrary *portLibrary,
			struct HyWin32SignalInfo *info, I_32 index,
			const char **name, void **value);

static void removeAsyncHandlers (HyPortLibrary * portLibrary);

static void fillInWin32SignalInfo (struct HyPortLibrary *portLibrary,
				   hysig_handler_fn handler,
				   EXCEPTION_POINTERS * exceptionInfo,
				   struct HyWin32SignalInfo *hyinfo);

static U_32 infoForSignal (struct HyPortLibrary *portLibrary,
			   struct HyWin32SignalInfo *info, I_32 index,
			   const char **name, void **value);

static U_32 infoForModule (struct HyPortLibrary *portLibrary,
			   struct HyWin32SignalInfo *info, I_32 index,
			   const char **name, void **value);

static U_32 countInfoInCategory (struct HyPortLibrary *portLibrary,
				 void *info, U_32 category);

static BOOL WINAPI consoleCtrlHandler (DWORD dwCtrlType);

int structuredExceptionHandler (struct HyPortLibrary *portLibrary,
				hysig_handler_fn handler, void *handler_arg,
				U_32 flags,
				EXCEPTION_POINTERS * exceptionInfo);

static U_32 infoForControl (struct HyPortLibrary *portLibrary,
			    struct HyWin32SignalInfo *info, I_32 index,
			    const char **name, void **value);

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_info
U_32 VMCALL
hysig_info (struct HyPortLibrary *portLibrary, void *info, U_32 category,
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
I_32 VMCALL
hysig_protect (struct HyPortLibrary * portLibrary, hysig_protected_fn fn,
	       void *fn_arg, hysig_handler_fn handler, void *handler_arg,
	       U_32 flags, UDATA * result)
{
  __try
  {
    *result = fn (portLibrary, fn_arg);
  } __except (structuredExceptionHandler
	      (portLibrary, handler, handler_arg, flags,
	       GetExceptionInformation ()))
  {
    /* this code is only reachable if the handler returned HYPORT_SIG_EXCEPTION_RETURN */
    *result = 0;
    return HYPORT_SIG_EXCEPTION_OCCURRED;
  }

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
  HyWin32AsyncHandlerRecord *cursor;
  HyWin32AsyncHandlerRecord **previousLink;

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
	      *previousLink = cursor->next;
	      portLibrary->mem_free_memory (portLibrary, cursor);

	      /* if this is the last handler, unregister the Win32 handler function */
	      if (asyncHandlerList == NULL)
		{
		  SetConsoleCtrlHandler (consoleCtrlHandler, FALSE);
		}
	    }
	  else
	    {
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
	  HyWin32AsyncHandlerRecord *record =
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

	      /* if this is the first handler, register the Win32 handler function */
	      if (asyncHandlerList == NULL)
		{
		  SetConsoleCtrlHandler (consoleCtrlHandler, TRUE);
		}

	      /* add the new record to the end of the list */
	      *previousLink = record;
	    }
	}
    }

  hythread_monitor_exit (asyncMonitor);

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_can_protect
I_32 VMCALL
hysig_can_protect (struct HyPortLibrary * portLibrary, U_32 flags)
{
  U_32 supportedFlags =
    HYPORT_SIG_FLAG_MAY_RETURN | HYPORT_SIG_FLAG_MAY_CONTINUE_EXECUTION;

  supportedFlags |=
    HYPORT_SIG_FLAG_SIGALLSYNC | HYPORT_SIG_FLAG_SIGQUIT |
    HYPORT_SIG_FLAG_SIGTERM;

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

#define CDEV_CURRENT_FUNCTION hysig_shutdown
/**
 * Shutdown the signal handling component of the port library
 */
void VMCALL
hysig_shutdown (struct HyPortLibrary *portLibrary)
{
  hythread_monitor_t globalMonitor = hythread_global_monitor ();

  removeAsyncHandlers (portLibrary);

  hythread_monitor_enter (globalMonitor);

  if (--attachedPortLibraries == 0)
    {
      hythread_monitor_destroy (asyncMonitor);
    }

  hythread_monitor_exit (globalMonitor);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_startup
/**
 * Start up the signal handling component of the port library
 */
I_32 VMCALL
hysig_startup (struct HyPortLibrary *portLibrary)
{
  hythread_monitor_t globalMonitor = hythread_global_monitor ();
  I_32 result = 0;

  hythread_monitor_enter (globalMonitor);

  if (attachedPortLibraries++ == 0)
    {
      if (hythread_monitor_init_with_name
	  (&asyncMonitor, 0, "portLibrary_hysig_async_monitor"))
	{
	  result = -1;
	}
    }

  hythread_monitor_exit (globalMonitor);

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION structuredExceptionHandler
int
structuredExceptionHandler (struct HyPortLibrary *portLibrary,
			    hysig_handler_fn handler, void *handler_arg,
			    U_32 flags, EXCEPTION_POINTERS * exceptionInfo)
{
  U_32 result;
  U_32 type;
  struct HyWin32SignalInfo hyinfo;

  if ((exceptionInfo->ExceptionRecord->
       ExceptionCode & (ERROR_SEVERITY_ERROR | APPLICATION_ERROR_MASK)) !=
      ERROR_SEVERITY_ERROR)
    {
      return EXCEPTION_CONTINUE_SEARCH;
    }

  type =
    mapWin32ExceptionToPortlibType (exceptionInfo->ExceptionRecord->
				    ExceptionCode);
  if (0 == (type & flags))
    {
      return EXCEPTION_CONTINUE_SEARCH;
    }

  fillInWin32SignalInfo (portLibrary, handler, exceptionInfo, &hyinfo);

  __try
  {
    result = handler (portLibrary, hyinfo.type, &hyinfo, handler_arg);
  }
  __except (EXCEPTION_EXECUTE_HANDLER)
  {
    /* if a recursive exception occurs, ignore it and pass control to the next handler */
    return EXCEPTION_CONTINUE_SEARCH;
  }

  if (result == HYPORT_SIG_EXCEPTION_CONTINUE_SEARCH)
    {
      return EXCEPTION_CONTINUE_SEARCH;
    }
  else if (result == HYPORT_SIG_EXCEPTION_CONTINUE_EXECUTION)
    {
      return EXCEPTION_CONTINUE_EXECUTION;
    }
  else				/* if (result == HYPORT_SIG_EXCEPTION_RETURN) */
    {
      return EXCEPTION_EXECUTE_HANDLER;
    }

}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION mapWin32ExceptionToPortlibType
static U_32
mapWin32ExceptionToPortlibType (U_32 exceptionCode)
{
  switch (exceptionCode)
    {

    case EXCEPTION_FLT_DIVIDE_BY_ZERO:
      return HYPORT_SIG_FLAG_SIGFPE_DIV_BY_ZERO;

    case EXCEPTION_INT_DIVIDE_BY_ZERO:
      return HYPORT_SIG_FLAG_SIGFPE_INT_DIV_BY_ZERO;

    case EXCEPTION_INT_OVERFLOW:
      return HYPORT_SIG_FLAG_SIGFPE_INT_OVERFLOW;

    case EXCEPTION_FLT_OVERFLOW:
    case EXCEPTION_FLT_UNDERFLOW:
    case EXCEPTION_FLT_INVALID_OPERATION:
    case EXCEPTION_FLT_INEXACT_RESULT:
    case EXCEPTION_FLT_DENORMAL_OPERAND:
    case EXCEPTION_FLT_STACK_CHECK:
      return HYPORT_SIG_FLAG_SIGFPE;

    case EXCEPTION_PRIV_INSTRUCTION:
    case EXCEPTION_ILLEGAL_INSTRUCTION:
      return HYPORT_SIG_FLAG_SIGILL;

    case EXCEPTION_ACCESS_VIOLATION:
      return HYPORT_SIG_FLAG_SIGSEGV;

    case EXCEPTION_IN_PAGE_ERROR:
    case EXCEPTION_DATATYPE_MISALIGNMENT:
      return HYPORT_SIG_FLAG_SIGBUS;

    default:
      return HYPORT_SIG_FLAG_SIGTRAP;

    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION fillInWin32SignalInfo
static void
fillInWin32SignalInfo (struct HyPortLibrary *portLibrary,
		       hysig_handler_fn handler,
		       EXCEPTION_POINTERS * exceptionInfo,
		       struct HyWin32SignalInfo *hyinfo)
{
  memset (hyinfo, 0, sizeof (*hyinfo));

  hyinfo->type =
    mapWin32ExceptionToPortlibType (exceptionInfo->ExceptionRecord->
				    ExceptionCode);
  hyinfo->handlerAddress = (void *) handler;
  hyinfo->handlerAddress2 = (void *) structuredExceptionHandler;
  hyinfo->ExceptionRecord = exceptionInfo->ExceptionRecord;
  hyinfo->ContextRecord = exceptionInfo->ContextRecord;

  /* module info is filled on demand */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForSignal
static U_32
infoForSignal (struct HyPortLibrary *portLibrary,
	       struct HyWin32SignalInfo *info, I_32 index, const char **name,
	       void **value)
{
  *name = "";

  switch (index)
    {

    case HYPORT_SIG_SIGNAL_TYPE:
    case 0:
      *name = "HyGeneric_Signal_Number";
      *value = &info->type;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_PLATFORM_SIGNAL_TYPE:
    case 1:
      *name = "ExceptionCode";
      *value = &info->ExceptionRecord->ExceptionCode;
      return HYPORT_SIG_VALUE_32;

    case HYPORT_SIG_SIGNAL_ADDRESS:
    case 2:
      *name = "ExceptionAddress";
      *value = &info->ExceptionRecord->ExceptionAddress;
      return HYPORT_SIG_VALUE_ADDRESS;

    case 3:
      *name = "ContextFlags";
      *value = &info->ContextRecord->ContextFlags;
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
      if (info->ExceptionRecord->ExceptionCode == EXCEPTION_ACCESS_VIOLATION)
	{
	  *name = "InaccessibleAddress";
	  *value = &info->ExceptionRecord->ExceptionInformation[1];
	  return HYPORT_SIG_VALUE_ADDRESS;
	}
      else
	{
	  return HYPORT_SIG_VALUE_UNDEFINED;
	}

    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForGPR
static U_32
infoForGPR (struct HyPortLibrary *portLibrary, struct HyWin32SignalInfo *info,
	    I_32 index, const char **name, void **value)
{
  *name = "";

  if (info->ContextRecord->ContextFlags & CONTEXT_INTEGER)
    {
      switch (index)
	{
	case HYPORT_SIG_GPR_X86_EDI:
	case 0:
	  *name = "EDI";
	  *value = &info->ContextRecord->Edi;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_GPR_X86_ESI:
	case 1:
	  *name = "ESI";
	  *value = &info->ContextRecord->Esi;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_GPR_X86_EAX:
	case 2:
	  *name = "EAX";
	  *value = &info->ContextRecord->Eax;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_GPR_X86_EBX:
	case 3:
	  *name = "EBX";
	  *value = &info->ContextRecord->Ebx;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_GPR_X86_ECX:
	case 4:
	  *name = "ECX";
	  *value = &info->ContextRecord->Ecx;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_GPR_X86_EDX:
	case 5:
	  *name = "EDX";
	  *value = &info->ContextRecord->Edx;
	  return HYPORT_SIG_VALUE_ADDRESS;

	default:
	  return HYPORT_SIG_VALUE_UNDEFINED;
	}
    }

  return HYPORT_SIG_VALUE_UNDEFINED;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForControl
static U_32
infoForControl (struct HyPortLibrary *portLibrary,
		struct HyWin32SignalInfo *info, I_32 index, const char **name,
		void **value)
{
  *name = "";

  if (info->ContextRecord->ContextFlags & CONTEXT_CONTROL)
    {
      switch (index)
	{
	case HYPORT_SIG_CONTROL_PC:
	case 0:
	  *name = "EIP";
	  *value = &info->ContextRecord->Eip;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_CONTROL_SP:
	case 1:
	  *name = "ESP";
	  *value = &info->ContextRecord->Esp;
	  return HYPORT_SIG_VALUE_ADDRESS;

	case HYPORT_SIG_CONTROL_BP:
	case 2:
	  *name = "EBP";
	  *value = &info->ContextRecord->Ebp;
	  return HYPORT_SIG_VALUE_ADDRESS;

	default:
	  return HYPORT_SIG_VALUE_UNDEFINED;
	}
    }
  return HYPORT_SIG_VALUE_UNDEFINED;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION infoForModule
static U_32
infoForModule (struct HyPortLibrary *portLibrary,
	       struct HyWin32SignalInfo *info, I_32 index, const char **name,
	       void **value)
{
  if (info->moduleBaseAddress == NULL)
    {
      MEMORY_BASIC_INFORMATION mbi;

      if (VirtualQuery
	  ((LPBYTE) info->ExceptionRecord->ExceptionAddress, &mbi,
	   sizeof (mbi)) == sizeof (mbi))
	{
	  if (MEM_FREE == mbi.State)
	    {
	      /* from Advanced Windows (Richter) */
	      mbi.AllocationBase = mbi.BaseAddress;
	    }

	  GetModuleFileName ((HINSTANCE) mbi.AllocationBase, info->moduleName,
			     sizeof (info->moduleName));
	  info->moduleBaseAddress = mbi.AllocationBase;
	  info->offsetInDLL =
	    (UDATA) info->ExceptionRecord->ExceptionAddress -
	    (UDATA) mbi.AllocationBase;
	}
    }

  *name = "";

  switch (index)
    {
    case HYPORT_SIG_MODULE_NAME:
    case 0:
      *name = "Module";
      *value = &info->moduleName;
      return HYPORT_SIG_VALUE_STRING;
    case 1:
      *name = "Module_base_address";
      *value = &info->moduleBaseAddress;
      return HYPORT_SIG_VALUE_ADDRESS;
    case 2:
      *name = "Offset_in_DLL";
      *value = &info->offsetInDLL;
      return HYPORT_SIG_VALUE_32;
    default:
      return HYPORT_SIG_VALUE_UNDEFINED;
    }
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

#define CDEV_CURRENT_FUNCTION consoleCtrlHandler
static BOOL WINAPI
consoleCtrlHandler (DWORD dwCtrlType)
{
  U_32 flags;
  BOOL result = FALSE;

  switch (dwCtrlType)
    {
    case CTRL_BREAK_EVENT:
      flags = HYPORT_SIG_FLAG_SIGQUIT;
      break;
    case CTRL_C_EVENT:
      flags = HYPORT_SIG_FLAG_SIGTERM;
      break;
    default:
      return result;
    }

  if (0 == hythread_attach (NULL))
    {
      HyWin32AsyncHandlerRecord *cursor;
      U_32 handlerCount = 0;

      /* incrementing the asyncThreadCount will prevent the list from being modified while we use it */
      hythread_monitor_enter (asyncMonitor);
      asyncThreadCount++;
      hythread_monitor_exit (asyncMonitor);

      cursor = asyncHandlerList;
      while (cursor)
	{
	  if (cursor->flags & flags)
	    {
	      cursor->handler (cursor->portLib, flags, NULL,
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

      /* TODO: possible timing hole. The thread library could be unloaded by the time we
       * reach this line. We can't use hythread_exit(), as that kills the async reporting thread
       */
      hythread_detach (NULL);
    }

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION removeAsyncHandlers
static void
removeAsyncHandlers (HyPortLibrary * portLibrary)
{
  /* clean up the list of async handlers */
  HyWin32AsyncHandlerRecord *cursor;
  HyWin32AsyncHandlerRecord **previousLink;

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

  if (asyncHandlerList == NULL)
    {
      SetConsoleCtrlHandler (consoleCtrlHandler, FALSE);
    }

  hythread_monitor_exit (asyncMonitor);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysig_set_options
/**
 * none of the current options are supported by windows IA32
 */
I_32 VMCALL
hysig_set_options (struct HyPortLibrary *portLibrary, U_32 options)
{

  if (options == 0)
    {
      signalOptions = options;
      return 0;
    }
  else
    {
      return -1;
    }
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
