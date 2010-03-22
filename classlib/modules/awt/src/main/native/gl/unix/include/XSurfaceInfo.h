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
 * @author Igor V. Stolyarov
 */
#ifndef __XSURFACE_H__
#define __XSURFACE_H__

#include <X11/extensions/Xrender.h>
#include <X11/extensions/XShm.h>

#ifdef _XSHM_H_
#include <sys/ipc.h>
#include <sys/shm.h>
#endif

#include <stdlib.h>
#include <stdio.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>

extern Bool hasXRender;
extern Bool hasXShm;
extern Bool hasXShmPixmap;

extern Visual *true_color_visual;

int error_handler(Display *, XErrorEvent *);

#endif
