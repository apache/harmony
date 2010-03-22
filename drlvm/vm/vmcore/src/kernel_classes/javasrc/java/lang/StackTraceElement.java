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
/**
 * @author Dmitry B. Yershov
 */

package java.lang;

import java.io.Serializable;

/**
 * @com.intel.drl.spec_ref 
 */
public final class StackTraceElement implements Serializable {

	private static final long serialVersionUID = 6992337162326171013L;

	private final String declaringClass;

	private final String methodName;

	private final String fileName;

	private final int lineNumber;

	/**
     * This method satisfies the requirements of the specification for the
     * {@link StackTraceElement#StackTraceElement(String, String, String, int)
     * StackTraceElement(String declaringClass, String methodName, 
     * String fileName, int lineNumber)} method.
     * <p>
     * Note that currently this constructor is not used by the VM.
     * @api2vm 
     */
	public StackTraceElement(String declaringClass, String methodName,
                      String fileName, int lineNumber) {	    
		this.declaringClass = declaringClass.toString();
		this.methodName = methodName.toString();
		this.fileName = fileName;
		this.lineNumber = lineNumber;
	}
    
	/**
     * @com.intel.drl.spec_ref 
     */
	public boolean equals(Object obj) {
	    if (obj == this) {
	        return true;
	    }
		if (obj != null && obj instanceof StackTraceElement) {
            StackTraceElement ste = (StackTraceElement)obj;
            return declaringClass.equals(ste.declaringClass)
                && methodName.equals(ste.methodName)
                && (fileName == ste.fileName || (fileName != null && fileName
                    .equals(ste.fileName))) && lineNumber == ste.lineNumber;
        }
		return false;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String getClassName() {
		return declaringClass;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String getFileName() {
		return fileName;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public int getLineNumber() {
		return lineNumber;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String getMethodName() {
		return methodName;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public int hashCode() {
        return declaringClass.hashCode() ^ methodName.hashCode();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
	public boolean isNativeMethod() {
	    return lineNumber == -2;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String toString() {
		StringBuffer sb = new StringBuffer();
			sb.append(declaringClass).append('.').append(methodName);
		if (fileName == null) {
			sb.append(lineNumber == -2 ? "(Native Method)" : "(Unknown Source)");
		} else {		    
			sb.append('(').append(fileName);
			if (lineNumber >= 0) {
				sb.append(':').append(lineNumber);
			}
			sb.append(')');
		}
		return sb.toString();
	}
}