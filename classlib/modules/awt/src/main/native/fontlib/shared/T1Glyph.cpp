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
#include "T1Glyph.h"
#include <string.h>

T1Glyph::T1Glyph(Type1Map *charStringMap, Type1Map *subrsMap, ufshort unicode, ufshort size, ffloat relativeSize, ffloat* fontBB):Glyph() {
	_charStringMap = charStringMap;
	_subrsMap = subrsMap;
	_unicode = unicode;
	_size = size;	
    _relativeSize = relativeSize;

    _advanceX = 0;
    _advanceY = 0;

    _glyphBB[0] = fontBB[0] * relativeSize;
    _glyphBB[1] = fontBB[1] * relativeSize;
    _glyphBB[2] = fontBB[2] * relativeSize;
    _glyphBB[3] = fontBB[3] * relativeSize;
}

T1Glyph::~T1Glyph() {
}

void T1Glyph::parseValueToOutline(EncodedValue *value, std::stack<ffloat> *stack, Outline *out, ffloat *curX, ffloat *curY, ffloat relativeSize){
	ffloat x1, y1, x2, y2, x3, y3;
	
	ufchar curChar;	

	for (ufshort count = 0; count < value->length; count ++) {
		
		curChar = value->text[count];		

		#ifdef GLYPH_OUTLINE_CREATE_DEBUG
		printf("\nchar = %u, ", curChar);
		#endif

		if (curChar > 31) {
			if (curChar > 31 && curChar < 247) { // -107  to  107
				stack->push((ffloat) (curChar - 139));
			} else if (curChar > 246 && curChar < 251) { // 108 to 1131
				stack->push((ffloat) ((curChar - 247) * 256 + 108 + (ufchar)(value->text[++count])));
			} else if (curChar > 250 && curChar < 255) { // -1131  to -108
				stack->push((ffloat) ((curChar - 251) * (-256) - 108 - (ufchar)(value->text[++count])));			
			} else if (curChar == 255) { // fint
				stack->push((ffloat) ((curChar << 24) + (ufchar)((value->text[++count]) << 16) + 
					(ufchar)((value->text[++count]) << 8) + (ufchar)(value->text[++count])));
			} 
		} else {
			switch (curChar) {
				case CH_STR_VMOVETO: {// vmoveto
					*curY += stack->top() * relativeSize;
					stack->pop();

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("moveTo( %f, %f), ",*curX, *curY);
					#endif
					out->moveTo(*curX, *curY);
					break;
				} 
				case CH_STR_RLINETO : {// rlineto
					*curY += (ffloat) stack->top() * relativeSize;
					stack->pop();
					*curX += (ffloat) stack->top() * relativeSize;
					stack->pop();			

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("lineTo( %f, %f), ",*curX, *curY);
					#endif
					out->lineTo(*curX, *curY);			
					break;
				} 
				case CH_STR_HLINETO : {// hlineto
					*curX += (ffloat) stack->top() * relativeSize;
					stack->pop();

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("lineTo( %f, %f), ",*curX, *curY);
					#endif
					out->lineTo(*curX, *curY);			
					break;
				} 
				case CH_STR_VLINETO : {// vlineto
					*curY += (ffloat) stack->top() * relativeSize;
					stack->pop();

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("lineTo( %f, %f), ",*curX, *curY);
					#endif
					out->lineTo(*curX, *curY);
					break;
				} 
				case CH_STR_RRCURVETO : {// rrcurveto equivalent to dx1 dy1 (dx1+dx2) (dy1+dy2) (dx1+dx2+dx3) (dy1+dy2+dy3) rcurveto.
					y3 = (ffloat) stack->top() * relativeSize;
					stack->pop();			
					x3 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					y2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					y1 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x1 = (ffloat) stack->top() * relativeSize;
					stack->pop();

					x1 += *curX;
					y1 += *curY;
					x2 += x1;
					y2 += y1;
					x3 += x2;
					y3 += y2;
					*curX = x3;
					*curY = y3;

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("curveTo( %f, %f, %f, %f, %f, %f), ",x1, y1, x2, y2, x3, y3);
					#endif
					out->curveTo(x1, y1, x2, y2, x3, y3);
					break;
				} 
				//case CH_STR_ENDCHAR :
				case CH_STR_CLOSEPATH : {// closePath
					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("closePath");
					#endif
					out->closePath();
					break;
				} 
				case CH_STR_CALLSUBR : {// callsubr
					x1 = (ffloat) stack->top();
					stack->pop();

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("call subr = %f, ", x1);
					#endif

					parseValueToOutline((*_subrsMap)[(const ufshort)x1], stack, out, curX, curY, relativeSize);
					break;
				} 
				case CH_STR_HSBW : {// hsbw
					y1 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x1 = (ffloat) stack->top() * relativeSize;
					stack->pop();

					_advanceX = y1;
					_advanceY = 0;

                    _glyphBB[0] = x1;
                    _glyphBB[2] = y1;

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("hsbw = %f,%f ; ", x1, y1);
					#endif

					*curX = x1;

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("moveTo( %f, %f), ",*curX, *curY);
					#endif

					out->moveTo(*curX, *curY);
					break;
				} 
				case CH_STR_RMOVETO : {// rmoveto
					*curY += (ffloat) stack->top() * relativeSize;
					stack->pop();
					*curX += (ffloat) stack->top() * relativeSize;
					stack->pop();			

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("moveTo( %f, %f), ",*curX, *curY);
					#endif
					out->moveTo(*curX, *curY);
					break;
				} 
				case CH_STR_HMOVETO : {// hmoveto
					*curX += (ffloat) stack->top() * relativeSize;
					stack->pop();

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("moveTo( %f, %f), ",*curX, *curY);
					#endif
					out->moveTo(*curX, *curY);		
					break;
				} 
				case CH_STR_VHCURVETO : {// vhcurveto			
					y3 = 0;
					x3 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					y2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					y1 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x1 = 0;

					x1 += *curX;
					y1 += *curY;
					x2 += x1;
					y2 += y1;
					x3 += x2;
					y3 += y2;
					*curX = x3;
					*curY = y3;

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("curveTo( %f, %f, %f, %f, %f, %f), ",x1, y1, x2, y2, x3, y3);
					#endif
					out->curveTo(x1, y1, x2, y2, x3, y3);			
					break;
				} 
				case CH_STR_HVCURVETO : {// hvcurveto
					y3 = (ffloat) stack->top() * relativeSize;
					stack->pop();			
					x3 = 0;
					y2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					x2 = (ffloat) stack->top() * relativeSize;
					stack->pop();
					y1 = 0;
					x1 = (ffloat) stack->top() * relativeSize;
					stack->pop();

					x1 += *curX;
					y1 += *curY;
					x2 += x1;
					y2 += y1;
					x3 += x2;
					y3 += y2;
					*curX = x3;
					*curY = y3;

					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("curveTo( %f, %f, %f, %f, %f, %f), ",x1, y1, x2, y2, x3, y3);
					#endif
					out->curveTo(x1, y1, x2, y2, x3, y3);			
					break;
				} 
				case CH_STR_ESCAPE : {// escape command
					curChar = value->text[++count];
					#ifdef GLYPH_OUTLINE_CREATE_DEBUG
					printf("escape command = %u, ", curChar);
					#endif
					if (curChar == CH_STR_ESCAPE_SEAC) {//seak						
						x3 = (ffloat) stack->top();
						stack->pop();
						y2 = (ffloat) stack->top();
						stack->pop();
						x2 = (ffloat) stack->top() * relativeSize;
						stack->pop();
						y1 = (ffloat) stack->top() * relativeSize;
						stack->pop();
						x1 = (ffloat) stack->top() * relativeSize;
						stack->pop();						

						#ifdef GLYPH_OUTLINE_CREATE_DEBUG
						printf("seak = %f,%f,%f,%f,%f ; ", x1, y1, x2, y2, x3);
						#endif

						ufshort aX = (ufshort) _advanceX;
						ufshort aY = (ufshort) _advanceY;

                        ffloat tempGlyphBB[4];

                        memcpy(tempGlyphBB, _glyphBB, 4 * sizeof(ffloat));

						parseValueToOutline((*_charStringMap)[STANDARD_ENCODING[(ufshort)y2]], stack, out, curX, curY, relativeSize);

						*curY = x2;
						*curX = y1;
						out->moveTo(*curX, *curY);

						parseValueToOutline((*_charStringMap)[STANDARD_ENCODING[(ufshort)x3]], stack, out, curX, curY, relativeSize);

						_advanceX = aX;
						_advanceY = aY;

                        memcpy(_glyphBB, tempGlyphBB, 4 * sizeof(ffloat));

						break;
					} else if (curChar == CH_STR_ESCAPE_SBW) { //sbw
						y2 = (ffloat) stack->top() * relativeSize;
						stack->pop();
						x2 = (ffloat) stack->top() * relativeSize;
						stack->pop();
						y1 = (ffloat) stack->top() * relativeSize;
						stack->pop();
						x1 = (ffloat) stack->top() * relativeSize;
						stack->pop();

						_advanceX = x1;
						_advanceY = y1;

                        _glyphBB[0] = x1;
                        _glyphBB[1] = y1;
                        _glyphBB[2] = x2;
                        _glyphBB[3] = y2;

						#ifdef GLYPH_OUTLINE_CREATE_DEBUG
						printf("sbw = %f,%f,%f,%f ; ", x1, y1, x2, y2);
						#endif

						*curX = x2;
						*curY = y2;

						#ifdef GLYPH_OUTLINE_CREATE_DEBUG
						printf("moveTo( %f, %f), ",*curX, *curY);
						#endif
						out->moveTo(*curX, *curY);
						break;
					} else if (curChar == CH_STR_ESCAPE_DIV) {//div
						y1 = (ffloat) stack->top();
						stack->pop();
						x1 = (ffloat) stack->top();
						stack->pop();
						#ifdef GLYPH_OUTLINE_CREATE_DEBUG
						printf("div (%f/%f) ", x1, y1);
						#endif
						stack->push(x1 / y1);
						break;
					} else {
					}
				}
			}
		}
	}
}

void T1Glyph::countPoints(std::stack<ffloat> *stack, EncodedValue *value, ufshort *point, ufshort *command) {	
	ufchar curChar;

	for (ufshort count = 0; count < value->length; count ++) {
		
		curChar = value->text[count];

		if (curChar > 31) {
			if (curChar > 31 && curChar < 247) { // -107  to  107
				stack->push((ffloat) (curChar - 139));
			} else if (curChar > 246 && curChar < 251) { // 108 to 1131
				stack->push((ffloat) ((curChar - 247) * 256 + 108 + (ufchar)(value->text[++count])));
			} else if (curChar > 250 && curChar < 255) { // -1131  to -108
				stack->push((ffloat) ((curChar - 251) * (-256) - 108 - (ufchar)(value->text[++count])));			
			} else if (curChar == 255) { // fint
				stack->push((ffloat) ((curChar << 24) + (ufchar)((value->text[++count]) << 16) + 
					(ufchar)((value->text[++count]) << 8) + (ufchar)(value->text[++count])));
			} 
		} else {
			switch (curChar) {									
				case CH_STR_RLINETO: 
				case CH_STR_HLINETO:
				case CH_STR_VLINETO:			
				case CH_STR_VMOVETO: 
				case CH_STR_RMOVETO: 
				case CH_STR_HMOVETO:
				case CH_STR_HSBW: {// HSBW
					*point += 2;
					*command += 1;
					break;
				} 
				case CH_STR_RRCURVETO: 
				case CH_STR_VHCURVETO: 
				case CH_STR_HVCURVETO: {// rrcurveto equivalent to dx1 dy1 (dx1+dx2) (dy1+dy2) (dx1+dx2+dx3) (dy1+dy2+dy3) rcurveto.
					*point += 6;
					*command += 1;		
					break;
				} 
				//case CH_STR_ENDCHAR :
				case CH_STR_CLOSEPATH : {// closePath
					*command += 1;
					break;
				} 
				case CH_STR_CALLSUBR : {// callsubr
					ufshort com = (ufshort) stack->top();
					stack->pop();

					countPoints(stack, (*_subrsMap)[com], point, command);
					break;
				} 								
				case CH_STR_ESCAPE : {// escape command
					curChar = value->text[++count];
					if (curChar == CH_STR_ESCAPE_DIV) {//div
						break;
					} else if (curChar == CH_STR_ESCAPE_SEAC) {//seac
						ufshort achar = (ufshort) stack->top();
						stack->pop();
						ufshort bchar = (ufshort) stack->top();
						stack->pop();											

						*point += 2;
						*command += 1;

						countPoints(stack, (*_charStringMap)[STANDARD_ENCODING[bchar]], point, command);
						countPoints(stack, (*_charStringMap)[STANDARD_ENCODING[achar]], point, command);

						break;
					} else if (curChar == CH_STR_ESCAPE_SBW) { //sbw
						*point += 2;
						*command += 1;
						break;
					} 
					break;
				} 
			}
		}
	}
}

ffloat* T1Glyph::getGlyphMetrics(void){
    if ((_advanceX == 0) && (_advanceY == 0)) {
        getOutline();
    }

    ffloat* gMetrics = new ffloat[6];

	gMetrics[0] = _advanceX;
	gMetrics[1] = _advanceY;
	gMetrics[2] = _glyphBB[0];
    gMetrics[3] = _glyphBB[1];
	gMetrics[4] = _glyphBB[2];
	gMetrics[5] = _glyphBB[3];

	return gMetrics;
}

Outline* T1Glyph::getOutline(void){
	ffloat curX, curY;
	curX = curY = 0;	
	ufshort point, command;
	point = command = 0;	
	Outline *out;// = new Outline((ufshort)200,(ufshort)200);	
	std::stack<ffloat> *stack = new std::stack<ffloat>();
	EncodedValue *value;

	#ifdef GLYPH_OUTLINE_CREATE_DEBUG			
	printf("\n");
	#endif
    Type1Map::iterator iter = _charStringMap->find(_unicode);	
	if (iter == _charStringMap->end()) {
		#ifdef GLYPH_OUTLINE_CREATE_DEBUG
		printf("default\n");
		#endif
		value = (*_charStringMap)[0];
	} else {
		#ifdef GLYPH_OUTLINE_CREATE_DEBUG
		printf("found value\n");
		#endif
		value = (EncodedValue *)iter->second;	
	}

	if (!value) {
		delete stack;
		return NULL;
	}

	#ifdef GLYPH_OUTLINE_CREATE_DEBUG
	printf("_unicode = %u\n",_unicode);
	#endif

	countPoints( stack, value, &point, &command);

	#ifdef GLYPH_OUTLINE_CREATE_DEBUG
	printf("final point = %u, command = %u\n", point, command);
	#endif

	out = new Outline(point, command);

	parseValueToOutline(value, stack, out, &curX, &curY, _relativeSize);

	delete stack;

	//out->trim();

	return out;	
}
