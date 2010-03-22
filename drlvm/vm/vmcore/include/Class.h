/**
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
 * @author Pavel Pervov
 */
#ifndef _CLASS_H_
#define _CLASS_H_
/**
 * @file
 * Interfaces to class functionality.
 */

#include <assert.h>
#include "open/gc.h"
#include "open/rt_types.h"
#include "port_malloc.h"
#include "String_Pool.h"
#include "jit_intf.h"

#include <vector>

//
// magic number, and major/minor version numbers of class file
//
#define CLASSFILE_MAGIC 0xCAFEBABE
#define CLASSFILE_MAJOR_MIN 45
// Supported class files up to this version
#define CLASSFILE_MAJOR_MAX 50
#define CLASSFILE_MINOR_MAX 0

// forward declarations
struct Class;

// external declarations
class Class_Member;
struct Field;
struct Method;
struct Class_Extended_Notification_Record;
class CodeChunkInfo;
class Lock_Manager;
class ByteReader;
struct ClassLoader;
class JIT;
struct Global_Env;
class Package;
struct VM_thread;
struct AnnotationTable;
struct VTable;
struct Intfc_Table;

/** The constant pool entry descriptor.
* For each constant pool entry, the descriptor content varies depending
* on the constant pool tag that corresponds to this constant pool entry.
* Content of each entry is described in
* <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/ClassFileFormat-Java5.pdf">
* The Java Virtual Machine Specification, Chapter 4</a>, <i>The Constant
* Pool</i> section, with the following exceptions:<ol>
* <li>A zero entry of the constant pool contains an array of tags
*  corresponding to entries in the constant pool entries array.</li>
* <li>As required by
* <a href=http://java.sun.com/docs/books/vmspec/2nd-edition/ConstantPool.pdf>
* The Java Virtual Machine Specification, Chapter 5</a>
* <i>Linking/Resolution</i> section, errors are cached for entries that have
* not been resolved earlier for some reason.</li></ol>
*/
union ConstPoolEntry {
    /** Zero entry of constant pool only: array of tags for constant pool.*/
    unsigned char* tags;
    /** CONSTANT_Class.*/
    struct {
        union {
            /** Resolved class*/
            Class* klass;
            /** Resolution error, if any.*/
            struct {
                /** Next resolution error in this constant pool.*/
                ConstPoolEntry* next;
                /** Exception object describing an error.*/
                ManagedObject* cause;
            } error;
        };
        /** Index to class name in this constant pool.*/
        uint16 name_index;
    } CONSTANT_Class;
    /** CONSTANT_String.*/
    struct {
        /** Resolved class.*/
        String* string;
        /** Index of CONSTANT_Utf8 for this string.*/
        uint16 string_index;
    }  CONSTANT_String;
    /** CONSTANT_{Field|Method|InterfaceMethod}ref.*/
    struct {
        union {
            /** Generic class member for CONSTANT_*ref.
            * Only valid for resolved refs.*/
            Class_Member* member;
            /** Resolved entry for CONSTANT_Fieldref.*/
            Field*  field;
            /** resolved entry for CONSTANT_[Interface]Methodref.*/
            Method* method;
            /** Resolution error, if any.*/
            struct {
                /** Next resolution error in this constant pool.*/
                ConstPoolEntry* next;
                /** Exception object describing error.*/
                ManagedObject* cause;
            } error;
        };
        /** Index of CONSTANT_Class for this CONSTANT_*ref.*/
        uint16 class_index;
        /** Index of CONSTANT_NameAndType for CONSTANT_*ref.*/
        uint16 name_and_type_index;
    } CONSTANT_ref;

    /** Shortcut to resolution error in CONSTANT_Class and CONSTANT_ref.*/
    struct {
        /** Next resolution error in this constant pool.*/
        ConstPoolEntry* next;
        /** Exception object describing error.*/
        ManagedObject* cause;
    } error;

    /** CONSTANT_Integer.*/
    U_32 int_value;
    /** CONSTANT_Float.*/
    float float_value;
    /** CONSTANT_Long and CONSTANT_Double.
    * @note In this case we pack all 8 bytes of long/double in one
    * ConstPoolEntry and leave the second ConstPoolEntry of the long/double
    * unused.*/
    struct {
        U_32 low_bytes;
        U_32 high_bytes;
    } CONSTANT_8byte;
    /** CONSTANT_NameAndType.*/
    struct {
        /** Resolved name.*/
        String* name;
        /** Resolved descriptor.*/
        String* descriptor;
        /** Name index in this constant pool.*/
        uint16 name_index;
        /** Descriptor index in this constant pool.*/
        uint16 descriptor_index;
    } CONSTANT_NameAndType;
    /** CONSTANT_Utf8.*/
    struct {
        /** Content of CONSTANT_Utf8 entry.*/
        String* string;
    } CONSTANT_Utf8;
};


/** Types of constant pool entries. These entry types are defined by a seperate
* byte array that the first constant pool entry points at.*/
enum ConstPoolTags {
    /** pointer to the tags array.*/
    CONSTANT_Tags               = 0,
    /** The next 11 tag values are taken from
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/ClassFileFormat-Java5.pdf">
     * The Java Virtual Machine Specification, Chapter 4</a>, <i>The Constant
     * Pool</i> section.*/
    CONSTANT_Utf8               = 1,
    CONSTANT_Integer            = 3,
    CONSTANT_Float              = 4,
    CONSTANT_Long               = 5,
    CONSTANT_Double             = 6,
    CONSTANT_Class              = 7,
    CONSTANT_String             = 8,
    CONSTANT_Fieldref           = 9,
    CONSTANT_Methodref          = 10,
    CONSTANT_InterfaceMethodref = 11,
    CONSTANT_NameAndType        = 12,
    CONSTANT_Last               = CONSTANT_NameAndType,
    /** used to mark second entry of Long and Double*/
    CONSTANT_UnusedEntry        = CONSTANT_Last + 1,    
};


/** The constant pool of a class and related operations.
 * The structure covers all operations that may be required to run
 * on the constant pool, such as parsing and processing queries.*/

struct ConstantPool {
private:
    // tag mask; 4 bits are sufficient for tag
    static const unsigned char TAG_MASK = 0x0F;
    // this entry contains resolution error information
    static const unsigned char ERROR_MASK = 0x40;
    // "entry is resolved" flag; msb
    static const unsigned char RESOLVED_MASK = 0x80;

    // constant pool size
    uint16 m_size;
    // constant pool entries; 0-th entry contains array of constant pool tags
    // for all entries
    ConstPoolEntry* m_entries;
    // List of constant pool entries, which resolution had failed
    // Required for fast enumeration of error objects
    ConstPoolEntry* m_failedResolution;

public:
    /** Initializes the constant pool to its initial values.*/
    ConstantPool() {
        init();
    }
    /** Clears constant pool content (if there are any).*/
    ~ConstantPool() {
        clear();
    }

    /** Checks whether the constant pool is not empty.
     * @return <code>true</code> if the constant pool contains
     *         certain entries; otherwise <code>false</code>.*/
    bool available() const { return m_size != 0; }

    /** Gets the size of the given constant pool.
     * @return The number of entries in the constant pool.*/
    uint16 get_size() const { return m_size; }

    /** Checks whether the index is a valid one in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the index is a valid one in the constant
     *         pool; otherwise <code>false</code>.*/
    bool is_valid_index(uint16 index) const {
        // index is valid if it's greater than zero and less than m_size
        // See specification 4.2 about constant_pool_count
        return index != 0 && index < m_size;
    }

    /** Checks whether the constant-pool entry is resolved.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the entry is resolved;
     *         otherwise <code>false</code>.*/
    bool is_entry_resolved(uint16 index) const {
        assert(is_valid_index(index));
        return (m_entries[0].tags[index] & RESOLVED_MASK) != 0;
    }

    /** Checks whether the resolution of the constant-pool entry has failed.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the resolution error is recorded
     *         for the entry.*/
    bool is_entry_in_error(uint16 index) const {
        assert(is_valid_index(index));
        return (m_entries[0].tags[index] & ERROR_MASK) != 0;
    }

    /** Checks whether the constant-pool entry represents the string of
     * the #CONSTANT_Utf8 type.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry is the <code>utf8</code> 
     *         string; otherwise <code>false</code>.*/
    bool is_utf8(uint16 index) const {
        return get_tag(index) == CONSTANT_Utf8;
    }

    /** Checks whether the constant-pool entry refers to a #CONSTANT_Class.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry is a class;
     *         otherwise <code>false</code>.*/
    bool is_class(uint16 index) const {
        return get_tag(index) == CONSTANT_Class;
    }

    /** Checks whether the constant-pool entry contains a constant.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains a constant; 
     *         otherwise <code>false</code>.*/
    bool is_constant(uint16 index) const {
        return get_tag(index) == CONSTANT_Integer
            || get_tag(index) == CONSTANT_Float
            || get_tag(index) == CONSTANT_Long
            || get_tag(index) == CONSTANT_Double
            || get_tag(index) == CONSTANT_String
            || get_tag(index) == CONSTANT_Class;
    }

    /** Checks whether the constant-pool entry is a literal constant.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains a string; 
     *         otherwise <code>false</code>.*/
    bool is_string(uint16 index) const {
        return get_tag(index) == CONSTANT_String;
    }

    /** Checks whether the constant-pool entry is #CONSTANT_NameAndType.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains name-and-type;
     *         otherwise <code>false</code>.*/
    bool is_name_and_type(uint16 index) const {
        return get_tag(index) == CONSTANT_NameAndType;
    }

    /** Checks whether the constant-pool entry contains a field reference,
     * #CONSTANT_Fieldref.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains a field reference;
     *         otherwise <code>false</code>.*/
    bool is_fieldref(uint16 index) const {
        return get_tag(index) == CONSTANT_Fieldref;
    }

    /** Checks whether the constant-pool entry contains a method reference,
     * #CONSTANT_Methodref.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains a method reference;
     *         otherwise <code>false</code>.*/
    bool is_methodref(uint16 index) const {
        return get_tag(index) == CONSTANT_Methodref;
    }

    /** Checks whether the constant-pool entry constains an interface-method
     * reference, #CONSTANT_InterfaceMethodref.
     * @param[in] index - an index in the constant pool
     * @return <code>true</code> if the given entry contains an interface-method
     *         reference; otherwise <code>false</code>.*/
    bool is_interfacemethodref(uint16 index) const {
        return get_tag(index) == CONSTANT_InterfaceMethodref;
    }

    /** Gets a tag of the referenced constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return A constant-pool entry tag for a given index.*/
    unsigned char get_tag(uint16 index) const {
        assert(is_valid_index(index));
        return m_entries[0].tags[index] & TAG_MASK;
    }

    /** Gets characters from the <code>utf8</code> string stored in the
     * constant pool.
     * @param[in] index - an index in the constant pool
     * @return Characters from the <code>utf8</code> string stored in
     *         the constant pool.*/
    const char* get_utf8_chars(uint16 index) const {
        return get_utf8_string(index)->bytes;
    }

    /** Gets the <code>utf8</code> string stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return The <code>utf8</code> string.*/
    String* get_utf8_string(uint16 index) const {
        assert(is_utf8(index));
        return m_entries[index].CONSTANT_Utf8.string;
    }

    /** Gets characters stored in the <code>utf8</code> string for
     * the #CONSTANT_String entry.
     * @param[in] index - an index in the constant pool
     * @return The <code>utf8</code> string characters for the given
     *         constant-pool entry.*/
    const char* get_string_chars(uint16 index) const {
        return get_string(index)->bytes;
    }

    /** Gets the <code>utf8</code> string stored for the #CONSTANT_String
     * entry.
     * @param[in] index - an index in the constant pool
     * @return The <code>utf8</code> string stored in the constant-pool entry.*/
    String* get_string(uint16 index) const {
        assert(is_string(index));
        return m_entries[index].CONSTANT_String.string;
    }

    /** Gets the <code>utf8</code> string representing the name part of the
     * name-and-type constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return The <code>utf8</code> string with the name part.*/
    String* get_name_and_type_name(uint16 index) const {
        assert(is_name_and_type(index));
        assert(is_entry_resolved(index));
        return m_entries[index].CONSTANT_NameAndType.name;
    }

    /** Gets the <code>utf8</code> string representing the descriptor part of
     * the name-and-type constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return The <code>utf8</code> string with the descriptor part.*/
    String* get_name_and_type_descriptor(uint16 index) const {
        assert(is_name_and_type(index));
        assert(is_entry_resolved(index));
        return m_entries[index].CONSTANT_NameAndType.descriptor;
    }

    /** Gets the generic class member for the <code>CONSTANT_*ref</code>
     * constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return The generic-class member for the given constant-pool entry.*/
    Class_Member* get_ref_class_member(uint16 index) const {
        assert(is_fieldref(index)
            || is_methodref(index)
            || is_interfacemethodref(index));
        return m_entries[index].CONSTANT_ref.member;
    }

    /** Gets the method from the #CONSTANT_Methodref or
     * the #CONSTANT_InterfaceMethodref constant-pool entry
     * @param[in] index - an index in the constant pool
     * @return The method from the given constant-pool entry.*/
    Method* get_ref_method(uint16 index) const {
        assert(is_methodref(index)
            || is_interfacemethodref(index));
        assert(is_entry_resolved(index));
        return m_entries[index].CONSTANT_ref.method;
    }

    /** Gets the field from the #CONSTANT_Fieldref
     * constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return The field from the given constant-pool entry.*/
    Field* get_ref_field(uint16 index) const {
        assert(is_fieldref(index));
        assert(is_entry_resolved(index));
        return m_entries[index].CONSTANT_ref.field;
    }

    /** Gets the class for the #CONSTANT_Class
     * constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return The class for the given constant-pool entry.*/
    Class* get_class_class(uint16 index) const {
        assert(is_class(index));
        assert(is_entry_resolved(index));
        return m_entries[index].CONSTANT_Class.klass;
    }

    /** Gets a 32-bit value (either interger or float) for a constant stored
     * in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return The value of a 32-bit constant stored in the constant pool.*/
    U_32 get_4byte(uint16 index) const {
        assert(get_tag(index) == CONSTANT_Integer
            || get_tag(index) == CONSTANT_Float);
        return m_entries[index].int_value;
    }

    /** Gets an integer value for a constant stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return The value of integer constant stored in the constant pool.*/
    U_32 get_int(uint16 index) const {
        assert(get_tag(index) == CONSTANT_Integer);
        return m_entries[index].int_value;
    }

    /** Gets a float value for a constant stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return A value of a float constant stored in the constant pool.*/
    float get_float(uint16 index) const {
        assert(get_tag(index) == CONSTANT_Float);
        return m_entries[index].float_value;
    }

    /** Gets a low word of the 64-bit constant (either long or double)
     * stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return A value of low 32-bits of 64-bit constant.*/
    U_32 get_8byte_low_word(uint16 index) const {
        assert(get_tag(index) == CONSTANT_Long
            || get_tag(index) == CONSTANT_Double);
        return m_entries[index].CONSTANT_8byte.low_bytes;
    }

    /** Gets a high word of the 64-bit constant (either long or double)
     * stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return A value of high 32-bits of 64-bit constant.*/
    U_32 get_8byte_high_word(uint16 index) const {
        assert(get_tag(index) == CONSTANT_Long
            || get_tag(index) == CONSTANT_Double);
        return m_entries[index].CONSTANT_8byte.high_bytes;
    }

    /** Gets an address of a constant stored in the constant pool.
     * @param[in] index - an index in the constant pool
     * @return An address of a constant.*/
    void* get_address_of_constant(uint16 index) const {
        assert(is_constant(index));
        assert(!is_string(index));
        return (void*)(m_entries + index);
    }

    /** Gets an exception, which has caused failure of the referred 
     * constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return An exception object, which is the cause of the
     *         resolution failure.*/
    jthrowable get_error_cause(uint16 index) const {
        assert(is_entry_in_error(index));
        return (jthrowable)(&(m_entries[index].error.cause));
    }

    /** Gets a head of a single-linked list containing resolution errors
     * in the given constant pool.
     * @return A head of a signle-linked list of constant-pool entries,
     *         which resolution had failed.*/
    ConstPoolEntry* get_error_chain() const {
        return m_failedResolution;
    }

    /** Gets an an index in the constant pool where the <code>utf8</code>
     * representation for #CONSTANT_String is stored.
     * @param[in] index - an index in the constant pool for the 
     *                    #CONSTANT_String entry
     * @return An an index in the constant pool with the <code>utf8</code>
     *         representation of the given string.*/
    uint16 get_string_index(uint16 index) const {
        assert(is_string(index));
        return m_entries[index].CONSTANT_String.string_index;
    }

    /** Gets an index of the constant-pool entry containing the
     * <code>utf8</code> string with the name part.
     * @param[in] index - an index in the constant pool
     * @return An an index in the constant pool with the <code>utf8</code>
     *         name string.*/
    uint16 get_name_and_type_name_index(uint16 index) const {
        assert(is_name_and_type(index));
        return m_entries[index].CONSTANT_NameAndType.name_index;
    }

    /** Gets an index of the constant-pool entry containing the 
     * <code>utf8</code> string with the descriptor part.
     * @param[in] index - an index in the constant pool
     * @return An an index in the constant pool with the <code>utf8</code>
     *         string for the descriptor.*/
    uint16 get_name_and_type_descriptor_index(uint16 index) const {
        assert(is_name_and_type(index));
        return m_entries[index].CONSTANT_NameAndType.descriptor_index;
    }

    /** Gets an index of the constant-pool entry containing a class for
     * the given <code>CONSTANT_*ref</code> entry.
     * @param[in] index - an index in the constant pool
     * @return An index of a class entry for the given constant-pool entry.*/
    uint16 get_ref_class_index(uint16 index) const {
        assert(is_fieldref(index)
            || is_methodref(index)
            || is_interfacemethodref(index));
        return m_entries[index].CONSTANT_ref.class_index;
    }

    /** Gets an index of <code>CONSTANT_NameAndType</code> for the given
     * constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @return An index of #CONSTANT_NameAndType for the given
     *         constant-pool entry.*/
    uint16 get_ref_name_and_type_index(uint16 index) const {
        assert(is_fieldref(index)
            || is_methodref(index)
            || is_interfacemethodref(index));
        return m_entries[index].CONSTANT_ref.name_and_type_index;
    }

    /** Gets a class-name an index in the constant pool for the
     * #CONSTANT_Class entry.
     * @param[in] index - an index in the constant pool
     * @return An index of the <code>utf8</code> name of the given class.*/
    uint16 get_class_name_index(uint16 index) const {
        assert(is_class(index));
        return m_entries[index].CONSTANT_Class.name_index;
    }

    /** Resolves an entry to the class.
     * @param[in] index - an index in the constant pool
     * @param[in] clss  - a class to resolve the given entry to*/
    void resolve_entry(uint16 index, Class* clss) {
        // we do not want to resolve entry of a different type
        assert(is_class(index));
        set_entry_resolved(index);
        m_entries[index].CONSTANT_Class.klass = clss;
    }

    /** Resolves an entry to the field.
     * @param[in] index - an index in the constant pool
     * @param[in] field - a field to resolve the given entry to*/
    void resolve_entry(uint16 index, Field* field) {
        // we do not want to resolve entry of different type
        assert(is_fieldref(index));
        set_entry_resolved(index);
        m_entries[index].CONSTANT_ref.field = field;
    }

    /** Resolves an entry to the method.
     * @param[in] index  - an index in the constant pool
     * @param[in] method - a method to resolve the given entry to*/
    void resolve_entry(uint16 index, Method* method) {
        // we do not want to resolve entry of a different type
        assert(is_methodref(index) || is_interfacemethodref(index));
        set_entry_resolved(index);
        m_entries[index].CONSTANT_ref.method = method;
    }

    /** Records a resolution error into a constant-pool entry.
     * @param[in] index - an index in the constant pool
     * @param[in] exn   - a cause of resolution failure
     * @note Disable suspension during this operation.*/
    void resolve_as_error(uint16 index, jthrowable exn) {
        assert(is_class(index)
            || is_fieldref(index)
            || is_methodref(index)
            || is_interfacemethodref(index));
        set_entry_error_state(index);
        m_entries[index].error.cause = *((ManagedObject**)exn);
        m_entries[index].error.next = m_failedResolution;
        assert(&(m_entries[index]) != m_failedResolution);
        m_failedResolution = &(m_entries[index]);
    }

    /** Parses in a constant pool for a class.
     * @param[in] clss        - a class containing the given constant pool
     * @param[in] string_pool - a reference to the string pool to intern strings in
     * @param[in] cfs         - a byte stream to parse the constant pool from
     * @return <code>true</code> if the constant pool was parsed successfully;
     *         <code>false</code> if some error was discovered during the parsing.*/
    bool parse(Class* clss, String_Pool& string_pool, ByteReader& cfs);

    /** Checks constant pool consistency. <ul>
     * <li>Makes sure that all indices to other constant pool entries are in range
     * and that contents of the entries are of the right type.
     * <li>Sets #CONSTANT_Class entries to point directly
     * to <code>String</code> representing the internal form of a fully qualified
     * form of a fully qualified name of <code>%Class</code>.
     * <li>Sets #CONSTANT_String entries to point directly to the
     * <code>String</code> representation. 
     * <li>Preresolves #CONSTANT_NameAndType entries to signatures. </ul>
     * @param[in] env           - VM environment
     * @param[in] clss          - the class that the given constant pool belongs to
     * @param[in] is_trusted_cl - defines whether class was loaded by
     *                            trusted classloader. User defined classloaders are not trusted.
     * @return <code>true</code> if the constant pool of clss is valid;
     *         otherwise <code>false</code>.*/
    bool check(Global_Env * env, Class* clss, bool is_trusted_cl);

    /** Clears the constant-pool content: tags and entries arrays.*/
    void clear() {
        if(m_size != 0) {
            delete[] m_entries[0].tags;
            delete[] m_entries;
        }
        init();
    }

    /** Initializes the constant pool to initial values.*/
    void init() {
        m_size = 0;
        m_entries = NULL;
        m_failedResolution = NULL;
    }
private:
    /** Sets a resolved flag in the constant-pool entry.
     * @param[in] index - an index in the constant pool*/
    void set_entry_resolved(uint16 index) {
        assert(is_valid_index(index));
        //// we do not want to resolve one entry twice
        // ppervov: FIXME: there is possible positive/negative race condition
        // in class resolution
        //assert(!is_entry_resolved(index));
        // we should not resolve failed entries
        // see comment above
        //assert(!is_entry_in_error(index));
        m_entries[0].tags[index] |= RESOLVED_MASK;
    }

    /** Sets an error flag in the constant-pool entry to mark it as failed.
     * @param[in] index - an index in the constant pool*/
    void set_entry_error_state(uint16 index) {
        assert(is_valid_index(index));
        //// we do not want to reset the resolved error
        // ppervov: FIXME: there is possible positive/negative race condition
        // in class resolution
        //assert(!is_entry_resolved(index));
        // we do not want to reset the reason of the failure
        // see comment above
        //assert(!is_entry_in_error(index));
        m_entries[0].tags[index] |= ERROR_MASK;
    }

    /** Resolves the #CONSTANT_NameAndType constant-pool entry 
     * to actual string values.
     * @param[in] index      - an index in the constant pool
     * @param[in] name       - name-and-type name
     * @param[in] descriptor - name-and-type type (descriptor)*/
    void resolve_entry(uint16 index, String* name, String* descriptor) {
        // we do not want to resolve entry of different type
        assert(is_name_and_type(index));
        set_entry_resolved(index);
        m_entries[index].CONSTANT_NameAndType.name = name;
        m_entries[index].CONSTANT_NameAndType.descriptor = descriptor;
    }

    /** Resolves the <code>CONSTANT_String</code> constant-pool entry to
     * actual string values.
     * @param[in] index - an index in the constant pool
     * @param[in] str   - an actual string*/
    void resolve_entry(uint16 index, String* str) {
        assert(is_string(index));
        set_entry_resolved(index);
        m_entries[index].CONSTANT_String.string = str;
    }
};


/** Converts a class name from an internal (VM) form to the Java form.
 * @param[in] class_name - the class name in an internal form
 * @return The class name in the Java form.*/
VMEXPORT String* class_name_get_java_name(const String* class_name);

// A Java class
extern "C" {

/** The state of the Java class*/

enum Class_State {
    ST_Start,                   /// the initial state
    ST_LoadingAncestors,        /// the loading super class and super interfaces
    ST_Loaded,                  /// successfully loaded
    ST_BytecodesVerified,       /// bytecodes for methods verified for the class
    ST_InstanceSizeComputed,    /// preparing the class; instance size known
    ST_Prepared,                /// successfully prepared
    ST_ConstraintsVerified,     /// constraints verified for the class
    ST_Initializing,            /// initializing the class
    ST_Initialized,             /// the class initialized
    ST_Error                    /// bad class or the class initializer failed
};


/** Access and properties flags for Class, Field and Method.*/
enum AccessAndPropertiesFlags {
    /** Public access modifier. Valid for Class, Field, Method. */
    ACC_PUBLIC       = 0x0001,
    /** Private access modifier. Valid for Field, Method.*/
    ACC_PRIVATE      = 0x0002,
    /** Protected access modifier. Valid for Field, Method.*/
    ACC_PROTECTED    = 0x0004,
    /** Static modifier. Valid for Field, Method.*/
    ACC_STATIC       = 0x0008,
    /** Final modifier. Valid for Class, Field, Method.*/
    ACC_FINAL        = 0x0010,
    /** Super modifier. Valid for Class.*/
    ACC_SUPER        = 0x0020,
    /** Synchronized modifier. Valid for Method.*/
    ACC_SYNCHRONIZED = 0x0020,
    /** Bridge modifier. Valid for Method (since J2SE 5.0).*/
    ACC_BRIDGE       = 0x0040,
    /** Volatile modifier. Valid for Field.*/
    ACC_VOLATILE     = 0x0040,
    /** Varargs modifier. Valid for Method (since J2SE 5.0).*/
    ACC_VARARGS      = 0x0080,
    /** Transient modifier. Valid for Field.*/
    ACC_TRANSIENT    = 0x0080,
    /** Native modifier. Valid for Method.*/
    ACC_NATIVE       = 0x0100,
    /** Interface modifier. Valid for Class.*/
    ACC_INTERFACE    = 0x0200,
    /** Abstract modifier. Valid for Class, Method.*/
    ACC_ABSTRACT     = 0x0400,
    /** Strict modifier. Valid for Method.*/
    ACC_STRICT       = 0x0800,
    /** Synthetic modifier. Valid for Class, Field, Method (since J2SE 5.0).*/
    ACC_SYNTHETIC    = 0x1000,
    /** Annotation modifier. Valid for Class (since J2SE 5.0).*/
    ACC_ANNOTATION   = 0x2000,
    /** Enum modifier. Valid for Class, Field (since J2SE 5.0).*/
    ACC_ENUM         = 0x4000
};

/** VM representation of Java class.
 * This class contains methods for parsing classes, querying class properties,
 * setting external properties of a class (source file name, class file name),
 * calling the verifier, preparing, resolving and initializing the class.*/

struct Class {
private:
    typedef struct {
        union {
            const String* name;
            Class* clss;
        };
        unsigned cp_index;
    } Class_Super;

    //
    // super class of this class; initially, it is the string name of super
    // class; after super class is loaded, it becomes a pointer to class
    // structure of the super class.
    //
    Class_Super m_super_class;

    // class name in internal (VM, class-file) format
    const String* m_name;
    // class canonical (Java) name
    String* m_java_name;
    // generic type information (since Java 1.5.0)
    String* m_signature;
    // simple name of the class as given in the source code; empty string if anonymous
    String* m_simple_name;
    // package to which this class belongs
    Package* m_package;

    // Distance in the hierarchy from java/lang/Object
    U_32 m_depth;

    // The field m_is_suitable_for_fast_instanceof should be 0
    // if depth==0 or depth>=vm_max_fast_instanceof_depth()
    // or is_array or access_flags&ACC_INTERFACE
    // It should be 1 otherwise
    int m_is_suitable_for_fast_instanceof;

    // string name of file from which this class has been loaded
    const char* m_class_file_name;
    // string name of source java file from which this class has been compiled
    const String* m_src_file_name;

    // unique class id
    // FIXME: current implementation of id is not thread safe
    // so, class id may not be unique
    unsigned m_id;

    // The class loader used to load this class.
    ClassLoader* m_class_loader;

    // This points to the location where java.lang.Class associated
    // with the current class resides. Similarly, java.lang.Class has a field
    // that points to the corresponding Class data structure.
    ManagedObject** m_class_handle;

    // class file major version
    uint16 m_version;

    // Access and properties flags of a class
    uint16 m_access_flags;

    // state of this class
    Class_State m_state;

    // Is this class marked as deprecated
    bool m_deprecated;

    // Does this class represent a primitive type?
    unsigned m_is_primitive : 1;

    // Does this class represent an array?
    unsigned m_is_array : 1;

    // Does base class of this array is primitive
    unsigned m_is_array_of_primitives : 1;

    // Does the class have a finalizer that is not inherited from
    // java.lang.Object?
    unsigned m_has_finalizer : 1;

    // Should access from this class be checked
    // (needed for certain special classes)
    unsigned m_can_access_all : 1;

    // Can instances of this class be allocated using a fast inline sequence
    // containing no calls to other routines
    unsigned char m_is_fast_allocation_possible;

    // Offset from the top by CLASS_ALLOCATED_SIZE_OFFSET
    // The number of bytes allocated for this object. It is the same as
    // instance_data_size with the constraint bit cleared. This includes
    // the OBJECT_HEADER_SIZE as well as the OBJECT_VTABLE_POINTER_SIZE
    unsigned int m_allocated_size;

    // This is the size of an instance without any alignment padding.
    // It can be used while calculating the field offsets of subclasses.
    // It does not include the OBJECT_HEADER_SIZE but does include the
    // OBJECT_VTABLE_POINTER_SIZE.
    // The m_allocated_size field will be this field properly aligned.
    unsigned m_unpadded_instance_data_size;

    // How should objects of this class be aligned by GC.
    int m_alignment;

    // Try to keep instance_data_size near vtable since they are used at the same time
    // by the allocation routines and sharing a cache line seem to help.

    // The next to high bit is set if allocation needs to consider class_properties.
    // (mumble->instance_data_size & NEXT_TO_HIGH_BIT_CLEAR_MASK) will always return the
    // actual size of and instance of class mumble.
    // Use get_instance_data_size() to get the actual size of an instance.
    // Use set_instance_data_size_constraint_bit() to set this bit.

    // For most classes the size of a class instance's data block.
    // This is what is passed to the GC. See above for details.
    unsigned m_instance_data_size;

    // ppervov: FIXME: the next two can be joined into a union;
    // vtable compression should be dropped in that case

    // virtual method table; <code>NULL</code> for interfaces
    VTable* m_vtable;

    // "Compressed VTable" - offset from the base of VTable allocation area
    Allocation_Handle m_allocation_handle;

    // number of virtual methods in vtable
    unsigned m_num_virtual_method_entries;

    // number of interface methods in vtable
    unsigned m_num_intfc_method_entries;

    // An array of pointers to Method descriptors, one descriptor
    // for each corresponding entry in m_vtable.methods[].
    Method** m_vtable_descriptors;

    // number of dimensions in array; current VM limitation is 255
    // Note, that you can derive the base component type of the array
    // by looking at m_name->bytes[m_num_dimensions].
    unsigned char m_num_dimensions;

    // for non-primitive arrays only, array_base_class is the base class
    // of an array
    Class* m_array_base_class;

    // class of the element of an array
    Class* m_array_element_class;

    // size of element of array; equals zero, if this class is not an array
    unsigned int m_array_element_size;

    // shift corresponding to size of element of array, undefined for non-arrays
    unsigned int m_array_element_shift;

    // Number of superinterfaces
    uint16 m_num_superinterfaces;

    // array of interfaces this class implements; size is m_num_superinterfaces
    // initially, it is an array of string names of interfaces and then,
    // after superinterfaces are loaded, this becomes an array pointers
    // to superinterface class structures
    Class_Super* m_superinterfaces;

    // constant pool of class
    ConstantPool m_const_pool;

    // number of fields in this class
    uint16 m_num_fields;
    // number of static fields in this class
    uint16 m_num_static_fields;
    // number of instance fields that are references
    unsigned m_num_instance_refs;

    // array of fields; size is m_num_fields
    Field* m_fields;

    // size of this class' static data block
    unsigned m_static_data_size;

    // block containing array of static data fields
    void* m_static_data_block;

    // number of methods in this class
    uint16 m_num_methods;

    // array of methods; size is m_num_methods
    Method* m_methods;

    // pointer to finalize method, NULL if none exists
    Method* m_finalize_method;

    // pointer to <clinit> method, NULL if none exists
    Method* m_static_initializer;

    // pointer to init()V method, cached for performance
    Method* m_default_constructor;

    // index of declaring class in constant pool of this class
    uint16 m_declaring_class_index;

    // index of CONSTANT_Class of outer class
    uint16 m_enclosing_class_index;

    // index of CONSTANT_MethodRef of outer method
    uint16 m_enclosing_method_index;

    // number of inner classes
    uint16 m_num_innerclasses;

    struct InnerClass {
        uint16 index;
        uint16 access_flags;
    };
    // indexes of inner classes descriptors in constant pool
    InnerClass* m_innerclasses;

    // annotations for this class
    AnnotationTable* m_annotations;

    //invisible annotations for this class
    AnnotationTable* m_invisible_annotations;
 
    // thread, which currently executes <clinit>
    VM_thread* m_initializing_thread;

    // These fields store information for
    // Class Hierarchy Analysis JIT optimizations
    // first class extending this class
    Class* m_cha_first_child;
    // next class which extends the same superclass
    Class* m_cha_next_sibling;

    // SourceDebugExtension class attribute support
    String* m_sourceDebugExtension;

    // verifier private data pointer
    void* m_verify_data;

    // class operations lock
    Lock_Manager* m_lock;

    // Per-class statistics
    // Number of times an instance of the class has been created
    // using new, newarray, etc
    uint64 m_num_allocations;

    // Number of bytes allocated for instances of the class
    uint64 m_num_bytes_allocated;

    // Number of instanceof/checkcast calls both from the user code
    // and the VM that were not subsumed by the fast version of instanceof
    uint64 m_num_instanceof_slow;

    // For subclasses of java.lang.Throwable only
    uint64 m_num_throws;

    // Number of times class is checked for initialization
    uint64 m_num_class_init_checks;

    // Number of "padding" bytes added per class instance to its fields to
    // make each field at least 32 bits
    U_32 m_num_field_padding_bytes;
public:

    /** Initializes class-member variables to their initial values.
     * @param[in] env  - VM environment
     * @param[in] name - a class name to assign to the given class
     * @param[in] cl   - a class loader for the given class*/
    void init_internals(const Global_Env* env, const String* name, ClassLoader* cl);

    /** Clears member variables within a class.*/
    void clear_internals();

    void notify_unloading();

    /** Determines whether the given class has a super class.
     * @return <code>true</code> if the current class has a super class;
     *         otherwise <code>false</code>.*/
    bool has_super_class() const {
        return m_super_class.clss != NULL;
    }

    /** Gets the name of the super class.
     *
     * @return The super class name or <code>NULL</code>, if the given class 
     *         is <code>java/lang/Object</code>.
     * @note It is valid until the super class is loaded; after that, use
     *       <code>get_super_class()->get_name()</code> to retrieve the 
     *       super class name.*/
    const String* get_super_class_name() const {
        return m_super_class.name;
    }

    /** Gets the super class of the given class.
     * @return The super class of the given class or <code>NULL</code>,
     *         if the given class is <code>java/lang/Object</code>.*/
    Class* get_super_class() const {
        return m_super_class.clss;
    }

    /** Gets the class loader of the given class.
     * @return the class loader of the given class.*/
    ClassLoader* get_class_loader() const {
        return m_class_loader;
    }
    /** Gets the class handle of <code>java.lang.Class</code> for the given class.
     * @return The <code>java.lang.Class</code> handle for the given class.*/

    ManagedObject** get_class_handle() const { return m_class_handle; }

    /** Gets the natively interned class name for the given class.
     * @return The class name in the VM format.*/

    const String* get_name() const { return m_name; }

    /** Gets a natively interned class name for the given class.
     * @return A class name in the Java format.*/
    String* get_java_name() {
        if(!m_java_name) {
            m_java_name = class_name_get_java_name(m_name);
        }
        return m_java_name;
    }

    /** Gets a class signature.
     * @return A class signature.*/
    String* get_signature() const { return m_signature; }

    /** Gets a simple name of the class.
     * @return A simple name of the class.*/
    String* get_simple_name();

    /** Gets a package containing the given class.
     * @return A package to which the given class belongs.*/
    Package* get_package() const { return m_package; }
    
    /** Gets depth in the hierarchy of the given class.
     * @return A number of classes in the super-class hierarchy.*/
    U_32 get_depth() const { return m_depth; }
    bool get_fast_instanceof_flag() const { return m_is_suitable_for_fast_instanceof; }

    /** Gets the vtable for the given class.
     * @return The vtable for the given class or <code>NULL</code>, if the given
     *         class is an interface.*/
    VTable* get_vtable() const { return m_vtable; }

    /** Gets an allocation handle for the given class.*/
    Allocation_Handle get_allocation_handle() const { return m_allocation_handle; }

    /** Gets the length of the source-file name.
     * @return The length in bytes of the source-file name.*/
    size_t get_source_file_name_length() {
        assert(has_source_information());
        return m_src_file_name->len;
    }

    /** Gets a source-file name.
     * @return A source-file name for the given class.*/
    const char* get_source_file_name() {
        assert(has_source_information());
        return m_src_file_name->bytes;
    }

    /** Gets a method localed at <code>method_idx</code> in 
     * the <code>m_vtable_descriptors</code> table.
     * @param method_idx - index of method in vtable descriptors table
     * @return A method from the vtable descriptors table.*/
    Method* get_method_from_vtable(unsigned method_idx) const {
        return m_vtable_descriptors[method_idx];
    }

    /// Returns the number of virtual methods in vtable
    unsigned get_number_of_virtual_method_entries() const {
        return m_num_virtual_method_entries;
    }
   
    /** Gets the first subclass for Class Hierarchy Analysis.
     * @return The first subclass.*/
    Class* get_first_child() const { return m_cha_first_child; }

    /** Return the next sibling for Class Hierarchy Analysis.
     * @return The next sibling.*/
    Class* get_next_sibling() const { return m_cha_next_sibling; }

    /** Gets offset of m_depth field in struct Class.
     * @note Instanceof helpers use returned offset.*/
    static size_t get_offset_of_depth() {
        Class* dummy=NULL;
        return (size_t)((char*)(&dummy->m_depth));
    }

    /** Gets offset of m_is_suitable_for_fast_instanceof field in struct Class.
     * @note Instanceof helper uses returned offset.*/
    static size_t get_offset_of_fast_instanceof_flag() {
        Class* dummy=NULL;
        return (size_t)((char*)(&dummy->m_is_suitable_for_fast_instanceof));
    }
    
    /** Gets an offset of <code>m_is_fast_allocation_possible</code> in
     * the class.
     * @note Allocation helpers use returned offset.*/
    size_t get_offset_of_fast_allocation_flag() {
        // else one byte ld in helpers will fail
        assert(sizeof(m_is_fast_allocation_possible) == 1);
        Class* dummy=NULL;
        return (size_t)((char*)(&dummy->m_is_fast_allocation_possible));
    }

    /** Gets an offset of <code>m_allocation_handle</code> in the class.
     * @note Allocation helpers use returned offset.*/
    size_t get_offset_of_allocation_handle() {
        assert(sizeof(m_allocation_handle)  == sizeof(void*));
        Class* dummy=NULL;
        return (size_t)((char*)(&dummy->m_allocation_handle));
    }

    /** Gets an offset of <code>m_instance_data_size</code> in the class.
     * @note Allocation helpers use returned offset.*/
    size_t get_offset_of_instance_data_size() {
        assert(sizeof(m_instance_data_size) == 4);
        Class* dummy=NULL;
        return (size_t)((char*)(&dummy->m_instance_data_size));
    }

    /** Gets an offset of <code>m_num_class_init_checks</code> in the class.
     * @note Class initialization helper on IPF uses returned offset.*/
    static size_t get_offset_of_class_init_checks() {
        Class* dummy = NULL;
        return (size_t)((char*)(&dummy->m_num_class_init_checks) - (char*)dummy);
    }

    /** Gets an offset of <code>m_array_element_class</code> in the class.
     * @note Class initialization helper on IPF uses returned offset.*/
    static size_t get_offset_of_array_element_class() {
        Class* dummy = NULL;
        assert(sizeof(dummy->m_array_element_class) == sizeof(void*));
        return (size_t)((char*)(&dummy->m_array_element_class) - (char*)dummy);
    }

    /** Gets an offset of <code>m_class_handle</code> in the class.
     * @note It used by VMHelper class*/
    static size_t get_offset_of_jlc_handle () {
        Class* dummy=NULL;       
        return (size_t)((char*)(&dummy->m_class_handle) - (char*)dummy);
    }

    /** Gets the number of array dimensions.
     * @return Number of dimentions in an array represented by this class.*/
    unsigned char get_number_of_dimensions() const {
        assert(is_array());
        return m_num_dimensions;
    }

    /** 
     * Gets the base class of the array (for non-primitive arrays only).
     * @return Class describing the base type of an array
     * represented by this class.*/
    Class* get_array_base_class() const {
        assert(is_array());
        return m_array_base_class;
    }

    /** Gets the class of the array element.
     * @return Class describing the element of an array
     * represented by this class.*/
    Class* get_array_element_class() const {
        assert(is_array());
        return m_array_element_class;
    }

    /** Gets the class state.
     * @return The class state.*/
    Class_State get_state() const { return m_state; }

    /** Gets a number of superinterfaces.
     * @return A number of superinterfaces of the given class.*/
    uint16 get_number_of_superinterfaces() const { return m_num_superinterfaces; }

    /** Gets a super-interface name from the array of super-interfaces that
     * the given class implements.
     * @param[in] index - an index of super-interface to return the name for
     * @return The requested super-interface name.*/
    const String* get_superinterface_name(uint16 index) const {
        assert(index < m_num_superinterfaces);
        return m_superinterfaces[index].name;
    }

    /** Gets a superinterface from the array of superinterfaces the given class 
     * implements.
     * @param[in] index - an index of a superinterface to return
     * @return A requested superinterface.*/
    Class* get_superinterface(uint16 index) const {
        assert(index < m_num_superinterfaces);
        return m_superinterfaces[index].clss;
    }

    /** Gets a constant pool of the given class.
     * @return A constant pool of the given class.*/
    ConstantPool& get_constant_pool() { return m_const_pool; }

    /** Gets a number of fields in the given class.
     * @return A number of fields in the given class.*/
    uint16 get_number_of_fields() const { return m_num_fields; }

    /** Gets a number of static fields in the given class.
     * @return A number of static fields in the given class.*/
    uint16 get_number_of_static_fields() const { return m_num_static_fields; }

    /** Gets a field from the given class by its position in the class-fields
     * array.
     * @param[in] index - an index in the class-fields array of a field to
     *                    retrieve
     * @return The requested field.*/
    Field* get_field(uint16 index) const;

    /** Gets an address of the memory block containing static data of
     * the given class.
     * @return An address of a static data block.*/
    void* get_static_data_address() const { return m_static_data_block; }

    /** Gets a number of methods in the given class.
     * @return A number of methods in the given class.*/
    uint16 get_number_of_methods() const { return m_num_methods; }

    /** Gets a method from the given class by its position in the
     * class-method array.
     * @param[in] index - an index in the class-method array of a
     *                    method to retrieve
     * @return A requested method.*/
    Method* get_method(uint16 index) const;

    /** Gets a constant-pool index of the declaring class.
     * @return An index in the constant pool describing the requested
     *         declaring class.*/
    uint16 get_declaring_class_index() const { return m_declaring_class_index; }

    /** Gets a constant-pool index of the enclosing class.
     * @return An index in the constant pool describing the requested 
     *         enclosing class.*/
    uint16 get_enclosing_class_index() const { return m_enclosing_class_index; }

    /** Gets a constant-pool index of the enclosing method.
     * @return An index in the constant pool describing the requested enclosing
     *         method.*/
    uint16 get_enclosing_method_index() const { return m_enclosing_method_index; }

    /** Gets a number of inner classes.
     * @return A number of inner classes.*/
    uint16 get_number_of_inner_classes() const { return m_num_innerclasses; }

    /** Gets an index in the constant pool of the given class, which
     * describes the inner class.
     * @param[in] index - an index of the inner class in the array of
     *                    inner classes in the given class
     * @return An index in the constant pool describing the requested inner
     *         class.*/
    uint16 get_inner_class_index(uint16 index) const {
        assert(index < m_num_innerclasses);
        return m_innerclasses[index].index;
    }

    /** Gets access flags for the inner class.
     * @param[in] index - an index of the inner class in the array of inner
     *                    classes in the given class
     * @return Access flags of the requested inner class.*/
    uint16 get_inner_class_access_flags(uint16 index) const {
        assert(index < m_num_innerclasses);
        return m_innerclasses[index].access_flags;
    }
    /** Gets a collection of annotations.
     * @return A collection of annotations.*/
    AnnotationTable* get_annotations() const { return m_annotations; }
    /** Gets a collection of invisible annotations.
     * @return A collection of invisible annotations.*/
    AnnotationTable* get_invisible_annotations() const {
        return m_invisible_annotations;
    }
    /** Gets a class instance size.
     * @return A size of the allocated instance in bytes.*/
    unsigned int get_allocated_size() const { return m_allocated_size; }
    unsigned int get_instance_data_size() const {
        return m_instance_data_size & NEXT_TO_HIGH_BIT_CLEAR_MASK;
    }

    /** Gets the array-alement size.
     * @return A size of the array element.
     * @note The given function assumes that the class is an array class.*/
    unsigned int get_array_element_size() const {
        assert(m_is_array == 1);
        return m_array_element_size;
    }

    /** Gets the class ID.*/
    unsigned get_id() const { return m_id; }

    /** Gets major version of class file.
     * @return Major version of class file.*/
    uint16 get_version() const { return m_version; }

    /** Gets access and properties flags of the given class.
     * @return The 16-bit integer representing access and properties flags
     *         the given class.*/
    uint16 get_access_flags() const { return m_access_flags; }

   /** Checks whether the given class represents the primitive type.
    * @return <code>true</code> if the class is primitive; otherwise 
    *         <code>false</code>.*/
    bool is_primitive() const { return m_is_primitive == 1; }

   /** Checks whether the given class represents an array.
    * @return <code>true</code> if the given class is an array, otherwise 
    *         <code>false</code>.*/
    bool is_array() const { return m_is_array == 1; }

   /** Checks whether the base class of the given array is primitive.
    * @return <code>true</code> if the base class is primitive, otherwise
    *         <code>false</code>.*/
    bool is_array_of_primitives() const { return m_is_array_of_primitives == 1; }

    /** Checks whether the class has the <code>ACC_PUBLIC</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_PUBLIC</code> 
     *         access flag set.*/
    bool is_public() const { return (m_access_flags & ACC_PUBLIC) != 0; }

    /** Checks whether the class has the <code>ACC_PUBLIC</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_PUBLIC</code> 
     *         access flag set.*/
    bool is_private() const { return (m_access_flags & ACC_PRIVATE) != 0; }

    /** Checks whether the class has the <code>ACC_PUBLIC</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_PUBLIC</code> 
     *         access flag set.*/
    bool is_protected() const { return (m_access_flags & ACC_PROTECTED) != 0; }

   /** Checks whether the class has the <code>ACC_FINAL</code> flag set.
    * @return <code>true</code> if the class has the <code>ACC_FINAL</code> 
    *         access flag set.*/
    bool is_final() const { return (m_access_flags & ACC_FINAL) != 0; }

   /** Checks whether the class has the <code>ACC_SUPER</code> flag set.
    * @return <code>true</code> if the class has the <code>ACC_SUPER</code> 
    *         access flag set.*/
 
    bool is_super() const { return (m_access_flags & ACC_SUPER) != 0; }

    /** Checks whether the class has the <code>ACC_INTERFACE</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_INTERFACE</code> 
     *         access flag set.*/

    bool is_interface() const { return (m_access_flags & ACC_INTERFACE) != 0; }
    
    /** Checks whether the class has the <code>ACC_ABSTRACT</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_ABSTRACT</code> 
     *         access flag set.*/
    bool is_abstract() const { return (m_access_flags & ACC_ABSTRACT) != 0; }

    /** Checks whether the class is enum, that is the <code>ACC_ENUM</code> 
     * flag is set.
     * @return <code>true</code> if the class is enum.*/
    bool is_enum() const { return (m_access_flags & ACC_ENUM) != 0; }

    /** Checks whether the class has the <code>ACC_SYNTHETIC</code> flag set.
     * @return <code>true</code> if the class has the <code>ACC_SYNTHETIC</code> 
     *         access flag set.*/

    bool is_synthetic() const { return (m_access_flags & ACC_SYNTHETIC) != 0; }
    
    /** Checks whether the class is an annotation.
     * @return <code>true</code> if the class is an annotation.*/
    bool is_annotation() const { return (m_access_flags & ACC_ANNOTATION) != 0; }

    /** Checks whether the given class has a finalizer.
     * @return <code>true</code> if the given class (or its super class) has
     *         a finalize method; otherwise <code>false</code>.*/
    bool has_finalizer() const { return m_has_finalizer == 1; }

    /** Checks whether the given class is an inner class of some other class.
     * @return <code>true</code> if the given class is an inner class of some 
     *         other class, otherwise <code>false</code>.*/
    bool is_inner_class() const { return m_declaring_class_index != 0; }

    /** Checks whether the given class can access <code>inner_class</code>.
     * @param[in] env         - VM environment
     * @param[in] inner_class - an inner class to check access to
     * @return <code>true</code> if the given class has access to the inner 
     *         class; otherwise <code>false</code>.*/
    bool can_access_inner_class(Global_Env* env, Class* inner_class);

    /** Checks whether the given class can access a member class.
     * @param[in] member - a class member to check access to
     * @return <code>true</code> if the given class can access a member class;
     *         otherwise <code>false</code>.*/
    bool can_access_member(Class_Member* member);

    /** Checks whether the given class has a source-file name available.
     * @return <code>true</code> if source file name is available for the given class;
     *         otherwise <code>false</code>.*/
    bool has_source_information() const { return m_src_file_name != NULL; }

    /** Checks whether the given class is in the process of initialization.
     * @return <code>true</code> if the class initialization method is executed;
     *         otherwise <code>false</code>.*/

    bool is_initializing() const { return m_state == ST_Initializing; }

    /** Checks whether the class is initialized.
     * @return <code>true</code> if the class is initialized;
     *         otherwise <code>false</code>.*/

    bool is_initialized() const { return m_state == ST_Initialized; }

    /** Checks whether the class is in the error state.
     * @return <code>true</code> if the class is in the error state;
     *         otherwise <code>false</code>.*/
    bool in_error() const { return m_state == ST_Error; }

    /** Checks whether the given class has a passed preparation stage.
     * @return <code>true</code> if the class has a passed preparation stage;
     *         otherwise <code>false</code>.*/
    bool is_at_least_prepared() const {
        return m_state == ST_Prepared
            || m_state == ST_ConstraintsVerified
            || m_state == ST_Initializing
            || m_state == ST_Initialized;
    }

   /** Checks whether the given class represents a class that is a subtype of 
    * <code>clss</code>, according to the Java instance of rules.
    * @param[in] clss - a class to check for being super relative
    * @return <code>true</code> if the given class represents a class that is
    *         a subtype of <code>clss</code>, otherwise <code>false</code>.*/
    bool is_instanceof(Class* clss);

    /** FIXME: all setter functions must be rethought to become private
     * or to be removed altogether, if possible.
     * Sets the name of a file from which the given class has been loaded.
     * @param[in] cf_name - a class-file name*/
    void set_class_file_name(const char* cf_name) {
        assert(cf_name);
        m_class_file_name = cf_name;
    }

    /** Sets instance data size constraint bit to let the allocation know
     * there are constraints on the way instance should be allocated.
     * @note Constaints are recorded in the <code>class_properties</code> 
     *       field of the class <code>VTable</code>.*/
    void set_instance_data_size_constraint_bit() {
        m_instance_data_size |= NEXT_TO_HIGH_BIT_SET_MASK;
    }

    /** Sets a class handle of <code>java.lang.Class</code> for the given class.
     * @param[in] oh - a class handle of <code>java.lang.Class</code>*/
    void set_class_handle(ManagedObject** oh) { m_class_handle = oh; }

    /** Constructs internal representation of a class from the byte array
     * (defines class).
     * @param[in] env - VM environment
     * @param[in] cfs - a class-file stream; byte array contaning class data*/
    bool parse(Global_Env* env, ByteReader& cfs);

   /** Loads a super class and super interfaces of the given class.
    * The given class's class loader is used for it.
    * @param[in] env - VM environment*/
    bool load_ancestors(Global_Env* env);

    /** Verifies bytecodes of the class.
     * @param[in] env - VM environment
     * @return <code>true</code> if bytecodes of a class were successfully verified;
     *         otherwise <code>false</code>.*/
    bool verify(const Global_Env* env);

    /** Verifies constraints for the given class collected during the bytecodes
     * verification.
     * @param[in] env - VM environment
     * @return <code>true</code> if constraints successfully pass verification;
     *         otherwise <code>false</code>.*/
    bool verify_constraints(const Global_Env* env);

    /** Setups the given class as representing a primitive type.
     * @param[in] cl - a class loader the given class belongs to
     * @note FIXME: <code>cl</code> is always a bootstrap class loader 
     *       for primitive types. Retrieve the bootstrap class loader 
     *       from VM environment here, not one level up the calling stack.*/
    void setup_as_primitive(ClassLoader* cl) {
        m_is_primitive = 1;
        m_class_loader = cl;
        m_access_flags = ACC_ABSTRACT | ACC_FINAL | ACC_PUBLIC;
        m_state = ST_Initialized;
    }

    /** Sets up the given class as representing an array.
     * @param[in] env                  - VM environment
     * @param[in] num_dimentions       - a number of dimentions this array has
     * @param[in] isArrayOfPrimitives  - does this array is an array of primitives
     * @param[in] baseClass            - base class of this array; for example,
     *                                   for <code>[[[Ljava/lang/String;</code> 
     *                                   base class is <code>java/lang/String</code>
     * @param[in] elementClass         - class representing element of this array;
     *                                   for example, for <code>[[I</code>, element 
     *                                   the class is <code>[I</code>
     * @note For single-dimentional arrays <code>baseClass<code> and 
     *       <code>elementClass</code> are the same.*/
    void setup_as_array(Global_Env* env, unsigned char num_dimensions,
        bool isArrayOfPrimitives, Class* baseClass, Class* elementClass);

   /** Prepares a class: <ol>
    * <li> assigns offsets:
    *      - the offset of instance data fields
    *      - virtual methods in a vtable
    *      - static data fields in a static data block</li>
    * <li> creates a class vtable</li>
    * <li> creates a static field block</li>
    * <li> creates a static method block </ol>
    * @param[in] env - vm environment
    * @return <code>true</code> if the class was successfully prepared; 
    *         otherwise <code>false</code>.*/
    bool prepare(Global_Env* env);

    /** Resolves a constant-pool entry to a class. Loads a class if neccessary.
     * @param[in] env      - VM environment
     * @param[in] cp_index - a constant-pool index of <code>CONSTANT_Class</code> 
     *                       to resolve
     * @return A resolved class, if a resolution attempt succeeds; 
     *         otherwise <code>NULL</code>.
     * @note Should become private as soon as wrappers become members of the 
     * struct #Class.*/
    Class* _resolve_class(Global_Env* env, unsigned cp_index);
    
    /** Resolves a declaring class.
     * @return A declaring class, if the given class is inner class of some
     *         other class and if the resolution was successful;
     *         otherwise <code>NULL</code>.*/
    Class* resolve_declaring_class(Global_Env* env);

    /** Resolves a field in the constant pool of the given class.
     * @param[in] env      - VM environment
     * @param[in] cp_index - an index of an entry in the constant pool,
     *                       which describes field to be resolved
     * @return A resolved field, if a resolution attempt succeeds; 
     *         otherwise <code>NULL</code>.
     * @note Should become private as soon as wrappers
     *       become members of the struct #Class.*/
    Field* _resolve_field(Global_Env* env, unsigned cp_index);

    /** Resolves a method in the constant pool of the given class.
     * @param[in] env      - VM environment
     * @param[in] cp_index - an index of an entry in the constant pool,
     *                       which describes method to be resolved
     * @return A resolved method, if a resolution attempt succeeds;
     *         otherwise <code>NULL</code>.
     * @note Should become private as soon as wrappers become members of
     *       the struct #Class.*/
    Method* _resolve_method(Global_Env* env, unsigned cp_index);

   /** Initializes the class.
    * @param[in] throw_exception - defines whether the exception should
    *                              be thrown or raised
    * @note The given method may raise exception, if an error occurs during
    *       the initialization of the class.*/
    void initialize();

    /** Looks up the field with specified name and descriptor in the given class only.
     * @param[in] name - the field name to look up for
     * @param[in] desc - the field descriptor to look up for
     * @return The looked up field if found in the given class, 
     *         otherwise <code>NULL</code>.*/
    Field* lookup_field(const String* name, const String* descriptor);

   /** Looks up the field with specified name and descriptor in the given class
     * and also in the super class and super-interfaces recursively.
    * @param[in] name - field name to look up for
    * @param[in] desc - field descriptor to look up for
    * @return The looked up field if found, <code>NULL</code> otherwise*/
    Field* lookup_field_recursive(const String* name, const String* descriptor);

    /** Looks up a method with a specified name and descriptor in the given
     * class only.
     * @param[in] name - a method name to look up for
     * @param[in] desc - a method descriptor to look up for
     * @return The looked-up method, if found in the given class;
     *         otherwise <code>NULL</code>.*/
    Method* lookup_method(const String* name, const String* desc);

    /** Allocates an instance of the given class and returns a pointer to it.
     * @return A managed pointer to the allocated class instance;
     *         <code>NULL</code>, if no memory is available and
     *         <code>OutOfMemoryError</code> exception is raised on a
     *         caller thread.*/
    ManagedObject* allocate_instance();

    /**
	 * Calculates a size of the block allocated for the array, which is represented by 
     * the given class.
     * @param[in] length the length of the array
     * @return The size of the array of specified length in bytes, or 0 if the size is too big.
	 */
    unsigned calculate_array_size(int length) const {
        if (length < 0) {
            return 0;
        }

        assert(m_array_element_size);
        assert(length >= 0);
        assert(is_array());
        unsigned first_elem_offset;
        if(m_array_element_shift < 3) {
            first_elem_offset = VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4;
        } else {
            first_elem_offset = VM_VECTOR_FIRST_ELEM_OFFSET_8;
        }

        // check overflow, we need:
        // first_elem_offset + (length << m_array_element_shift)
        //      + GC_OBJECT_ALIGNMENT < NEXT_TO_HIGH_BIT_SET_MASK
        //
        if (((NEXT_TO_HIGH_BIT_SET_MASK - GC_OBJECT_ALIGNMENT - first_elem_offset)
                    >> m_array_element_shift) <= (unsigned)length) {
            // zero means overflow
            return 0;
        }

        unsigned size = first_elem_offset + (length << m_array_element_shift);
        size = (((size + (GC_OBJECT_ALIGNMENT - 1)) & (~(GC_OBJECT_ALIGNMENT - 1))));
        assert((size % GC_OBJECT_ALIGNMENT) == 0);
        assert((size & NEXT_TO_HIGH_BIT_SET_MASK) == 0);
        return size;
    }

    /** Estimates the amount of memory allocated for C++ part of the given class.
     * @return The size of memory allocated for the given class.*/
    unsigned calculate_size();

    /** Gets the interface vtable for interface <code>iid</code> within 
     * object <code>obj</code>.
     * @param[in] obj - an object to retrieve an interface table entry from
     * @param[in] iid - an interface class to retrieve vtable for
     * @return An interface vtable from object; <code>NULL</code>, if no 
     *         such interface exists for the object.*/
    static void* helper_get_interface_vtable(ManagedObject* obj, Class* iid);

    // SourceDebugExtension class attribute support

    /** Checks whether the given class has the 
     * <code>SourceDebugExtension</code> attribute.
     * @return <code>true</code> if the <code>SourceDebugExtension</code> 
     *         attribute is available for the given class;
     *         otherwise <code>false</code>.*/
    bool has_source_debug_extension() const {
        return m_sourceDebugExtension != NULL;
    }

    /** Gets length of the <code>SourceDebugExtension</code> attribute.
     * @return The <code>SourceDebugExtension</code> attribute length.*/
    unsigned get_source_debug_extension_length() const {
        return m_sourceDebugExtension->len;
    }

    /** Gets data from the <code>SourceDebugExtension</code> attribute.
     * @return <code>SourceDebugExtension</code> attribute bytes.*/
    const char* get_source_debug_extension() const {
        return m_sourceDebugExtension->bytes;
    }

    /** Stores a verifier specific pointer into the given class.
     * @param[in] data - a verifier specific data pointer*/
    void set_verification_data(void* data) {
        assert(m_verify_data == NULL);
        m_verify_data = data;
    }

    /** Gets a pointer to verifier specific data, previously stored with
     * the call to <code>set_verification_data</code>.
     * @return A pointer to verifier specific data or <code>NULL</code>, if 
     *         none was set.*/
    void* get_verification_data() {
        return m_verify_data;
    }

    /** Locks access to the given class.*/
    void lock();

    /** Unlocks access to the given class.*/
    void unlock();

    // Statistics update

    /** Updates allocation statistics.
    * @param[in] size - a size of an allocated instance*/
    void instance_allocated(unsigned size) {
        m_num_allocations++;
        m_num_bytes_allocated += size;
    }

    /** Updates an instance of slow path statistics.*/
    void instanceof_slow_path_taken() { m_num_instanceof_slow++; }

    /** Updates throwing statistics for <code>java/lang/Throwable</code> decendants.*/
    void class_thrown() { m_num_throws++; }

    /** Allocates memory for code from pool of defining classloader for the class.*/
    void* code_alloc(size_t size, size_t alignment, Code_Allocation_Action action);

    /** Updates initialization check statistics.*/
    void initialization_checked() { m_num_class_init_checks++; }

    /** Gets the number of times instance of the given class was allocated.
     * @return The number of allocations of the given class.*/
    uint64 get_times_allocated() const { return m_num_allocations; }

    /** Gets the total number of bytes allocated for instances of the given class.
     * @return The number of bytes allocated for all instances of the given class.*/
    uint64 get_total_bytes_allocated() const { return m_num_bytes_allocated; }

    /** Gets the number of times the slow path of the check instance was taken.
     * @return The number of times the slow path was taken.*/
    uint64 get_times_instanceof_slow_path_taken() const { return m_num_instanceof_slow; }

    /** Gets the number of times the given class was thrown.
     * @return The number of times the given class was thrown.*/
    uint64 get_times_thrown() const { return m_num_throws; }

    /** Gets the number of times the initialization of the given class
     * was checked by run-time helpers.
     * @return The number of times initialization of the given class was checked.*/
    uint64 get_times_init_checked() const { return m_num_class_init_checks; }
    
    /** Gets the number of excessive bytes used for aligning class fields.
     * @return A number of excessive bytes used for aligning class fields.*/
    uint64 get_total_padding_bytes() const { return m_num_field_padding_bytes; }
private:
    
    /** Parses super-interfaces information from a class-file stream.
     * @param[in] cfs - a class-file stream to parse superinterfaces information from
     * @return <code>true</code> if information was succesfully parsed;
     *         otherwise <code>false</code>.*/
    bool parse_interfaces(ByteReader &cfs);

    /** Parses class fields from a class-file stream.
     * @param[in] env           - VM enviroment
     * @param[in] cfs           - a class-file stream
     * @param[in] is_trusted_cl - defines whether class was loaded by
     *                            trusted classloader. User defined classloaders are not trusted.
     * @return <code>true</code> if successfully parses fields; <code>false</code> 
     *         if any parse error occurs.*/
    bool parse_fields(Global_Env* env, ByteReader &cfs, bool is_trusted_cl);

    /** Parses class methods from a class-file stream.
     * @param[in] env           - VM enviroment
     * @param[in] cfs           - a class file stream
     * @param[in] is_trusted_cl - defines whether class was loaded by
     *                            trusted classloader. User defined classloaders are not trusted.     
     * @return <code>true</code> if successfully parses methods; <code>false</code> 
     *         if any parse error occurs.*/
    bool parse_methods(Global_Env* env, ByteReader &cfs, bool is_trusted_cl);

    /** Calculates and assigns offsets to class fields during preparation.*/
    void assign_offsets_to_fields();

    /** Assign offsets to class-instance fields.
     * @param[in] field_ptrs          - an array of class fields to assign 
     *                                  offsets to
     * @param[in] do_field_compaction - defines whether the class fields should 
     *                                  be compacted*/
    void assign_offsets_to_instance_fields(Field** field_ptrs, bool do_field_compaction);

    /** Assigns an offset to an instance field.
     * @param[in] field               - a field to calculate offset for
     * @param[in] do_field_compaction - defines whether the class fields should 
     *                                  be compacted*/
    void assign_offset_to_instance_field(Field* field, bool do_field_compaction);

    /** Assigns offsets to static fields of a class.
     * @param[in] field_ptrs          - an array of fields
     * @param[in] do_field_compaction - defines whether the class fields should 
     *                                  be compacted*/
    void assign_offsets_to_static_fields(Field** field_ptrs, bool do_field_compaction);

    /** Initializes an interface class.
     * @return <code>true</code> if  the interface was successfully initialized; 
     *         otherwise <code>false</code>.*/
    bool initialize_static_fields_for_interface();

    /** Assigns offsets (from the base of the vtable) and the <code>VTable</code> index to
     * class methods.
     * @param[in] env - VM environment*/
    void assign_offsets_to_methods(Global_Env* env);

    /** Creates the vtable for the given class.
     * @param[in] n_vtable_entries - a number of entries for the vtable*/
    void create_vtable(unsigned n_vtable_entries);

    /** Populates the vtable descriptors table with methods overriding as necessary.
     * @param[in] intfc_table_entries - must contain all interfaces of the given class
     */
    void populate_vtable_descriptors_table_and_override_methods(const std::vector<Class*>& intfc_table_entries);

    /** Creates and populates an interface table for a class.
     *  @param[in] intfc_table_entries - must contain all interfaces of the given class
     * @return The created interface table.*/
    Intfc_Table* create_and_populate_interface_table(const std::vector<Class*>& intfc_table_entries);

     /** Sets vtable entries to methods addresses.*/
    void point_vtable_entries_to_stubs();

    /** Adds any required "fake" methods to a class. These are interface methods
     * inherited by an abstract class that are not implemented by that class
     * or any superclass. Such methods will never be called, but they are added
     * so they have the correct offset in the virtual method part of the vtable
     * (that is the offset of the "real" method in the vtable for a concrete class).*/
    void add_any_fake_methods();
}; // struct Class
} // extern "C"


/** Gets instance of java/lang/Class associated with this class.
 * @param[in] clss - class to retrieve java/lang/Class instance for.
 * @return Instance of java/lang/Class associated with class.*/
ManagedObject* struct_Class_to_java_lang_Class(Class* clss);
/** Gets handle of java/lang/Class instance associated with this class.
 * @param[in] clss - class to retrieve handle with java/lang/Class instance for.
 * @return Handle with instance of java/lang/Class associated with class.
 * @note This function allocates local handle and stores reference to
 * java/lang/Class into it.*/
jclass struct_Class_to_jclass(Class* clss);
/** Gets native class from the java/lang/Class handle.
 * @param[in] jc - handle to retrieve struct Class from.
 * @return Class for the given handle.*/
Class* jclass_to_struct_Class(jclass jc);
/** Gets native class for any given object handle.
 * @param[in] jobj - object to retrieve class for.
 * @return Class for the given object handle.*/
Class* jobject_to_struct_Class(jobject jobj);
/** Gets pointer to instance of java/lang/Class associated with the class.
 * @param[in] clss - class to retrieve pointer to instance of java/lang/Class for.
 * @return Pointer to instance of java/lang/Class associated with the class.
 * @note This is NOT real handle, so, when using this function, make sure the
 * returned value will never be passed to ANY user code including JVMTI
 * agent callbacks.*/
jobject struct_Class_to_java_lang_Class_Handle(Class* clss);
/** Gets native class from instance of java/lang/Class.
 * @param[in] jlc - instance of java/lang/Class to retrieve class from.
 * @return Native class from instance of java/lang/Class.*/
Class* java_lang_Class_to_struct_Class(ManagedObject* jlc);

/** Looks up field in class and its superclasses.
 * @param[in] clss - class to lookup field in.
 * @param[in] name - name of the field.
 * @param[in] desc - descriptor of the field.
 * @return Requested field, if the field exists, <code>NULL</code> otherwise.*/
Field* class_lookup_field_recursive(Class* clss, const char* name, const char* desc);
/** Looks up method in class and its superclasses.
 * @param[in] clss - class to lookup method in.
 * @param[in] name - name of the method as VM String.
 * @param[in] desc - descriptor of the method as VM String.
 * @return Requested method, if the method exists,
 *         <code>NULL</code> otherwise.
 * @note VMEXPORT specifier is solely for interpreter.*/
VMEXPORT
Method* class_lookup_method_recursive(Class* clss, const String* name, const String* desc);
/** Looks up method in class and its superclasses.
 * @param[in] clss - class to lookup method in.
 * @param[in] name - name of the method.
 * @param[in] desc - descriptor of the method.
 * @return Requested method, if the method exists,
 *         <code>NULL</code> otherwise.*/
Method* class_lookup_method_recursive(Class* clss, const char* name, const char* desc);
/** Looks up method in the given class only.
 * @param[in] clss - class to lookup method in.
 * @param[in] name - name of the method.
 * @param[in] desc - descriptor of the method.
 * @return Requested method, if the method exists,
 *         <code>NULL</code> otherwise.*/
Method* class_lookup_method(Class* clss, const char* name, const char* desc);
/** Gets method given its offset in the vtable.
 * @param[in] vt - vtable containing method.
 * @param[in] offset - offset of the method in the vtable.
 * @return Method at the specified offset.*/
Method* class_get_method_from_vt_offset(VTable* vt, unsigned offset);

/** Loads a class and performs the first two parts of the link process:
 * verify and prepare.
 * @param[in] env - VM environment.
 * @param[in] classname - name of a class to load.
 * @param[in] cl - class loader to load class with.
 * @return Loaded class, if loading and linking succeeded,
 *         <code>NULL</code> otherwise.*/
Class* class_load_verify_prepare_by_loader_jni(Global_Env* env,
                                               const String* classname,
                                               ClassLoader* cl);

/** Loads a class with bootstrap class loader and performs the first two parts
 * of the link process: verify and prepare.
 * @param[in] env - VM environment.
 * @param[in] classname - name of a class to load.
 * @param[in] cl - class loader to load class with.
 * @return Loaded class, if loading and linking succeeded,
 *         <code>NULL</code> otherwise.
 * @note The class is loaded .*/
Class* class_load_verify_prepare_from_jni(Global_Env* env,
                                          const String* classname);

/** Executes static initializer of class.
 * @param[in] clss - class to initialize.*/
void class_initialize_from_jni(Class *clss);

/** Registers a number of native methods to a given class.
 * @param[in] klass       - a specified class
 * @param[in] methods     - an array of methods
 * @param[in] num_methods - a number of methods
 * @return <code>false</code>, if methods resistration is successful;
 *         otherwise <code>true</code>.
 * @note Function raises <code>NoSuchMethodError</code> with the method name in 
 *       exception message, if one of the methods in the <code>JNINativeMethod*</code> 
 *       array is not present in a specified class.*/
bool
class_register_methods(Class_Handle klass, const JNINativeMethod* methods, int num_methods);

/** Unregisters a native methods off a given class.
 * @param[in] klass       - specified class
 * @return <code>false</code>, if methods unresistration is successful;
 *         otherwise <code>true</code>.*/
bool
class_unregister_methods(Class_Handle klass);

//method is defined in Resolve.cpp
Class* resolve_class_new_env(Global_Env* env, Class* clss, unsigned cp_index, bool raise_exn);
Method* resolve_special_method_env(Global_Env *env, Class_Handle curr_clss, unsigned index, bool raise_exn);
Method* resolve_static_method_env(Global_Env *env, Class *clss, unsigned cp_index, bool raise_exn);
Method* resolve_virtual_method_env(Global_Env *env, Class *clss, unsigned cp_index, bool raise_exn);
Method* resolve_interface_method_env(Global_Env *env, Class *clss, unsigned cp_index, bool raise_exn);
Field* resolve_static_field_env(Global_Env *env, Class *clss, unsigned cp_index, bool putfield, bool is_runtume);
Field* resolve_nonstatic_field_env(Global_Env* env, Class* clss, unsigned cp_index, unsigned putfield, bool raise_exn);




#endif // _CLASS_H_
