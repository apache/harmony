/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.platform;


import com.ibm.oti.vm.VM;

/**
 * OSComponent
 * 
 */
public class OSComponent implements IAdaptable {

	/**
	 * 
	 */
	public OSComponent() {
		super();
		if (VM.callerClassLoader() != null) {
			throw new SecurityException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.core.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

}
