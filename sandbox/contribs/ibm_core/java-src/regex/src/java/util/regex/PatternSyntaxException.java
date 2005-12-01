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

package java.util.regex;

import java.io.Serializable;


/**
 * TODO Type description
 * 
 */
public class PatternSyntaxException extends IllegalArgumentException implements
		Serializable {
	static final long serialVersionUID = -3864639126226059218L;

	public PatternSyntaxException(String desc, String regex, int index) {
	}

	public String getDescription() {
		return null;
	}

	public int getIndex() {
		return 0;
	}

	public String getPattern() {
		return null;
	}

	public String getMessage() {
		return null;
	}
}
