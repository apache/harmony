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
#include <stdlib.h>
#include <stdio.h>

#include "XSurfaceInfo.h"
#include "org_apache_harmony_awt_wtk_linux_XServerConnection.h"

Bool hasXRender;
Bool hasXShm;
Bool hasXShmPixmap;

Visual *true_color_visual;
/*
 * Method: org.apache.harmony.awt.wtk.linux.XServerConnection.init(JI)V
 */
JNIEXPORT void JNICALL 
Java_org_apache_harmony_awt_wtk_linux_XServerConnection_init
    (JNIEnv *env, jobject obj, jlong display, jint screen)
{
    int minor, major, num_visuals = 0;

    XVisualInfo templ;

    templ.screen = screen;
    templ.depth = 32;
    templ.red_mask = 0xff0000;
    templ.green_mask = 0xff00;
    templ.blue_mask = 0xff;

    XVisualInfo *info = XGetVisualInfo((Display *)display, VisualScreenMask | VisualDepthMask | 
        VisualRedMaskMask | VisualGreenMaskMask | VisualBlueMaskMask, &templ, &num_visuals);

    if(num_visuals) true_color_visual = info[0].visual;
    else true_color_visual = NULL;

    hasXRender = false;
    hasXShm = false;

    int num;
    char **ext_list = XListExtensions((Display *)display, &num);

#ifdef _XRENDER_H_

    Bool renderExists = False;

    for(int i = 0; i < num; i++){
        if(strstr(ext_list[i], "RENDER")){
            renderExists = True;
            break;
        }
    }

    if(renderExists){
        XRenderQueryVersion((Display *)display, &major, &minor);

        if(major == 0 && minor > 7){
            hasXRender = true;
        }
    }
#endif

#ifdef _XSHM_H_

    Bool shmExists = False;

    for(int i = 0; i < num; i++){
        if(strstr(ext_list[i], "MIT-SHM")){
            shmExists = True;
            break;
        }
    }

    if(shmExists){
        if(XShmQueryVersion((Display *)display, &major, &minor, &hasXShmPixmap)){
            hasXShm = true;
        }
    }
#endif

}

