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

package javax.naming.directory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * This is the main interface to a directory service.
 * <p>
 * A <code>DirContext</code> is the interface through which a client interacts
 * with a particular concrete directory service provider. The API provides
 * methods for searching, reading and writing attributes of directory entries.
 * </p>
 * <p>
 * The name of a directory entry is taken as relative to the context receiving
 * the method invocation. Names cannot be null and the empty name has a special
 * meaning of the context itself.
 * </p>
 * <p>
 * In this interface there are duplicated methods that take either a
 * <code>String</code> or <code>Name</code> parameter. This is simply a
 * convenience and the behavior of each method is identical.
 * </p>
 * <p>
 * The semantics of a name in a <code>DirContext</code> is exactly equivalent
 * to that of a name in a regular naming <code>Context</code>.
 * </p>
 * 
 * <em>Attribute storage models</em>
 * <p>
 * JNDI supports two logical models of attribute storage:
 * <ul>
 * <li>Type A : where an attribute operation on a named object is equivalent to
 * a lookup in the <code>DirContext</code> on the given name followed by
 * application of the operation to the resulting empty <code>DirContext</code>.
 * Think of this as attributes being stored on the object itself.</li>
 * <li>Type B : where an attribute operation on a named object is equivalent to
 * a lookup on that name in the <em>parent</em> <code>DirContext</code>
 * followed by application of the operation on the parent
 * <code>DirContext</code> providing the name as an argument. Think of this as
 * the attributes being stored in the parent context.
 * <p>
 * In this model objects that are not <code>DirContext</code> can have
 * attributes, provided their parents are <code>DirContext</code>.</li>
 * </ul>
 * </p>
 * <p>
 * The directory service provider can implement either of these logical models,
 * and the client is expected to know which model it is dealing with.
 * </p>
 * 
 * <em>Attribute Name aliasing</em>
 * <p>
 * Directory service providers are free to implement attribute name aliasing. If
 * the service employs aliasing then the list of attribute names that are
 * returned as a result of API calls to get a named attribute, or search for a
 * set of attributes may include attributes whose name was not in the search
 * list. Implementations should not rely on the preservation of attribute names.
 * </p>
 * 
 * <em>Searching and operational attributes</em>
 * <p>
 * Some directory service providers support "operational attributes" on objects.
 * These are attributes that are computed by the provider, or have other special
 * semantics to the directory service. The directory service defines which
 * attributes are operational.
 * </p>
 * <p>
 * The API calls for searching for attributes, and those for getting named
 * attributes using a list of names are defined to interpret the
 * <code>null</code> argument to match all non-operational attributes.
 * </p>
 * <p>
 * It is therefore possible to get a specific named attribute that is not
 * returned in a global retrieval of all object attributes.
 * </p>
 * 
 * <em>Conditions</em>
 * <p>
 * Some APIs require that the name resolves to another <code>DirContext</code>
 * and not an object. In such cases, if this postcondition is not met then the
 * method should throw a <code>NotContextException</code>. Other methods can
 * resolve to be either objects or <code>DirContext</code>.
 * </p>
 * <p>
 * Service providers must not modify collection parameters such as
 * <code>Attribute</code>, <code>SearchControl</code> or arrays. Similarly,
 * clients are expected not to modify the collections while the service provider
 * iterates over such collections -- the service provider should be given
 * exclusive control of the collection until the method returns.
 * </p>
 * <p>
 * APIs that return collections are safe -- that is, the service provider will
 * not modify collections that are returned to clients.
 * </p>
 * 
 * <em>Exceptions</em>
 * <p>
 * Any method may throw a <code>NamingException</code> (or subclass) as
 * defined by the exception descriptions.
 * </p>
 */
public interface DirContext extends Context {

    /**
     * Constant value indicating the addition of an attribute.
     * <p>
     * The new value is added to the existing attributes at that identifier
     * subject to the following constraints:
     * <ol>
     * <li>If the attribute is being created and the value is empty, an
     * <code>InvalidAttributeValueException</code> is thrown if the attribute
     * must have a value.</li>
     * <li>If the attribute already exists with a single value and the schema
     * requires that the attribute can only have a single value, an
     * <code>AttributeInUseException</code> is thrown.</li>
     * <li>If the attribute is being created with a multi-value and the schema
     * requires that the attribute can only have a single value, an
     * <code>InvalidAttributeValueException</code> is thrown.</li>
     * </ol>
     * </p>
     */
    public static final int ADD_ATTRIBUTE = 1;

    /**
     * Constant value indicating the replacement of an attribute value.
     * <p>
     * If the attribute does not exist then it is created with the given
     * attribute identifier and attribute. If the value contravenes the schema,
     * an <code>
     * InvalidAttributeValueException</code> is thrown.
     * </p>
     * <p>
     * If the attribute exists then all of its values are replaced by the given
     * values. If the attribute is defined to take a single value and the new
     * value is a multi-value then an
     * <code>InvalidAttributeValueException</code> is thrown. If no value is
     * given then all of the values are removed from the attribute.
     * </p>
     * <p>
     * If an attribute is defined as requiring at least one value, then removing
     * values results in the removal of the attribute itself.
     * </p>
     */
    public static final int REPLACE_ATTRIBUTE = 2;

    /**
     * Constant field indicating the removal of an attribute.
     * <p>
     * If the attribute exists then the resulting values of the attribute is the
     * set of values given by removing all values in the given set from the
     * existing attribute set.
     * </p>
     * <p>
     * If the given set of attributes is <code>null</code> that should be
     * interpreted as a request to remove all values from the existing attribute
     * set.
     * </p>
     * <p>
     * If the attribute does not exist, or a value in the given set does not
     * appear in the existing attribute set then the service provider is free to
     * either ignore the fact it does not exist, or throw a
     * <code>NamingException</code>.
     * </p>
     */
    public static final int REMOVE_ATTRIBUTE = 3;

    /**
     * Binds a <code>Name</code> to an <code>Object</code> in this directory
     * to produce a binding.
     * 
     * <p>
     * This binding can have attributes, which are specified by the
     * <code>attributes</code> parameter if it is non-null. If the
     * <code>attributes</code> parameter is null and <code>obj</code> is a
     * <code>DirContext</code> with attributes, the binding will have the
     * attributes of <code>obj</code>.
     * </p>
     * <p>
     * Note that null is not a valid value for <code>name</code>. Neither is
     * the empty <code>Name</code> because this is reserved to refer to the
     * context.
     * </p>
     * <p>
     * If <code>name</code> is already bound in this <code>DirContext</code>
     * this method throws a <code>NameAlreadyBoundException</code>.
     * </p>
     * <p>
     * If there are mandatory attributes for this binding in this
     * <code>DirContext</code>, and they are not specified, this method
     * throws an <code>InvalidAttributesException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name to be bound
     * @param obj
     *            the object to be bound
     * @param attributes
     *            the attributes of this binding, can be null
     * @throws NamingException
     *             If any occurs.
     */
    void bind(Name name, Object obj, Attributes attributes)
            throws NamingException;

    /**
     * Binds a string name to an <code>Object</code> in this directory to
     * produce a binding.
     * 
     * @param s
     *            the string representative of name to be bound
     * @param obj
     *            the object to be bound
     * @param attributes
     *            the attributes of this binding, can be null
     * @throws NamingException
     *             thrown if any occurs
     * @see #bind(Name name, Object obj, Attributes attributes)
     */
    void bind(String s, Object obj, Attributes attributes)
            throws NamingException;

    /**
     * Creates and binds a new subcontext.
     * <p>
     * The new subcontext might not be an immediate subcontext of this one. If
     * it is not an immediate subcontext, all the intervening subcontexts
     * specified in <code>name</code> must already exist. If the attributes
     * parameter is non-null the specified attributes are added to the new
     * subcontext.
     * </p>
     * <p>
     * Possible exceptions are <code>NameAlreadyBoundException</code> and
     * <code>InvalidAttributesException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name bound to the new subcontext
     * @param attributes
     *            the attributes of the new subcontxt, can be null
     * @return the new subcontext
     * @throws NamingException
     *             If any occurs.
     */
    DirContext createSubcontext(Name name, Attributes attributes)
            throws NamingException;

    /**
     * Creates and binds a new subcontext.
     * 
     * @param s
     *            the string representative of name bound to the new subcontext
     * @param attributes
     *            the attributes of the new subcontxt, can be null
     * @return the new subcontext
     * @throws NamingException
     *             If any occurs.
     * @see #createSubcontext(Name n, Attributes attributes)
     */
    DirContext createSubcontext(String s, Attributes attributes)
            throws NamingException;

    /**
     * Gets all attributes of <code>name</code>.
     * <p>
     * See note in description about operational attributes.
     * </p>
     * <p>
     * The returned set of attributes is empty if <code>name</code> has no
     * attributes.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            name to be searched for attributes
     * @return all attributes of <code>name</code>
     * @throws NamingException
     *             If any occurs.
     */
    Attributes getAttributes(Name name) throws NamingException;

    /**
     * Gets the attributes for <code>name</code> that match the strings in
     * array <code>as</code>.
     * <p>
     * If any string in <code>as</code> is not matched it is skipped. More
     * attributes may be returned than the number of strings in <code>as</code> -
     * see notes on attribute aliasing.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            name to be searched for attributes
     * @param as
     *            the array of strings to match attributes
     * @return all attributes for <code>name</code> that match the strings in
     *         array <code>as</code>.
     * @throws NamingException
     *             If any occurs.
     */
    Attributes getAttributes(Name name, String as[]) throws NamingException;

    /**
     * Gets all attributes of name represented by <code>s</code>.
     * 
     * @param s
     *            representative of name to be searched for attributes
     * @return all attributes of name represented by <code>s</code>
     * @throws NamingException
     *             If any occurs.
     * @see #getAttributes(Name name)
     */
    Attributes getAttributes(String s) throws NamingException;

    /**
     * Gets the attributes for name represented by <code>s</code> that match
     * the strings in array <code>as</code>.
     * 
     * @param s
     *            representative of name to be searched for attributes
     * @param as
     *            the array of strings to match attributes
     * @return all attributes for name represented by <code>s</code> that
     *         match the strings in array <code>as</code>.
     * @throws NamingException
     *             If any occurs.
     * @see #getAttributes(Name name, String[] as)
     */
    Attributes getAttributes(String s, String as[]) throws NamingException;

    /**
     * Gets the top level of the schema for object <code>name</code>.
     * <p>
     * If <code>name</code> does not support a schema this method throws an
     * <code>OperationNotSupportedException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the object to be searched for schema
     * @return the top level of the schema for object <code>name</code>
     * @throws NamingException
     *             If any occurs.
     */
    DirContext getSchema(Name name) throws NamingException;

    /**
     * Gets the top level of the schema for name represented by <code>s</code>.
     * 
     * @param s
     *            representative of name to be searched for schema
     * @return the top level of the schema for object <code>name</code>
     * @throws NamingException
     *             If any occurs.
     * @see #getSchema(Name name)
     */
    DirContext getSchema(String s) throws NamingException;

    /**
     * Gets the class definition for <code>name</code> from its schema.
     * <p>
     * A class definition from a schema specifies a type and its mandatory and
     * optional attributes. Note that the term "class" here refers to the
     * general concept of a data type, not a Java class.
     * </p>
     * <p>
     * If <code>name</code> does not support a schema this method throws an
     * <code>OperationNotSupportedException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name to searched for the class definition from its schema
     * @return the class definition for <code>name</code> from its schema.
     * @throws NamingException
     *             If any occurs.
     */
    DirContext getSchemaClassDefinition(Name name) throws NamingException;

    /**
     * Gets the class definition for name represented by <code>s</code> from
     * its schema.
     * 
     * @param s
     *            the string representative of name to searched for the class
     *            definition from its schema
     * @return the class definition for <code>name</code> from its schema.
     * @throws NamingException
     *             If any occurs.
     * @see #getSchemaClassDefinition(Name name)
     */
    DirContext getSchemaClassDefinition(String s) throws NamingException;

    /**
     * Modifies the attributes of <code>name</code>.
     * <p>
     * Parameter <code>i</code> is modification operation type and is
     * constrained to be one of <code>ADD_ATTRIBUTE</code>,
     * <code>REPLACE_ATTRIBUTE</code>, <code>REMOVE_ATTRIBUTE</code>. The
     * implementation should try to make the modifications atomic.
     * </p>
     * <p>
     * This method throws an <code>AttributeModificationException</code> if
     * there is a problem completing the modification.
     * </p>
     * <p>
     * This method throws any <code>NamingException<code> that occurs.</p>
     * 
     * @param name				the name which attributes will be modified
     * @param i					the modification operation type
     * @param attributes		the modified attributes
     * @throws NamingException  If any occurs.
     */
    void modifyAttributes(Name name, int i, Attributes attributes)
            throws NamingException;

    /**
     * Modifies the attributes of <code>name</code> in the order given by the
     * array parameter <code>amodificationitem</code>.
     * <p>
     * The required operations are specified by the elements of
     * <code>modificationItems</code>.
     * </p>
     * <p>
     * This method throws an <code>AttributeModificationException</code> if
     * there is a problem completing the modifications.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name which attributes will be modified
     * @param modificationItems
     *            the array of modification item
     * @throws NamingException
     *             If any occurs.
     */
    void modifyAttributes(Name name, ModificationItem[] modificationItems)
            throws NamingException;

    /**
     * Modifies the attributes of name represented by <code>s</code>.
     * 
     * @param s
     *            name represented by <code>s</code>
     * @param i
     *            the modification operation type
     * @param attributes
     *            the modified attributes
     * @throws NamingException
     *             If any occurs.
     * @see #modifyAttributes(Name name, int i, Attributes attributes)
     */
    void modifyAttributes(String s, int i, Attributes attributes)
            throws NamingException;

    /**
     * Modifies the attributes of name represented by <code>s</code> in the
     * order given by the array parameter <code>modificationItems</code>.
     * 
     * @param s
     *            name represented by <code>s</code>
     * @param modificationItems
     *            the array of modification item
     * @throws NamingException
     *             If any occurs.
     * @see #modifyAttributes(Name name, ModificationItem[] modificationItems)
     */
    void modifyAttributes(String s, ModificationItem[] modificationItems)
            throws NamingException;

    /**
     * Rebinds <code>name</code> to <code>obj</code>.
     * <p>
     * If the attributes parameter is non-null, the attributes it specifies
     * become the only attributes of the binding. If the attributes parameter is
     * null but <code>obj</code> is an instance of <code>DirContext</code>
     * then the attributes of <code>obj</code> become the only attributes of
     * the binding. If the <code>attributes</code> parameter is null and
     * <code>obj</code> is not an instance of <code>DirContext</code> then
     * any attributes of the previous binding remain.
     * </p>
     * <p>
     * If a schema defines mandatory attributes for the binding but they are not
     * all present this method throws an <code>InvalidAttributesException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name to be bound
     * @param obj
     *            the object to be bound
     * @param attributes
     *            the attributes of the binding
     * @throws NamingException
     *             If any occurs.
     */
    void rebind(Name name, Object obj, Attributes attributes)
            throws NamingException;

    /**
     * Rebinds name represented by <code>s</code> to <code>obj</code>.
     * 
     * @param s
     *            the string representative of name to be bound
     * @param obj
     *            the object to be bound
     * @param attributes
     *            the attributes of the binding
     * @throws NamingException
     *             If any occurs.
     * @see #rebind(Name name, Object o, Attributes attributes)
     */
    void rebind(String s, Object obj, Attributes attributes)
            throws NamingException;

    /**
     * Searches in the context specified by <code>name</code> only, for any
     * objects that have attributes that match the <code>attributes</code>
     * parameter.
     * <p>
     * This method is equivalent to passing a null <code>as</code> parameter
     * to <code>search(Name name, Attributes attributes, String[] as)</code>.
     * Objects with attributes that match the <code>attributes</code>
     * parameter are selected and all attributes are returned for selected
     * objects.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name specifies the context to be searched
     * @param attributes
     *            the attributes to be matched when search
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see #search(Name name, Attributes attributes, String[] as)
     */
    NamingEnumeration<SearchResult> search(Name name, Attributes attributes)
            throws NamingException;

    /**
     * This method searches in the context specified by <code>name</code>
     * only, for any objects that have attributes that match the
     * <code>attributes</code> parameter.
     * 
     * <p>
     * It uses default <code>SearchControls</code>. An object is selected if
     * it has every attribute in the <code>attributes</code> parameter,
     * regardless of whether it has extra attributes. If the
     * <code>attributes</code> parameter is null or empty then every object in
     * the context is a match.
     * </p>
     * <p>
     * The definition of attribute matching is
     * <ol>
     * <li>both attributes have the same identifier;</li>
     * <li>all values of the attribute from the attributes parameter are found
     * in the attribute from the target object.</li>
     * </ol>
     * </p>
     * <p>
     * Attribute ordering is ignored. If an attribute from the
     * <code>attributes</code> parameter has no values it is matched by any
     * attribute that has the same identifier. The definition of attribute value
     * equality is left to the directory service - it could be
     * <code>Object.equals(Object obj)</code>, or a test defined by a schema.
     * </p>
     * <p>
     * For each of the selected objects, this method collects and returns the
     * attributes with identifiers listed in parameter <code>as</code>. Note
     * that these may be different to those in the <code>attributes</code>
     * parameter. If a selected object does not have one of the attributes
     * listed in <code>as</code>, the missing attribute is simply skipped for
     * that object. Attribute aliasing may mean that an attribute in the
     * <code>as</code> parameter list maps to more than one returned
     * attribute. If parameter <code>as</code> is empty, no attributes are
     * returned, but if <code>a</code>s is null all attributes are returned.
     * </p>
     * <p>
     * The return value is an enumeration of <code>SearchResult</code>
     * objects, which is empty if no matches are found. It is not specified how
     * subsequent changes to context specified by <code>name</code> will
     * affect an enumeration that this method returns.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @param name
     *            the name specifies the context to be searched
     * @param attributes
     *            the attributes to be matched when search
     * @param as
     *            the array of string representative of attributes to be
     *            returned
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     */
    NamingEnumeration<SearchResult> search(Name name, Attributes attributes,
            String as[]) throws NamingException;

    /**
     * This method searches in the context specified by <code>name</code>
     * only, using the filter specified by parameter <code>filter</code> and
     * controlled by <code>searchControls</code>.
     * 
     * <p>
     * The parameter <code>filter</code> is an RFC2254 filter. It may contain
     * variables such as "{N}", which refer to element N of the
     * <code>objs</code> array.
     * </p>
     * <p>
     * The "{N}" variables can be used in place of "attr", "value", or
     * "matchingrule" from RFC2254 section 4. If an "{N}" variable refers to a
     * <code>String</code> object, that string becomes part of the filter
     * string, but with any special characters escaped as defined by RFC 2254.
     * The directory service implementation decides how to interpret filter
     * arguments with types other than <code>String</code>. The result of
     * giving invalid variable substitutions is not specified.
     * </p>
     * <p>
     * If <code>searchControls</code> is null, the default
     * <code>SearchControls</code> object is used: i.e. the object created by
     * the no-args <code>SearchControls()</code> constructor.
     * </p>
     * <p>
     * The return value is an enumeration of <code>SearchResult</code>
     * objects. The object names used may be relative to the context specified
     * in the <code>name</code> parameter, or a URL string. If the
     * <code>name</code> context itself is referred to in the results, the
     * empty string is used. It is not specified how subsequent changes to
     * context specified by <code>name</code> will affect an enumeration that
     * this method returns.
     * </p>
     * <p>
     * If an "{N}" variable in <code>s</code> references a position outside
     * the bounds of array <code>objs</code> this method will throw an
     * <code>ArrayIndexOutOfBoundsException</code>.
     * </p>
     * <p>
     * If <code>searchControls</code> is invalid this method will throw
     * <code>InvalidSearchControlsException</code>.
     * </p>
     * <p>
     * If the filter specified by <code>filter</code> and <code>objs</code>
     * is invalid this method will throw an
     * <code>InvalidSearchFilterException</code>.
     * </p>
     * <p>
     * This method throws any <code>NamingException</code> that occurs.
     * 
     * @param name
     *            the name specifies the context to be searched
     * @param filter
     *            the search filter
     * @param objs
     *            array of objects referred by search filter
     * @param searchControls
     *            the search controls
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see SearchControls
     */
    NamingEnumeration<SearchResult> search(Name name, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException;

    /**
     * This method searches in the context specified by <code>name</code>
     * only, using the filter specified by parameter <code>filter</code> and
     * controlled by <code>searchControls</code>.
     * <p>
     * This method can throw <code>InvalidSearchFilterException<c/ode>,
     * <code>InvalidSearchControlsException</code>, <code>NamingException</code>.</p>
     * 
     * @param name				the name specifies the context to be searched
     * @param filter			the search filter
     * @param searchControls 	the search controls
     * @return					<code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException  If any occurs.
     * @see #search(Name, String, Object[], SearchControls)
     */
    NamingEnumeration<SearchResult> search(Name name, String filter,
            SearchControls searchControls) throws NamingException;

    /**
     * Searches in the context specified by name represented by
     * <code>name</code> only, for any objects that have attributes that match
     * the <code>attributes</code> parameter.
     * 
     * @param name
     *            the string representative of name which specifies the context
     *            to be searched
     * @param attributes
     *            the attributes to be matched when search
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see #search(Name, Attributes)
     */
    NamingEnumeration<SearchResult> search(String name, Attributes attributes)
            throws NamingException;

    /**
     * This method searches in the context specified by name represented by
     * <code>name</code> only, for any objects that have attributes that match
     * the <code>attributes</code> parameter.
     * 
     * @param name
     *            the string representative of name which specifies the context
     *            to be searched
     * @param attributes
     *            the attributes to be matched when search
     * @param as
     *            the array of string representative of attributes to be
     *            returned
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see #search(Name, Attributes, String[])
     */
    NamingEnumeration<SearchResult> search(String name, Attributes attributes,
            String as[]) throws NamingException;

    /**
     * This method searches in the context specified by name represented by
     * <code>name</code> only, using the filter specified by parameter
     * <code>filter</code> and controlled by <code>searchControls</code>.
     * 
     * @param name
     *            the string representative of name which specifies the context
     *            to be searched
     * @param filter
     *            the search filter
     * @param objs
     *            array of objects referred by search filter
     * @param searchControls
     *            the search controls
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see #search(Name, String, Object[], SearchControls)
     */
    NamingEnumeration<SearchResult> search(String name, String filter,
            Object[] objs, SearchControls searchControls)
            throws NamingException;

    /**
     * This method searches in the context specified by name represented by
     * <code>name</code> only, using the filter specified by parameter
     * <code>filter</code> and controlled by <code>searchControls</code>.
     * 
     * @param name
     *            the string representative of name which specifies the context
     *            to be searched
     * @param filter
     *            the search filter
     * @param searchControls
     *            the search controls
     * @return <code>NamingEnumeration</code> of <code>SearchResult</code>
     * @throws NamingException
     *             If any occurs.
     * @see #search(Name, String, SearchControls)
     */
    NamingEnumeration<SearchResult> search(String name, String filter,
            SearchControls searchControls) throws NamingException;

}
