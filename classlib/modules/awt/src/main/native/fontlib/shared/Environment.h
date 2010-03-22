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
#ifndef __GRAPHICS_ENVIRONMENT_H__
#define __GRAPHICS_ENVIRONMENT_H__

#include "Font.h"
#include "TypeDefinition.h"

typedef enum FontTypeTag {
    TrueType = 0, 
    Type1 = 1} FontType;

class FontHeader
{
public:
	fwchar_t *_familyName;
	fchar* _filePath;
	StyleName _style;
    Font* _font;
	FontType _fType;
	FontHeader* _nextHeader;
	FontHeader* _head;

	FontHeader();
	FontHeader(fchar** filePath, FontType fType);

	~FontHeader();
}; 

class Environment
{
public:
	static fint _length; //length of list 
//	static fint addPath(fchar* argPath);
	static FontHeader* addFile(fchar* argPath, FontType ft);
	static FontHeader* getAllFonts();
};

#endif // __GRAPHICS_ENVIRONMENT_H__
