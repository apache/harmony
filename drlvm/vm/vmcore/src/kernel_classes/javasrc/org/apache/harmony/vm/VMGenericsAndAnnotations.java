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
package org.apache.harmony.vm;

import java.lang.annotation.Annotation;

/**
 * Provides the methods to get signatures used to encode Java programming 
 * language type information such as generic type and method declarations and 
 * parameterized types.
 * <p>
 * Provides also the methods to get annotations and their elements values.
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more detailes.
 * 
 * @author Serguei S. Zapreyev, Alexey V. Varlamov
 * 
 * @api2vm
 */
public final class VMGenericsAndAnnotations {

    /**
     * This class is not supposed to be instantiated.
     */
    private VMGenericsAndAnnotations() {
    }
    
    /**
     * This method returns the String representation of a Signature 
     * attribute corresponding to {@link Method}, {@link Constructor} 
     * or {@link Field} object declaration.
     * <p>
     * @param id an identifier of a reflection member.
     * @return the String representation of a Signature attribute or null 
     * if a caller's declaration does not use any generics.
     *
     * @api2vm
     */
    public static native String getSignature(long id);
    
    /**
     * This method returns the String representation of a Signature 
     * attribute corresponding to {@link Class} object declaration.
     * <p>
     * @param cls a class to be reflected.
     * @return the String representation of a Signature attribute or null 
     * if a caller's declaration does not use any generics.
     *
     * @api2vm
     */
    public static native String getSignature(Class cls);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Field#getDeclaredAnnotations() Field.getDeclaredAnnotations()}, 
     * {@link Method#getDeclaredAnnotations() Method.getDeclaredAnnotations()},
     * {@link Constructor#getDeclaredAnnotations()
     * Constructor.getDeclaredAnnotations()} methods.
     * <p>
     * @param id an identifier of the caller (Field, or Method,
     * or Constructor type).
     * @return all annotations directly present on this element or zero-sized 
     * array if there are no annotations.
     * @throws TypeNotPresentException if enum-valued or nested annotation member 
     * (of an annotation) refers to a class that is not accessible.
     * @throws AnnotationFormatError if reading an annotation from a class file    
     * determines that the annotation is malformed.
     * @api2vm
     */
    public static native Annotation[] getDeclaredAnnotations(long id);
    
    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaredAnnotations() Class.getDeclaredAnnotations()}. 
     * <p>
     * @param clss annotated element of the Class type
     * @return all annotations directly present on this element or zero-sized 
     * array if there are no annotations.
     * @throws TypeNotPresentException if enum-valued or nested annotation member 
     * (of an annotation) refers to a class that is not accessible.
     * @throws AnnotationFormatError if reading an annotation from a class file    
     * determines that the annotation is malformed.
     * @api2vm
     */
    public static native Annotation[] getDeclaredAnnotations(Class clss);
    
    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#getParameterAnnotations() Method.getParameterAnnotations()}
     * and {@link Constructor#getParameterAnnotations() 
     * Constructor.getParameterAnnotations()} methods.
     * <p>
     * @param id an identifier of the caller (annotated element of the Method 
     * or Constructor type) class.
     * @return an array of arrays that represent the annotations on the formal
     * parameters, in declaration order, of the method represented by 
     * this Method object. Returns an array of length zero if the underlying method has 
     * no parameters. If the method has a parameter or more, a nested array of length
     * zero is returned for each parameter with no annotations.
     * @throws TypeNotPresentException if Class-valued member (of an annotation)
     * referring to a class that is not accessible in this VM.
     * @api2vm
     */     
    public static native Annotation[][] getParameterAnnotations(long id);
    
    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#getDefaultValue() Method.getDefaultValue()} method. 
     * But it takes one additional id parameter.
     * <p>
     * @param id an identifier of the caller (annotated element of the Method 
     * type) class.
     * @return the default value for the annotation member represented by this
     * Method instance or null.
     * @throws TypeNotPresentException if the annotation is of type Class and
     * no definition can be found for the default class value
     *
     * @api2vm
     */
    public static native Object getDefaultValue(long id);
}
