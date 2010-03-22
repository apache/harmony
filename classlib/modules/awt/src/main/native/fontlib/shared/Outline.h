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
#ifndef __SHARED_OUTLINE_CLASS_H
#define __SHARED_OUTLINE_CLASS_H

#include <string>
#include "TypeDefinition.h"

/*typedef enum {
	SEG_CLOSE = 0,
	SEG_LINETO = 1, 
	SEG_MOVETO = 2, 
	SEG_CUBICTO = 3, 
	SEG_QUADTO = 4	
} SegmentType;*/

/*typedef enum SegmentTypeTag {
    SEG_MOVETO = 0, 	
	SEG_LINETO = 1, 
	SEG_QUADTO = 2,
	SEG_CUBICTO = 3, 		
    SEG_CLOSE = 4
} SegmentType;*/

static const ufchar SEG_MOVETO = 0;
static const ufchar SEG_LINETO = 1;
static const ufchar SEG_QUADTO = 2;
static const ufchar SEG_CUBICTO = 3;
static const ufchar SEG_CLOSE = 4;

class Outline {
public:

	Outline(ufshort pointsNumber, ufshort commandNumber);

	~Outline();

	void lineTo(ffloat x, ffloat y);

	void moveTo(ffloat x, ffloat y);

	void quadTo(ffloat x1, ffloat y1, ffloat x2, ffloat y2);

	void curveTo(ffloat x1, ffloat y1, ffloat x2, ffloat y2, ffloat x3, ffloat y3);

	void closePath(void);

	ufshort getPointsLength(void);
    ufshort getCommandLength(void);

	void trim(void);

    ffloat *_points;
	ufchar *_commands;
	
private:
	ufshort _pointsLength;	
    ufshort _commandLength;	

	ufshort pointsCount;
	ufshort commandsCount;
};

#endif //__SHARED_OUTLINE_CLASS_H
