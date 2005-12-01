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

#if !defined(jsigunix_h)
#define jsigunix_h
#if defined(__cplusplus)
extern "C"
{
#endif
#include "jsig.h"
/* Unix specific types */
  typedef int (*sigactionfunc_t) (int, const struct sigaction *,
                                  struct sigaction *);
/* Shared Unix functions */
  sig_handler_t bsd_signal (int, sig_handler_t);
  sig_handler_t sysv_signal (int, sig_handler_t);
/* Platform dependent Unix functions */
  int real_sigaction (int sig, const struct sigaction *act,
                      struct sigaction *oact);
  int real_sigprocmask (int option, const sigset_t * new_set,
                        sigset_t * old_set);
#if defined(__cplusplus)
}
#endif
#endif                          /* jsigunix_h */
