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

package java.nio;


/**
 * A <code>BufferOverflowException</code> is thrown when you try to write 
 * elements to a buffer, but there is not enough remaining space in the 
 * buffer.
 * 
 */
public class BufferOverflowException extends RuntimeException {

	static final long serialVersionUID = -5484897634319144535L;

	/**
	 * Construts a <code>BufferOverflowException</code>.
	 */
	public BufferOverflowException() {
		super();
	}
}
