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
#include "TTCurve.h"
#include "memory.h"

TTCurve::TTCurve()
{
	_len = 0;
	_outlineCommandsNumb = 1; // for last closePath();
	_coords = 0;
	_flags = 0;
}

TTCurve::~TTCurve()
{
	delete[] _coords;
	delete[] _flags;
}

fint TTCurve::add(ffloat x, ffloat y, ufchar flag)
{
	_len +=2;
	
	if (flag == OPEN_FLAG && _outlineCommandsNumb)
		_outlineCommandsNumb+=2;
	else if (flag != 0)
		_outlineCommandsNumb++;

	ffloat* tmpC = _coords;
	ufchar* tmpF = _flags;

	_coords = new ffloat[_len];
	_flags = new ufchar[(_len+1)/2];

	memcpy(_coords,tmpC,(_len-2)*sizeof(ffloat));
    _coords[_len-2] = x;
	_coords[_len-1] = y;
    
	memcpy(_flags,tmpF,(_len-1)/2);
    _flags[(_len-1)/2] = flag;

	delete[] tmpC;
	delete[] tmpF;
	return 0;
}
