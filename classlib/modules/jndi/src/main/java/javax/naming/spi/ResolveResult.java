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

package javax.naming.spi;

import javax.naming.Name;
import javax.naming.InvalidNameException;
import javax.naming.CompositeName;

/**
 * An instance of <code>ResolveResult</code> is produced when a name
 * resolution operation has completed. The instance must contain the object
 * associated with the successfully resolved name, and any remaining portion of
 * the name yet to be resolved. Where a <code>String</code> parameter is used
 * to specify a name, it should be considered to be a composite name.
 * <p>
 * Multithreaded access to a single <code>ResolveResult</code> instance is
 * only safe when client code locks the object first.
 * </p>
 */
public class ResolveResult implements java.io.Serializable {

    private static final long serialVersionUID = -4552108072002407559L;

    /**
     * This field holds the object associated with the resolved name. It may be
     * null only when a subclass is constructed. It must be initialized to a
     * non-null value by constructors of this class.
     * 
     * @serial
     */
    protected Object resolvedObj;

    /**
     * This field holds the portion of a resolved name that remains to be
     * resolved. It may be null only when a subclass is constructed. It must be
     * initialized to a non-null value by constructors of this class.
     * 
     * @serial
     */
    protected Name remainingName;

    /**
     * This is the default constructor implicitly invoked by subclass
     * constructors. This constructor set both the resolved object and the
     * remaining name to null.
     */
    protected ResolveResult() {
        this.resolvedObj = null;
        this.remainingName = null;
    }

    /**
     * This constructor creates a instance with the specified resolved object
     * and a specified remaining name of type <code>String</code>. The name
     * argument may not be null, but may be empty. The object argument may not
     * be null.
     * 
     * @param o
     *            may not be null
     * @param s
     *            may not be null, but may be empty
     */
    public ResolveResult(Object o, String s) {
        this.resolvedObj = o;
        try {
            this.remainingName = new CompositeName(s);
        } catch (InvalidNameException e) {
            this.remainingName = null;
        }
    }

    /**
     * This constructor creates a instance with the specified resolved object
     * and a remaining name of type <code>Name</code>. The name argument may
     * not be null, but may be empty. The object argument may not be null.
     * 
     * @param o
     *            may not be null
     * @param n
     *            may not be null
     */
    public ResolveResult(Object o, Name n) {
        this.resolvedObj = o;
        if (null == n) {
            this.remainingName = null;
        } else {
            this.remainingName = (Name) n.clone();
        }
    }

    /**
     * Extends the remaining name (remainingName) with a single specified name
     * component. The name argument may be null, but this leaves the remaining
     * name unmodified.
     * 
     * @param s
     *            the name component to be added to the remaining name. A null
     *            leaves the remaining name unmodified.
     */
    public void appendRemainingComponent(String s) {
        if (null != s) {
            if (null == this.remainingName) {
                this.remainingName = new CompositeName();
            }
            try {
                remainingName.add(s);
            } catch (InvalidNameException e) {
                throw new Error(e.getMessage());
            }
        }
    }

    /**
     * Extends the remaining name (remainingName) with all components of the
     * specified name. The name argument may be null, but this leaves the
     * remaining name unmodified.
     * 
     * @param n
     *            the name to be added to the remaining name A null leaves the
     *            remaining name unmodified.
     */
    public void appendRemainingName(Name n) {
        if (null != n) {
            if (null == this.remainingName) {
                this.remainingName = (Name) n.clone();
            } else {
                try {
                    this.remainingName.addAll(n);
                } catch (InvalidNameException e) {
                    throw new Error(e.getMessage());
                }
            }
        }
    }

    /**
     * Returns any unresolved portion of the name that was resolved (the
     * remaining name). The returned <code>Name</code> may be empty, but may
     * not be null.
     * 
     * @return any unresolved portion of the name that was resolved (the
     *         remaining name).
     */
    public Name getRemainingName() {
        return this.remainingName;
    }

    /**
     * Returns the non-null object that was resolved (resolved object).
     * 
     * @return the non-null object that was resolved (resolved object).
     */
    public Object getResolvedObj() {
        return this.resolvedObj;
    }

    /**
     * Sets the remaining name (remainingName) to a copy of the specified
     * <code>Name</code> parameter may be empty, but not null.
     * 
     * @param n
     *            a name, may be empty, but no null
     */
    public void setRemainingName(Name n) {
        if (null == n) {
            this.remainingName = null;
        } else {
            this.remainingName = (Name) n.clone();
        }
    }

    /**
     * Sets the resolved object (resolved object) to o which may not be null.
     * 
     * @param o
     *            an object, may not be null
     */
    public void setResolvedObj(Object o) {
        this.resolvedObj = o;
    }

}
