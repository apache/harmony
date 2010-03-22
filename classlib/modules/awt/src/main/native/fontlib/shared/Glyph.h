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
#ifndef __SHARED_GLYPH_CLASS_H
#define __SHARED_GLYPH_CLASS_H

#include "Outline.h"
#include "TypeDefinition.h"

class Glyph {
public:
	Glyph();
	virtual ~Glyph(void);
	virtual Outline* getOutline(void);
	virtual ffloat* getGlyphMetrics(void);

//protected:
	ufshort _size;
	ufshort _unicode;	
	ffloat _advanceX;
	ffloat _advanceY;
};

#endif //__SHARED_GLYPH_CLASS_H
