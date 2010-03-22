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
 * @author Intel, Pavel Pervov
 */
#ifndef _VM_CLASS_MANIPULATION_H
#define _VM_CLASS_MANIPULATION_H

#include "open/types.h"
#include "open/common.h"

/**
 * @file
 * Part of Class Support interface related to retrieving and changing
 * different class properties.
 *
 * The list of properties includes, but is not limited to, class name
 * class super class, fields, methods and so on.
 */

#ifdef __cplusplus
extern "C" {
#endif

/** 
 * Returns the class name.
 *
 * @param klass - the class handle
 *
 * @return Class name bytes.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(const char*, class_get_name, (Class_Handle klass));

/**
 * Returns class major version.
 *
 * @param klass - class handler
 *
 * @return Class major version.
 *
 * @note Assertion is raised if <code>klass</code> is equal
 *       to <code>NULL</code>.
 */
DECLARE_OPEN(unsigned short, class_get_version, (Class_Handle klass));

/** 
 * Returns the class name.
 *
 * @param klass - the class handle
 *
 * @return Class name bytes.
 *
 * @ingroup Extended
 */
const char*
class_get_java_name(Class_Handle klass);


/** 
 * Returns the super class of the current class.
 *
 * @param klass - the class handle
 *
 * @return The class handle of the super class.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(Class_Handle, class_get_super_class, (Class_Handle klass));

/**
 * Returns non-zero value if the class represented by Class_Handle
 * is a descendant of java.lang.ref.Reference. The particular type
 * of reference (weak, soft or phantom) is encoded by the return
 * value as WeakReferenceType.
 *
 * @param clss - the class handle
 *
 * @return Type of weak reference represented by the class handle;
 *         <code>NOT_REFERENCE</code> (0) otherwise
 */
VMEXPORT WeakReferenceType class_is_reference(Class_Handle clss);

/**
 * Returns the class loader of the current class.
 *
 * @param klass - the class handle
 *
 * @return The class loader.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(Class_Loader_Handle, class_get_class_loader, (Class_Handle klass));

/**
 * Checks whether the current class is a primitive type.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> for a primitive class; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_primitive, (Class_Handle klass));

/**
 * Checks whether the current class is an array.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> for an array; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_array, (Class_Handle klass));

/**
 * Checks whether the current class is an instance of another class.
 *
 * @param klass - the class handle
 * @param super_klass - super class handle
 *
 * @return <code>TRUE</code> if <i>klass</i> is an instance of a <i>super_class</i>; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> or <i>super_klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_instanceof, (Class_Handle klass, Class_Handle super_klass));

/**
 * Checks whether the current class is abstract.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> for an abstract class; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 * @note Replaces the class_is_abstract function.
 */
DECLARE_OPEN(BOOLEAN, class_is_abstract, (Class_Handle klass));

/**
 * Checks whether the current class is an interface class.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> for an interface class; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_interface, (Class_Handle klass));

/**
 * Checks whether the current class is final.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> for a final class; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_final, (Class_Handle klass));

/**
 * Checks whether the given classes are the same.
 *
 * @param klass1  - the first class handle
 * @param klass2 - the second class handle
 *
 * @return <code>TRUE</code> for the same classes; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>klass1</i> or <i>klass2</i> equal to <code>NULL</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_same_class, (Class_Handle klass1, Class_Handle klass2));

/**
 * Checks whether the given classes have the same package.
 *
 * @param klass1 - class handler
 * @param klass2 - class handler
 *
 * @return If classes have the same package returns <code>true</code>, else returns <code>false</code>.
 *
 * @note Assertion is raised if klass1 or klass2 are equal to null.
 */
DECLARE_OPEN(BOOLEAN, class_is_same_package, (Class_Handle klass1, Class_Handle klass2));

/**
 * Returns number of super interfaces of the class.
 *
 * @param klass - class handler
 *
 * @return Number of super interfaces of class.
 *
 * @note Assertion is raised if klass is equal to null.
 */
DECLARE_OPEN(unsigned short, class_get_superinterface_number, (Class_Handle klass));

/**
 * Returns superinterface of class by the given number.
 *
 * @param klass - class handler
 * @param index - super interface number
 *
 * @return Super interface of class.
 *
 * @note Assertion is raised if klass is equal to null or index is out of range.
 */
DECLARE_OPEN(Class_Handle, class_get_superinterface, (Class_Handle klass, unsigned short index));

/**
 * Returns the offset of the referent field 
 * in the <code>java.lang.ref.Reference</code> object.
 *
 * It is assumed that the class represents the reference object,
 * that is, running the class_is_reference function returns a non-zero value.
 *
 * @note The returned value is most probably a constant
 *             and does not depend on the class.
 *
 * @note This interface allows only one weak, soft or phantom reference per object.
 *             It seems to be sufficient for the JVM spec.
 */
DECLARE_OPEN(unsigned, class_get_referent_offset, (Class_Handle clss));

/**
 * Returns the VM_Data_Type value for the given class.
 *
 * @param klass - the class handle
 *
 * @return The VM_Data_Type value.
 */
DECLARE_OPEN(VM_Data_Type, class_get_primitive_type_of_class, (Class_Handle klass));

/**
 * Returns the class corresponding to the primitive type.
 *
 * @param type - the primitive type
 *
 * @return The class corresponding to a primitive type.
 *
 * @note For all primitive types <i>type</i> is:
 *            <code>type == class_get_primitive_type_of_class(class_get_class_of_primitive_type(type))</code>
 */
DECLARE_OPEN(Class_Handle, class_get_class_of_primitive_type, (VM_Data_Type type));

/**
 * For given a class handle <i>klass</i> constructs a class of
 * the type representing on-dimentional array of <i>klass</i>.
 * For example, given the class of Ljava/lang/String; this function
 * will return array class [Ljava/lang/String;.
 *
 * @param klass - the class handle
 *
 * @return The class handle of the type representing the array of <i>klass</i>.
 */
DECLARE_OPEN(Class_Handle, class_get_array_of_class, (Class_Handle klass));

/**
 * Returns the class of the array element of the given class.
 *
 * @param klass - the class handle
 *
 * @return The class of the array element of the given class.
 *
 * @note The behavior is undefined if the parameter does not represent
 * an array class.
 */
DECLARE_OPEN(Class_Handle, class_get_array_element_class, (Class_Handle klass));

/**
 * Returns class with the given name extended by the given class.
 *
 * @param klass      - checked klass
 * @param super_name - parent class name
 *
 * @return If the given class extends the class with given name,
 *         function returns extended class handle, otherwise <code>NULL</code> is returned.
 *
 * @note Assertion is raised if <i>klass</i> or <i>super_name</i> are equal to null.
 */
DECLARE_OPEN(Class_Handle, class_get_extended_class, (Class_Handle klass, const char* super_name));

/**
 * Function returns number of methods for current class.
 * @param klass - class handler
 * @return Number of methods for class.
 * @note Assertion is raised if klass is equal to null.
 */
DECLARE_OPEN(unsigned short, class_get_method_number, (Class_Handle klass));

/** 
 * Function returns method of current class.
 * @param klass - class handler
 * @param index - method index
 * @return Method handler.
 * @note Assertion is raised if klass is equal to null or index is out of range.
 */
DECLARE_OPEN(Method_Handle, class_get_method, (Class_Handle klass, unsigned short index));

/**
 * Returns the type info for the elements of the array for array classes.
 *
 * @param klass - the class handle
 *
 * @return Type information for the elements of the array.
 */
DECLARE_OPEN(Type_Info_Handle, class_get_element_type_info, (Class_Handle klass));

/**
 * Gets the handle for the field. 
 *
 * @param klass - the class handle
 * @param index - this value is the sequence number of field in the set of fields
 *                both inherited and defined in this class.
 *
 * @return The handle for the field. If <i>index</i> is greater than or equal to
 * <code>class_num_instance_fields_recursive</code>, function returns NULL.
 */
DECLARE_OPEN(Field_Handle, class_get_instance_field_recursive, (Class_Handle klass, unsigned index));

/**
 * Returns the size of the element of the array for class handles that represent an array.
 * 
 * This function is a combination of functions class_get_instance_size and
 * class_get_array_element_class. 
 *
 * @param klass - the class handle
 *
 * @return The size of the element of the array.
 *
 * @ingroup Extended
 */
DECLARE_OPEN(unsigned, class_get_array_element_size, (Class_Handle klass));

/**
 * Returns the vtable handle for the given class.
 *
 * @param klass - the class handle
 *
 * @return The vtable handle for the given class.
 */
DECLARE_OPEN(VTable_Handle, class_get_vtable, (Class_Handle klass));

/**
 * Verifies that the class is fully initialized.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> if the class is already fully
 *                initialized; otherwise, <code>FALSE</code>. 
 */
DECLARE_OPEN(BOOLEAN, class_is_initialized, (Class_Handle klass));

/**
 * Gets the alignment of the class.
 *
 * @param klass - the class handle
 *
 * @return The alignment of the class.
 */
DECLARE_OPEN(unsigned, class_get_alignment, (Class_Handle klass));

/**
 * Checks whether the class has a non-trivial finalizer.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> if the class has a non-trivial finalizer. 
 *                : otherwise, <code>FALSE</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_finalizable, (Class_Handle klass));

/**
 * Checks whether the class is an array of primitives.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> if this is an array of primitives.
 *               : otherwise, <code>FALSE</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_non_ref_array, (Class_Handle klass));

/**
 * Checks whether the class represented by Class_Handle
 * is a descendant of <code>java.lang.ref.Reference</code>. 
 * 
 * The type of reference (weak, soft or phantom) is encoded in the return 
 * value of WeakReferenceType.
 *
 * @param klass - the class handle
 *
 * @return One of WEAK_REFERENCE, SOFT_REFERENCE, or PHANTOM_REFERENCE if
 * the given class is corresponding descendant of <code>java.lang.ref.Reference</code>.
 * NOT_REFERENCE (0) is returned otherwise.
 */
WeakReferenceType
class_get_reference_type(Class_Handle klass);

/**
 * Checks whether the class is likely to be used as an exception object.
 *
 * This is a hint only. Even if this function returns <code>FALSE</code>,
 * the class might still be used for exceptions.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> if the class is likely to be used
 *                as an exception object; otherwise, <code>FALSE</code>. 
 */
DECLARE_OPEN(BOOLEAN, class_is_throwable, (Class_Handle klass));

/**
 * Checks whether the class represents an enumerator.
 *
 * @param klass - the class handle
 *
 * @return <code>TRUE</code> if the class represents an enum.
 *               : otherwise, <code>FALSE</code>.
 */
DECLARE_OPEN(BOOLEAN, class_is_enum, (Class_Handle klass));

/**
 * Returns the number of instance fields defined in the given class.
 * This number includes inherited fields.
 *
 * @param klass - the class handle
 *
 * @return The number of instance fields defined in a class.
 *
 * @note Replaces the class_num_instance_fields_recursive function.
 */
unsigned
class_get_all_instance_fields_number(Class_Handle klass);

/**
 * Returns the name of the package containing the class.
 *
 * @param klass - the class handle
 *
 * @return The name of the package containing the class.
 *
 * @note Not used
 */
DECLARE_OPEN(const char*, class_get_package_name, (Class_Handle klass));

/**
 * Returns the pointer to the location of the constant.
 *
 * @param klass  - the class handle
 * @param index - interpreted as a constant pool index
 *
 * @return The pointer to the location of the constant in the
 * constant pool of the class.
 *
 * @note This function must not be called for constant strings.  
 *             Instead, one of the following can be done:
 *            <ol>
 *               <li>JIT-compiled code gets the string object at run time by calling
 *                   VM_RT_LDC_STRING
 *               <li>The class_get_const_string_intern_addr() function is used.
 *            </ol>
 */
DECLARE_OPEN(const void *, class_cp_get_const_addr, (Class_Handle klass, unsigned short index));

/**
 * Returns the type of the compile-time constant.
 *
 * @param klass  - the class handle
 * @param index - interpreted as a constant pool index
 *
 * @return The type of a compile-time constant.
 */
DECLARE_OPEN(VM_Data_Type, class_cp_get_const_type, (Class_Handle ch, unsigned short idx));

/**
 * Returns the address of the interned version of the string.
 * 
 * Calling this function has a side-effect of interning the string,
 * so that the JIT compiler can load a reference to the interned string
 * without checking whether it is <code>NULL</code>.
 *
 * @param klass  - the class handle
 * @param index - interpreted as a constant pool index
 *
 * @return The address of the interned version of the string.
 *
 * @note Not used
 * FIXME the above side effect is no longer true.
 */
DECLARE_OPEN(const void*, class_get_const_string_intern_addr, (Class_Handle ch, unsigned short idx));

/**
 * Checks whether the entry by the specified constant pool index is resolved.
 *
 * @param klass     - the class handle
 * @param cp_index  - interpreted as a constant pool index
 *
 * @return <code>TRUE</code> if the constant pool entry is resolved; <code>FALSE</code> otherwise.
 */
DECLARE_OPEN(BOOLEAN, class_cp_is_entry_resolved, (Class_Handle klass, unsigned short cp_index));

/**
 * Returns the name for the field or method/interface in the constant pool entry.
 *
 * @param klass         - the class handle
 * @param cp_index - interpreted as a constant pool index
 *
 * @return The name for field or method/interface in constant pool entry.
 */
DECLARE_OPEN(const char*, class_cp_get_entry_name, (Class_Handle cl, unsigned short index));

/**
 * Returns the descriptor for the field or method/interface in the constant pool entry.
 *
 * @param klass         - the class handle
 * @param cp_index - interpreted as a constant pool index
 *
 * @return The descriptor for field or method/interface in constant pool entry.
 */
DECLARE_OPEN(const char*, class_cp_get_entry_descriptor, (Class_Handle klass, unsigned short cp_index));

/**
 * Returns the name of the class of the field or method/interface in the constant pool entry.
 *
 * @param klass         - the class handle
 * @param cp_index - interpreted as a constant pool index
 *
 * @return The name of the class of field or method/interface in constant pool entry.
 */
DECLARE_OPEN(const char *, class_cp_get_entry_class_name, (Class_Handle cl, unsigned short index));

/**
 * Returns number of dimentions of symbolic link to an array class
 *
 * @param klass     - the class handle
 * @param cp_index  - index into the constant pool of the <code>klass</code>
 *
 * @return The number of dimentions in array linked from the constant pool
 */
DECLARE_OPEN(unsigned, class_cp_get_num_array_dimensions, (Class_Handle klass, unsigned short cp_index));

/**
 * Returns the data type for the field in the constant pool entry.
 *
 * @param klass         - the class handle
 * @param cp_index - interpreted as a constant pool index
 *
 * @return The data type for the field in the constant pool entry.
 */
DECLARE_OPEN(VM_Data_Type, class_cp_get_field_type, (Class_Handle src_class, unsigned short cp_index));

/**
 * Returns the class constant pool size.
 *
 * @param klass - the class handle
 *
 * @return Constant pool size.
 *
 * @note An assertion is raised if <i>klass</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(unsigned short, class_cp_get_size, (Class_Handle klass));

/**
 * Returns the constant pool entry tag.
 *
 * @param klass  - the class handle
 * @param index - the constant pool entry index
 *
 * @return The constant pool entry tag.
 *
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned char, class_cp_get_tag, (Class_Handle klass, unsigned short index));

/** 
 * Returns the class name entry index in the constant pool.
 * This function is only legal for constant pool entries with CONSTANT_Class tag.
 *
 * @param klass  - the class handle
 * @param index - the constant pool entry index
 *
 * @return The class name entry index.
 *
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_class_name_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns the class name from the constant pool.
 * This function is only legal for constant pool entries with CONSTANT_Class tag.
 *
 * @param klass - the class handle
 * @param index - the constant pool entry index
 *
 * @return The class name.
 *
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(const char*, class_cp_get_class_name, (Class_Handle cl, unsigned short index));

/** 
 * Returns the class name entry index in the constant pool.
 * 
 * The function is legal for constant pool entries with  CONSTANT_Fieldref, 
 * CONSTANT_Methodref and CONSTANT_InterfaceMethodref tags.
 *
 * @param klass  - the class handle
 * @param index - the constant pool entry index
 *
 * @return The class name entry index.
 *
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_ref_class_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns the name and type entry index in the constant pool.
 *
 * @param klass  - the class handle
 * @param index - the constant pool entry index
 *
 * @return The name_and_type entry index.
 *
 * @note Function is valid for constant pool entries with 
 *       CONSTANT_Fieldref, CONSTANT_Methodref and CONSTANT_InterfaceMethodref tags.
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_ref_name_and_type_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns the string entry index in the constant pool.
 *
 * @param klass  - the class handle
 * @param index - the constant pool entry index
 *
 * @return The string entry index.
 *
 * @note Function is valid for constant pool entries with CONSTANT_String tag.
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_string_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns the name entry index in the constant pool.
 *
 * @param klass - the class handle
 * @param index - the constant pool entry index
 *
 * @return  The name entry index.
 *
 * @note Function is valid for constant pool entries with CONSTANT_NameAndType tag.
 * @note An assertion is raised if <i>klass</i> equals to 
 *       <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_name_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns the descriptor entry index in the constant pool.
 *
 * @param klass   - the class handle
 * @param index - the constant pool entry index
 *
 * @return The descriptor entry index.
 *
 * @note Function is valid for constant pool entries with CONSTANT_NameAndType tag.
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(unsigned short, class_cp_get_descriptor_index, (Class_Handle klass, unsigned short index));

/** 
 * Returns bytes for the UTF8 constant pool entry.
 * This function is legal for constant pool entries with CONSTANT_UTF8 tags.
 *
 * @param klass - the class handle
 * @param index - the constant pool entry index
 *
 * @return Bytes for the UTF8 constant pool entry. 
 *
 * @note Function is valid for constant pool entries with CONSTANT_UTF8 tag.
 * @note An assertion is raised if <i>klass</i> equals to 
 *            <code>NULL</code> or if <i>index</i> is out of range.
 */
DECLARE_OPEN(const char*, class_cp_get_utf8_bytes, (Class_Handle klass, unsigned short index));

/**
 * Function sets verify data to a given class.
 *
 * @param klass     - class handler
 * @param data      - verify data
 *
 * @note Assertion is raised if class is equal to null.
 * @note Function makes non thread safe operation and 
 *       must be called in thread safe manner.
 */
DECLARE_OPEN(void, class_set_verify_data_ptr, (Class_Handle klass, void* data));

/**
 * Function returns verify data for a given class.
 *
 * @param klass - class handler
 *
 * @return Verify data for a given class.
 *
 * @note Assertion is raised if klass is equal to null.
 */
DECLARE_OPEN(void*, class_get_verify_data_ptr, (Class_Handle klass));

/**
 * Resolves the class for the constant pool entry.
 *
 * @param klass - the class handle
 * @param index - the constant pool entry index
 * @param exc   - the pointer to exception
 *
 * @return The class resolved for the constant pool entry.
 *
 * @note Replaces the vm_resolve_class and resolve_class functions.
 */
Class_Handle
class_resolve_class(Class_Handle klass, unsigned short index);

/**
 * Resolves the class for the constant pool entry 
 * and checks whether an instance can be created.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 * @param exc     - the pointer to the exception
 *
 * @return The class resolved for the constant pool entry. If no instances
 *                 of the class can be created, returns <code>NULL</code> and raises and exception.
 *
 * @note Replaces the vm_resolve_class_new and resolve_class_new functions.
 *
 */
Class_Handle
class_resolve_class_new(Class_Handle klass, unsigned short index);

/**
 * Resolves the class method for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 *
 * @return  interface method resolved for the constant pool entry.
 */
DECLARE_OPEN(Method_Handle, class_resolve_method, (Class_Handle klass, unsigned short index));

/**
 * Resolves the class interface method for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 *
 * @return  interface method resolved for the constant pool entry.
 *
 * @note Replace the resolve_interface_method function.
 */
Method_Handle
class_resolve_interface_method(Class_Handle klass, unsigned short index);

/**
 * Resolves class static method for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 * @param exc     - the pointer to exception
 *
 * @return The static method resolved for the constant pool entry.
 *
 * @note Replaces the resolve_static_method function.
 */
Method_Handle
class_resolve_static_method(Class_Handle klass, unsigned short index);

/**
 * Resolves the class virtual method for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 * @param exc     - the pointer to exception
 *
 * @return The virtual method resolved for the constant pool entry.
 *
 * @note Replaces the resolve_virtual_method function.
 */
Method_Handle
class_resolve_virtual_method(Class_Handle klass, unsigned short index);

/**
 * Resolves the class special method for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 * @param exc     - pointer to exception
 *
 * @return The special method resolved for the constant pool entry.
 *
 * @note Replaces the resolve_special_method function.
 */
Method_Handle
class_resolve_special_method(Class_Handle klass, unsigned short index);

/**
 * Resolves the class static field for the constant pool entry.
 *
 * @param klass - the class handle
 * @param index - the constant pool entry index
 * @param exc   - pointer to exception
 *
 * @return The static field resolved for the constant pool entry.
 *
 * @note Replaces the resolve_static_field function.
 */
Field_Handle
class_resolve_static_field(Class_Handle klass, unsigned short index);

/**
 * Resolves the class non-static field for the constant pool entry.
 *
 * @param klass   - the class handle
 * @param index  - the constant pool entry index
 * @param exc     - pointer to exception
 *
 * @return The non-static field resolved for the constant pool entry.
 *
 * @note Replaces the resolve_nonstatic_field function.
 */
DECLARE_OPEN(Field_Handle, class_resolve_nonstatic_field, (Class_Handle klass, unsigned short index));

/**
 * Provides the initialization phase for the given class.
 *
 * @param klass  - class handle
 *
 * @note For interpreter use only.
 */
DECLARE_OPEN(void, class_initialize, (Class_Handle klass));

#ifdef __cplusplus
}
#endif

#endif // _VM_CLASS_MANIPULATION_H
