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

/*
	MOTIVATION:
	===========
	Getting signals correct on the different UNIX platforms is difficult.
	Each platform provides slighlty different semantics for signals depending
	on the call used (signal/sigaction/sigset).
	
	The macros in this file are designed to minimize those differences, and
	if used as recommended can completely hide the platform differences.

	USE:
	====

	***NOTE: These functions only work with signals whose signal number is less
		than 50. This can be changed by changing the value of HyMaxSignals.

	The following macros are defined

		HySigSet (aSignal, aFn, previousHandler);
			Installs a signal handler called aFn.

			aSignal - is the platform constant for the signal
				(e.g. SIG_USR1)
			aFn - is the signal handler you want to install
				(fn of type SIGHANDLERFN)
			previousHandler - is a storage location for the previous handler,
				or action
				(fn of type HySignalHandler, or constant such as SIG_DFL)

		HySigBlock(aSignal)
		HySigUnblock(aSignal)


	The Handler function should have a prototype of:
		void mySigHandler(int SignalNumber);

	To invoke a previous signal handler use:
		HyInvokePreviousHandler(previousHandler, asig);

		The previous handler will only be invoked if it was not a system
		action such as (SIG_IGN, or SIG_DFL).

	NOTE:
		HySigUnblock must be called before the signal handler returns, to ensure the
		signal can be handled again.

	

	EXAMPLES:
	=========

===>1) If the handler *can* be interrupted by additional signals
		use the following style:

	static HySigHandler previousHandler;

	myHandler(aSig)
	{
		HySigUnblock(aSig);
		... do some interruptable work ...
		HyInvokePreviousHandler(previousHandler, aSig);
	}

	main()
	{
		HySetSig(SIG_USR1, myHandler, previousHandler);
	}
		
===>2) If the handler *cannot* be interrupted by the same signal,
	use the following style:

	static HySigHandler previousHandler;

	myHandler(aSig)
	{
		HySigBlock(aSig);
		... do some uninterruptable work ...
		HySigUnblock(aSig);
		HyInvokePreviousHandler(previousHandler, aSig);
	}

	main()
	{
		HySetSig(SIG_USR1, myHandler, previousHandler);
	}

	
===>3) If it does not matter whether or not the handler can be interrupted
	by additional signals use the following style:
	NOTE: the signal must be unblocked before the signal handler returns
	or a long jump is executed.

	static HySigHandler previousHandler;

	myHandler(aSig)
	{
		... do some interruptable work ...
		HySigUnblock(aSig);
		HyInvokePreviousHandler(previousHandler, aSig);
	}

	main()
	{
		HySetSig(SIG_USR1, myHandler, previousHandler);
	}

	SPECIFICATION NOTES: (for porting)
	==================================

	1. The signal handler must be reliable. This means: the sighandler will not
		be set to SIG_DFL when the signal handler is entered. So there is
		no need to set it again. (A problem when cascading signal handlers).
	2. The blocking state will remain unspecified when the sig handler is
		entered. So in order to set the state, the developer must specify
		HySigBlock(aSig), or HySigUnblock(aSig)
	3. The previous signal handler may or may not be returned if it was set
		using a fn other than HySigSet(...) or sigaction(...). e.g. if
		signal() was used to set the handler it may or may not be returned
		in the previousHandler variable. (This is platform specific).
	4. These macros work only on Solaris, SPARC, HPUX and AIX. Other Unix platforms
		may be added in the future.
	5. When signals are blocked they are actually queued by the OS. However,
		the queue that the OS uses is extremely small, and some signals may
		be lost, even though they are blocked.
	6. In a multithreaded environment a signal handler must be able to handle signals
		meant for other threads.

*/

#if !defined(HYSIGNAL_H)
#define HYSIGNAL_H
 
#include <signal.h>
/* declare protototypes - different for each platform */



#if defined(LINUXPPC)
typedef void(*HySignalHandler)PROTOTYPE((int sig, struct sigcontext *context));
#endif

#if defined(LINUX) && !defined(LINUXPPC)
typedef void(*HySignalHandler)PROTOTYPE((int sig));
#endif

#define HyMaxSignals 50 /* This implementation can only handle the first 50 signals */

/* A set of signal handlers indexed by signal number. This is useful for storing previous
signal handlers */
typedef HySignalHandler HySignalHandlerSet[HyMaxSignals];
#define HySigUnblock(aSig_es)\
	{sigset_t asigset_es;\
		sigemptyset(&asigset_es);\
		sigaddset(&asigset_es, (aSig_es));\
		sigprocmask(SIG_UNBLOCK, &asigset_es, NULL);\
	}
#define HySigBlock(aSig_es)\
	{sigset_t asigset_es;\
		sigemptyset(&asigset_es);\
		sigaddset(&asigset_es, (aSig_es));\
		sigprocmask(SIG_BLOCK, &asigset_es, NULL);\
	}
#if false
/* Note: On the solaris platform the .sa_flags = NODEFER option does not
	work properly on Solaris 2.4. Behaviour on other versions is unknown */
#define HySigSet(aSignal_es, aFn_es, previousHandler_es)\
	{\
		struct sigaction newact_es, oldact_es;\
		sigemptyset(&newact_es.sa_mask);\
		newact_es.sa_flags = SA_RESTART;\
		newact_es.sa_handler = (void (*)(int))aFn_es;\
		HYJSIG_SIGACTION(aSignal_es,&newact_es,&oldact_es);\
		(previousHandler_es) = (HySignalHandler)oldact_es.sa_handler;\
	}
#elif defined(LINUX)
#define HySigSet(aSignal_es, aFn_es, previousHandler_es)\
	{\
		struct sigaction newact_es, oldact_es;\
		sigemptyset(&newact_es.sa_mask);\
		newact_es.sa_flags = SA_RESTART;\
		if ((void *)aFn_es != (void *)SIG_DFL &&\
		    (void *)aFn_es != (void *)SIG_IGN) {\
			newact_es.sa_flags |= SA_SIGINFO;\
			newact_es.sa_sigaction = (void (*)(int, siginfo_t*, void*))aFn_es;\
		} else {\
			newact_es.sa_handler = (void (*)(int))aFn_es;\
		}\
		HYJSIG_SIGACTION(aSignal_es,&newact_es,&oldact_es);\
		(previousHandler_es) = (HySignalHandler)oldact_es.sa_handler;\
	}
#elif defined(HYZOS390)
#define HySigSet(aSignal_es, aFn_es, previousHandler_es)\
		{\
	struct sigaction newact_es, oldact_es;\
	sigemptyset(&newact_es.sa_mask);\
	newact_es.sa_flags = SA_RESTART;\
	if ((void *)aFn_es != (void *)SIG_DFL &&\
	    (void *)aFn_es != (void *)SIG_IGN) {\
		newact_es.sa_flags |= SA_SIGINFO;\
		newact_es.sa_sigaction = (void (*)(int, siginfo_t*, void*))aFn_es;\
	} else {\
		newact_es.sa_handler = (void (*)(int))aFn_es;\
	}\
	HYJSIG_SIGACTION(aSignal_es,&newact_es,&oldact_es);\
	(previousHandler_es) = (HySignalHandler)oldact_es.sa_sigaction;\
		}
#else
#endif
/* AIX, HP and SOLARIS all return NULL for the previous handler if
   there wasn't one defined */
/* Invoke the previous handler 
	- stored in the signal set <previousHandlerSet_es>
	- at index <aSig_es>
	- passing it signal number <aSig_es> */

#if (defined(LINUX) && !defined(LINUXPPC))
#define HyInvokePreviousHandlerIn(previousHandlerSet_es,aSig_es)\
	if ((previousHandlerSet_es)[(aSig_es)] != NULL) (*(previousHandlerSet_es)[(aSig_es)])((aSig_es))
#endif

#if defined(LINUXPPC)
#define HyInvokePreviousHandlerIn(previousHandlerSet_es, aSig_es, aScp_es)\
	if ((previousHandlerSet_es)[(aSig_es)] != NULL) (*(previousHandlerSet_es)[(aSig_es)])((aSig_es), (aScp_es))
#endif
/*store the previous handler 
	- in the signal set <previousHandlerSet_es>
	- at index <aSig_es>
	- where the previous handler is <previousHandler_es> */
#define HyAddPreviousHandlerIn(previousHandlerSet_es, previousHandler_es, aSig_es)\
	(previousHandlerSet_es)[(aSig_es)] = previousHandler_es
/*remove the previous handler 
	- in the signal set <previousHandlerSet_es>
	- at index <aSig_es> */
#define HyRemovePreviousHandlerIn(previousHandlerSet_es, aSig_es)\
	(previousHandlerSet_es)[(aSig_es)] = NULL
/*answer the previous handler 
	- in the signal set <previousHandlerSet_es>
	- at index <aSig_es>
	-if there was no previous handler answer NULL */
#define HyPreviousHandlerIn(previousHandlerSet_es, aSig_es)\
	((previousHandlerSet_es)[(aSig_es)])

#endif /* HYSIGNAL_H defined */
