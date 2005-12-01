/* Copyright 2002, 2005 The Apache Software Foundation or its licensors, as applicable
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

/*[INCLUDE-IF mJava14]*/
package java.lang;

/**
 * An implementation of this class is provided, but the documented constructor
 * can be used by the vm specific implementation to create instances.
 * 
 * StackTraceElement represents a stack frame.
 * 
 * @see Throwable#getStackTrace()
 */
public final class StackTraceElement implements java.io.Serializable {
	static final long serialVersionUID = 6992337162326171013L;

	String declaringClass, methodName, fileName;

	int lineNumber;

	/**
	 * Create a StackTraceElement from the parameters.
	 * 
	 * @param cls
	 *            The class name
	 * @param method
	 *            The method name
	 * @param file
	 *            The file name
	 * @param line
	 *            The line number
	 */
	StackTraceElement(String cls, String method, String file, int line) {
		if (cls == null || method == null)
			throw new NullPointerException();
		declaringClass = cls;
		methodName = method;
		fileName = file;
		lineNumber = line;
	}

	// prevent instantiation from java code - only the VM creates these
	private StackTraceElement() {
		// Empty
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof StackTraceElement))
			return false;
		StackTraceElement castObj = (StackTraceElement) obj;

		// Unknown methods are never equal to anything (not strictly to spec,
		// but spec does not allow null method/class names)
		if ((methodName == null) || (castObj.methodName == null))
			return false;

		if (!getMethodName().equals(castObj.getMethodName()))
			return false;
		if (!getClassName().equals(castObj.getClassName()))
			return false;
		String localFileName = getFileName();
		if (localFileName == null) {
			if (castObj.getFileName() != null)
				return false;
		} else {
			if (!localFileName.equals(castObj.getFileName()))
				return false;
		}
		if (getLineNumber() != castObj.getLineNumber())
			return false;

		return true;
	}

	/**
	 * Returns the full name (i.e. including the package) of the class where
	 * this stack trace element is executing.
	 * 
	 * @return the fully qualified type name of the class where this stack trace
	 *         element is executing.
	 */
	public String getClassName() {
		return (declaringClass == null) ? "<unknown class>" : declaringClass;
	}

	/**
	 * If available, returns the name of the file containing the Java code
	 * source which was compiled into the class where this stack trace element
	 * is executing.
	 * 
	 * @return if available, the name of the file containing the Java code
	 *         source for the stack trace element's excuting class. If no such
	 *         detail is available, a <code>null</code> value is returned.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * If available, returns the line number in the source for the class where
	 * this stack trace element is executing.
	 * 
	 * @return if available, the line number in the source file for the class
	 *         where this stack trace element is executing. If no such detail is
	 *         available, a number &lt; <code>0</code>.
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Returns the name of the method where this stack trace element is
	 * executing.
	 * 
	 * @return the name of the method where this stack trace element is
	 *         executing.
	 */
	public String getMethodName() {
		return (methodName == null) ? "<unknown method>" : methodName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		// either both methodName and declaringClass are null, or neither are
		// null
		if (methodName == null)
			return 0; // all unknown methods hash the same
		// declaringClass never null if methodName is non-null
		return methodName.hashCode() ^ declaringClass.hashCode();
	}

	/**
	 * Returns <code>true</code> if the method name returned by
	 * {@link #getMethodName()} is implemented as a native method.
	 * 
	 * @return if the method in which this stack trace element is executing is a
	 *         native method
	 */
	public boolean isNativeMethod() {
		return lineNumber == -2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(80);

		buf.append(getClassName());
		buf.append('.');
		buf.append(getMethodName());

		if (isNativeMethod()) {
			buf.append("(Native Method)");
		} else {
			String fName = getFileName();

			if (fName == null) {
				buf.append("(Unknown Source)");
			} else {
				int lineNum = getLineNumber();

				buf.append('(');
				buf.append(fName);
				if (lineNum >= 0) {
					buf.append(':');
					buf.append(lineNum);
				}
				buf.append(')');
			}
		}
		return buf.toString();
	}
}
