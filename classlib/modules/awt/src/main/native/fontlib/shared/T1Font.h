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
#ifndef __TYPE_1_FONT_CLASS_H
#define __TYPE_1_FONT_CLASS_H

#include <map>
#include <math.h>

#include "Font.h"
#include "Type1Structs.h"

class T1Font : public Font {
public:
	T1Font(fwchar_t *family, StyleName sn, fchar* pathToFile);
	~T1Font(void);
	Glyph* createGlyph(ufshort unicode, ufshort size);
	fwchar_t* getPSName();
	ffloat* getLineMetrics(); 
	bool canDisplay(ufshort c);

	ufshort getUnicodeByIndex(ufshort ind);

//	ffloat* GetExtraMetrics();


private:
	void initFont(FILE *font);
    void parseAFM(FILE *font);
	Type1Map subrsMap;//number -- subrutine
	Type1Map charStringMap;//unicode -- info

    //Type1AFMMap afmMap;//unicode -- afm string

    Type1GlyphCodeMap glyphCodeMap;//glyphCode -- unicode

    fwchar_t *fullName;

    ffloat matrixScale;
};

#endif //__TYPE_1_FONT_CLASS_H
