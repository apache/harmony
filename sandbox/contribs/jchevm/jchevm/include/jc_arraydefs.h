
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id: jc_arraydefs.h,v 1.1.1.1 2004/02/20 05:15:10 archiecobbs Exp $
 */

#ifndef _JC_ARRAYDEFS_H_
#define _JC_ARRAYDEFS_H_

/*
 * The purpose of this file is to define a macro that declares external
 * '_jc_type' structures corresponding to all of the array types (up
 * to 255 dimensions) for a given base type. Although all dimensionalities
 * are declared, only the dimensionalities actually used are created.
 *
 * As a special case, for 1-dimensional arrays the dimension may be
 * omitted.
 */

// This macro declares one array type for a given base type
#define _JC_DECL_ARRAYS_1(type, dims, suffix)				\
extern _jc_type _jc_ ## type ## $array ## dims ## $ ## suffix;

// This macro declares ten array types for a given base type
#define _JC_DECL_ARRAYS_10(type, prefix, suffix)			\
_JC_DECL_ARRAYS_1(type, prefix ## 0, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 1, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 2, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 3, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 4, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 5, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 6, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 7, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 8, suffix);				\
_JC_DECL_ARRAYS_1(type, prefix ## 9, suffix)

// This macro declares all 255 array types for a given base type
#define _JC_DECL_ARRAYS(type, suffix)					\
_JC_DECL_ARRAYS_1(type, , suffix);	/* "$array" same as "$array1" */\
_JC_DECL_ARRAYS_10(type, , suffix);					\
_JC_DECL_ARRAYS_10(type, 1, suffix);					\
_JC_DECL_ARRAYS_10(type, 2, suffix);					\
_JC_DECL_ARRAYS_10(type, 3, suffix);					\
_JC_DECL_ARRAYS_10(type, 4, suffix);					\
_JC_DECL_ARRAYS_10(type, 5, suffix);					\
_JC_DECL_ARRAYS_10(type, 6, suffix);					\
_JC_DECL_ARRAYS_10(type, 7, suffix);					\
_JC_DECL_ARRAYS_10(type, 8, suffix);					\
_JC_DECL_ARRAYS_10(type, 9, suffix);					\
_JC_DECL_ARRAYS_10(type, 10, suffix);					\
_JC_DECL_ARRAYS_10(type, 11, suffix);					\
_JC_DECL_ARRAYS_10(type, 12, suffix);					\
_JC_DECL_ARRAYS_10(type, 13, suffix);					\
_JC_DECL_ARRAYS_10(type, 14, suffix);					\
_JC_DECL_ARRAYS_10(type, 15, suffix);					\
_JC_DECL_ARRAYS_10(type, 16, suffix);					\
_JC_DECL_ARRAYS_10(type, 17, suffix);					\
_JC_DECL_ARRAYS_10(type, 18, suffix);					\
_JC_DECL_ARRAYS_10(type, 19, suffix);					\
_JC_DECL_ARRAYS_10(type, 20, suffix);					\
_JC_DECL_ARRAYS_10(type, 21, suffix);					\
_JC_DECL_ARRAYS_10(type, 22, suffix);					\
_JC_DECL_ARRAYS_10(type, 23, suffix);					\
_JC_DECL_ARRAYS_10(type, 24, suffix);					\
_JC_DECL_ARRAYS_1(type, 250, suffix);					\
_JC_DECL_ARRAYS_1(type, 251, suffix);					\
_JC_DECL_ARRAYS_1(type, 252, suffix);					\
_JC_DECL_ARRAYS_1(type, 253, suffix);					\
_JC_DECL_ARRAYS_1(type, 254, suffix);					\
_JC_DECL_ARRAYS_1(type, 255, suffix)

#endif	/* _JC_ARRAYDEFS_H_ */

