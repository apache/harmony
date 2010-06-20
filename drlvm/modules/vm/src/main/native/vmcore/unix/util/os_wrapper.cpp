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
/** 
 * @author Intel, Evgueni Brevnov
 */  

#include <stdio.h>
#include <signal.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/time.h>
#include <assert.h>
#include <errno.h>

#include "port_malloc.h"
#include "platform_lowlevel.h"

#ifndef __SMP__
#error
-- recompile with -D__SMP__
#endif

#ifndef _REENTRANT
#error
-- recompile with -D_REENTRANT
#endif

#ifndef __SIGRTMIN
#else
#if __SIGRTMAX - __SIGRTMIN >= 3
// good, this will work. java dbg, also vm can use SIGUSR1, SIGUSR2
#else
#error
-- must be using an old version of pthreads
-- which uses SIGUSR1, SIGUSR2 (which conflicts with the java app debugger and vm)
#endif
#endif
