
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
 * $Id: jc_invoke.h,v 1.1 2005/01/13 04:44:59 archiecobbs Exp $
 */

/*
 * VM printer function type.
 */
typedef int	_jc_printer(FILE *stream, const char *fmt, va_list args);

/*
 * Return values from _jc_invoke()
 */
#define _JC_RETURN_NORMAL	0		/* normal return */
#define _JC_RETURN_EXCEPTION	1		/* exception thrown */
#define _JC_RETURN_ERROR	2		/* error during startup */

/*
 * Invoke the JC Java virtual machine.
 */
extern int	_jc_invoke(int ac, const char **av,
			int ignoreUnrecognized, _jc_printer *printer);

