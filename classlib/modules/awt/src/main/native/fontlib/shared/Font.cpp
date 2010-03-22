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
#include "Font.h"
#include "Environment.h"
#include "TTFont.h"
#include "T1Font.h"
#include <string.h>

Font::Font() {
    _famName = NULL;
}

Font::~Font() {
	for( std::map<const uflong, Glyph*>::iterator iter = _glyphMap.begin(); iter != _glyphMap.end(); iter++ ) {		
		delete iter->second;		
	}

	delete[] _famName;

	/*ufshort famLength = fwcslen((fwchar_t *)_famName);
	fchar *family = new fchar[famLength+1];

	ufshort i;
	for (i = 0;i < famLength;i++) {
		(family)[i] = _famName[i];
	}
	(family)[i] = '\0';

	FontHeader* fh = GraphicsEnvironment::getAllFonts()->_head;
	//printf("\nfaund = -%s-\n", family);
	for(fint i=0; i<GraphicsEnvironment::_length; i++){
		//printf("font = -%s-\n", fh->_familyName);
		if (strcmp(fh->_familyName,family)==0 && fh->_style == _style) {
			fh->_font = NULL;
			break;
		}

		fh=fh->_nextHeader;
	}

	delete[] family;*/
}

Glyph* Font::createGlyph(ufshort unicode, ufshort size){
	return NULL; 
}

fwchar_t* Font::getPSName()
{
	return NULL;
}

ffloat* Font::getLineMetrics()
{
	return NULL;
}

fint	Font::getMissingGlyphCode()
{
	return 0;
}

bool Font::canDisplay(ufshort c)
{
	return 0;
}

ufshort Font::getUnicodeByIndex(ufshort ind)
{
	return 0;
}

//unicode = 0 - default glyph
Glyph* Font::getGlyph(ufshort unicode, ufshort size) {
	uflong id;
    
	//printf("unicode = %lu, size = %lu\n", unicode,size);

    if (!canDisplay(unicode)) {
		id = (uflong)(size << 16);
        unicode = 0;
    } else {
		id = (uflong)(size << 16) + unicode;
    }	

    //printf("unicode = %lu, size = %lu, id = %lu\n", unicode,size,id);

	std::map<const uflong, Glyph*>::iterator iter = _glyphMap.find(id);	
	if (iter != _glyphMap.end()) {
//printf("return the glyph");
		return (Glyph *)(*iter).second;	
	}
//printf("creation of the glyph");
	Glyph *glyph = createGlyph(unicode, size);
	
	_glyphMap[id] = glyph;

	return glyph;	
}

// Creation of font depending on font type
Font* createFont(fchar* family, StyleName sn) {
	fint nLen = (fint) strlen(family);
	fwchar_t* name = new fwchar_t[nLen+1];
	for (fint i = 0; i <= nLen; i++)
		name[i] = family[i];

	Font* retFont = createFont(name, sn);

	delete[] name;

	return retFont;
}

// Creation of font depending on font type
Font* createFont(fwchar_t* family, StyleName sn) 
{
	Font* retFont;
	bool isFound = false;

	if (Environment::getAllFonts() == NULL)	return NULL;

	for(FontHeader* fh = Environment::getAllFonts();
		fh != NULL; fh=fh->_nextHeader)
	{

		if (fwcscmp(fh->_familyName,family)==0 && fh->_style == sn)
		{

			switch(fh->_fType)
			{
				case TrueType:
						retFont = new TTFont(fh->_filePath);
						fh->_font=retFont;

					break;
				case Type1:					
						retFont = new T1Font(family,sn,fh->_filePath);
						fh->_font=retFont;

					break;
			}
			isFound = true;
			break;
		}
	}
	if (!isFound)
	{
//		printf("Font not found");
		return 0; //Font not found
	}    
	return retFont;
}
