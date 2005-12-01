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

package com.ibm.platform.struct;


public final class RuntimeMemorySpy extends AbstractMemorySpy {

	public RuntimeMemorySpy() {
		super();
	}

	public void alloc(PlatformAddress address, long size) {
		super.alloc(address, size);
		// Pay a tax on the allocation to see if there are any frees pending.
		Object ref = notifyQueue.poll(); // non-blocking check
		while (ref != null) {
			orphanedMemory(ref);
			ref = notifyQueue.poll();
		}
	}
}
