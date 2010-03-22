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

import java.util.Hashtable;

/**
 * Naming operations throw a <code>CannotProceedException</code> when the
 * service provider context implementation is resolving a name but reaches a
 * name component that does not belong to the namespace of the current context.
 * <p>
 * The service provider is able to create a <code>CannotProceedException</code>
 * object and use methods on that object (including baseclass methods) to
 * provide full details of how far name resolution had progressed.
 * </p>
 * <p>
 * Typically, the methods used might include:
 * <ul>
 * <li><code>setEnvironment</code> to record the environment from the current
 * context</li>
 * <li> <code>setAltNameCtx</code> to record the current context</li>
 * <li> <code>setResolvedObj</code> to record the resolved object for the next
 * naming system</li>
 * <li> <code>setAltName</code> to record the name of the resolved object</li>
 * <li> <code>setRemainingName</code> to record the remaining unresolved name</li>
 * </ul>
 * </p>
 * <p>
 * If the incomplete naming operation is <code>rename</code>, the service
 * provider should also use the <code>setRemainingNewName</code> method to
 * record the unresolved part of the new name.
 * </p>
 * <p>
 * The service provider can pass the <code>CannotProceedException</code> as a
 * parameter to <code>NamingManager</code> methods such as
 * <code>getContinuationContext</code> to attempt to locate another service
 * provider for the next naming system. If successful, that service provider can
 * return a new <code>Context</code> object on which the naming operation can
 * proceed further. If unsuccessful, the <code>CannotProceedException</code>
 * can be thrown by the service provider so that the JNDI application can handle
 * it and take appropriate action.
 * </p>
 * <p>
 * Multithreaded access to a single <code>CannotProceedException</code>
 * instance is only safe when client code uses appropriate synchronization and
 * locking.
 * </p>
 * 
 */
public class CannotProceedException extends NamingException {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private final static long serialVersionUID = 1219724816191576813L;

    /**
     * Contains a composite name that is the name of the resolved object which
     * is relative to the context in <code>altNameCtx</code>. This field may
     * be null and is initially null. This field should be accessed and modified
     * using only <code>getAltName</code> and <code>setAltName</code>.
     */
    protected Name altName = null;

    /**
     * Contains the context to which the <code>altName</code> field is
     * relative. This field may be null and is initially null. A null value
     * implies the default initial context. This field should be accessed and
     * modified using only <code>getAltNameCtx</code> and
     * <code>setAltNameCtx</code>.
     */
    protected Context altNameCtx = null;

    /**
     * Contains the environment of the context in which name resolution for the
     * naming operation could not proceed further. Initially null. Should only
     * be manipulated using <code>getEnvironment</code> and
     * <code>setEnvironment methods</code>.
     */
    protected Hashtable<?, ?> environment = null;

    /**
     * Contains a composite name that is the unresolved part of the new name
     * that was specified in a <code>Context.rename</code> operation and may
     * be used to continue the <code>rename</code> operation. This field may
     * be null and is initially null. This field should be accessed and modified
     * using only <code>getRemainingNewName</code> and
     * <code>setRemainingNewName</code>.
     */
    protected Name remainingNewName = null;

    /**
     * Constructs a <code>CannotProceedException</code> object. All
     * unspecified fields are initialized to null.
     */
    public CannotProceedException() {
        super();
    }

    /**
     * Constructs a <code>CannotProceedException</code> object with an
     * optionally specified <code>String</code> parameter containing a
     * detailed explanation message. The <code>String</code> parameter may be
     * null. All unspecified fields are initialized to null.
     * 
     * @param s
     *            The detailed explanation message for the exception. It may be
     *            null.
     */
    public CannotProceedException(String s) {
        super(s);
    }

    /**
     * Retrieves the value of the <code>altName</code> field.
     * 
     * @return the value of the <code>altName</code> field.
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(Object, Name,
     *      Context, Hashtable)
     */
    public Name getAltName() {
        return altName;
    }

    /**
     * Retrieves the value of the <code>altNameCtx</code> field.
     * 
     * @return the value of the <code>altNameCtx</code> field.
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(Object, Name,
     *      Context, Hashtable)
     */
    public Context getAltNameCtx() {
        return altNameCtx;
    }

    /**
     * Retrieves the value of the protected field <code>environment</code>
     * which may be null.
     * 
     * @return the value of the protected field <code>environment</code> which
     *         may be null.
     */
    public Hashtable<?, ?> getEnvironment() {
        return environment;
    }

    /**
     * Retrieves the value of the <code>remainingNewName</code> field.
     * 
     * @return the value of the <code>remainingNewName</code> field.
     */
    public Name getRemainingNewName() {
        return remainingNewName;
    }

    /**
     * Modifies the value of the <code>altName</code> field to the specified
     * <code>Name</code> parameter which is a composite name and may be null.
     * 
     * @param name
     *            the name to set.
     */
    public void setAltName(Name name) {
        altName = name;
    }

    /**
     * Modifies the value of the <code>altNameCtx</code> field to the
     * specified <code>Context</code> parameter which may be null.
     * 
     * @param context
     *            the new context to set.
     */
    public void setAltNameCtx(Context context) {
        altNameCtx = context;
    }

    /**
     * Sets the value of the protected field <code>environment</code> from the
     * <code>environment</code> parameter which may be null.
     * 
     * @param hashtable
     *            the new environment to set.
     */
    public void setEnvironment(Hashtable<?, ?> hashtable) {
        environment = hashtable;
    }

    /**
     * Modifies the value of the <code>remainingNewName</code> field to the
     * specified parameter which may be null. But otherwise is a composite name.
     * When needing to specify other names, first convert them into a composite
     * name with a single component, and then specify that composite name when
     * invoking this method.
     * <p>
     * When a non-null name is specified, a clone of the composite name is
     * stored in the <code>remainingNewName</code> field that becomes
     * independent of the specified name.
     * </p>
     * 
     * @param name
     *            the new name to set.
     */
    public void setRemainingNewName(Name name) {
        remainingNewName = (null == name) ? null : (Name) name.clone();
    }

}
