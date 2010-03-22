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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;

import org.apache.harmony.vm.VMStack;

/**
 * @com.intel.drl.spec_ref 
 */
public class Throwable implements Serializable {

	private static final long serialVersionUID = -3042686055658047285L;

	private Throwable cause = this;

	private final String detailMessage;

	private StackTraceElement[] stackTrace;

    private transient Class [] stackClasses;

    private transient Object state;

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable() {
		fillInStackTrace();
		detailMessage = null;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable(String message) {
		fillInStackTrace();
		this.detailMessage = message;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable(String message, Throwable cause) {
		this(message);
        this.cause = cause;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable(Throwable cause) {
		this((cause == null ? null : cause.toString()), cause);
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable fillInStackTrace() {
		state = VMStack.getStackState();
        stackClasses = VMStack.getStackClasses(state);
		return this;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable getCause() {
		return cause == this ? null : cause;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String getLocalizedMessage() {
		return detailMessage;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String getMessage() {
		return detailMessage;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
    public StackTraceElement[] getStackTrace() {
        return getStackTrace(true);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
	public Throwable initCause(Throwable initialCause) {
		if (cause == this) {
			if (initialCause != this) {
				cause = initialCause;
				return this;
			}
			throw new IllegalArgumentException("A throwable cannot be its own cause.");
		}
		// second call of initCause(Throwable)
		throw new IllegalStateException("A cause can be set at most once." + 
                    " Illegal attempt to re-set the cause of " + this);
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public void printStackTrace() {
		System.err.println(makeThrowableString());
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public void printStackTrace(PrintStream ps) {
		ps.println(makeThrowableString());
	}

    /**
     * @com.intel.drl.spec_ref 
     */
    public void printStackTrace(PrintWriter pw) {
		pw.println(makeThrowableString());
    }

    private void initStackTrace() {
        if (stackTrace == null) {
            stackTrace = VMStack.getStackTrace(state);
            state = null;
            stackClasses = null;
        }
    }

    private StackTraceElement[] getStackTrace(boolean copyArray)
    {
        StackTraceElement[] st;
        initStackTrace();

        if (copyArray) {
            st = new StackTraceElement[stackTrace.length];
            System.arraycopy(stackTrace, 0, st, 0, stackTrace.length);
        } else {
            st = stackTrace;
        }
        return st;
    }

	private String makeThrowableString() {
        StringBuffer sb = new StringBuffer();
        sb.append(toString());
	    if (stackTrace == null) {
            initStackTrace();
	    }
	    // FIXME stackTrace should never be null here
        if (stackTrace != null) {
            for (int i = 0; i < stackTrace.length; i++) {
                sb.append("\n	at ").append(stackTrace[i].toString());
            }
        } else {
            sb.append("\n	<no stack trace available>");
        }
        //causes
        Throwable wCause = this;
        while (wCause != wCause.getCause() && wCause.getCause() != null) {
            StackTraceElement[] parentStackTrace = wCause.stackTrace;
            wCause = wCause.getCause();
    	    if (wCause.stackTrace == null) {
                wCause.initStackTrace();
    	    }
            sb.append("\nCaused by: ").append(wCause.toString());
    	    // FIXME wCause.stackTrace should never be null here
            if (wCause.stackTrace != null &&  wCause.stackTrace.length != 0) {                
                if (parentStackTrace == null || parentStackTrace.length == 0) {
                    for (int i = 0; i < wCause.stackTrace.length; i++) {
                        sb.append("\n	at ").append(
                            wCause.stackTrace[i].toString());
                    }
                } else {
                    int thisCount = wCause.stackTrace.length - 1;
                    int parentCount = parentStackTrace.length - 1;
                    int framesEqual = 0;
                    while (parentCount > -1 && thisCount > -1) {
                        if (wCause.stackTrace[thisCount]
                            .equals(parentStackTrace[parentCount])) {
                            framesEqual++;
                            thisCount--;
                            parentCount--;
                        } else {
                            break;
                        }
                    }
                    if (framesEqual > 1) { //to conform with the spec and the common practice (F1F1EE)
                        framesEqual--;
                    }
                    int len = wCause.stackTrace.length - framesEqual;
                    for (int i = 0; i < len; i++) {
                        sb.append("\n	at ").append(
                            wCause.stackTrace[i].toString());
                    }
                    if (framesEqual > 0) {
                        sb.append("\n	... ").append(framesEqual)
                            .append(" more");
                    }
                }
            } else {
                sb.append("\n	<no stack trace available>");
            }
        }
        return sb.toString();
    }
	
    /**
     * @com.intel.drl.spec_ref 
     */
	public void setStackTrace(StackTraceElement[] stackTrace) {
		int len = stackTrace.length;
		StackTraceElement[] st = new StackTraceElement[len];
		for (int i =0 ; i < len; i++) {
			if (stackTrace[i] == null) {
				throw new NullPointerException();
			}
			st[i] = stackTrace[i];
		}
		this.stackTrace = st;
	}

    /**
     * @com.intel.drl.spec_ref 
     */
	public String toString() {
	    String str = getMessage();
		return getClass().getName() + (str == null ? "" : ": " + str);
	}

        private void writeObject(ObjectOutputStream s) throws IOException {
            if (stackTrace == null) {
                initStackTrace();
            }
            s.defaultWriteObject();
        }
}