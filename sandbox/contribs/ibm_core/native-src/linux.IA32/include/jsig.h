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

#if !defined(jsig_h)
#define jsig_h

#if defined(__cplusplus)
extern "C" {
#endif

#include <signal.h>

#if defined(WIN32)
#if !defined(JSIGDLLSPEC)
#define JSIGDLLSPEC __declspec(dllimport)
#endif
typedef void (__cdecl *sig_handler_t)(int);
JSIGDLLSPEC int jsig_handler(int sig, void *siginfo, void *uc);
JSIGDLLSPEC sig_handler_t jsig_primary_signal(int sig, sig_handler_t disp);
#else /* UNIX */
typedef void (*sig_handler_t)(int);
int jsig_handler(int sig, void *siginfo, void *uc);
int  jsig_primary_sigaction(int sig, const struct sigaction *act, struct sigaction *oact);
sig_handler_t jsig_primary_signal(int sig, sig_handler_t disp);
#endif /* UNIX */

#if defined(__cplusplus)
}
#endif
#define JSIG_RC_DEFAULT_ACTION_REQUIRED 0
#define JSIG_RC_SIGNAL_HANDLED 1

#endif     /* jsig_h */
