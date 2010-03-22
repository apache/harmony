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
#include "Outline.h"
#include <string.h>

Outline::Outline(ufshort pointsNumber, ufshort commandNumber) {
	pointsCount = 0;
	commandsCount = 0;

	_points = new ffloat[_pointsLength = pointsNumber];
	_commands = new ufchar[_commandLength = commandNumber];    

	/*for (commandsCount = 0 ; commandsCount < commandNumber; commandsCount ++) {
		this->_commands[commandsCount] = SEG_CLOSE;
	}

	commandsCount = 0;*/
}

Outline::~Outline() {
	delete[] _points;
	delete[] _commands;
}

void Outline::trim() {
    if (_commandLength == commandsCount) {
		return;
	}

	//printf("_length = %u, commandsCount = %u\n", _commandLength, commandsCount);

	ufchar *commandMas = new ufchar[commandsCount];
	ffloat *pointsMas = new ffloat[pointsCount];
	
    memcpy(commandMas, _commands, commandsCount);
    memcpy(pointsMas, _points, pointsCount * sizeof(ffloat));

	delete[] _points;
	delete[] _commands;

	_points = pointsMas;
	_commands = commandMas;

    _commandLength = commandsCount;
    _pointsLength = pointsCount;
}

void Outline::lineTo(ffloat x, ffloat y) {
	_points[pointsCount ++] = x;
	_points[pointsCount ++] = -y;
	_commands[commandsCount ++] = SEG_LINETO;
	//printf("SEG_LINETO ");
}

void Outline::moveTo(ffloat x, ffloat y) {
	_commands[commandsCount ++] = SEG_MOVETO;
	_points[pointsCount ++] = x;
	_points[pointsCount ++] = -y;
//	printf("SEG_MOVETO ");
}

void Outline::quadTo(ffloat x1, ffloat y1, ffloat x2, ffloat y2) {
	_points[pointsCount ++] = x1;
	_points[pointsCount ++] = -y1;
	_points[pointsCount ++] = x2;
	_points[pointsCount ++] = -y2;
	_commands[commandsCount ++] = SEG_QUADTO;
	//printf("SEG_QUADTO ");
}

void Outline::curveTo(ffloat x1, ffloat y1, ffloat x2, ffloat y2, ffloat x3, ffloat y3) {
	_points[pointsCount ++] = x1;
	_points[pointsCount ++] = -y1;
	_points[pointsCount ++] = x2;
	_points[pointsCount ++] = -y2;
	_points[pointsCount ++] = x3;
	_points[pointsCount ++] = -y3;
	_commands[commandsCount ++] = SEG_CUBICTO;
	//printf("SEG_CUBICTO ");
}

void Outline::closePath(void) {
	//if (_commands[commandsCount - 1] != SEG_CLOSE) {
		_commands[commandsCount ++] = SEG_CLOSE;
	//} 
        /*else {
		printf("two close path\n");
	}*/
	//printf("SEG_CLOSE\n");
}

ufshort Outline::getPointsLength(void) {
	return _pointsLength;
}

ufshort Outline::getCommandLength(void) {
    return _commandLength;
}
