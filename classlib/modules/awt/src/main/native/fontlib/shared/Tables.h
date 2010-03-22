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
#ifndef __TABLES_H__
#define __TABLES_H__


#include "TTCurve.h"
#include "TTFont.h"
#include "TypeDefinition.h"

#define CMAP_TABLE "cmap"   /* character to glyph mapping   */
#define GLYF_TABLE "glyf"   /* glyph data                   */
#define HEAD_TABLE "head"   /* font header                  */
#define HHEA_TABLE "hhea"   /* horizontal header            */
#define HMTX_TABLE "hmtx"   /* horizontal metrics           */
#define LOCA_TABLE "loca"   /* index to location            */
#define MAXP_TABLE "maxp"   /* maximum profile              */
#define NAME_TABLE "name"   /* naming                       */
#define POST_TABLE "post"   /* PostScript                   */
#define TTCF_TABLE "ttcf"   /* TrueType font collection     */
#define KERN_TABLE "kern"   /* Kerning table                */
#define BASE_TABLE "BASE"   /* Baseline table               */
#define OS2_TABLE  "OS/2"   /* OS-metrics table             */

#define WINDOWS_PLATFORM_ID 3   /* Windows platform identifier  */
#define FAMILY_NAME_ID 1        /* Family name identifier       */
#define SUBFAMILY_NAME_ID 2     /* Family name identifier       */
#define FULL_NAME_ID 4          /* Family name identifier       */
#define POSTSCRIPT_NAME_ID 6    /* Family name identifier       */

#define SYMBOL_ENCODING 0
#define UNICODE_ENCODING 1
#define SHIFT_JIS_ENCODING 2
#define BIG5_ENCODING 3
#define PRC_ENCODING 4

typedef flong Fixed;
typedef long long LONGDT;

enum
{
	ON_CURVE	= 0x01, // on curve or not
	REPEAT		= 0x08,	// next byte specifies the number of 
						//additional times this set of flags is to be repeated
    X_POSITIVE	= 0x12, // positive x-value
	X_NEGATIVE	= 0x02, // negative x-value
	X_SAME		= 0x10,	// x is same
	X_DWORD		= 0x00,	    

	Y_POSITIVE	= 0x24, // positive y-value
	Y_NEGATIVE	= 0x04, // negative y-value
	Y_SAME		= 0x20,	// y is same
	Y_DWORD		= 0x00,	    
};

/* From Win GDI 
typedef struct { 
  ufchar bFamilyType; 
  ufchar bSerifStyle; 
  ufchar bWeight; 
  ufchar bProportion; 
  ufchar bContrast; 
  ufchar bStrokeVariation; 
  ufchar bArmStyle; 
  ufchar bLetterform; 
  ufchar bMidline; 
  ufchar bXHeight; 
} PANOSE; */

typedef struct
{
    Fixed version;
    ufshort num_tables;
    ufshort search_range;
    ufshort entry_selector;
    ufshort range_shift;
} Table_Offset;

typedef struct
{
    fchar    tag[4];
    uflong   checkSum;
    uflong   offset;
    uflong   length;
} Table_Directory;

typedef struct
{
    ufshort platformID;
    ufshort encodingID;
    ufshort languageID;
    ufshort nameID;
    ufshort string_length;
    ufshort string_offset;
} Name_Entry;

typedef struct 
{
    ufshort format;
    ufshort num_name_records;
    ufshort storage_offset;
    Name_Entry name_record[1];
} Table_name; 

/* TrueType 'maxp' table */
typedef struct
{
    Fixed  version;
    ufshort numGlyphs; // number of Glyphs
    ufshort maxPoints;
    ufshort maxContours;
    ufshort maxCompositePoints;
    ufshort maxCompositeContours;
    ufshort maxZones;
    ufshort maxTwilightPoints;
    ufshort maxStorage;
    ufshort maxFunctionDefs;
    ufshort maxInstructionDefs;
    ufshort maxStackElements;
    ufshort maxSizeOfInstructions;
    ufshort maxComponentElements;
    ufshort maxComponentDepth;
} Table_maxp;

/* TrueType Font Header table */
typedef struct
{
    Fixed table_version;
    Fixed font_revision;
    uflong checksum_adjust;
    uflong magic_number;
    ufshort flags;
    ufshort units_per_EM;
	LONGDT created;
	LONGDT modified;
    fshort xMin;
    fshort yMin;
    fshort xMax;
    fshort yMax;
    ufshort mac_style;
    ufshort lowest_rec_PPEM;
    fshort font_direction;
    fshort index_to_loc_format;  // format of 'loca' table
    fshort glyph_data_format;
} Table_head;

typedef struct
{
    Fixed table_version;
	fshort ascender;	             /* typographic ascent */
	fshort descender;             /* typographic descent */
	fshort line_gap;              /* typographic line gap */
	ufshort advance_width_max;    /* Maximum advance width value in 'hmtx' table */
	fshort min_left_sidebearing;
	fshort min_right_sidebearing; /* Min(aw - lsb - (xMax - xMin)) */
	fshort xMaxExtent;            /* Max(lsb + (xMax - xMin)) */
	fshort caret_slope_rise;
	fshort caret_slope_run;
	fshort first_reserved;
	fshort second_reserved;
	fshort third_reserved;
	fshort fourth_reserved;
	fshort fifth_reserved;
    fshort metric_data_format;
	ufshort number_of_hMetrics;
} Table_hhea;

typedef struct
{
	ufshort table_version;
	fshort xAvgCharWidth;
	ufshort usWeightClass;
	ufshort usWidthClass;
	fshort fsType;
	fshort ySubscriptXSize;
	fshort ySubscriptYSize;
	fshort ySubscriptXOffset;
	fshort ySubscriptYOffset;
	fshort ySuperscriptXSize;
	fshort ySuperscriptYSize;
	fshort ySuperscriptXOffset;
	fshort ySuperscriptYOffset;
	fshort yStrikeoutSize;
	fshort yStrikeoutPosition;
	fshort sFamilyClass;
//	PANOSE panose;
	ufchar panose[10];
	uflong ulUnicodeRange1;
	uflong ulUnicodeRange2;
	uflong ulUnicodeRange3;
	uflong ulUnicodeRange4;
	ufchar achVendID[4];
	ufshort fsSelection;
	ufshort usFirstCharIndex;
	ufshort usLastCharIndex;
	ufshort sTypoAscender;
	ufshort sTypoDescender;
	ufshort sTypoLineGap;
	ufshort sWinAscent;
	ufshort sWinDescent;
	uflong ulCodePageRange1;
	uflong ulCodePageRange2;
} Table_os2;

typedef struct
{
	ufshort platform;	 //identifier of platform
	ufshort encodingID;	 //identifier fo encoding
	uflong  table_offset; //offset of the encoding table	
} Cmap_Entry;

typedef struct
{
	ufshort table_version;       // =0
    ufshort numSubTables;        //number subtables
	Cmap_Entry tableHeaders[1]; //headers of subtables
} Table_cmap;

typedef struct
{
    Fixed format; //format type
	Fixed italic_angle;
	fshort underlineOffset;
	fshort underlineThickness;
	uflong isFixedPitch;
	uflong minMemType42;
	uflong maxMemType42;
	uflong minMemType1;
	uflong maxMemType1;
} Table_post;

/* first part of the encoding table identical for all format of them */
typedef struct
{
	ufshort format;
	ufshort length; //length in bytes
	ufshort version;
} Table_encode_header;

/*
typedef struct
{
	ufshort format; // =0,2,4,6
	ufshort length; // size
	ufshort version;
	ufchar map[256];
} Table_encode_0;


typedef struct
{
//	ufshort segCountX2;       // 2 x segCount
	ufshort search_range;     // 2 x (2**floor(log_2(segCount)))
	ufshort entry_selector;   // log_2(search_range/2)
	ufshort range_shift;      // 2 x segCount - search_range
	ufshort end_count[1];     // end characterCode for each segment, last =0xFFFF, length = segCount
	ufshort reservedPad;      // = 0
	ufshort start_count[1];   // Start character code for each segment, length = segCount
	ufshort idDelta[1];       // Delta for all character codes in segment, length = segCount
    ufshort idRangeOffset[1]; // Offsets into glyphIdArray or 0, length = segCount
	ufshort glyphIdArray[];  // Glyph index array (arbitrary length)
} Table_encode_4;
*/

typedef struct
{
	fshort number_of_contours; // <0 for composite glyph
    fshort xMin;
	fshort yMin;
	fshort xMax;
	fshort yMax;
} Glyph_header;

template<fshort n> // n = number_of_contours from Glyph_header
struct SimpleGlyphDescription
{
    ufshort endPtsOfContours[n];
	ufshort instruction_length;
//	ufchar instructions[instruction_length];
//	ufchar flags[n];
//	ufchar(fshort) xCoordinates[n];
//	ufchar(fshort) yCoordinates[n];
};

fint parseCmapTable(FILE* tt_file, TableEncode* te);
fint parseNameTable(FILE* tt_file, fwchar_t** familyName, fwchar_t** psName, StyleName* fontStyle);
fint parseHeadTable(FILE* tt_file, ffloat* bbox, fshort* format, ufshort* unitsPerEm);
fint parseHheaTable(FILE* tt_file, ufshort* numOfHMetrics, ffloat* ascent, ffloat* descent, ffloat* lineGap);
fint parseMaxpTable(FILE* tt_file, ufshort *numGlyphs);
fint parseLocaTable(FILE* tt_file, GlyphOffsets* gOffsets, ufshort numGlyphs);
fint parseOs2Table(FILE* tt_file, ffloat* strikeOutSize, ffloat* strikeOutOffset);
fint parsePostTable(FILE* tt_file, fshort* uOffset, fshort* uThickness);
fint parseGlyphData(FILE* tt_file, const GlyphOffsets gO, ufshort numGlyphs, ufshort glyphIndex, TTCurve *curve, fshort* bRect, ffloat transform);
fint parseHmtxTable(FILE* tt_file, ufshort numOfHMetrics, HMetrics** hm);
bool isCompositeGlyph(FILE* tt_file, const GlyphOffsets gO, ufshort numGlyphs, ufshort glyphIndex);

#endif
