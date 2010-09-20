/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public enum StandardLocation implements JavaFileManager.Location {
    CLASS_OUTPUT, SOURCE_OUTPUT, CLASS_PATH, SOURCE_PATH, ANNOTATION_PROCESSOR_PATH, PLATFORM_CLASS_PATH, ;

	static HashMap<String, JavaFileManager.Location> slist = new HashMap<String, JavaFileManager.Location>();

	static {
		slist.put("ANNOTATION_PROCESSOR_PATH", ANNOTATION_PROCESSOR_PATH);
		slist.put("CLASS_OUTPUT", CLASS_OUTPUT);
		slist.put("CLASS_PATH", CLASS_PATH);
		slist.put("PLATFORM_CLASS_PATH", PLATFORM_CLASS_PATH);
		slist.put("SOURCE_OUTPUT", SOURCE_OUTPUT);
		slist.put("SOURCE_PATH", SOURCE_PATH);
	}

	public static JavaFileManager.Location locationFor(String name) {
		JavaFileManager.Location ret = slist.get(name);
		if (null == ret) {
			JavaFileManager.Location l = new ExtraLocation(name);
			slist.put(name, l);
			return l;
		}
		return ret;
	}

	public String getName() {
		Set<Entry<String, JavaFileManager.Location>> set = slist.entrySet();
		for (Iterator<Entry<String, JavaFileManager.Location>> iter = set
				.iterator(); iter.hasNext();) {
			Entry<String, JavaFileManager.Location> element = (Entry<String, JavaFileManager.Location>) iter
					.next();
			if (element.getValue() == this) {
				return element.getKey();
			}
		}
		return null;
	}

	public boolean isOutputLocation() {
		if (getName().endsWith("_OUTPUT")) {
			return true;
		}
		return false;
	}
}

class ExtraLocation implements JavaFileManager.Location {
	private String name;

	public ExtraLocation(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isOutputLocation() {
		if (name != null && name.endsWith("_OUTPUT")) {
			return true;
		}
		return false;
	}
}