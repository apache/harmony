/* Copyright 2001, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.oti.lang.reflect;


import java.lang.reflect.Method;

class ProxyMethod {
	Method method;

	Class[] commonExceptions;

	ProxyMethod(Method method) {
		this.method = method;
		this.commonExceptions = method.getExceptionTypes();
	}

	Class[] getCheckedExceptions() {
		Class[] newExceptions = (Class[]) commonExceptions.clone();
		int cLength = newExceptions.length;
		for (int c = 0, cL = cLength; c < cL; c++) {
			Class ex = newExceptions[c];
			if (Throwable.class == ex)
				return new Class[0]; // if Throwable is included then treat
										// as if no exceptions are checked
			if (Error.class.isAssignableFrom(ex)
					|| RuntimeException.class.isAssignableFrom(ex)) {
				newExceptions[c] = null;
				cLength--;
			}
		}

		// All errors & runtime exceptions are passed back without being
		// wrappered
		Class[] result = new Class[cLength + 2];
		int index = 0;
		result[index++] = Error.class;
		result[index++] = RuntimeException.class;
		for (int i = 0, length = newExceptions.length; i < length; i++) {
			Class ex = newExceptions[i];
			if (ex != null)
				result[index++] = ex;
		}
		return result;
	}

	boolean matchMethod(Method otherMethod) {
		if (!method.getName().equals(otherMethod.getName()))
			return false;

		Class[] params1 = method.getParameterTypes();
		Class[] params2 = otherMethod.getParameterTypes();
		int p = params1.length;
		if (p != params2.length)
			return false;
		while (--p >= 0)
			if (params1[p] != params2[p])
				return false;

		if (method.getReturnType() != otherMethod.getReturnType())
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString(
					"K00f2", method.getName()));
		if (commonExceptions.length != 0) {
			Class[] otherExceptions = otherMethod.getExceptionTypes();
			if (otherExceptions.length == 0) {
				commonExceptions = otherExceptions;
			} else {
				int cLength = commonExceptions.length;
				nextException: for (int c = 0, cL = cLength, oL = otherExceptions.length; c < cL; c++) {
					Class cException = commonExceptions[c];
					for (int o = 0; o < oL; o++) {
						Class oException = otherExceptions[o];
						if (cException == oException)
							continue nextException;
						if (oException.isAssignableFrom(cException))
							continue nextException; // cException is a subclass
						if (cException.isAssignableFrom(oException)) {
							// oException is a subclass, keep it instead
							commonExceptions[c] = cException = oException;
							continue nextException;
						}
					}
					commonExceptions[c] = null;
					cLength--;
				}
				if (cLength != commonExceptions.length) {
					Class[] newExceptions = new Class[cLength];
					for (int i = 0, j = 0, length = commonExceptions.length; i < length; i++) {
						Class ex = commonExceptions[i];
						if (ex != null)
							newExceptions[j++] = ex;
					}
					commonExceptions = newExceptions;
				}
			}
		}
		return true;
	}
}
