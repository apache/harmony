
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
 * $Id: main.c,v 1.2 2005/01/13 04:44:59 archiecobbs Exp $
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include "jc_invoke.h"

int
main(int ac, char **av)
{
	int status = 0;

	/* Invoke the JC VM */
	switch (_jc_invoke(ac, (const char **)av, 1, vfprintf)) {
	case _JC_RETURN_NORMAL:
		break;
	case _JC_RETURN_ERROR:
	case _JC_RETURN_EXCEPTION:
		status = 1;
		break;
	}

	/* Done */
	return status;
}

