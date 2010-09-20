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

package javax.lang.model.element;

import java.util.HashSet;

public enum SourceVersion {
	RELEASE_0, RELEASE_1, RELEASE_2, RELEASE_3, RELEASE_4, RELEASE_5, RELEASE_6;

	static String[] keywordList = { "abstract", "else", "interface", "super",
			"boolean", "extends", "long", "switch", "break", "final", "native",
			"synchronized", "byte", "finally", "new", "this", "case", "float",
			"package", "throw", "catch", "for", "private", "throws", "char",
			"if", "protected", "transient", "class", "implements", "public",
			"try", "continue", "import", "return", "void", "default",
			"instanceof", "short", "volatile", "do", "int", "static", "while",
			"double" };

	static HashSet<String> keywords;

	static {
		keywords = new HashSet<String>();
		for (int i = 0; i < keywordList.length; i++) {
			keywords.add(keywordList[i]);
		}
	}

	public static boolean isIdentifier(CharSequence name) {
		if (!Character.isJavaIdentifierStart(name.charAt(0))) {
			return false;
		}
		for (int i = 1; i < name.length(); i++) {
			if (!Character.isJavaIdentifierPart(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isKeyword(CharSequence s) {
		return keywords.contains(s.toString());
	}

	public static boolean isName(CharSequence name) {
		char first = name.charAt(0);
		if (first == '.' || first == ')' || first == '('){
			return false;
		}
		String[] lists = name.toString().split(".");
		for (int i = 0; i < lists.length; i++) {
			if (isKeyword(name)){
				System.out.println("iskey");
				return false;
			}
			if(!isIdentifier(name)){
				return false;
			}
		}
		return true;
	}

	public static SourceVersion latest() {
		return RELEASE_6;
	}

	public static SourceVersion latestSupported() {
		return RELEASE_6;
	}

}