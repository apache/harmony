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
#include <stdio.h>
#include <memory.h>

#include "Tables.h"

static inline uflong dwReverse(uflong data)
{
    ufchar *dataElems = (ufchar *) &data;
    return (uflong)((dataElems[0]<<24) | (dataElems[1]<<16) | (dataElems[2]<<8) | dataElems[3]);
}

/* Reverses WORD bytes order */
static inline ufshort wReverse(ufshort data)
{
    return (ufshort)(((data<<8) & 0xFF00) | ((data>>8) & 0x00FF));
}

/* Reverses WORD bytes order */
static inline fshort wReverse(fshort data)
{
    return (fshort)(((data<<8) & 0xFF00) | ((data>>8) & 0x00FF));
}

/* Searching of table, 
	return TRUE if table founded */
bool searchTable(uflong table, uflong* offset, FILE* tt_file)
{
    Table_Offset tableOffset;
    Table_Directory tableDirectory;

    bool isFound = false;
    fint size;
    fint i;

    /* Open font file stream */
//	if( (tt_file = fopen( fPath,"rb")) == NULL ){
//		printf("Error opening font file");
//		return 0;
//  }

	size = (fint)fseek(tt_file,0,SEEK_SET);
	if (size != 0)
	{
#ifdef DEBUG
		printf("Error seeking table\n");
#endif 
		return 0;
	}

    size = (fint)fread(&tableOffset, sizeof(Table_Offset), 1, tt_file);
    if (size != 1){
#ifdef DEBUG
		printf("Error reading font file\n");
#endif
        return 0;
    }

    /* Reverse byte order */
    tableOffset.version = dwReverse(tableOffset.version);
    tableOffset.num_tables = wReverse(tableOffset.num_tables);
//    tableOffset.search_range = wReverse(tableOffset.search_range);
//    tableOffset.entry_selector = wReverse(tableOffset.entry_selector);

    /* check whether the version is 1.0 */
    if(tableOffset.version != 0x10000)
        return 0;

    /* look for 'head' table */
    for(i=0; i< tableOffset.num_tables; i++)
	{
		size = (fint)fread(&tableDirectory, sizeof(Table_Directory), 1, tt_file);
        if ( size != 1){
#ifdef DEBUG
            printf("Error reading Table Directory from file.");
#endif
            return 0;
        }
        if (* (uflong*)tableDirectory.tag == table){
            isFound = true;
//            tableDirectory.length = dwReverse(tableDirectory.length);
//            tableDirectory.offset = dwReverse(tableDirectory.offset);
            *offset = dwReverse(tableDirectory.offset);
            break;
        }
    }
	return isFound;
}

fint getTableEncode_4(FILE* tt_file, TableEncode* te, ufshort table_len)
{
	ufshort* tableEncode;
	ufshort length;
	fint size, i; 

	length = (table_len - sizeof(Table_encode_header))/sizeof(ufshort); // in USHORTs
	tableEncode = new ufshort[length];
	
	/* reading tail of the table */
	size = (fint)fread(tableEncode,sizeof(ufshort),length,tt_file);
	if(size != length)
	{
#ifdef DEBUG
		printf("Error reading table encode format 4 from 'cmap' table");
#endif
		delete[] tableEncode;
		tableEncode = NULL;
		return -1;
	}

	for(i=0;i<length;i++)
	{
		tableEncode[i]=wReverse(tableEncode[i]);
	}
    
	te->TableEncode = tableEncode; // pointer to tail of 'Table_encode' subtable (of 'cmap')
	return 0;
}

//#ifndef WIN32
static inline bool compare(fwchar_t* wstr, fchar* str)
{
//  fchar cstr[256];
//	wcstombs(cstr,wstr,256);
//	return !strcasecmp(cstr,str);

	fchar tmpstr[256];
	fwchar_t* tmpwstr = wstr;
    fint i = 0;
	for (; i<= fwcslen(tmpwstr); i++)
	{
        tmpstr[i] = (fchar)((*wstr) & 0xFF);
	}
	tmpstr[i]=0;

	return !strcmp(tmpstr,str);
}
//#endif

fint parseNameTable(FILE* tt_file, fwchar_t** familyName, fwchar_t** psName, StyleName* fontStyle)
{
    uflong dwTable = *(uflong*)NAME_TABLE;
    uflong offset;
    Table_name tableName;
    Name_Entry nameRecord;
    flong curPos;
	ufshort *subFamilyName;

    fint i, j;
    fint size;
	bool inFamilyNameCase = false, inSubfamilyNameCase = false, inPSNameCase = false;

    if (searchTable(dwTable, &offset, tt_file))
	{
        /* move position to the 'name' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
#ifdef DEBUG
            printf("Error executing fseek() for 'name' table.");
#endif
            return -1;
        }

		/* read 'name' table header */
        size = (fint)fread(&tableName, sizeof(Table_name) - sizeof(Name_Entry), 1, tt_file);
        if (size != 1){
#ifdef DEBUG
            printf("Error reading Table 'Name' from file.");
#endif
            return -1;
        }

        tableName.num_name_records = wReverse(tableName.num_name_records);
        tableName.storage_offset = wReverse(tableName.storage_offset);

        /* enumerating NameRecords and finding Family Name value */
        for(i=0; i < tableName.num_name_records; i++)
		{
            size = (fint)fread(&nameRecord, sizeof(Name_Entry), 1, tt_file);
            if (size != 1)
			{
#ifdef DEBUG
                printf("Error reading Name Record from file.");
#endif
                return -1;
            }

            nameRecord.nameID = wReverse(nameRecord.nameID);
			nameRecord.platformID = wReverse(nameRecord.platformID);
            if(nameRecord.platformID == WINDOWS_PLATFORM_ID)
				switch (nameRecord.nameID)
				{
				case FAMILY_NAME_ID:
					if (familyName != NULL && !inFamilyNameCase)
					{
						nameRecord.string_length = wReverse(nameRecord.string_length);
		                nameRecord.string_offset = wReverse(nameRecord.string_offset);

						/* Save current position if someting wrong with family name */
						curPos = ftell(tt_file);
						size = fseek(   tt_file,
							    offset + tableName.storage_offset + nameRecord.string_offset,
								SEEK_SET);
				        if (size != 0){
#ifdef DEBUG
				            printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}


//		                ZeroMemory(&fontFamilyName, nameRecord.string_length/2 + sizeof(ufshort));

						ufshort *fontFamilyName = new ufshort[nameRecord.string_length/2+1];

//						ZeroMemory(&(fontName[nameRecord.string_length]),1);

						size = (fint)fread(fontFamilyName, sizeof(ufshort), nameRecord.string_length/2, tt_file);
						if (size != nameRecord.string_length/2)
						{
#ifdef DEBUG
						    printf("Error reading Family Name from file.");
#endif
							delete[] fontFamilyName;
							fontFamilyName = NULL;
							return -1;
						}

						for(j=0; j < nameRecord.string_length/2; j++)
						{
							(fontFamilyName)[j] = wReverse((fontFamilyName)[j]);
//printf("%c",(fchar)(fontFamilyName)[j]);
						}
//printf("\n");
						(fontFamilyName)[j] = 0;
						inFamilyNameCase = true;

//#ifdef WIN32
						*familyName = (fwchar_t*)fontFamilyName;
/*#else
//TODO: To unify this cycle and previous
						*familyName = new fwchar_t[nameRecord.string_length/2+1];

						for(j=0; j < nameRecord.string_length/2+1; j++)
						{
                            (*familyName)[j] = (fwchar_t)fontFamilyName[j];
						}
						delete fontFamilyName;
#endif*/

						size = fseek( tt_file, curPos, SEEK_SET);
				        if (size != 0){
#ifdef DEBUG
							printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}
					}
					break;
				case POSTSCRIPT_NAME_ID:
					if (psName != NULL && !inPSNameCase)
					{
						nameRecord.string_length = wReverse(nameRecord.string_length);
		                nameRecord.string_offset = wReverse(nameRecord.string_offset);

						/* Save current position if someting wrong with postscript name */
						curPos = ftell(tt_file);
						size = fseek(   tt_file,
							    offset + tableName.storage_offset + nameRecord.string_offset,
								SEEK_SET);
				        if (size != 0){
#ifdef DEBUG
				            printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}

						ufshort *fontPSName = new ufshort[nameRecord.string_length/2+1];

						size = (fint)fread(fontPSName, sizeof(ufshort), nameRecord.string_length/2, tt_file);
						if (size != nameRecord.string_length/2)
						{
#ifdef DEBUG
						    printf("Error reading PostScript Name from file.");
#endif
							delete[] fontPSName;
							fontPSName = NULL;
							return -1;
						}

						for(j=0; j < nameRecord.string_length/2; j++)
						{
							(fontPSName)[j] = wReverse((fontPSName)[j]);
						}
						(fontPSName)[j] = 0;
						inPSNameCase = true;

//#ifdef WIN32
						*psName = (fwchar_t*)fontPSName;
/*#else

						*psName = new fwchar_t[nameRecord.string_length/2+1];
//TODO: To unify this cycle and previous
						for(j=0; j < nameRecord.string_length/2+1; j++)
						{
                            (*psName)[j] = (fwchar_t)fontPSName[j];
						}
						delete fontPSName;
#endif*/

						size = fseek( tt_file, curPos, SEEK_SET);
				        if (size != 0){
#ifdef DEBUG 
							printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}
					}
					break;
				case SUBFAMILY_NAME_ID:
					if(fontStyle != NULL && !inSubfamilyNameCase)
					{
						nameRecord.string_length = wReverse(nameRecord.string_length);
						nameRecord.string_offset = wReverse(nameRecord.string_offset);

					/* Save current position if someting wrong with subfamily name */
						curPos = ftell(tt_file);
						size = fseek(   tt_file,
							    offset + tableName.storage_offset + nameRecord.string_offset,
								SEEK_SET);
					    if (size != 0){
#ifdef DEBUG
						    printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}

//					    ZeroMemory(&fontName, nameRecord.string_length + sizeof(ufshort));

						subFamilyName = new ufshort[nameRecord.string_length/2+1];

//						ZeroMemory(&(fontName[nameRecord.string_length]),1);

						size = (fint)fread(subFamilyName, sizeof(ufshort), nameRecord.string_length/2, tt_file);
					    if (size != nameRecord.string_length/2)
						{
#ifdef DEBUG
						    printf("Error reading SubFamily Name from file.");
#endif
							delete[] subFamilyName;
							subFamilyName = NULL;
							return -1;
						}

						for(j=0; j < nameRecord.string_length/2; j++)
						{
							subFamilyName[j] = wReverse(subFamilyName[j]);
						}
						subFamilyName[j] = 0;

#define COMPARE_IT		(!fwcscmp((ufshort *)subFamilyName,"Italic") || !fwcscmp((ufshort *)subFamilyName,"Oblique"))
#define COMPARE_BD		(!fwcscmp((ufshort *)subFamilyName,"Bold"))
#define COMPARE_BDIT	(!fwcscmp((ufshort *)subFamilyName,"Bold Italic") || !fwcscmp((ufshort *)subFamilyName,"Bold Oblique"))
#define COMPARE_REG		(!fwcscmp((ufshort *)subFamilyName,"Regular") || !fwcscmp((fwchar_t *)subFamilyName,"Normal") || !fwcscmp((fwchar_t *)subFamilyName,"Book"))

						if COMPARE_IT
						{
							*fontStyle = Italic;
							inSubfamilyNameCase = true;
						}
						else if COMPARE_BD
						{
							*fontStyle = Bold;
							inSubfamilyNameCase = true;
						}
						else if COMPARE_BDIT
						{
							*fontStyle = BoldItalic;
							inSubfamilyNameCase = true;
						}
						else if COMPARE_REG
						{
							*fontStyle = Regular;
							inSubfamilyNameCase = true;
						}

						delete[] subFamilyName;
						subFamilyName = NULL;

						size = fseek( tt_file, curPos, SEEK_SET);
				        if (size != 0){
#ifdef DEBUG
				            printf("Error executing fseek() for 'name' table.");
#endif
							return -1;
						}

					}

				}
			}
		}


     /* Close font file stream */
/*     if( fclose( tt_file ) ){
        printf("Error closing TrueType font file.");
        return 0;
     }
*/
//	fclose(tt_file);
	
	if  (!inSubfamilyNameCase)
		fontStyle = NULL;
	return 0;
}

fint parseHeadTable(FILE* tt_file, ffloat* bbox, fshort* format, ufshort* unitsPerEm)
{
    uflong hTable = *(uflong*)HEAD_TABLE;
	uflong offset;
    Table_head tableHead;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'head' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
#ifdef DEBUG
            printf("Error executing fseek() for 'head' table.");
#endif
            return -1;
        }

		/* read 'head' table header */
        size = (fint)fread(&tableHead, sizeof(Table_head), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'Head' from file.");
            return -1;
        }

		bbox[0] = wReverse(tableHead.xMin);
		bbox[1] = wReverse(tableHead.yMin);
		bbox[2] = wReverse(tableHead.xMax);//for wingding must be -432(?)
		bbox[3] = wReverse(tableHead.yMax);

		tableHead.index_to_loc_format=wReverse(tableHead.index_to_loc_format);
		if (wReverse(tableHead.glyph_data_format))
		{
//		if (tableHead.index_to_loc_format == 1 || tableHead.index_to_loc_format == 0)
            *format = !tableHead.index_to_loc_format;
		}
		else 
		{
			*format = tableHead.index_to_loc_format;
//			printf("debug information: see 'loca' table format!!!\n");
		}

		*unitsPerEm = wReverse(tableHead.units_per_EM);
	}

// ******* Validating data *********

//	HDC hDC = GetDC(NULL);
//	HDC hDC = GetDC(NULL);
//	Table_head tableHead1;
//	uflong tbl = (uflong)"head";
//	fint m = GetFontData(hDC,tbl,(uflong)offset,&tableHead1,sizeof(Table_head));

// ******* Validating data *********
//	return *bbox;
//	fclose(tt_file);
	return 0;
}

fint parseMaxpTable(FILE* tt_file, ufshort* numGlyphs)
{
    uflong hTable = *(uflong*)MAXP_TABLE;
	uflong offset;
    Table_maxp tableMaxp;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'maxp' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'maxp' table.");
            return -1;
        }

		/* read 'maxp' table header */
        size = (fint)fread(&tableMaxp, sizeof(Table_maxp), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'maxp' from file.");
            return -1;
        }
	}else 
	{
		return -1;
	}

	*numGlyphs = wReverse(tableMaxp.numGlyphs);
	tableMaxp.maxPoints = wReverse(tableMaxp.maxPoints);
	tableMaxp.maxContours = wReverse(tableMaxp.maxContours);
	tableMaxp.maxCompositePoints = wReverse(tableMaxp.maxCompositeContours); //must be 141 for wingding(?)
	tableMaxp.maxCompositeContours = wReverse(tableMaxp.maxCompositeContours);
	tableMaxp.maxStackElements = wReverse(tableMaxp.maxStackElements);
	tableMaxp.maxSizeOfInstructions = wReverse(tableMaxp.maxSizeOfInstructions);

//	fclose(tt_file);
	return 0;
}

fint parseHheaTable(FILE* tt_file, ufshort* numOfHMetrics, ffloat* ascent, ffloat* descent, ffloat* lineGap)
{
    uflong hTable = *(uflong*)HHEA_TABLE;
	uflong offset;
    Table_hhea tableHhea;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'hhea' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'hhea' table.");
            return -1;
        }

	/* read 'hhea' table header */
        size = (fint)fread(&tableHhea, sizeof(Table_hhea), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'hhea' from file.");
            return -1;
        }
	}else
	{
		return -1;
	}

	*numOfHMetrics = wReverse(tableHhea.number_of_hMetrics);
	*ascent = wReverse(tableHhea.ascender);
	*descent = wReverse(tableHhea.descender);
	*lineGap = wReverse(tableHhea.line_gap);

	return 0;
}

fint parseOs2Table(FILE* tt_file, ffloat* strikeOutSize, ffloat* strikeOutOffset)
{
    uflong hTable = *(uflong*)OS2_TABLE;
	uflong offset;
    Table_os2 tableOs2;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'OS/2' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'OS/2' table.");
            return -1;
        }

	/* read 'OS/2' table header */
        size = (fint)fread(&tableOs2, sizeof(Table_os2), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'OS/2' from file.");
            return -1;
        }
	} else
	{
		return -1;
	}

//	tableOs2.usFirstCharIndex = wReverse(tableOs2.usFirstCharIndex);
//	*ascent = wReverse(tableOs2.sTypoAscender);
//	*descent = wReverse(tableOs2.sTypoDescender);
	*strikeOutSize = wReverse(tableOs2.yStrikeoutSize);
	*strikeOutOffset = wReverse(tableOs2.yStrikeoutPosition);
//	fclose(tt_file);

	return 0;
}

fint parsePostTable(FILE* tt_file, fshort* uOffset, fshort* uThickness)
{
    uflong hTable = *(uflong*)POST_TABLE;
	uflong offset;
    Table_post tablePost;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'post' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'post' table.");
            return -1;
        }

	/* read 'post' table header */
        size = (fint)fread(&tablePost, sizeof(Table_post), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'post' from file.");
            return -1;
        }
	} else
	{
		return -1;
	}

	*uOffset = wReverse(tablePost.underlineOffset);
	*uThickness = wReverse(tablePost.underlineThickness);

	return 0;
}

fint parseHmtxTable(FILE* tt_file, ufshort numOfHMetrics, HMetrics** hm)
{
    uflong hTable = *(uflong*)HMTX_TABLE;
	uflong offset;

    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'hmtx' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'OS/2' table.");
            return -1;
        }
		/* read 'hmtx' table */
        *hm = new HMetrics[numOfHMetrics];

        size = (fint)fread(*hm, sizeof(HMetrics), numOfHMetrics, tt_file);
        if (size != numOfHMetrics){
			delete[] *hm;
//            printf("Error reading Table 'hmtx' from file.");
            return -1;
        }

		for (fint i=0; i<numOfHMetrics;i++)
		{
			(*hm)[i].adwance_width = wReverse((*hm)[i].adwance_width);
		}

	} else
	{
		return -1;
	}

	return 0;
}

fint parseLocaTable(FILE* tt_file, GlyphOffsets* gOffsets, ufshort numGlyphs)
{
	uflong hTable = *(uflong*)LOCA_TABLE;
	uflong gTable = *(uflong*)GLYF_TABLE;
	uflong offset,localGlyfOffset;

	uflong *gLongOffsets = NULL;
	ufshort *gShortOffsets = NULL;
    fint size, i;

    if (searchTable(hTable, &offset, tt_file) && searchTable(gTable,&localGlyfOffset,tt_file))
	{
        gLongOffsets = new uflong[numGlyphs+1];
        if (!gOffsets->format)
            gShortOffsets = new ufshort[numGlyphs+1];

        /* move position to the 'loca' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0)
		{
//            printf("Error executing fseek() for 'loca' table.");

			delete[] gLongOffsets;
			gLongOffsets = NULL;

			if (!gOffsets->format)
			{
				delete[] gShortOffsets;
			}
            return -1;
        }

		/* read 'loca' table */
		if (gOffsets->format)
		{
			size = (fint)fread(gLongOffsets, sizeof(flong),numGlyphs+1, tt_file);
			if (size != numGlyphs+1)
			{
//				printf("Error reading Table 'loca' from file.");
				delete[] gLongOffsets;
				gLongOffsets = NULL;
				return -1;
			}
			for (i=0; i<=numGlyphs; i++)
				gLongOffsets[i] = dwReverse(gLongOffsets[i])+localGlyfOffset;
		}else
		{
			size = (fint)fread(gShortOffsets, sizeof(fshort), numGlyphs+1, tt_file);
			if (size != numGlyphs+1)
			{
//				printf("Error reading Table 'loca' from file.");
				delete[] gShortOffsets;
				gShortOffsets = NULL;
				delete[] gLongOffsets;
				gLongOffsets = NULL;
				return -1;
			}
			for (i=0;i<=numGlyphs;i++)
				gLongOffsets[i] = wReverse(gShortOffsets[i])*2+localGlyfOffset;
		}
		gOffsets->offsets = gLongOffsets;
		delete[] gShortOffsets;
	}

//	fclose(tt_file);

	return 0;
};

fint parseCmapTable(FILE* tt_file, TableEncode* te)
{
	uflong hTable = *(uflong*)CMAP_TABLE;
	uflong offset;
    Table_cmap tableCmap;
	Cmap_Entry cmapRecord;
	Table_encode_header tableEncodeHeader;
	flong curPos;

    fint i;
    fint size;

    if (searchTable(hTable, &offset, tt_file))
	{
        /* move position to the 'cmap' table */
        size = fseek(tt_file, offset, SEEK_SET);
        if (size != 0){
//            printf("Error executing fseek() for 'cmap' table.");
            return -1;
        }

		/* read 'cmap' table header */
        size = (fint)fread(&tableCmap, sizeof(Table_cmap) - sizeof(Cmap_Entry), 1, tt_file);
        if (size != 1){
//            printf("Error reading Table 'cmap' from file.");
            return -1;
        }
        
		tableCmap.numSubTables = wReverse(tableCmap.numSubTables);

		for(i=0; i < tableCmap.numSubTables; i++)
		{
            size = (fint)fread(&cmapRecord, sizeof(Cmap_Entry), 1, tt_file);
            if (size != 1)
			{
//                printf("Error reading cmap Record from file.");
                return -1;
            }

			cmapRecord.encodingID = wReverse(cmapRecord.encodingID);
			cmapRecord.platform = wReverse(cmapRecord.platform);

            if(cmapRecord.platform == WINDOWS_PLATFORM_ID)
			{
				switch (cmapRecord.encodingID)
				{
				case UNICODE_ENCODING:
					cmapRecord.table_offset = dwReverse(cmapRecord.table_offset);

					/* Save current position if someting wrong with family name */
					curPos = ftell(tt_file);
					size = fseek(   tt_file, offset + cmapRecord.table_offset, SEEK_SET);
			        if (size != 0){
//			            printf("Error executing fseek() for 'cmap' table.");
			            return -1;
					}


					size = (fint)fread(&tableEncodeHeader, sizeof(Table_encode_header), 1, tt_file);
					if (size != 1)
					{
//						printf("Error reading Table Encode from file.");
						return -1;
					}

					tableEncodeHeader.format	= wReverse(tableEncodeHeader.format);
					tableEncodeHeader.length	= wReverse(tableEncodeHeader.length);
					tableEncodeHeader.version	= wReverse(tableEncodeHeader.version);

					if (tableEncodeHeader.format == 0 && te->TableEncode == NULL) 
					{
						ufchar *map = new ufchar[256];
						te->format = 0;
                        size = (fint)fread(map, sizeof(ufchar), 256, tt_file);
						if (size != 256)
						{
//							printf("Error reading map format 0");
							delete[] map;
							map = NULL;
						} else
						{
							te->TableEncode = map;
						}

					}else if (tableEncodeHeader.format == 4 && te->TableEncode == NULL)
					{
						te->format = 4;
						getTableEncode_4(tt_file, te, tableEncodeHeader.length);
					}
					size = fseek( tt_file, curPos, SEEK_SET);
			        if (size != 0){
//			            printf("Error executing fseek() for 'name' table.");
						return -1;
					}

					break;
				}
			}
		}
	}

//	fclose(tt_file);
	return 0;
}

fint parseGlyphData(FILE* tt_file, const GlyphOffsets gO, ufshort numGlyphs, ufshort glyphIndex, TTCurve *curve, fshort* bRect, ffloat transform)
{
	uflong offset;
	Glyph_header glyphHeader;
	fshort numOfContours;
	ufshort *endPtsOfContours = NULL; 
	ufshort instructionLength;//instruction length in bytes
	ufchar* instructions = NULL;
	ufchar* flags = NULL;
	ufchar* xCoord = NULL; //pointer to array of X coordinates
	ufchar* yCoord = NULL; //pointer to array of Y coordinates
	ufchar* tmp = NULL;

	fint numPoints; // number of Points
	fint size, i, j, curLen;
	fint flagIndex = 0; 
	fint xCoordIndex = 0;
	fint yCoordIndex = 0;
	fint rep = 0;

	fshort xLength = 0; //length of array of X coordinates
	fshort yLength = 0; //length of array of Y coordinates
	ufchar curFlag = 0;

	if (glyphIndex >= numGlyphs)
	{
//		printf("debug info: glyphIndex out of range");
		glyphIndex = 0;
	}

	offset = gO.offsets[glyphIndex];
	if (offset == gO.offsets[glyphIndex+1])
	{
		curve->add(0,0, 1);
		return 0;
	}

	size = fseek(tt_file, offset, SEEK_SET);
	if (size != 0){
//		printf("Error executing fseek() for someone glyph.");
		return -1;
	}

	/* read 'Glyph_header' table */
	size = (fint)fread(&glyphHeader, sizeof(Glyph_header), 1, tt_file);
	if (size != 1){
//		printf("Error reading 'Glyph_header' table from file.");
		return -1;
	}

	glyphHeader.number_of_contours = wReverse(glyphHeader.number_of_contours);
	numOfContours = glyphHeader.number_of_contours;
	bRect[0] = (short)(wReverse(glyphHeader.xMin)*transform);
	bRect[1] = (short)(wReverse(glyphHeader.yMin)*transform);
	bRect[2] = (short)(wReverse(glyphHeader.xMax)*transform);
	bRect[3] = (short)(wReverse(glyphHeader.yMax)*transform);
	
	if (numOfContours > 0)
	{
		endPtsOfContours = new ufshort[numOfContours];
		size = (fint)fread(endPtsOfContours, sizeof(fshort),numOfContours,tt_file);
		if (size != numOfContours)
		{
//			printf("Error reading endPtsOfContours for someone glyph.");
			delete[] endPtsOfContours;
			return -1;
		}

		for (i=0; i<numOfContours; i++)
		{
			endPtsOfContours[i] = wReverse(endPtsOfContours[i]);
		}
		numPoints = endPtsOfContours[i-1] + 1;

		size = (fint)fread(&instructionLength,sizeof(fshort),1,tt_file);
		if (size != 1)
		{
//			printf("Error reading length of instructions./n");
			delete[] endPtsOfContours;
			return -1;
		}
		instructionLength = wReverse(instructionLength);

		instructions = new ufchar[instructionLength];
		size = (fint)fread(instructions,sizeof(ufchar),instructionLength,tt_file);
		if (size != instructionLength)
		{
//			printf("Error reading instructions./n");
			delete[] instructions;
			delete[] endPtsOfContours;
			return -1;
		}

		curLen=0;
		flags = NULL;

		for (i=0; i<numPoints; i++)
		{
            fint x_repeat = 0;
			fint y_repeat = 0;

			tmp = flags;
			curLen++;
			flags = new ufchar[curLen];
			memcpy(flags,tmp,curLen-1);
			delete[] tmp;
			size = (fint)fread(&(flags[curLen-1]),sizeof(ufchar),1,tt_file);
			if (size != 1)
			{
//                printf("Error reading array of flags from font file.\n");
				delete[] flags; 
				delete[] instructions;
				delete[] endPtsOfContours;
				return -1;
			}
            
			if ((flags[curLen-1] & X_POSITIVE) == X_POSITIVE ||
				(flags[curLen-1] & X_POSITIVE) == X_NEGATIVE)	
				x_repeat = 1;
			else if ((flags[curLen-1] & X_POSITIVE) == X_DWORD)
				x_repeat = 2;

			if ((flags[curLen-1] & Y_POSITIVE) == Y_POSITIVE ||
				(flags[curLen-1] & Y_POSITIVE) == Y_NEGATIVE)	
				y_repeat = 1;
			else if ((flags[curLen-1] & Y_POSITIVE) == Y_DWORD)
				y_repeat = 2;

			if(flags[curLen-1] & REPEAT)
			{
                tmp = flags;
				curLen++;
				flags = new ufchar[curLen];
				memcpy(flags,tmp,curLen-1);
				delete[] tmp;
				size=(fint)fread(&(flags[curLen-1]),sizeof(ufchar),1,tt_file);
				if (size != 1)
				{
//					printf("Error reading array of flags from font file.\n");
					delete[] flags;
					delete[] instructions;
					delete[] endPtsOfContours;
					return -1;
				}

                xLength += x_repeat*(flags[curLen-1] + 1);
				yLength += y_repeat*(flags[curLen-1] + 1);

				i += flags[curLen-1];

			} else
				xLength += x_repeat;
				yLength += y_repeat;
		}

        xCoord = new ufchar[xLength];
		yCoord = new ufchar[yLength];

		size = (fint)fread(xCoord,sizeof(ufchar),xLength,tt_file);
		if (size != xLength)
		{
//			printf("Error reading x-coordinate of glyph's point.\n");
			delete[] flags;
			delete[] instructions;
			delete[] endPtsOfContours;
            delete[] xCoord;
            delete[] yCoord;
			return -1;
		}

		size = (fint)fread(yCoord,sizeof(ufchar),yLength,tt_file);
		if (size != yLength)
		{
//			printf("Error reading coordinates of glyph points.\n");
			delete[] flags;
			delete[] instructions;
			delete[] endPtsOfContours;
            delete[] xCoord;
            delete[] yCoord;
			return -1;
		}
		
		i=0;
		rep=0;
		fint x=0, y=0;
		ffloat xFirstInContour,yFirstInContour;
		bool contBegin;

		for (j=0; j<numOfContours;j++)
		{
			fint repLim = endPtsOfContours[j];
			contBegin = 1;
			
			while(i<=repLim)
			{
                if (rep==0)
				{
					curFlag = flags[flagIndex];
					flagIndex++;
					rep = 1;

					if (curFlag & REPEAT)
					{
						rep += flags[flagIndex];
						flagIndex++;
					}
				}

				fint xChange = 0, yChange = 0;

				if ((curFlag & X_POSITIVE) == X_POSITIVE)
				{
					xChange = xCoord[xCoordIndex]; 
					xCoordIndex++;
				}else if ((curFlag & X_POSITIVE) == X_NEGATIVE)
				{
					xChange = -xCoord[xCoordIndex]; 
					xCoordIndex++;
				}else if ((curFlag & X_POSITIVE) == X_DWORD)
				{
					xChange = (fshort)((xCoord[xCoordIndex]<<8)+xCoord[xCoordIndex+1]);
					xCoordIndex+=2;
				}

				if ((curFlag & Y_POSITIVE) == Y_POSITIVE)
				{
					yChange = yCoord[yCoordIndex]; 
					yCoordIndex++;
				}else if ((curFlag & Y_POSITIVE) == Y_NEGATIVE)
				{
					yChange = -yCoord[yCoordIndex]; 
					yCoordIndex++;
				}else if ((curFlag & Y_POSITIVE) == Y_DWORD)
				{
					yChange = (fshort)((yCoord[yCoordIndex]<<8)+yCoord[yCoordIndex+1]);
					yCoordIndex+=2;
				}

				if((flagIndex >1) && !(curFlag & ON_CURVE ) && 
					((rep == 1) ? (!(flags[flagIndex-2] & ON_CURVE)) : (!(flags[flagIndex-3] & ON_CURVE))))
					curve->add((x+xChange/2)*transform,(y+yChange/2)*transform,FLAG_ONCURVE);


				x+=xChange;
				y+=yChange;

				if (contBegin)
				{
					curve->add(x*transform,y*transform, 1);
					xFirstInContour = x*transform;
					yFirstInContour = y*transform;

					contBegin = 0;
				} else 
                    curve->add(x*transform,y*transform, curFlag & ON_CURVE ? FLAG_ONCURVE : 0);

				rep--;
				i++;
			}
			curve->add(xFirstInContour,yFirstInContour,FLAG_ONCURVE);
		}
	}


	delete[] xCoord;
	delete[] yCoord;
	delete[] flags;
	delete[] endPtsOfContours;
	delete[] instructions;

	return 0;
}

/* Should be removed when the composite glyph parsing will be realized */
bool isCompositeGlyph(FILE* tt_file, const GlyphOffsets gO, ufshort numGlyphs, ufshort glyphIndex)
{
	uflong offset;
	Glyph_header glyphHeader;

	fint size;

	if (glyphIndex >= numGlyphs) 
		glyphIndex = 0;

	offset = gO.offsets[glyphIndex];
	if (offset == gO.offsets[glyphIndex+1])
		return false;

/*printf("glyphIndex = %d\n",glyphIndex);
if (tt_file == NULL)
printf("file is NULL\n");
else
printf("file isn't NULL\n");*/
	size = fseek(tt_file, offset, SEEK_SET);
	if (size != 0)
{
//printf("return false\n");
		return false;
}
//printf("second return\n");
	/* read 'Glyph_header' table */
	size = (fint)fread(&glyphHeader, sizeof(Glyph_header), 1, tt_file);
	if (size != 1)
		return false;
//printf("third return\n");
	return wReverse(glyphHeader.number_of_contours) < 0;

}
