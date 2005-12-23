
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
 */

#include "libjc.h"

int
_jc_build_trampoline(u_char *code, _jc_method *method, const void *func)
{
	u_char buf[100 + 14 * method->num_parameters];
	u_char *pc;

	/* Sanity check */
	_JC_ASSERT(_JC_OFFSETOF(_jc_env, interp) < 0x80);

	/* Handle length only computation */
	if (code == NULL)
		code = buf;
	pc = code;

    {
    	u_char ptypebuf[2];
	u_char offsets[4];
	u_char isfloat[3];
    	const u_char *ptypes;
	int nparams2;
	int nparams;
	int offset;
	int pnum;

	/* Count total number of parameter words */
	nparams2 = 1;						/* "env" */
	if (!_JC_ACC_TEST(method, STATIC))
		nparams2++;					/* "this" */
	nparams2 += method->num_parameters;
	for (pnum = 0; pnum < method->num_parameters; pnum++) {
		if (_jc_dword_type[method->param_ptypes[pnum]])
			nparams2++;
	}

	/* Get types of parameter words 1 and 2 (we know type for 0, "env") */
	if (_JC_ACC_TEST(method, STATIC)) {
		ptypes = method->param_ptypes;
		nparams = method->num_parameters;
	} else {
		ptypebuf[0] = _JC_TYPE_REFERENCE;	/* this */
		ptypebuf[1] = method->param_ptypes[0];	/* first parameter */
		ptypes = ptypebuf;
		nparams = method->num_parameters + 1;
	}

	/* Prologue */
	*pc++ = 0x55;				/* push %ebp */
	*pc++ = 0x89;				/* mov %esp,%ebp */
	*pc++ = 0xe5;

	/* Calculate offsets to parameter words not passed in registers */
	memset(&offsets, 0, sizeof(offsets));
	memset(&isfloat, 0, sizeof(isfloat));
	offset = 2;				/* skip %ebp, return address */
	if (nparams >= 1) {
		switch (ptypes[0]) {
		case _JC_TYPE_DOUBLE:
			offsets[1] = offset++;
			offsets[2] = offset++;
			break;
		case _JC_TYPE_FLOAT:
			offsets[1] = offset++;
			isfloat[1] = JNI_TRUE;
			if (nparams >= 2) {
				switch (ptypes[1]) {
				case _JC_TYPE_DOUBLE:
				case _JC_TYPE_LONG:
					offsets[2] = offset++;
					break;
				case _JC_TYPE_FLOAT:
					offsets[2] = offset++;
					isfloat[2] = JNI_TRUE;
					break;
				default:
					break;
				}
			}
			break;
		case _JC_TYPE_LONG:
			break;
		default:
			if (nparams >= 2) {
				switch (ptypes[1]) {
				case _JC_TYPE_DOUBLE:
				case _JC_TYPE_LONG:
					offsets[2] = offset++;
					break;
				case _JC_TYPE_FLOAT:
					offsets[2] = offset++;
					isfloat[2] = JNI_TRUE;
					break;
				default:
					break;
				}
			}
			break;
		}
		offsets[3] = offset++;
	}

	/* Copy 4th and later parameter words into parameter array */
	if (nparams2 >= 4) {
		int pword;

		/* Save %eax */
		*pc++ = 0x50;		/* push %eax */

		/* Copy 4th and later parameter words */
		offset = (offsets[3] + (nparams2 - 4)) * 4;
		pnum = method->num_parameters - 1;
		if (_jc_dword_type[method->param_ptypes[pnum]])
			pnum = -pnum;
		for (pword = nparams2 - 1; pword >= 3; pword--, offset -= 4) {

			/* Load parameter word from stack and then push it */
			if (pnum >= 0
			    && method->param_ptypes[pnum] == _JC_TYPE_FLOAT) {

				/* Load float */
				if (offset < 0x80) {
					/* flds 0xOFFSET(%ebp) */
					*pc++ = 0xd9;
					*pc++ = 0x45;
					*pc++ = offset;
				} else {
					/* flds 0xOFFSET(%ebp) */
					*pc++ = 0xd9;
					*pc++ = 0x85;
					memcpy(pc, &offset, 4);
					pc += 4;
				}

				/* Push double */
				*pc++ = 0x83;		/* sub 0x8,%esp */
				*pc++ = 0xec;
				*pc++ = 0x8;
				*pc++ = 0xdd;		/* fstpl (%esp,1) */
				*pc++ = 0x1c;
				*pc++ = 0x24;
			} else {
				if (offset < 0x80) {
					/* mov 0xOFFSET(%ebp),%eax */
					*pc++ = 0x8b;
					*pc++ = 0x45;
					*pc++ = offset;
				} else {
					/* mov 0xOFFSET(%ebp),%eax */
					*pc++ = 0x8b;
					*pc++ = 0x85;
					memcpy(pc, &offset, 4);
					pc += 4;
				}
				*pc++ = 0x50;		/* push %eax */
			}

			/* Keep track of corresponding parameter index */
			if (pnum < 0)	/* was second word of long/double */
				pnum = -pnum;
			else if (_jc_dword_type[method->param_ptypes[--pnum]])
				pnum = -pnum;
		}

		/* Restore %eax */
		*pc++ = 0x8b;		/* mov -4(%ebp),%eax */
		*pc++ = 0x45;
		*pc++ = 0xfc;
	}

	/* Recall 3rd parameter word from stack (if necessary) and push */
	if (nparams2 >= 3) {
		if (isfloat[2]) {
			*pc++ = 0xd9;		/* flds 0xNN(%ebp) */
			*pc++ = 0x45;
			*pc++ = offsets[2] * 4;
			*pc++ = 0x83;		/* sub 0x8,%esp */
			*pc++ = 0xec;
			*pc++ = 0x8;
			*pc++ = 0xdd;		/* fstpl (%esp,1) */
			*pc++ = 0x1c;
			*pc++ = 0x24;
		} else {
			if (offsets[2] > 0) {
				*pc++ = 0x8b;	/* mov 0xNN(%ebp),%ecx */
				*pc++ = 0x4d;
				*pc++ = offsets[2] * 4;
			}
			*pc++ = 0x51;		/* push %ecx */
		}
	}

	/* Recall 2nd parameter word from stack (if necessary) and push */
	if (nparams2 >= 2) {
		if (isfloat[1]) {
			*pc++ = 0xd9;		/* flds 0xNN(%ebp) */
			*pc++ = 0x45;
			*pc++ = offsets[1] * 4;
			*pc++ = 0x83;		/* sub 0x8,%esp */
			*pc++ = 0xec;
			*pc++ = 0x8;
			*pc++ = 0xdd;		/* fstpl (%esp,1) */
			*pc++ = 0x1c;
			*pc++ = 0x24;
		} else {
			if (offsets[1] > 0) {
				*pc++ = 0x8b;	/* mov 0xNN(%ebp),%edx */
				*pc++ = 0x55;
				*pc++ = offsets[1] * 4;
			}
			*pc++ = 0x52;		/* push %edx */
		}
	}

	/* Push "env" (first parameter) onto the stack */
	*pc++ = 0x50;				/* push %eax */

	/* Set env->interp = method */
	*pc++ = 0xc7;				/* mov METHOD,OFFSET(%eax) */
	*pc++ = 0x40;
	*pc++ = _JC_OFFSETOF(_jc_env, interp);
	memcpy(pc, &method, 4);
	pc += 4;

	/* Call function */
	*pc++ = 0xe8;				/* call func */
	func = (void *)((jint)func - (jint)(pc + 4));
	memcpy(pc, &func, 4);
	pc += 4;

	/* Epilogue */
	*pc++ = 0xc9;				/* leave */
	*pc++ = 0xc3;				/* ret */
    }

	/* Done */
	return pc - code;
}

#include <stdio.h>

void
_jc_dynamic_invoke(const void *func, int jcni, int nparams,
	const u_char *ptypes, int nwords, _jc_word *words, _jc_rvalue *retval)
{
	fprintf(stdout, "WARNING: Call to unimplemented function _jc_dynamic_invoke(...).\n");
	exit(0);
}

