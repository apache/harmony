
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
 * $Id$
 */

#include "libjc.h"

/*
 * Map a PC return address within a function to a Java line number.
 * This assumes the PC is within the function. Returns 0 if unknown.
 *
 * The PC return address should point to the instruction just after the
 * instruction that makes the function call.
 */
int
_jc_exec_pc_to_jline(_jc_method *method, const void *pc)
{
	_jc_pc_map_info *const pcmap = &method->u.exec.u.pc_map;
	_jc_pc_map *base;
	int span;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));

	/* Sanity check that the PC lies within the method */
	_JC_ASSERT(pc > method->function && pc <= method->u.exec.function_end);

	/* If PC map does not exist, bail out */
	if (!_JC_ACC_TEST(method, PCMAP))
		return 0;

	/* Binary search PC map */
	for (base = pcmap->map, span = pcmap->len; span != 0; span >>= 1) {
		_jc_pc_map *const sample = &base[span >> 1];

		if (pc <= sample->pc)
			continue;
		if (pc > (sample + 1)->pc) {
			base = sample + 1;
			span--;
			continue;
		}
		return sample->jline;
	}

	/* Not found */
	return 0;
}

