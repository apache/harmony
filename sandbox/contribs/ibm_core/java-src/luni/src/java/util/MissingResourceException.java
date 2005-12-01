/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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

package java.util;


/**
 * This runtime exception is thrown by ResourceBundle when a resouce bundle
 * cannot be found or a resource is missing from a resource bundle.
 * 
 * @see ResourceBundle
 */
public class MissingResourceException extends RuntimeException {

	static final long serialVersionUID = -4876345176062000401L;

	String className, key;

	/**
	 * Constructs a new instance of this class with its walkback, message, the
	 * class name of the resource bundle and the name of the missing resource.
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 * @param className
	 *            String The class name of the resource bundle.
	 * @param resourceName
	 *            String The name of the missing resource.
	 */
	public MissingResourceException(String detailMessage, String className,
			String resourceName) {
		super(detailMessage);
		this.className = className;
		key = resourceName;
	}

	/**
	 * Answers the class name of the resource bundle from which a resource could
	 * not be found, or in the case of a missing resource, the name of the
	 * missing resource bundle.
	 * 
	 * @return String The class name of the resource bundle.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Answers the name of the missing resource, or an empty string if the
	 * resource bundle is missing.
	 * 
	 * @return String The name of the missing resource.
	 */
	public String getKey() {
		return key;
	}

}
