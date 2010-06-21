
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
 * @author Sergey L. Ivashin
 *
 */

#ifndef _FIXFILENAME_H_
#define _FIXFILENAME_H_

#include <stddef.h>


namespace Jitrino
{

// max acceptable file name part (between '/' or '.')
const size_t MAXFILEPARTSIZE = 75;

// max acceptable file name
const size_t MAXFILENAMESIZE = MAXFILEPARTSIZE * 4;

// used in StrSubstitution only
const size_t MAXLOGFILENAME = 500;


void fix_file_name (char* goodname, int goodmax, const char* badname);


}


#endif //#ifndef _FIXFILENAME_H_
