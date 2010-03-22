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
 
#ifndef __BLITTER__
#define __BLITTER__

#include "SurfaceDataStructure.h"
#include "LUTTables.h"

const static int COMPOSITE_CLEAR    = 1;
const static int COMPOSITE_SRC      = 2;
const static int COMPOSITE_SRC_OVER = 3;
const static int COMPOSITE_DST_OVER = 4;
const static int COMPOSITE_SRC_IN   = 5;
const static int COMPOSITE_DST_IN   = 6;
const static int COMPOSITE_SRC_OUT  = 7;
const static int COMPOSITE_DST_OUT  = 8;
const static int COMPOSITE_DST      = 9;
const static int COMPOSITE_SRC_ATOP = 10;
const static int COMPOSITE_DST_ATOP = 11;
const static int COMPOSITE_XOR      = 12;

void src_over_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void src_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void xor_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void xor_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void src_atop_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_atop_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void src_in_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_in_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void src_out_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_out_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void clear_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_atop_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_atop_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void dst_in_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_in_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void dst_over_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_over_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void dst_out_custom
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void dst_out_custom_bg
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int, int);

void src_over_intrgb_intrgb
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_intrgb_intargb
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_intargb_intrgb
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_intargb_intargb
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_byteindexed_intargb
(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void getRGB
(int, int, SURFACE_STRUCTURE *, void *, unsigned char &, unsigned char &, unsigned char &, unsigned char &, bool);

void setRGB
(int, int, SURFACE_STRUCTURE *, void *, unsigned char, unsigned char, unsigned char, unsigned char, bool);

extern void (* src_over_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);
extern void (* src_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);
extern void (* xor_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);
#endif
