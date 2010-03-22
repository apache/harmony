/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.naming;

import java.io.Serializable;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * <code>NameClassPair</code> associates a name in a naming service with a
 * specified class name and also with a relative flag. In JNDI,
 * <code>NameClassPair</code> is extended by <code>javax.naming.Binding</code>;
 * <code>Binding</code> objects are used in <code>javax.naming.Context</code>
 * implementations.
 * <p>
 * A <code>NameClassPair</code> object is not thread-safe unless appropriate
 * synchronization is applied to any code manipulating these objects.
 * </p>
 * <p>
 * As this class implements the <code>Serializable</code> interface, it is
 * important that fields below are declared with the same names.
 * </p>
 */
public class NameClassPair implements Serializable {

    private static final long serialVersionUID = 5620776610160863339L;

    /**
     * The name used in a naming service. This field may be null and has default
     * value of null.
     * 
     * @serial
     */
    private String name;

    /**
     * The class of an object represented by this name in a naming service. This
     * field may be null and has default value null.
     * 
     * @serial
     */
    private String className;

    /**
     * 
     * @serial
     */
    private String fullName;

    /**
     * This flag indicates whether the name s used in a naming service is
     * relative to the context. It is set by setRelative and is not derived.
     * This field has default value true. If this is set to false then the name
     * is not relative and is actually a URL.
     * 
     * @serial
     */
    private boolean isRel;

    /**
     * Construct a <code>NameClassPair</code> from a name and a class. Both
     * arguments can be null. Relative flag is true.
     * 
     * @param name
     *            a name used in naming service
     * @param className
     *            a class name
     */
    public NameClassPair(String name, String className) {
        this(name, className, true);
    }

    /**
     * Construct a <code>NameClassPair</code> from a name, a class and a
     * relative flag. The name and class arguments can be null.
     * 
     * @param name
     *            a name used in naming service
     * @param className
     *            a class name
     * @param relative
     *            a relative flag
     */
    public NameClassPair(String name, String className, boolean relative) {
        if (name == null) {
            // jndi.00=name must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.00")); //$NON-NLS-1$
        }
        this.name = name;
        this.className = className;
        this.isRel = relative;
        this.fullName = null;
    }

    /**
     * Returns the value of the class which may be null.
     * 
     * @return the value of the class which may be null.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the value of the name field which may be null.
     * 
     * @return the value of the name field which may be null.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of the relative flag.
     * 
     * @return the value of the relative flag.
     */
    public boolean isRelative() {
        return isRel;
    }

    /**
     * Set the class of this object. The argument can be null.
     * 
     * @param className
     *            a class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Set the name of this object. The argument can be null.
     * 
     * @param name
     *            a name used in naming service
     */
    public void setName(String name) {
        if (name == null) {
            // jndi.00=name must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.00")); //$NON-NLS-1$
        }
        this.name = name;
    }

    /**
     * Set the isRelative flag field of this object.
     * 
     * @param relative
     *            a relative flag
     */
    public void setRelative(boolean relative) {
        this.isRel = relative;
    }

    /**
     * Returns the value of the full name field which may be null.
     * 
     * @return the value of the full name field which may be null.
     * 
     * @throws UnsupportedOperationException
     */
    public String getNameInNamespace() {
        if (fullName == null) {
            // jndi.01=full name doesn't apply to this binding
            throw new UnsupportedOperationException(Messages
                    .getString("jndi.01")); //$NON-NLS-1$
        }
        return fullName;
    }

    /**
     * Set the full name of this object. The argument can be null.
     * 
     * @param fullName
     *            a full name
     */
    public void setNameInNamespace(String fullName) {
        this.fullName = fullName;
    }

    /**
     * If the flag is set to false then the string is preceded with "(not
     * relative)" and then has the name value, ": " and the class value.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (!isRel) {
            buf.append("(not relative)"); //$NON-NLS-1$
        }
        buf.append(getName());
        buf.append(": "); //$NON-NLS-1$
        buf.append(getClassName()); // getClassName() is overridden by subclass
        return buf.toString();
    }

}
