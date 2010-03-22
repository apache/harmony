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

package javax.imageio.metadata;

import javax.imageio.ImageTypeSpecifier;
import java.util.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class IIOMetadataFormatImpl implements IIOMetadataFormat {
    @SuppressWarnings({"ConstantDeclaredInAbstractClass"})
    public static final String standardMetadataFormatName = "javax_imageio_1.0";

    @SuppressWarnings({"StaticNonFinalField"})
    private static IIOMetadataFormatImpl standardFormat;

    private String rootName;
    private HashMap<String, Element> elementHash = new HashMap<String, Element>();

    private String resourceBaseName = getClass().getName() + "Resources";

    public IIOMetadataFormatImpl(String rootName, int childPolicy) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        if (
                childPolicy < CHILD_POLICY_EMPTY ||
                childPolicy > CHILD_POLICY_MAX ||
                childPolicy == CHILD_POLICY_REPEAT
        ) {
            throw new IllegalArgumentException("childPolicy is not one of the predefined constants");
        }

        this.rootName = rootName;
        Element root = new Element();
        root.name = rootName;
        root.childPolicy = childPolicy;
        elementHash.put(rootName, root);
    }

    public IIOMetadataFormatImpl(String rootName, int minChildren, int maxChildren) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }

        this.rootName = rootName;
        Element root = new Element();
        root.name = rootName;
        root.minChildren = minChildren;
        root.maxChildren = maxChildren;
        root.childPolicy = CHILD_POLICY_REPEAT;
        elementHash.put(rootName, root);
    }

    @SuppressWarnings({"AbstractMethodOverridesAbstractMethod"})
    public abstract boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    protected void addAttribute(
            String elementName, String attrName, int dataType,
            boolean required, int listMinLength, int listMaxLength
    ) {
        if (attrName == null) {
            throw new IllegalArgumentException("attrName == null!");
        }
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }
        if (listMinLength < 0 || listMinLength > listMaxLength) {
            throw new IllegalArgumentException("Invalid list bounds!");
        }

        Element element = findElement(elementName);
        Attlist attr = new Attlist();
        attr.name = attrName;
        attr.dataType = dataType;
        attr.required = required;
        attr.listMinLength = listMinLength;
        attr.listMaxLength = listMaxLength;
        attr.valueType = VALUE_LIST;

        element.attributes.put(attrName, attr);
    }

    protected void addAttribute(
            String elementName, String attrName, int dataType,
            boolean required, String defaultValue
    ) {
        if (attrName == null) {
            throw new IllegalArgumentException("attrName == null!");
        }
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }

        Element element = findElement(elementName);
        Attlist attr = new Attlist();
        attr.name = attrName;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.valueType = VALUE_ARBITRARY;

        element.attributes.put(attrName, attr);
    }

    protected void addAttribute(
            String elementName, String attrName, int dataType,
            boolean required, String defaultValue, List<String> enumeratedValues
    ) {
        if (attrName == null) {
            throw new IllegalArgumentException("attrName == null!");
        }
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }
        if (enumeratedValues == null || enumeratedValues.isEmpty()) {
            throw new IllegalArgumentException("enumeratedValues is empty or null");
        }

        try {
            for (String enumeratedValue : enumeratedValues) {
                if (enumeratedValue == null) {
                    throw new IllegalArgumentException("enumeratedValues contains a null!");
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("enumeratedValues contains a non-String value!");
        }

        Element element = findElement(elementName);
        Attlist attr = new Attlist();
        attr.name = attrName;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.enumeratedValues = enumeratedValues;
        attr.valueType = VALUE_ENUMERATION;

        element.attributes.put(attrName, attr);
    }

    protected void addAttribute(
            String elementName, String attrName, int dataType,
            boolean required, String defaultValue,
            String minValue, String maxValue,
            boolean minInclusive, boolean maxInclusive
    ) {
        if (attrName == null) {
            throw new IllegalArgumentException("attrName == null!");
        }
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }

        Element element = findElement(elementName);
        Attlist attr = new Attlist();
        attr.name = attrName;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.minValue = minValue;
        attr.maxValue = maxValue;
        attr.minInclusive = minInclusive;
        attr.maxInclusive = maxInclusive;

        attr.valueType = VALUE_RANGE;
        attr.valueType |= minInclusive ? VALUE_RANGE_MIN_INCLUSIVE_MASK : 0;
        attr.valueType |= maxInclusive ? VALUE_RANGE_MAX_INCLUSIVE_MASK : 0;

        element.attributes.put(attrName, attr);
    }

    protected void addBooleanAttribute(
            String elementName, String attrName,
            boolean hasDefaultValue, boolean defaultValue
    ) {
        String defaultVal = hasDefaultValue ? (defaultValue ? "TRUE" : "FALSE") : null;
        ArrayList<String> values = new ArrayList<String>(2);
        values.add("TRUE");
        values.add("FALSE");

        addAttribute(elementName, attrName, DATATYPE_BOOLEAN, true, defaultVal, values);
    }

    protected void addChildElement(String elementName, String parentName) {
        Element parent = findElement(parentName);
        Element element = findElement(elementName);
        parent.children.add(element.name);
    }

    protected void addElement(String elementName, String parentName, int childPolicy) {
        if (
                childPolicy < CHILD_POLICY_EMPTY ||
                childPolicy > CHILD_POLICY_MAX ||
                childPolicy == CHILD_POLICY_REPEAT
        ) {
            throw new IllegalArgumentException("childPolicy is not one of the predefined constants");
        }
        
        Element parent = findElement(parentName);
        Element element = new Element();
        element.name = elementName;
        element.childPolicy = childPolicy;
        elementHash.put(elementName, element);
        parent.children.add(elementName);
    }

    protected void addElement(
            String elementName, String parentName,
            int minChildren, int maxChildren
    ) {
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }

        Element parent = findElement(parentName);
        Element element = new Element();
        element.name = elementName;
        element.childPolicy = CHILD_POLICY_REPEAT;
        element.minChildren = minChildren;
        element.maxChildren = maxChildren;
        elementHash.put(elementName, element);
        parent.children.add(elementName);
    }

    protected void addObjectValue(
            String elementName, Class<?> classType,
            int arrayMinLength, int arrayMaxLength
    ) {
        Element element = findElement(elementName);

        ObjectValue objVal = new ObjectValue();
        objVal.classType = classType;
        objVal.arrayMaxLength = arrayMaxLength;
        objVal.arrayMinLength = arrayMinLength;
        objVal.valueType = VALUE_LIST;

        element.objectValue = objVal;
    }

    protected <T> void addObjectValue(
            String elementName, Class<T> classType,
            boolean required, T defaultValue
    ) {
        // note: reqired is an unused parameter
        Element element = findElement(elementName);

        ObjectValue<T> objVal = new ObjectValue<T>();
        objVal.classType = classType;
        objVal.defaultValue = defaultValue;
        objVal.valueType = VALUE_ARBITRARY;

        element.objectValue = objVal;
    }

    protected <T> void addObjectValue(
            String elementName, Class<T> classType,
            boolean required, T defaultValue,
            List<? extends T> enumeratedValues
    ) {
        // note: reqired is an unused parameter
        if (enumeratedValues == null || enumeratedValues.isEmpty()) {
            throw new IllegalArgumentException("enumeratedValues is empty or null");
        }

        try {
            for (T enumeratedValue : enumeratedValues) {
                if (enumeratedValue == null) {
                    throw new IllegalArgumentException("enumeratedValues contains a null!");
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("enumeratedValues contains a value not of class classType!");
        }

        Element element = findElement(elementName);

        ObjectValue<T> objVal = new ObjectValue<T>();
        objVal.classType = classType;
        objVal.defaultValue = defaultValue;
        objVal.enumeratedValues = enumeratedValues;
        objVal.valueType = VALUE_ENUMERATION;

        element.objectValue = objVal;
    }

    protected <T extends Object & Comparable<? super T>> void addObjectValue(
            String elementName, Class<T> classType,
            T defaultValue, Comparable<? super T> minValue, Comparable<? super T> maxValue,
            boolean minInclusive, boolean maxInclusive
    ) {
        Element element = findElement(elementName);

        ObjectValue<T> objVal = new ObjectValue<T>();
        objVal.classType = classType;
        objVal.defaultValue = defaultValue;
        objVal.minValue = minValue;
        objVal.maxValue = maxValue;
        objVal.minInclusive = minInclusive;
        objVal.maxInclusive = maxInclusive;

        objVal.valueType = VALUE_RANGE;
        objVal.valueType |= minInclusive ? VALUE_RANGE_MIN_INCLUSIVE_MASK : 0;
        objVal.valueType |= maxInclusive ? VALUE_RANGE_MAX_INCLUSIVE_MASK : 0;

        element.objectValue = objVal;
    }

    public int getAttributeDataType(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        return attr.dataType;
    }

    public String getAttributeDefaultValue(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        return attr.defaultValue;
    }

    public String getAttributeDescription(String elementName, String attrName, Locale locale) {
        findAttribute(elementName, attrName);
        return getResourceString(elementName + "/" + attrName, locale);
    }

    public String[] getAttributeEnumerations(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        if (attr.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Attribute is not an enumeration!");
        }

        return attr.enumeratedValues.toArray(new String[attr.enumeratedValues.size()]);
    }

    public int getAttributeListMaxLength(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute is not a list!");
        }
        return attr.listMaxLength;
    }

    public int getAttributeListMinLength(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute is not a list!");
        }
        return attr.listMinLength;
    }

    public String getAttributeMaxValue(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) == 0) {
            throw new IllegalArgumentException("Attribute is not a range!");
        }
        return attr.maxValue;        
    }

    public String getAttributeMinValue(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) == 0) {
            throw new IllegalArgumentException("Attribute is not a range!");
        }
        return attr.minValue;
    }

    public String[] getAttributeNames(String elementName) {
        Element element = findElement(elementName);
        return element.attributes.keySet().toArray(new String[element.attributes.size()]);
    }

    public int getAttributeValueType(String elementName, String attrName) {
        Attlist attr = findAttribute(elementName, attrName);
        return attr.valueType;                
    }

    public String[] getChildNames(String elementName) {
        Element element = findElement(elementName);
        if (element.childPolicy == CHILD_POLICY_EMPTY) { // Element cannot have children
            return null;
        }
        return element.children.toArray(new String[element.children.size()]);
    }

    public int getChildPolicy(String elementName) {
        Element element = findElement(elementName);
        return element.childPolicy;
    }

    public String getElementDescription(String elementName, Locale locale) {
        findElement(elementName); // Check if there is such element
        return getResourceString(elementName, locale);
    }

    public int getElementMaxChildren(String elementName) {
        Element element = findElement(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy is not CHILD_POLICY_REPEAT!");
        }
        return element.maxChildren;
    }

    public int getElementMinChildren(String elementName) {
        Element element = findElement(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy is not CHILD_POLICY_REPEAT!");
        }
        return element.minChildren;
    }

    public int getObjectArrayMaxLength(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null || v.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return v.arrayMaxLength;
    }

    public int getObjectArrayMinLength(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null || v.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return v.arrayMinLength;
    }

    public Class<?> getObjectClass(String elementName) {
        ObjectValue v = findObjectValue(elementName);
        return v.classType;
    }

    public Object getObjectDefaultValue(String elementName) {
        ObjectValue v = findObjectValue(elementName);
        return v.defaultValue;
    }

    public Object[] getObjectEnumerations(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null || v.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Not an enumeration!");
        }
        return v.enumeratedValues.toArray();
    }

    public Comparable<?> getObjectMaxValue(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null || (v.valueType & VALUE_RANGE) == 0) {
            throw new IllegalArgumentException("Not a range!");
        }
        return v.maxValue;
    }

    public Comparable<?> getObjectMinValue(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null || (v.valueType & VALUE_RANGE) == 0) {
            throw new IllegalArgumentException("Not a range!");
        }
        return v.minValue;
    }

    public int getObjectValueType(String elementName) {
        Element element = findElement(elementName);
        if (element.objectValue == null) {
            return VALUE_NONE;
        }
        return element.objectValue.valueType;
    }

    protected String getResourceBaseName() {
        return resourceBaseName;
    }

    public String getRootName() {
        return rootName;
    }

    public static IIOMetadataFormat getStandardFormatInstance() {
        if (standardFormat == null) {
            standardFormat = new IIOStandardMetadataFormat();
        }

        return standardFormat;
    }

    public boolean isAttributeRequired(String elementName, String attrName) {
        return findAttribute(elementName, attrName).required;
    }

    protected void removeAttribute(String elementName, String attrName) {
        Element element = findElement(elementName);
        element.attributes.remove(attrName);
    }

    protected void removeElement(String elementName) {
        Element element;
        if ((element = elementHash.get(elementName)) != null) {
            elementHash.remove(elementName);
            for (Element e : elementHash.values()) {
                e.children.remove(element.name);
            }
        }
    }

    protected void removeObjectValue(String elementName) {
        Element element = findElement(elementName);
        element.objectValue = null;
    }
    
    protected void setResourceBaseName(String resourceBaseName) {
        if (resourceBaseName == null) {
            throw new IllegalArgumentException("resourceBaseName == null!");
        }
        this.resourceBaseName = resourceBaseName;
    }

    @SuppressWarnings({"ClassWithoutConstructor"})
    private class Element {
        String name;

        ArrayList<String> children = new ArrayList<String>();
        HashMap<String, Attlist> attributes = new HashMap<String, Attlist>();

        int minChildren;
        int maxChildren;
        int childPolicy;

        ObjectValue objectValue;
    }

    @SuppressWarnings({"ClassWithoutConstructor"})
    private class Attlist {
        String name;

        int dataType;
        boolean required;
        int listMinLength;
        int listMaxLength;
        String defaultValue;
        List<String> enumeratedValues;
        String minValue;
        String maxValue;
        boolean minInclusive;
        boolean maxInclusive;

        int valueType;
    }

    @SuppressWarnings({"ClassWithoutConstructor"})
    private class ObjectValue<T> {
        Class<T> classType;
        int arrayMinLength;
        int arrayMaxLength;
        T defaultValue;
        List<? extends T> enumeratedValues;
        Comparable<? super T> minValue;
        Comparable<? super T> maxValue;
        boolean minInclusive;
        boolean maxInclusive;

        int valueType;
    }

    private Element findElement(String name) {
        Element element;
        if ((element = elementHash.get(name)) == null) {
            throw new IllegalArgumentException("element name is null or no such element: " + name);
        }

        return element;
    }

    private Attlist findAttribute(String elementName, String attributeName) {
        Element element = findElement(elementName);
        Attlist attribute;
        if ((attribute = element.attributes.get(attributeName)) == null) {
            throw new IllegalArgumentException("attribute name is null or no such attribute: " + attributeName);
        }

        return attribute;
    }

    private ObjectValue findObjectValue(String elementName) {
        Element element = findElement(elementName);
        ObjectValue v = element.objectValue;
        if (v == null) {
            throw new IllegalArgumentException("No object within element");
        }
        return v;
    }

    private String getResourceString(String key, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        // Get the context class loader and try to locate the bundle with it first
        ClassLoader contextClassloader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
        });

        // Now try to get the resource bundle
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle(resourceBaseName, locale, contextClassloader);
        } catch (MissingResourceException e) {
            try {
                rb = ResourceBundle.getBundle(resourceBaseName, locale);
            } catch (MissingResourceException e1) {
                return null;
            }
        }

        try {
            return rb.getString(key);
        } catch (MissingResourceException e) {
            return null;
        } catch (ClassCastException e) {
            return null; // Not a string resource
        }
    }
}
