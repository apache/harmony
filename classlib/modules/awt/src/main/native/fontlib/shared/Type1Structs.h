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
 * @author Viskov Nikolay 
 */

#ifndef __TYPE_1_STRUCTS_H
#define __TYPE_1_STRUCTS_H

#include <map>
#include "EncodedValue.h"
#include "AGL.h"
#include "TypeDefinition.h"

typedef std::map<const ufshort, EncodedValue*> Type1Map;

typedef std::map<ufshort, ufshort> Type1GlyphCodeMap;

typedef std::map<ufshort, fchar*> Type1AFMMap;

typedef enum DecodeStateTag {HEADER, PRIVATE_DIR, SUBRS_MASSIVE, CHAR_STRING} DecodeState;

typedef std::map<const fchar*, const ufshort> Type1CharMap;//inner glyph number -- unicode

static const ufshort MAX_STR_LENGTH = 1024;
static const ufshort C1 = 52845;
static const ufshort C2 = 22719;
static const ufshort DEF_R_EXEC = 55665;
static const ufshort DEF_LENIV = 4;
static const ufshort DEF_R_CHARSTRING = 4330;

static const ufchar CH_STR_HSTEM = 1;
static const ufchar CH_STR_VSTEM = 3;
static const ufchar CH_STR_VMOVETO = 4;
static const ufchar CH_STR_RLINETO = 5;
static const ufchar CH_STR_HLINETO = 6;
static const ufchar CH_STR_VLINETO = 7;
static const ufchar CH_STR_RRCURVETO = 8;
static const ufchar CH_STR_CLOSEPATH = 9;
static const ufchar CH_STR_CALLSUBR = 10;
static const ufchar CH_STR_RETURN = 11;
static const ufchar CH_STR_ESCAPE = 12;
static const ufchar CH_STR_HSBW = 13;
static const ufchar CH_STR_ENDCHAR = 14;
static const ufchar CH_STR_RMOVETO = 21;
static const ufchar CH_STR_HMOVETO = 22;
static const ufchar CH_STR_VHCURVETO = 30;
static const ufchar CH_STR_HVCURVETO = 31;

static const ufchar CH_STR_ESCAPE_DOTSECTION = 0;
static const ufchar CH_STR_ESCAPE_VSTEM3 = 1;
static const ufchar CH_STR_ESCAPE_HSTEM3 = 2;
static const ufchar CH_STR_ESCAPE_SEAC = 6;
static const ufchar CH_STR_ESCAPE_SBW = 7;
static const ufchar CH_STR_ESCAPE_DIV = 12;
static const ufchar CH_STR_ESCAPE_CALLOTHERSUBR = 16;
static const ufchar CH_STR_ESCAPE_POP = 17;
static const ufchar CH_STR_ESCAPE_SETCURRENTPOINT = 33;

//#define GLYPH_OUTLINE_CREATE_DEBUG

#endif //__TYPE_1_STRUCTS_H
