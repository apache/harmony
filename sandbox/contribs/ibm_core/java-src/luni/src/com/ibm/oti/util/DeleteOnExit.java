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

package com.ibm.oti.util;


import java.io.File;
import java.util.Vector;

import com.ibm.oti.vm.VM;

public class DeleteOnExit {
	private static Vector deleteList = new Vector();

	static {
		VM.deleteOnExit();
	}

	public static void addFile(String toDelete) {
		deleteList.addElement(toDelete);
	}

	public static void deleteOnExit() {
		for (int i = 0; i < deleteList.size(); i++) {
			String name = (String) deleteList.elementAt(i);
			new File(name).delete();
		}
	}
}
