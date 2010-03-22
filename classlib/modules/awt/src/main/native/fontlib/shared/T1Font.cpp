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

#include <stdio.h>
#include <string>
#include <string.h>
#include <stdlib.h>
#if defined(LINUX)
#include <ctype.h>
#endif
#include "T1Font.h"
#include "T1Glyph.h"

T1Font::T1Font(fwchar_t *family, StyleName sn, fchar* pathToFile):Font() {
	//this->_famName = family;
	_style = sn;   

    fullName = NULL;

	FILE *inputFile;

	if( inputFile = fopen(pathToFile, "rb")) {	

		try {
			initFont(inputFile);
		} catch (fchar*) {
			//printf("%s", str);
		} 
		
		fclose(inputFile);	

        //set default ascent and descent
        _ascent = 649;
        _descent = 195;


        fchar path[MAX_STR_LENGTH];
        size_t length = strlen(pathToFile) - 3;

        strncpy(path, pathToFile, length);
        strcpy(path + length, "afm");        

        if( inputFile = fopen(path, "rb")) {

		    try {
			    parseAFM(inputFile);
		    } catch (...) {
			    //printf("%s", str);
		    } 
    		
		    fclose(inputFile);
	    }

	}

}

T1Font::~T1Font(void) {

	for( Type1Map::iterator iter = subrsMap.begin(); iter != subrsMap.end(); iter++ ) {		
		delete iter->second;		
	}

	for( Type1Map::iterator iter = charStringMap.begin(); iter != charStringMap.end(); iter++ ) {		
		delete iter->second;		
	}

    /*for( Type1AFMMap::iterator iter = afmMap.begin(); iter != afmMap.end(); iter++ ) {		
		delete[] iter->second;		
	}*/

    delete fullName;
}

Glyph* T1Font::createGlyph(ufshort unicode, ufshort size) {    

    /*ffloat floatMas[4];
    
    Type1AFMMap::iterator iter = afmMap.find(unicode); 

    //return iter == glyphCodeMap.end()? NULL : (ufshort)iter->second;

    memcpy(floatMas, _boundingBox, 4 * sizeof(ffloat));    

    if (iter != afmMap.end()) {
        //printf("%s\n", iter->second);
        fchar* curValue = strstr( iter->second, " B ");
        if (curValue != NULL) {

            curValue += 3; 

            floatMas[0] = (ffloat) atof(curValue);
            //printf("0 = %f\n", floatMas[0]);

            while (*(++curValue) != ' ') {
			}

            floatMas[1] = (ffloat) atof(curValue);
            //printf("1 = %f\n", floatMas[1]);

            while (*(++curValue) != ' ') {
			}

            floatMas[2] = (ffloat) atof(curValue);           
            //printf("2 = %f\n", floatMas[2]);

            while (*(++curValue) != ' ') {
			}

            floatMas[3] = (ffloat) atof(curValue);
            //printf("3 = %f\n", floatMas[3]);
        }
    }*/

    Glyph *glyph = new T1Glyph(&(this->charStringMap), &(this->subrsMap), unicode, size, size / matrixScale, _boundingBox);
	
	return glyph;
}

//TODO: owerwrite this:
fwchar_t* T1Font::getPSName() {
	return fullName;
}

ffloat* T1Font::getLineMetrics() {
    /*
     * metrics[0] - ascent<p>
     * metrics[1] - descent<p>
     * metrics[2] - external leading<p>
     * metrics[3] - underline thickness<p>
     * metrics[4] - underline offset<p>
     * metrics[5] - strikethrough thickness<p>
     * metrics[6] - strikethrough offset<p>
     * metrics[7] - maximum fchar width<p>*/

    ffloat* floatMas = new ffloat[FONT_METRICS_QUANTITY];

    floatMas[0] = _ascent / matrixScale; //ascent
    floatMas[1] = _descent / matrixScale; //descent
    floatMas[2] = (_height - _ascent - _descent) / matrixScale;//_externalLeading;

	floatMas[3] = _underlineThickness / matrixScale;
	floatMas[4] = _underlineOffset / matrixScale;
	
    floatMas[5] = _underlineThickness / matrixScale;//_strikeOutSize;
	floatMas[6] = -_ascent/(2 * matrixScale);//_strikeOutOffset;

	floatMas[7] = ((ffloat)(_boundingBox[3] - _boundingBox[1])) / matrixScale;
	
	return floatMas;
}

bool T1Font::canDisplay(ufshort ch) {    
	return this->charStringMap.find(ch) != this->charStringMap.end();
}

ufshort T1Font::getUnicodeByIndex(ufshort index) {
    Type1GlyphCodeMap::iterator iter = glyphCodeMap.find(index); 

    return iter == glyphCodeMap.end()? 0 : (ufshort)iter->second;
}

void error(){
	//printf("invalidfont");
	throw "invalidfont";
}

inline ufshort hexCharToUShort(fchar ch){
	return  ch >= '0' && ch <= '9' ? ch - '0' :
			ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 :
			ch >= 'a' && ch <= 'f' ? ch - 'a' + 10 :			
			0;   
}

void static getNextLine(fchar* str, FILE* font) {
	ufshort count = 0;
	ufchar ch = ' ';

    while (!feof(font) && (ch == ' ' || ch == '\n' || ch == '\r')) {
        ch = getc(font);
	}

    str[count] = ch;
	while (!feof(font) && ch != '\r' && ch != '\n') {
		str[++count] = (ch = getc(font));
	}
	
	str[count] = '\0';
}

static ufchar getNextChar(FILE* font) {
	ufchar ch;	
	do {
		ch = getc(font);
	} while (ch == '\r' || ch == '\n');

	return ch;
}

static ufchar decryptNextSimbol(FILE* font, ufshort* r, bool isASCII) {
	ufchar clipher = (ufchar)(
		isASCII ? 
		(ufchar) ((hexCharToUShort(getNextChar(font)) << 4 ) + (hexCharToUShort(getNextChar(font)))) : 
		getc(font)
	);

	ufchar plain = (ufchar)(clipher ^ (*r >> 8));
	*r = ( (ufchar)clipher + *r ) * C1 + C2;	
	return plain;
}

void static decodeASCIILine(fchar* str, ufshort* r, ufshort n, ufshort length) {
	fchar* p = str;
	ufchar plain;
	ufchar clipher;
	ufshort count = 0;	
	length /= 2;	
	
	while(count < length) {
		clipher = (ufchar)((hexCharToUShort(*p) << 4 ) + (hexCharToUShort(*(p + 1))));

		plain = (ufchar)(clipher ^ (*r >> 8));
		*r = ( (ufchar)clipher + *r ) * C1 + C2;

		if (count >= n) {
			str[count - n] = plain;
		}

		count ++;

		p+= 2;
	}		
	str[count - n] = '\0';
}

void static decodeBinaryLine(fchar* str, ufshort* r, ufshort n, ufshort length) {
	fchar* p = str;
	ufchar plain;
	ufchar clipher;
	ufshort count = 0;		
	while(count < length) {
		clipher = (ufchar)*p;
		
		plain = (ufchar)(clipher ^ (*r >> 8));
		*r = ( (ufchar)clipher + *r ) * C1 + C2;

		if (count >= n) {
			str[count - n] = plain;
		}

		count ++;

		p ++;
	}		
	str[count - n] = '\0';
}

void static decodeLine(fchar* str, ufshort* r, ufshort n, bool isASCII, ufshort length) {

	if (isASCII) {
		decodeASCIILine(str,r,n,length);
	} else {
		decodeBinaryLine(str,r,n,length);
	}
	return;
	/*fchar* p = str;
	ufchar plain;
	ufchar clipher;
	ufshort count = 0;	
	if (isASCII) {
		length /= 2;	
	}
	while(count < length) {
		clipher = (ufchar)(isASCII ? ((hexCharToUShort(*p) << 4 ) + (hexCharToUShort(*(p + 1)))) : *p);

		plain = (ufchar)(clipher ^ (*r >> 8));
		*r = ( (ufchar)clipher + *r ) * C1 + C2;

		//printf("%u ---- %u,%u\n", clipher, plain, *r);
		if (count >= n) {
			str[count - n] = plain;
		}

		count ++;

		p+= isASCII ? 2 : 1;
	}		
	str[count - n] = '\0';//*/
}

void static getNextDecodeLine(fchar* str, FILE* font, ufshort* r, bool isASCII) {
	ufshort count = 0;
	ufchar plain;
	while(!feof(font)) {		
		plain = decryptNextSimbol(font, r, isASCII);

		str[count ++] = plain;		

		if (plain == '\r' || plain == '\n') {
			break;		
		}
	}		
	
	str[count] = '\0';
}

void static getNextDecodeLexeme(fchar* str, FILE* font, ufshort* r, bool isASCII) {
	ufchar ch;
	ufshort count = 0;

	while (!feof(font) && ((ch = decryptNextSimbol(font, r, isASCII)) == ' ' || ch == '\n' || ch == '\r')) {
	}

	str[count ++] = ch;	
	while (!feof(font) && (ch = decryptNextSimbol(font, r, isASCII)) != ' ' && ch != '\n' && ch != '\r') {
		str[count ++] = ch;
	}

	str[count] = '\0';	
}

void static getNextLexeme(fchar* str, FILE* font) {
	ufchar ch;
	ufshort count = 0;

	while (!feof(font) && ((ch = getc(font)) == ' ' || ch == '\n' || ch == '\r' && ch != '{')) {
	}

	str[count ++] = ch;	
    while (!feof(font) && (ch = getc(font)) != ' ' && ch != '\n' && ch != '\r' && ch != '{') {
		str[count ++] = ch;
	}

	str[count] = '\0';	
}

ufshort static findUnicode(const fchar *str) {
	
	ufshort count = 0;
	ufshort strCount = 0;
	ufshort lastStrCount = 0;

	while(true) {
		if (GLYPH_LIST[count] == str[strCount]) {
			count ++;
			strCount ++;
			//next iteration				
		} else if ((GLYPH_LIST[count] ^ (1 << 7)) == str[strCount]) {
			strCount ++;
			if (str[strCount] == '\0') {
				return (ufshort)((GLYPH_LIST[count + 1] << 8) + GLYPH_LIST[count + 2]);
			}

			count = ((GLYPH_LIST[count + 5] << 8) + GLYPH_LIST[count + 6]);

			lastStrCount = strCount;

			if (!count) {
				return FONT_NOT_FOUND_UNICODE_VALUE;
			}

			//on next level
		} else {
			strCount = lastStrCount;

			for (;!(GLYPH_LIST[count] & (1 << 7)) ; ) {
				count ++;
			}

			count = ((GLYPH_LIST[count + 3] << 8) + GLYPH_LIST[count + 4]);

			if (!count) {
				return FONT_NOT_FOUND_UNICODE_VALUE;
			}

			//next on this level
		}
	}
}

ufshort static getUnicode(fchar *name) {
	if (!strncmp(name, ".notdef", 7)) {
		return 0;	
	}

	return findUnicode(name);
}

void T1Font::parseAFM(FILE *font) {
    fchar curStr[MAX_STR_LENGTH];

    while (!feof(font)) {
        getNextLexeme(curStr, font);
        //printf("%s\n",curStr);

        if (!strcmp(curStr, "EndFontMetrics") || !strcmp(curStr, "StartCharMetrics")) {

            return;
        } else if (!strcmp(curStr, "Ascender")) {
            getNextLexeme(curStr, font);
            _ascent = (ffloat) fabs(atof(curStr));
            //printf("ascend = %f\n", _ascent);
            
        } else if (!strcmp(curStr, "Descender")) {
            getNextLexeme(curStr, font);
            _descent = (ffloat) fabs(atof(curStr));
            //printf("descent = %f\n", _descent);
            
        } /*else if (!strcmp(curStr, "StartCharMetrics")) {
            getNextLexeme(curStr, font);            
            fchar* curValue;
            fchar psName[MAX_STR_LENGTH];
            ufshort count = (ufshort) atoi(curStr);
            for (ufshort i = 0; i < count; i ++) {
                getNextLine(curStr, font);

                //printf("%s\n",curStr);
                curValue = strstr( curStr, " N ");
                if (curValue != NULL) {

                    curValue += 3;

                    //printf("%s\n", curValue);
                    
                    ufshort i;
                    for (i = 0; curValue[i] != ' ' && curValue[i] != '\0'; i ++) {
                        psName[i] = curValue[i];
                    }
                    psName[i] = '\0';                    

                    curValue = new fchar[strlen(curStr) + 1]; 

                    strcpy(curValue, curStr);

                    //printf("%u = %s = %s\n",findUnicode(psName),curValue,psName);

				    afmMap[getUnicode(psName)] = curValue;
                }
            }
            
        }*/ else {
            getNextLine(curStr, font);
        }
    }
}


void T1Font::initFont(FILE *font) {
	fchar curStr[MAX_STR_LENGTH];

	DecodeState state = HEADER;

	ufshort r = DEF_R_EXEC;
	ufshort n = 4;

	ufshort lenIV = DEF_LENIV;	
	ufshort charStringR;	

	ufshort count = 0;
	ufshort tempShort = 0;
	ufshort length = 0;
	ufshort valueLength = 0;

    matrixScale = 1000;

	bool isASCII = true;

	ufchar ch;	
	EncodedValue *curValue;

	ch = getc(font);

	if (ch == 0x80 && getc(font) == 0x01) {
		isASCII = false;	
	} else if (ch == '%' && getc(font) == '!') {
		isASCII = true;	
	} else {
		error();	
	}	

	while (!feof(font)) {
		switch (state) {
		case HEADER: {
			getNextLexeme(curStr, font);

            if (!strcmp(curStr, "/UnderlinePosition")) {
                getNextLexeme(curStr, font);
                _underlineOffset = (ffloat) - atof(curStr);
		    } else if (!strcmp(curStr, "/UnderlineThickness")) {
                getNextLexeme(curStr, font);
                _underlineThickness = (ffloat) atof(curStr);			
		    } else if (strstr(curStr, "/FontBBox") != NULL) {
                //getNextLexeme(curStr, font);

                while (!feof(font) && ((ch = getc(font)) == '{')) {
				}

                ungetc(ch, font);

                getNextLexeme(curStr, font);
                _boundingBox[0] = (ffloat) atof(curStr);

                getNextLexeme(curStr, font);
                _boundingBox[1] = (ffloat) atof(curStr);

                getNextLexeme(curStr, font);
                _boundingBox[2] = (ffloat) atof(curStr);

                getNextLexeme(curStr, font);                
                _boundingBox[3] = (ffloat) atof(curStr);

                _height = ((ffloat)(_boundingBox[2] -_boundingBox[0]));

		    } else if (!strcmp(curStr, "/FullName")) {
                //getNextLexeme(curStr, font);                

				while (!feof(font) && ((ch = getc(font)) == '(')) {
				}

				curStr[count ++] = ch;	
				while (!feof(font) && (ch = getc(font)) != ')') {
					curStr[count ++] = ch;
				}

				curStr[count] = '\0';	

				fchar *ptr = curStr;

				ch = 0;
				fullName = new fwchar_t[count + 1];
				while (*ptr != '\0') {
					fullName[ch ++] = *ptr;
					ptr ++;
				}

				fullName[ch] = L'\0';
		    } else if (!strcmp(curStr, "eexec")) {
				state = PRIVATE_DIR;			
			
				if (isASCII) {
					for (count = 0; count < lenIV * 2; count ++) {
						if (!isascii(ch = getc(font))) {
							error();					
						}					
						curStr[count] = ch;
					}
					decodeASCIILine(curStr, &r, n, count);
				} else {
					for (count = 0; count < 6; count ++) {					
						curStr[count] = getc(font);
					}
					
					if (curStr[0] != (fchar)0x80 || curStr[1] != 0x02) {
						error();
					}
					for (count = 0; count < lenIV; count ++) {
						curStr[count] = getc(font);
					}
					decodeBinaryLine(curStr, &r, n, count);
				}
			}
			break;			
		}
		case PRIVATE_DIR: {
			getNextDecodeLexeme(curStr, font, &r, isASCII);
			
			if (!strcmp(curStr, "/Subrs")) {

				getNextDecodeLexeme(curStr, font, &r, isASCII);
				valueLength = atoi(curStr);
				
				count = 0;
				state = SUBRS_MASSIVE;	 
				getNextDecodeLine(curStr, font, &r, isASCII);
			} else if (!strcmp(curStr, "/CharStrings")) {

				getNextDecodeLexeme(curStr, font, &r, isASCII);
				valueLength = atoi(curStr);		
				
				count = 0;
				state = CHAR_STRING;
				getNextDecodeLine(curStr, font, &r, isASCII);
			}
			break;
		}
		case SUBRS_MASSIVE: {			
			curValue = new EncodedValue();
			getNextDecodeLexeme(curStr, font, &r, isASCII);

			getNextDecodeLexeme(curStr, font, &r, isASCII);
			curValue->number = (ufshort) atoi(curStr);	

			getNextDecodeLexeme(curStr, font, &r, isASCII);
			length = (ufshort) atoi(curStr);
			curValue->length = length - lenIV;

			getNextDecodeLexeme(curStr, font, &r, isASCII);
			
			for (tempShort = 0; tempShort - length < 0; tempShort ++) {
				curStr[tempShort] = decryptNextSimbol(font, &r, isASCII);
			}

			charStringR = DEF_R_CHARSTRING;
			decodeBinaryLine(curStr, &charStringR, lenIV, length);

			curValue->text = new fchar[curValue->length];
			for (tempShort = 0; tempShort - curValue->length < 0; tempShort ++) {
				curValue->text[tempShort] = curStr[tempShort];
			}

			subrsMap[curValue->number] = curValue;

			getNextDecodeLine(curStr, font, &r, isASCII);

			if (++count >= valueLength) {
				state = PRIVATE_DIR;
				count = 0;
			}
			
			break;
		}
		case CHAR_STRING: {
			getNextDecodeLexeme(curStr, font, &r, isASCII);
			tempShort = getUnicode(curStr + 1);

			if (tempShort != FONT_NOT_FOUND_UNICODE_VALUE) {

                glyphCodeMap[count] = tempShort;

				curValue = new EncodedValue(); 

				curValue->number = tempShort;

				getNextDecodeLexeme(curStr, font, &r, isASCII);
				length = (ufshort) atoi(curStr);
				curValue->length = length - lenIV;	

				getNextDecodeLexeme(curStr, font, &r, isASCII);
				for (tempShort = 0; tempShort - length < 0; tempShort ++) {
					curStr[tempShort] = decryptNextSimbol(font, &r, isASCII);
				}

				charStringR = DEF_R_CHARSTRING;
				decodeBinaryLine(curStr, &charStringR, lenIV, length);

				curValue->text = new fchar[curValue->length];
				for (tempShort = 0; tempShort - curValue->length < 0; tempShort ++) {
					curValue->text[tempShort] = curStr[tempShort];
				}
				charStringMap[curValue->number] = curValue;
			} else {
				getNextDecodeLexeme(curStr, font, &r, isASCII);
				length = (ufshort) atoi(curStr);							

				getNextDecodeLexeme(curStr, font, &r, isASCII);
				for (tempShort = 0; tempShort - length < 0; tempShort ++) {
					decryptNextSimbol(font, &r, isASCII);
				}
			}			

			getNextDecodeLine(curStr, font, &r, isASCII);

			if (++count >= valueLength) {
				return;		
			}
			
			break;
		}
	}
	}
}
