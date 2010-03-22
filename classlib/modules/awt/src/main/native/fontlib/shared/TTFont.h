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
 * @author Dmitriy S. Matveev
 */
#ifndef __TTFONT_H__
#define __TTFONT_H__

#include "Font.h"
#include "Glyph.h"
#include "Outline.h"
#include "TTCurve.h"
#include "TypeDefinition.h"

typedef struct
{
	fshort format; //format of the 'loca' table
	uflong* offsets;
} GlyphOffsets;

typedef struct
{
	fshort format;
	void* TableEncode;
} TableEncode;

typedef struct
{
	ufshort adwance_width;
	fshort lsb;
}HMetrics;

class TTGlyph;

class TTFont:public Font
{
friend class TTGlyph;
private:
	fchar* _pathToFile; // path to font file
	fwchar_t *_psName; // postscript name of font
	GlyphOffsets _glyphOffsets; //glyphs offsets in font file
	TableEncode _tableEncode; // table with indexes of glyphs
	ufshort _unitsPerEm; //size of em-square
	ufshort _numOfHMetrics; // for 'hmtx' table
	HMetrics* _hMetrics; // horizontal metrics for all glyphs
	FILE* _ttfile;

	ufshort getGlyphIndex(ufshort symb);
	ufshort getUnicodeByIndex(ufshort ind);
//	friend ufshort TTGlyph::getGlyphIndex(ufshort symb);
//	friend fint TTGlyph::initialize();

public:
	TTFont(fchar* pathToFile);
	~TTFont(void);
	
	Glyph* createGlyph(ufshort unicode, ufshort size);
	fwchar_t* getPSName();
	ffloat* getLineMetrics(); 
	bool canDisplay(ufshort c);

//	ffloat* GetExtraMetrics();
};


class TTGlyph : public Glyph {
private:
	TTFont* _ttfont;			
	ufshort _index; 
	TTCurve* _curve;
	fshort _boundingRect[4]; 

	
	friend Glyph* TTFont::createGlyph(ufshort unicode, ufshort size);

public:
	TTGlyph(TTFont *font, ufshort unicode, ufshort size);
	~TTGlyph();
	Outline* getOutline(void);
	ffloat* getGlyphMetrics(void);
};

#endif //__TTFONT_H__
