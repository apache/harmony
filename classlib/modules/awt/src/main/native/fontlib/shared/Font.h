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
 * @author Dmitriy S. Matveev, Viskov Nikolay 
 */
#ifndef __FONT_H__
#define __FONT_H__

#include <map>
#include "Glyph.h"
#include "TypeDefinition.h"

typedef enum StyleNameTag {
    Regular = 0, 
    Bold = 1, 
    Italic = 2, 
    BoldItalic = 3
} StyleName;

typedef enum FlagsTag {ANGLE, IS_FIXED_PITCH, BOLD} Flags;

static const ufchar FONT_METRICS_QUANTITY = 8;
static const ufchar GLYPH_METRICS_QUANTITY = 6;

/*
typedef struct
{
	fint numChars;
	fint baselineIndex;
	ffloat underlineThickness;
	ffloat underlineOffset;
	ffloat strikethroughThickness;
    ffloat strikethroughOffset;
	ffloat leading;
	ffloat height;
	ffloat descent;
	ffloat ascent;
	ffloat baseLineOffsets[1];
}LineMetrics;
*/

class Font {
public:
	Font();
	virtual ~Font();

	Glyph* getGlyph(ufshort unicode, ufshort size);
	Glyph* getDefaultGlyph();
	fint	getMissingGlyphCode();

	virtual Glyph* createGlyph(ufshort unicode, ufshort size);
	virtual	ffloat* getLineMetrics(); 
	virtual fwchar_t* getPSName();
	virtual bool canDisplay(ufshort c);
	virtual ufshort getUnicodeByIndex(ufshort ind);


//protected:
	ufshort _numGlyphs; // Number of available glyphs
	fwchar_t *_famName; // (ufshort*) Family name
    StyleName _style; // Font style 
	//fshort *_bitmaps; // - (?)
	ffloat _boundingBox[4]; // Glyphs bounding box - array of 4 shorts
	ffloat _ascent; 
	ffloat _descent;
	ffloat _externalLeading; //lineGap in TrueType
	ffloat _height;
	ffloat _strikeOutSize;
	ffloat _strikeOutOffset;
    ffloat _underlineOffset;
    ffloat _underlineThickness;
	ufshort _size;
	std::map<const uflong, Glyph*> _glyphMap;//(size << 16 + unicode) -- Glyph	
	Flags _flags;	

//	virtual ufshort* getBitmap();
//	virtual ufshort* getOutline();
//	virtual ufshort* getGlyph();
private:
	inline Glyph* findGlyph(ufshort unicode, ufshort size, uflong id);
};

Font* createFont(fwchar_t* family, StyleName sn);
Font* createFont(fchar* family, StyleName sn);

#endif //__FONT_H__
