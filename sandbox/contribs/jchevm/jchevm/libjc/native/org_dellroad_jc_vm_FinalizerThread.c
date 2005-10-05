
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
 * $Id: org_dellroad_jc_vm_FinalizerThread.c,v 1.3 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "org_dellroad_jc_vm_FinalizerThread.h"

/*
 * static native void finalizeObjects()
 */
void _JC_JCNI_ATTR
JCNI_org_dellroad_jc_vm_FinalizerThread_finalizeObjects(_jc_env *env)
{
	if (_jc_gc_finalize(env) != JNI_OK)
		_jc_throw_exception(env);
}

