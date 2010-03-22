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

/**
 * Binding extends <code>NameClassPair</code> to associate an object in a
 * naming service with its name, specified class name and relative flag. As with
 * <code>NameClassPair</code>, a class name is only specified when it is
 * necessary to override the real class name of the associated object.
 * <p>
 * Multithreaded access to a <code>Binding</code> instance is only safe when
 * client code locks the object first.
 * </p>
 */
public class Binding extends NameClassPair {

    private static final long serialVersionUID = 8839217842691845890L;

    private Object boundObj;

    /**
     * Construct a <code>Binding</code> from a name and a class. Relative flag
     * is true.
     * 
     * @param name
     *            a name, may not be <code>null</code>.
     * @param obj
     *            an object bound with the name, may be <code>null</code>.
     */
    public Binding(String name, Object obj) {
        this(name, null, obj, true);
    }

    /**
     * Construct a <code>Binding</code> from a name, an object and a relative
     * flag.
     * 
     * @param name
     *            a name, which may not be <code>null</code>.
     * @param obj
     *            an object bound with the name, may be <code>null</code>.
     * @param relative
     *            a relative flag
     */
    public Binding(String name, Object obj, boolean relative) {
        this(name, null, obj, relative);
    }

    /**
     * Construct a <code>Binding</code> from a name, a class, and an object.
     * The class and object parameters may be null. Relative flag is true.
     * 
     * @param name
     *            a name, which may not be <code>null</code>.
     * @param className
     *            a class name, may be <code>null</code>.
     * @param obj
     *            an object bound with the name, may be <code>null</code>.
     */
    public Binding(String name, String className, Object obj) {
        this(name, className, obj, true);
    }

    /**
     * Construct a <code>Binding</code> from a name, a class, an object and a
     * relative flag. The class and object parameters may be null.
     * 
     * @param name
     *            a name, which may not be <code>null</code>.
     * @param className
     *            a class name, may be <code>null</code>.
     * @param obj
     *            an object bound with the name, may be <code>null</code>.
     * @param relative
     *            a relative flag
     */
    public Binding(String name, String className, Object obj, boolean relative) {
        super(name, className, relative);
        this.boundObj = obj;
    }

    /**
     * Get the class name of this <code>Binding</code>. It may have been
     * specified, in which case the class name field is set, and that is the
     * string returned by this method. If the class name field has not been
     * specified then the object associated with this <code>Binding</code> is
     * interrogated to find its actual class name. If there is no class name
     * field specified and no associated object then this method returns null.
     * 
     * @return the class name
     */
    @Override
    public String getClassName() {
        if (super.getClassName() != null) {
            return super.getClassName();
        }
        if (boundObj != null) {
            return boundObj.getClass().getName();
        }
        return null;
    }

    /**
     * Get the object associated with this <code>Binding</code>. May return
     * null.
     * 
     * @return the object associated with this <code>Binding</code>. May
     *         return null.
     */
    public Object getObject() {
        return boundObj;
    }

    /**
     * Set the object o associated with this <code>Binding</code>. The object
     * may be null.
     * 
     * @param object
     *            an object
     */
    public void setObject(Object object) {
        this.boundObj = object;
    }

    /**
     * Provide a string representation of this object. This is the same as for
     * <code>NameClassPair</code> but with the string representation of the
     * <code>Binding</code> object appended to the end.
     * 
     * @return a string representation of this <code>Binding</code>
     */
    @Override
    public String toString() {
        return super.toString() + ":" + boundObj; //$NON-NLS-1$
    }

}
