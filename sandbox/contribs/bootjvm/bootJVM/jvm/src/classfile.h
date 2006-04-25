#ifndef _classfile_h_included_
#define _classfile_h_included_

/*!
 * @file classfile.h
 *
 * @brief Definitions for <em>The Java Virtual Machine Specification,
 * version 2 Chapter 4, The Class File Format</em>.
 *
 * <em>The Class File Format</em>, from JDK 1.5 revision of this
 * section, entitled <b>ClassFileFormat-final.draft.pdf</b>,
 * has been used to define the contents of this file.  Originally,
 * the standard and well-published JDK 1.2 revision was used,
 * and there are a number of intermediate and semi-official
 * documents, but this seems to be the best and most accurate.
 *
 * ALL definitions that are not @e explicitly defined in this
 * document, yet are either conveniently placed here due to
 * intrinsic association with those that are in the document,
 * or those needed for late binding into the runtime structures
 * of this implementation, are prefixed with the string token
 * @b LOCAL_ for ease of identification.
 *
 * Any field of any data structure that is not documented will have
 * its definition in the JVM specfication itself.  The larger structures
 * tend to have at least some notes, but individual fields that are
 * fully described in the JVM spec are likely not to have redundant
 * commentary here.
 *
 * The JVM specification is available from Sun Microsystems' web site
 * at http://java.sun.com/docs/books/vmspec/index.html and
 * may be read online at
http://java.sun.com/docs/books/vmspec/2nd-edition/html/VMSpecTOC.doc.html
 *
 * The Java 5 class file format is available as a PDF file separately at
http://java.sun.com/docs/books/vmspec/2nd-edition/ClassFileFormat-final-draft.pdf
 * and was the basis for the ClassFile structure of this implementation.
 *
 *
 * @todo HARMONY-6-jvm-classfile.h-1 Need to verify which web document
 *       for the Java 5 class file definition is either "official",
 *       actually correct, or is the <em>de facto</em> standard.
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

ARCH_HEADER_COPYRIGHT_APACHE(classfile, h,
"$URL$",
"$Id$");

/*!
 * @internal Class file structures and code depend on packed structures.
 *           Change from global project-wide -fpack-struct (GCC version
 *           of structure packing option) to localized pragma for the
 *           structures that need it.
 *
 * @todo HARMONY-6-jvm-classfile.h-2 Need to look through class file
 *       code and remove dependency on structure packing in the way
 *       the code is written.  The macros from
 *       @link jvm/src/classfile.c classfile.c@endlink that are a
 *       good place to start are
 *       @link ALLOC_CP_INFO() ALLOC_CP_INFO@endlink and
 *       @link ALLOC_CF_ITEM() ALLOC_CF_ITEM@endlink.
 *
 */
#pragma pack(1)

/*!
 * @brief Attributes of a field, method, or class.
 *
 * See spec section 4.8.
 *
 * The definitions of @link #u1 u1@endlink and @link #u2 u2@endlink
 * and @link #u4 u4@endlink may be found in
 * @link jvm/src/jrtypes.h jrtypes.h@endlink to decouple these
 * primative types from class file specifics.  This is because
 * these three definitions are used all over the code
 * and not just in class file areas.
 *
 * @internal typedef placed here to avoid forward reference.
 *
 */
typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;

    u1 info[1]; /**< Mark space for one, but
                     @b attribute_length will reserve the correct
                     amount of space.  See spec pseudo-code:
                     @c @b info[attribute_length]; */

} attribute_info;


/*!
 * @brief @c @b constant_pool entry.
 *
 * See spec section 4.5.
 *
 * @internal typedef placed here to avoid forward reference.
 *
 */
typedef struct
{
    u1 tag;

    u1 *info; /**< Spec pseudo-code says: @c @b info[]; */

} cp_info;


/*!
 * @name Byte aligned structure entries.
 *
 * @brief Frame @p @b cp_info and @p @b attribute_info entries for
 * correct byte alignment on real machine architectures that
 * require it.
 *
 * The following structure is used for aligning (cp_info) data
 * on 4-byte addresses with the exception of the one-byte @p @b tag
 * field.  The next one aligns (attribute_info) similarly.
 * Doing this suppresses problem w/ C/C++ compilers
 * where certain architectures yield a @b SIGSEGV on odd-aligned
 * 16-bit integer accesses or non-4-byte aligned 32-bit integer
 * accesses.  For 16-bit accesses, such operations as,
 *
 * @verbatim
  
       short x, *p; // (short)
       ...
       p = some_value();
       ...
       x = *p;
  
   and for 32-bit accesses,
  
       int x, *p; // (int), etc.
       ...
       p = some_value();
       ...
       x = *p;
  
   @endverbatim
 *
 * both produce a @b SIGSEGV when @b p is not aligned on an 2- or 4-byte
 * address, respectively.
 *
 * By introducing the command line option to force structure packing,
 * this situation can occur (e.g.: GCC @c @b -fpack-struct option,
 * which suppresses 4-byte structure alignment).  Unfortunately, this
 * option is needed in order to properly size the structures in this
 * header file. Yet, some standard library calls fail when compiled
 * like this (like @b fprintf(3) ), so choose judiciously.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define CP_INFO_NUM_EMPTIES 3 /**< Size of padding for cp_info
                  * structures in size of cp_info_dup structure */

/*!
 * @brief Pad cp_info structures for proper multi-byte field
 * address boundary alignment.
 * @see FILL_INFO_DUP0 et al.
 *
 * @todo HARMONY-6-jvm-classfile.h-9 Rename cp_info_dup to become
 * cp_info_mem_align
 */
typedef struct
{
    rbyte empty[CP_INFO_NUM_EMPTIES];
                 /**< Align @p @b tag u1 byte against
                      END of 4-byte  word */

    cp_info cp;  /**< Misalign cp_info (on odd address) so that
                  *   all items except @p @b tag are on even addresses.
                  *   This assumes they are all u2 or u4.
                  */
} cp_info_dup;


#define ATTRIBUTE_INFO_NUM_EMPTIES 2 /**< Size of padding for
                  * attribute_info structures in size of
                  * attribute_info_dup structure */
/*!
 * @brief Pad attribute_info structures for proper multi-byte field
 * address boundary alignment.
 * @see FILL_INFO_DUP0 et al.
 *
 */
typedef struct
{
    rbyte empty[ATTRIBUTE_INFO_NUM_EMPTIES]; /**< Align
                  * @p @b attribute_name_index u2 jshort
                  * against END of 4-byte word
                  */

    attribute_info ai; /**< Misalign attribute_info (on non-4-byte
                  * address) so that @p @b attribute_length is on a
                  * 4-byte addresses.  Subsequent members may not 
                  * be  4-byte aligned, (per spec definitions),
                  * but those can be handled on a case-by-case basis.
                  * At least here, everything starts out on the correct
                  * alignment.
                  */

} attribute_info_dup;

/*@} */ /* End of grouped definitions */


/*!
 * @brief Field table entry.
 *
 * See spec section 4.6
 *
 * @internal typedef placed here to avoid forward reference.
 *
 */
typedef struct
{
    u2 access_flags;  /**< Bitwise access flags, containing various of
                       * the @link #ACC_PUBLIC ACC_xxx@endlink
                       * definitions */

    u2 name_index;    /**< Index into @c @b constant_pool of UTF8 string
                       *   containing the name of this field */

    u2 descriptor_index; /**< Index into @c @b constant_pool of UTF8
                          * string containing the descriptor of this
                          * field */

    u2 attributes_count; /**< Size of @p @b attributes */

    attribute_info_dup **attributes; /**< Field attributes array.
                       * The spec pseudo-code defines this as:
            <b><code>attributes_info fields[attributes_count]</code></b>
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * @c @b attributes_count elements.
                       *
                       * Notice that the @c @b attribute_info
                       * structure is found with this
                       * @c @b attribute_info_dup structure.
                       * The purpose for this is proper byte alignment
                       * of multi-byte items such as 2- and 4-byte
                       * integers on machine architectures that demand
                       * it lest they complain with @b SIGSEGV and the
                       * like.
                       */



    /*!
     * @brief Local implementation late binding extensions for fields.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     * @internal An @p @b oiflagJVM boolean is not needed since
     *           @p @b access_flags above contains this information
     *           in the @link #ACC_STATIC ACC_STATIC@endlink bit.
     *
     */
    struct LOCAL_field_binding
    {
        jvm_field_index fluidxJVM; /**< JVM class table field
                       * lookup index of this field.  If this is a
                       * class static field, use this value to index
                       * the @link #rclass.class_static_field_lookup
                         rclass.class_static_field_lookup@endlink
                       * array for the @link #ClassFile.fields
                       * ClassFile.fields[]@endlink index and in the
                       * @link #rclass.class_static_field_data
                         rclass.class_static_field_data@endlink
                       * array for the data jvalue.
                       *
                       * If this is an object instance field, use this
                       * value to index the
                       * @link #rclass.object_instance_field_lookup
                         rclass.object_instance_field_lookup@endlink
                       * array for the @link #ClassFile.fields
                       * ClassFile.fields[]@endlink index and in the
                       * @link #robject.object_instance_field_data
                         robject.object_instance_field_data@endlink
                       * array for the data jvalue.
                       */

    } LOCAL_field_binding;

} field_info;


/*!
 * @brief Method table entry.
 *
 * See spec section 4.7
 *
 * @internal typedef placed here to avoid forward reference.
 *
 */
typedef struct
{
    u2 access_flags;  /**< Bitwise access flags, containing various of
                       * the @link #ACC_PUBLIC ACC_xxx@endlink
                       * definitions */

    u2 name_index;    /**< Index into @c @b constant_pool of UTF8 string
                       *   containing the name of this method */

    u2 descriptor_index; /**< Index into @c @b constant_pool of UTF8
                          * string containing the descriptor of this
                          * method */


    u2 attributes_count; /**< Size of @p @b attributes */

    attribute_info_dup **attributes; /**< Method attributes array.
                       * The spec pseudo-code defines this as:
            <b><code>attributes_info fields[attributes_count]</code></b>
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * @c @b attributes_count elements.
                       *
                       * Notice that the @c @b attribute_info
                       * structure is found with this
                       * @c @b attribute_info_dup structure.
                       * The purpose for this is proper byte alignment
                       * of multi-byte items such as 2- and 4-byte
                       * integers on machine architectures that demand
                       * it lest they complain with @b SIGSEGV and the
                       * like.
                       */


    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for methods.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_method_binding
    {
        jvm_attribute_index codeatridxJVM; /**< JVM method attribute
                       * table index of the code for this method.
                       * It is @e required to have a valid attribute
                       * index here.
                       */

        jvm_attribute_index excpatridxJVM; /**< JVM method attribute
                       * table index of the exceptions for this method
                       * A valid attribute index here is optional.
                       * Methods are not required to use exceptions.
                       */

        jvm_native_method_ordinal nmordJVM; /**< JVM local native
                       * method ordinal number of this method if it
                       * is a native method.  Not applicable otherwise.
                       * If the native method is @e not a local one,
                       * that is, not implemented within the core JVM
                       * code, then this value @e must be set to @link
                         #JVMCFG_JLOBJECT_NMO_NULL
                         JVMCFG_JLOBJECT_NMO_NULL@endlink
                       * (as known to the JVM core code), which is the
                       * same as
                       * @link #JLOBJECT_NMO_NULL
                         JLOBJECT_NMO_NULL@endlink
                       * (as known to the outside world throught the
                       * JNI interface).
                       */

    } LOCAL_method_binding;

} method_info;


/* At last:  The class file typedef itself! */

/*!
 * @name JVM spec Section 4.2: The ClassFile structure.
 *
 *
 * <b>The following structure definition is NOT MEANT TO BE AN EXACT
 * REPRESENTATION OF THE DATA STRUCTURES SHOWN IN THE JAVA CLASS FILE
 * SPECIFICATION!</b>  That document contains numerous pseudo-code
 * representations of data structures.  Instead, this definition is
 * designed to be an actual real machine implementation of that
 * specification.
 *
 * Each of the variable-length structures from the spec are stored as
 * arrays of pointers to those structures, which are located in heap
 * allocated memory areas and their length, being always defined in
 * the spec as preceding the variable-length area, tells how many
 * elements are found in each array.  For example, @link
   #ClassFile.constant_pool_count ClassFile.constant_pool_count@endlink
 * and @link #ClassFile.constant_pool ClassFile.constant_pool@endlink.
 * Each element may, of course, be of variable size according to its
 * actual contents, and this will be reflected by the size of each heap
 * allocation.
 *
 * Furthermore, any variable or data type marked @b LOCAL_ in this
 * structure and its subsidiary components is a local implementation
 * extension that is no found in the specification, but is used by
 * the JVM runtime environment to facilitate processing, expecially to
 * expedite class linkages.  For example, CONSTANT_Class_info and
 * CONSTANT_Class_info.LOCAL_Class_binding.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The @b ClassFile structure definition.
 *
 * See spec section 4.2.
 *
 */
typedef struct
{
    u4 magic;         /**< Magic number for this file type.
                       * It @e must contain the constant value
                       * @link #CLASSFILE_MAGIC CLASSFILE_MAGIC@endlink
                       */

    u2 minor_version; /**< Minor class file version number */

    u2 major_version; /**< Major class file version number */

    u2 constant_pool_count; /**< Size of @c @b constant_pool plus one
                       * (which represents the unrepresented
                       * @c @b java.lang.Object ).  Index zero
                       * of @c @b constant_pool is this unrepresented
                       * slot, and @c @b constant_pool[0] is a NULL
                       * pointer.
                       */

    cp_info_dup **constant_pool; /**< Constant pool array.  The spec
                       * pseudo-code defines this as:
      <b><code>cp_info constant_pool[constant_pool_count - 1]</code></b>
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * <b><code>constant_pool_count - 1</code></b>
                       * elements.
                       *
                       * Notice that the @c @b cp_info structure
                       * is found with this @c @b cp_info_dup
                       * structure.  The purpose for this is proper
                       * byte alignment of multi-byte items such as
                       * 2- and 4-byte integers on machine architectures
                       * that demand it lest they complain with
                       * @b SIGSEGV and the like.
                       */

    u2 access_flags;  /**< Bitwise access flags, containing various of
                       * the @link #ACC_PUBLIC ACC_xxx@endlink
                       * definitions */

    u2 this_class;    /**< @c @b constant_pool index of the class of
                       * the @c @b this pointer, namely,
                       * what is the name of this selfsame class as
                       * found here in the class file. */

    u2 super_class;   /**< @p @b constant_pool index of the class of
                       * the @c @b super pointer, namely,
                       * what is the name of the super-class of this
                       * selfsame class as found here in the class
                       * file. */

    u2 interfaces_count; /**< Size of @p @b interfaces */

    u2 *interfaces;   /**< Interface array.  The spec
                       * pseudo-code defines this as:
                       * @c @b interfaces[interfaces_count]
                       * but it is implemented as an array of pointers.
                       * The length of this array is 
                       * @c @b interfaces_count elements.
                       */

    u2 fields_count;  /**< Size of @p @b fields */

    field_info **fields; /**< Fiels array.  The spec
                       * pseudo-code defines this as:
                       * @c @b fields[fields_count]
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * @c @b fields_count elements.
                       */

    u2 methods_count; /**< Size of @p @b methods */

    method_info **methods; /**< Methods array.  The spec
                       * pseudo-code defines this as:
                       * @c @b methods[methods_count]
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * @c @b methods_count elements.
                       */

    u2 attributes_count; /**< Size of @p @b attributes */

    attribute_info_dup **attributes; /**< Class file attributes array.
                       * The spec pseudo-code defines this as:
                    @c @b attributes_info @c @b fields[attributes_count]
                       * but it is implemented as a pointer to an array
                       * of pointers.  The length of this array is 
                       * @c @b attributes_count elements.
                       *
                       * Notice that the @c @b attribute_info
                       * structure is found with this
                       * @c @b attribute_info_dup structure.
                       * The purpose for this is proper byte alignment
                       * of multi-byte items such as 2- and 4-byte
                       * integers on machine architectures that demand
                       * it lest they complain with @b SIGSEGV and the
                       * like.
                       */

} ClassFile;


#define CLASSFILE_MAGIC 0xCAFEBABE /**< Magic number for class files */

/*@} */ /* End of grouped definitions */


/*!
 * @name List of all class file versions
 *
 */

/*@{ */ /* Begin grouped definitions */

#define VERSION_MAJOR_JDK1 45
#define VERSION_MAJOR_JDK2 46
#define VERSION_MAJOR_JDK3 47
#define VERSION_MAJOR_JDK4 48
#define VERSION_MAJOR_JDK5 49

/*@} */ /* End of grouped definitions */

/*!
 * @brief Default minor version number for primative pseudo-classes
 *
 */
#define VERSION_MINOR_DEFAULT 0

/*!
 * @name Define range of supported class file versions
 *
 */

/*@{ */ /* Begin grouped definitions */

#define VERSION_MAJOR_LOW  VERSION_MAJOR_JDK1
#define VERSION_MINOR_LOW   0
#define VERSION_MAJOR_HIGH VERSION_MAJOR_JDK5
#define VERSION_MINOR_HIGH  0

/*@} */ /* End of grouped definitions */



/*!
 * @name Table 4.1: Access Flags for Classes
 *
 * The first definition of all such tokens is found here.  Throughout
 * the spec, many of these symbols will be @e redefined to be the
 * same value.  Such redefinitions are commented here in source, but
 * are not redefined <em>per se</em>.  There is no "grand union"
 * definition.
 *
 * For other @link #ACC_PUBLIC ACC_xxx@endlink definitions,
 * see also table 4.4, 4.5, 4.7, where more definitions are found.
 *
 */

/*@{ */ /* Begin grouped definitions */


#define LOCAL_ACC_EMPTY 0x0000 /**< Not defined, empty (not in spec) */

#define ACC_PUBLIC    0x0001   /**< Declared @b public; may be accessed
                                    from outside its package. */

#define ACC_FINAL     0x0010   /**< Declared @b final;no subclasses
                                    allowed */

#define ACC_SUPER     0x0020   /**< Treat superclass methods specially
                                    when invoked by the
                                    @b INVOKESPECIAL instruction*/

#define ACC_INTERFACE 0x0200   /**< Is an interface, not a class. */

#define ACC_ABSTRACT  0x0400   /**< Declared @c @b abstract ; may not
                                    be instantiated. */

#define ACC_SYNTHETIC 0x1000   /**< Declared @c @b synthetic ; not
                                    present in the source code. */

#define ACC_ANNOTATION 0x2000  /**< Declared as an annotation type */

#define ACC_ENUM      0x4000   /**< Declared as an enum type */

/*@} */ /* End of grouped definitions */


/*!
 * @brief Root Java language object name
 *
 */

#define CONSTANT_UTF8_JAVA_LANG_OBJECT "java/lang/Object"


/*!
 * @name Section 4.3:  The Internal Form of Fully Qualified Class \
and Interface Names
 */

/*@{ */ /* Begin grouped definitions */

#define CLASSNAME_EXTERNAL_DELIMITER_CHAR   '.'
#define CLASSNAME_EXTERNAL_DELIMITER_STRING "."

#define CLASSNAME_INTERNAL_DELIMITER_CHAR   '/'
#define CLASSNAME_INTERNAL_DELIMITER_STRING "/"

#define CLASSNAME_INNERCLASS_MARKER_CHAR   '$'
#define CLASSNAME_INNERCLASS_MARKER_STRING "$"

#define CLASSNAME_INNERCLASS_ESCAPE_CHAR   '\\'
#define CLASSNAME_INNERCLASS_ESCAPE_STRING "\\"

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.4:  Descriptors-- Section 4.4.1:  Grammar Notation
 *
 */

/*@{ */ /* Begin grouped definitions */

#define GRAMMAR_PARM_OPEN  '(' /**< Open parenthesis for starting
                                    a param defn */
#define GRAMMAR_PARM_CLOSE ')' /**< Open parenthesis for starting
                                    a param defn */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.4.2:  Field Descriptors
 *
 * Table 4.2:  BaseType Character Definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

#define BASETYPE_CHAR_B  'B'   /**< Signed byte */
#define BASETYPE_CHAR_C  'C'   /**< Unicode character */
#define BASETYPE_CHAR_D  'D'   /**< Double-precision floating-point
                                    value */
#define BASETYPE_CHAR_F  'F'   /**< Single-precision floating-point 
                                    value */
#define BASETYPE_CHAR_I  'I'   /**< Integer */
#define BASETYPE_CHAR_J  'J'   /**< Long integer */


/*!
 * @internal @c @b L/class/name; includes
 *           @b _CHAR_ and @b _STRING_ forms
 */

#define BASETYPE_CHAR_L   'L'  /**< an instance of class '/class/name'*/
#define BASETYPE_STRING_L "L"  /**< null-terminated string form
                                    of BASETYPE_CHAR_L */

#define BASETYPE_CHAR_L_TERM   ';' /**< terminator for instance of
                                        class */
#define BASETYPE_STRING_L_TERM ";" /**< null-terminated string form
                                        of BASETYPE_CHAR_L_TERM */



#define BASETYPE_CHAR_S       'S'   /**< Signed short */
#define BASETYPE_CHAR_Z       'Z'   /**< Boolean, @b true or @b false */

#define BASETYPE_CHAR_ARRAY   '['   /**< Reference to one array 
                                         dimension */
#define BASETYPE_STRING_ARRAY "["   /**< null-terminated string form
                                         of BASETYPE_CHAR_ARRAY */

#define LOCAL_BASETYPE_ERROR '?'    /**< Invalid basetype due to
                                         malformed @p @b tag or other
                                         error (not in spec) */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.4.3:  Method Descriptors
 */

/*@{ */ /* Begin grouped definitions */

#define METHOD_CHAR_VOID         'V'  /**< No return type, instead:
                                           (rvoid) fn(p1,p2,...) */
#define METHOD_STRING_VOID       "V"  /**< null-terminated string form
                                           of METHOD_CHAR_VOID */

#define METHOD_PARM_MAX          255  /**< Max length of method
                                           descriptor string */

#define METHOD_CHAR_OPEN_PARM    '('  /**< Open parameter list */
#define METHOD_STRING_OPEN_PARM  "("  /**< null-terminated string form
                                           of METHOD_CHAR_OPEN_PARM */

#define METHOD_CHAR_CLOSE_PARM   ')'  /**< Open parameter list */
#define METHOD_STRING_CLOSE_PARM ")"  /**< null-terminated string form
                                           of METHOD_CHAR_OPEN_PARM */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.4.4:  Signatures
 *
 */

/*@{ */ /* Begin grouped definitions */

#define BASETYPE_CHAR_T  'T'   /**< a type variable signature */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5:  The Constant Pool
 *
 * typedef struct cp_info:  See fwd refs above typedef ClassFile
 *
 * See also table 4.3:  ConstantType Definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

#define CONSTANT_CP_DEFAULT_INDEX    0 /**< References
                                        @c @b java.lang.Object */
#define CONSTANT_CP_START_INDEX      1 /**< Ref first constant item */

/*@} */ /* End of grouped definitions */

/*!
 * @name Table 4.3:  ConstantType Definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

#define CONSTANT_Class               7
#define CONSTANT_Fieldref            9
#define CONSTANT_Methodref          10
#define CONSTANT_InterfaceMethodref 11
#define CONSTANT_String              8
#define CONSTANT_Integer             3
#define CONSTANT_Float               4
#define CONSTANT_Long                5
#define CONSTANT_Double              6
#define CONSTANT_NameAndType        12
#define CONSTANT_Utf8                1

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.1:  The CONSTANT_Class_info structure
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u2 name_index;


    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Class_info CONSTANT_Class_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Class_binding
    {
        jvm_class_index clsidxJVM; /**< JVM class table index of
                                        this class */

    } LOCAL_Class_binding;

} CONSTANT_Class_info;


#define CONSTANT_MAX_ARRAY_DIMS      255 /**< Highest number of array
                                              dimensions */

#define LOCAL_CONSTANT_NO_ARRAY_DIMS   0 /**< Not stated in spec, but
                                              implied */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.2:  The CONSTANT_Fieldref_info, \
CONSTANT_Methodref_info, and \
CONSTANT_InterfaceMethodref_info structures
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u2 class_index;
    u2 name_and_type_index;


    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Fieldref_info CONSTANT_Fieldref_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Fieldref_binding
    {
        jvm_class_index clsidxJVM;   /**< JVM class table index of
                                          this class */

        jvm_field_index fluidxJVM;   /**< JVM class table field lookup
                                        index of this field */

        rboolean        oiflagJVM;   /**< JVM class table flag: class
                                      * static vs object instance
                                      * field.  This information is
                                      * derived directly from the
                                      * field's definition of its
                                      * @link #field_info.access_flags
                                        field_info.access_flags@endlink,
                                      * namely @link #ACC_STATIC
                                        ACC_STATIC@endlink.  This
                                      * information is duplicated here
                                      * for a localized copy of this
                                      * information for implementation
                                      * convenience.  It is not
                                      * something that will ever change
                                      * over the life of the loaded
                                      * class.  It is @link #rtrue
                                        rtrue@endlink if this field is
                                      * part of an object instance
                                      * and @link #rfalse rfalse@endlink
                                      * if it is part of a class static
                                      * instance.
                                      */

        jvm_basetype  jvaluetypeJVM; /**< JVM class table field type.
                                      * It is needed to know which kind
                                      * of load and store to use for
                                      * a field access, which involves
                                      * a member of a jvalue.
                                      */

    } LOCAL_Fieldref_binding;

} CONSTANT_Fieldref_info;

typedef struct
{
    u1 tag;
    u2 class_index;
    u2 name_and_type_index;


    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Methodref_info CONSTANT_Methodref_info@endlink.
     *
     * Notice that the contents of this binding includes @e everything
     * needed to load a @link #jvm_pc jvm_pc@endlink structure plus
     * a related native method ordinal, if any.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Methodref_binding
    {
        jvm_class_index clsidxJVM;   /**< JVM class table index of
                                          this class */

        jvm_method_index mthidxJVM;  /**< JVM method table index
                                          of this method */

        jvm_attribute_index
            codeatridxJVM;           /**< JVM method attribute table
                                      *   index of the code for this
                                      *   method.
                                      */

        jvm_attribute_index
            excpatridxJVM;           /**< JVM method attribute table
                                      *   index of the exceptions for
                                      *   this method.
                                      */

        jvm_native_method_ordinal
            nmordJVM;                /**< JVM local native method
                                      *   ordinal number of this
                                      *   native method.
                                      */

    } LOCAL_Methodref_binding;

} CONSTANT_Methodref_info;

typedef struct
{
    u1 tag;
    u2 class_index;
    u2 name_and_type_index;


    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_InterfaceMethodref_info
       CONSTANT_InterfaceMethodref_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_InterfaceMethodref_binding
    {
        jvm_class_index clsidxJVM;   /**< JVM class table index of
                                          this class */

        jvm_method_index mthidxJVM;  /**< JVM method table index of
                                          this method */

        jvm_attribute_index
            codeatridxJVM;           /**< JVM method attribute table
                                          index of the code for this
                                          method */

        jvm_attribute_index
            excpatridxJVM;           /**< JVM method attribute table
                                      *   of the exceptions for this
                                      *   method.
                                      */

        jvm_native_method_ordinal
            nmordJVM;               /**< JVM local native method
                                     *   ordinal number of this native
                                     *   method.
                                     */

    } LOCAL_InterfaceMethodref_binding;

} CONSTANT_InterfaceMethodref_info;


#define METHOD_CHAR_INIT '<'        /**< Special character for naming
                                         of an object constructor method
                                         name, that is, the
                                         @c @b \<init\> method, and the
                                         class constructor method
                                         name, that is, the
                                         @c @b \<clinit\> method */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.3:  The CONSTANT_String_info structure
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u2 string_index;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_String_info CONSTANT_String_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_String_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         *
         */
    } LOCAL_String_binding;

} CONSTANT_String_info;

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.4:  The CONSTANT_Integer_info and
 *                       CONSTANT_Float_info structures
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u4 bytes;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Integer_info CONSTANT_Integer_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Integer_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         *
         */
    } LOCAL_Integer_binding;

} CONSTANT_Integer_info;

typedef struct
{
    u1 tag;
    u4 bytes;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Float_info CONSTANT_Float_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Float_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         */
    } LOCAL_Float_binding;

} CONSTANT_Float_info;

/*@} */ /* End of grouped definitions */

/*!
 * @name Representation of special case single-precision
 * floating point numbers
 *
 */
/*@{ */ /* Begin grouped definitions */

#define JINT_LARGEST_POSITIVE     ((juint) 0x7fffffff)
#define JINT_LARGEST_NEGATIVE     ((juint) 0x80000000)


/*!
 * @todo HARMONY-6-jvm-classfile.h-3 Need to verify the proper
 *       values of single-precision floating point positive zero
 *       and negative zero.
 *
 */
#define JFLOAT_POSITIVE_ZERO      ((juint) 0x00000000)
#define JFLOAT_NEGATIVE_ZERO      ((juint) 0x80000000)


#define JFLOAT_POSITIVE_INFINITY  ((juint) 0x7f800000)
#define JFLOAT_NEGATIVE_INFINITY  ((juint) 0xff800000)


#define JFLOAT_NAN_RANGE1_MIN ((juint) 0x7f800001)
#define JFLOAT_NAN_RANGE1_MAX ((juint) 0x7fffffff)

#define JFLOAT_NAN_RANGE2_MIN ((juint) 0xff800001)
#define JFLOAT_NAN_RANGE2_MAX ((juint) 0xffffffff)


/*!
 * @brief Determine if a single-precision floating point number
 * is a @b NAN case, not a number.
 *
 * The parameter must be a @link #jlong jlong@endlink representation
 * of a @link #jfloat jfloat@endlink value as forcibly converted
 * using @link #FORCE_JINT() FORCE_JINT()@endlink.
 *
 *
 * @param forced_jint  @link #jlong jlong@endlink representation of a
 *                     single-precision floating point number, per
 *                     above instructions.
 *
 *
 * @returns non-zero if @c @b forced_jint is a single-precision
 *          floating point @b NAN value, not a number.  Zero otherwise.
 *
 */
#define JFLOAT_IS_NAN(forced_jint)                          \
    (((JFLOAT_NAN_RANGE1_MIN <= (juint) (forced_jint)) &&    \
      (JFLOAT_NAN_RANGE1_MAX >= (juint) (forced_jint)))      \
                                                         || \
     ((JFLOAT_NAN_RANGE2_MIN <= (juint) (forced_jint)) &&    \
      (JFLOAT_NAN_RANGE2_MAX >= (juint) (forced_jint))))

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.5:  The CONSTANT_Long_info and
 *                       CONSTANT_Double_info structures
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u4 high_bytes;
    u4 low_bytes;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Long_info CONSTANT_Long_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Long_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         */
    } LOCAL_Long_binding;

} CONSTANT_Long_info;

typedef struct
{
    u1 tag;
    u4 high_bytes;
    u4 low_bytes;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Double_info CONSTANT_Double_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Double_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         */
    } LOCAL_Double_binding;

} CONSTANT_Double_info;

/*@} */ /* End of grouped definitions */

/*!
 * @name Representation of special case double-precision
 * floating point numbers
 *
 * @note In order to keep GCC from complaining, 'warning' integer
 *       constant is too large for "long" type', these constants
 *       have been presented as expressions instead.  It seems
 *       that (long long) constants must be 32-bit values for
 *       32-bit compilations, but arithmetic may be done on
 *       those expressions without harm.
 *
 */
/*@{ */ /* Begin grouped definitions */

#define JLONG_LARGEST_POSITIVE ((((julong) 0x7fffffff) << 32) | \
                                                   0xffffffff)
#define JLONG_LARGEST_NEGATIVE  (((julong) 0x80000000) << 32)


/*!
 * @todo HARMONY-6-jvm-classfile.h-4 Need to verify the proper
 *       values of double-precision floating point positive zero
 *       and negative zero.
 *
 */
#define JDOUBLE_POSITIVE_ZERO      (((julong) 0x00000000) << 32)
#define JDOUBLE_NEGATIVE_ZERO      (((julong) 0x80000000) << 32)


#define JDOUBLE_POSITIVE_INFINITY  (((julong) 0x7ff00000) << 32)
#define JDOUBLE_NEGATIVE_INFINITY  (((julong) 0xfff00000) << 32)


#define JDOUBLE_NAN_RANGE1_MIN ((((julong) 0x7ff00000) << 32) | 1)
#define JDOUBLE_NAN_RANGE1_MAX ((((julong) 0x7fffffff) << 32) | \
                                                   0xffffffff)

#define JDOUBLE_NAN_RANGE2_MIN ((((julong) 0xfff00000) << 32) | 1)
#define JDOUBLE_NAN_RANGE2_MAX ((((julong) 0xffffffff) << 32) | \
                                                   0xffffffff)

/*!
 * @brief Determine if a double-precision floating point number
 * is a @b NAN case, not a number.
 *
 * The parameter must be a @link #jlong jlong@endlink representation
 * of a @link #jdouble jdouble@endlink value as forcibly converted
 * using @link #FORCE_JLONG() FORCE_JLONG()@endlink.
 *
 *
 * @param forced_jlong @link #jlong jlong@endlink representation of a
 *                     double-precision floating point number, per
 *                     above instructions
 *
 *
 * @returns non-zero if @c @b dpfjlong is a double-precision floating
 *          point @b NAN value, not a number.  Zero otherwise.
 *
 */
#define JDOUBLE_IS_NAN(forced_jlong)                          \
    (((JDOUBLE_NAN_RANGE1_MIN <= (julong) forced_jlong) &&    \
      (JDOUBLE_NAN_RANGE1_MAX >= (julong) forced_jlong))   || \
                                                              \
     ((JDOUBLE_NAN_RANGE2_MIN <= (julong) forced_jlong) &&    \
      (JDOUBLE_NAN_RANGE2_MAX >= (julong) forced_jlong)))

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.6:  The CONSTANT_NameAndType_info structure
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u2 name_index;
    u2 descriptor_index;

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for @link
       #CONSTANT_NameAndType_info CONSTANT_NameAndType_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_NameAndType_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         */
    } LOCAL_NameAndType_binding;

} CONSTANT_NameAndType_info;

#define CONSTANT_UTF8_INSTANCE_CONSTRUCTOR "<init>"

#define CONSTANT_UTF8_INSTANCE_CONSTRUCTOR_DESCRIPTOR_DEFAULT "()V" /**<
                     (Not in this section of spec, but very relevant) */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.7:  Support for the CONSTANT_Utf8_info structure
 *
 * <ul>
 * <li>
 * <b>SINGLE-BYTE UTF-8 FORMAT</b>:
 *                           Range of 7-bit ASCII (ANSI alphabet 5),
 *                           but @e without the @b NUL character 0x00
 * </li>
 *
 * <li>
 * <b>DOUBLE-BYTE UTF-8 FORMAT</b>:
 *                           Range of 8th bit on ASCII (plus 7-bit ASCII
 *                           NUL character: '\\u0000'), namely '\\u0080'
 *                           through '\\u00ff', then extending on up
 *                           3 more bits, thus adding '\\u100' through
 *                           '\\u07ff'.
 * </li>
 *
 * <li>
 * <b>TRIPLE-BYTE UTF-8 FORMAT:</b>:
 *                          Character ranges '\\u0800' through '\\uFFFF'
 * </li>
 *
 * <li>
 * <b>HEXTUPLE-BYTE UTF-16 FORMAT</b>:
 *                           There is a typographical error in spec
 *                           section 4.5.7 describing describing 21-bit
 *                           character encoding.  Therefore, this
 *                           format has not been implemented pending
 *                           proper resolution.  It probably is correct
 *                           to assume that the spurious "-1" in the
 *                           second byte of the first triple should say
 *                           "(bits 20-16)" instead of "(bits 20-16)-1".
 *                           This needs to be researched and
 *                           implemented for full spec compliance.
 * </li>
 *
 * <li>
 * <b>OTHER UTFx FORMATS</b>: The JVM does not (yet) recognized these.
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-classfile.h-5 Clarify the hextuple-byte UTF-16
 *       encoding requirements, per above commentary.
 *
 */


/*@{ */ /* Begin grouped definitions */

/* SINGLE-BYTE UTF-8 FORMAT */

#define UTF8_SINGLE_MIN   0x01  /**< '\\u0001', UTF-8 representation */
#define UTF8_SINGLE_MAX   0x7f  /**< '\\u007f', UTF-8 representation */

#define UTF8_SINGLE_MASK1 0x80  /**< Bit 7 must be clear */
#define UTF8_SINGLE_MASK0 0x7f  /**< Bits 0-6 contain data
                                     (except != 0, or NUL) */

#define UNICODE_SINGLE_MIN 0x0001 /**< '\\u0001', Unicode
                                       representation */
#define UNICODE_SINGLE_MAX 0x007f /**< '\\u007f', Unicode
                                       representation */


/* DOUBLE-BYTE UTF-8 FORMAT */

#define UTF8_DOUBLE_NUL 0x0000  /**< '\\u0000', UTF-8 representation */
#define UTF8_DOUBLE_MIN 0x0080  /**< '\\u0080', UTF-8 representation */
#define UTF8_DOUBLE_MAX 0x07ff  /**< '\\u07ff', UTF-8 representation */

#define UNICODE_DOUBLE_NUL 0x0000  /**< '\\u0000', Unicode
                                        representation */
#define UNICODE_DOUBLE_MIN 0x0080  /**< '\\u0080', Unicode
                                        representation */
#define UNICODE_DOUBLE_MAX 0x07ff  /**< '\\u07ff', Unicode
                                        representation */


/* Definition of first byte (byte "x") of two-byte format: */

#define UTF8_DOUBLE_FIRST_MASK1  0xe0  /**< Top 3 bits of first byte */
#define UTF8_DOUBLE_FIRST_VAL    0xc0  /**< Top 3 bits are '110' */

#define UTF8_DOUBLE_FIRST_MASK0  0x1f  /**< Bottom 5 bits contain data
                                            bits 10-6 */
#define UTF8_DOUBLE_FIRST_SHIFT     6  /**< Move first byte up to bits
                                            10-6 */


/* Definition of second byte (byte "y") of two-byte format: */

#define UTF8_DOUBLE_SECOND_MASK1 0xc0  /**< Top 2 bits of second byte */
#define UTF8_DOUBLE_SECOND_VAL   0x80  /**< Top 2 bits are '10' */

#define UTF8_DOUBLE_SECOND_MASK0 0x3f  /**< Bottom 6 bits contain data
                                            bits 0-5 */


/* TRIPLE-BYTE UTF-8 FORMAT: */

#define UTF8_TRIPLE_MIN 0x0800  /**< '\\u0800', UTF-8 representation */
#define UTF8_TRIPLE_MAX 0xffff  /**< '\\uffff', UTF-8 representation */

#define UNICODE_TRIPLE_MIN 0x0800  /**< '\\u0800', Unicode
                                        representation */
#define UNICODE_TRIPLE_MAX 0xffff  /**< '\\uffff', Unicode
                                        representation */


/* Definition of first byte (byte "x") of three-byte format: */

#define UTF8_TRIPLE_FIRST_MASK1  0xf0  /**! Top 4 bits of first byte */
#define UTF8_TRIPLE_FIRST_VAL    0xe0  /**! Top 4 bits are '1110' */

#define UTF8_TRIPLE_FIRST_MASK0  0x0f  /**! Bottom 5 bits contain data
                                            bits 15-12 */
#define UTF8_TRIPLE_FIRST_SHIFT    12  /**! Move first byte up to
                                            bits 15-12 */


/* Definition of second byte (byte "y") of three-byte format: */

#define UTF8_TRIPLE_SECOND_MASK1 0xc0  /**! Top 2 bits of second byte */
#define UTF8_TRIPLE_SECOND_VAL   0x80  /**! Top 2 bits are '10' */

#define UTF8_TRIPLE_SECOND_MASK0 0x3f  /**! Bottom 6 bits contain data
                                            bits 11-6 */
#define UTF8_TRIPLE_SECOND_SHIFT    6  /**! Move second byte up to
                                            bits 10-6 */


/* Definition of third byte (byte "z") of three-byte format: */

#define UTF8_TRIPLE_THIRD_MASK1  0xc0  /**! Top 2 bits of third byte */
#define UTF8_TRIPLE_THIRD_VAL    0x80  /**! Top 2 bits are '10' */

#define UTF8_TRIPLE_THIRD_MASK0  0x3f  /**! Bottom 6 bits contain data
                                            bits 5-0 */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.5.7:  The CONSTANT_Utf8_info structure itself
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u1 tag;
    u2 length;
    u1 bytes[1];  /**< spec says:
                       <b><code>u1 bytes[length];</code></b> */

    /*** LOCAL IMPLEMENTATION LATE BINDING EXTENSIONS ***/

    /*!
     * @brief Local implementation late binding extensions for
     * @link #CONSTANT_Utf8_info CONSTANT_Utf8_info@endlink.
     *
     * Uses LOCAL_ as a prefix to signify an item that is @e not
     * part of the JVM spec itself, but an implementation detail.
     *
     */
    struct LOCAL_Utf8_binding
    {
        /*!
         * @internal There is not late binding for this structure.
         *           However, to maintain compatibility with the
         *           @link #ALLOC_CP_INFO() ALLOC_CP_INFO()@endlink
         *           macro in @link jvm/src/classfile.c
                     classfile.c@endlink, this empty structure
         *           is provided.
         *
         * @warning  If ever a late binding must be implemented,
         *           the above @c @b bytes[1] definition must
         *           be adjusted to allow for this in some way
         *           that either globally changes the implementation
         *           or does not disturb it at all.
         */
    } LOCAL_Utf8_binding;

} CONSTANT_Utf8_info;

/*@} */ /* End of grouped definitions */


/*!
 * @name Forbidden UTF values.
 *
 * @brief No byte in bytes[] may contain these values because
 * they present certain parsing problems in the Java subset of UTF-8.
 *
 */
/*@{ */ /* Begin grouped definitions */

#define UTF8_FORBIDDEN_ZERO      0x00  /**< Looks suspiciously like
                                            ASCII NUL */

#define UTF8_FORBIDDEN_MIN       0xf0  /**< Min of range for 4-byte UTF
                                            values,etc */
#define UTF8_FORBIDDEN_MAX       0xff  /**< Max of forbidden range */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.6: Fields
 *
 * Table 4.4: Access Flags for Fields (New comments,
 * plus new definitions)
 *
 * typedef struct field_info:  See forward references
 * above definition of ClassFile.
 *
 * Defined first in section 4.6 (Fields), possibly later also.
 *
 * For other @link #ACC_PUBLIC ACC_xxx@endlink definitions,
 * see also table 4.1, 4.5, 4.7.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*      ACC_PUBLIC           */

#define ACC_PRIVATE   0x0002 /**< Declared @c @b private ; usable only
                                  within the defining class. */

#define ACC_PROTECTED 0x0004 /**< Declared @c @b protected ; may be
                                  accessed within subclasses. */

#define ACC_STATIC    0x0008 /**< Declared @c @b static . */

/*      ACC_FINAL            ** Declared @c @b final ; no further
                                  assignment after initialization. */

#define ACC_VOLATILE  0x0040 /**< Declared @c @b volatile ; cannot be
                                   cached */

#define ACC_TRANSIENT 0x0080 /**< Declared @c @b transient ; not written
                                  or read by a persistent object
                                  manager */

/*      ACC_SYNTHETIC        ** Declared @c @b synthetic ; not present
                                  in the source code. */

/*      ACC_ENUM             ** Declared as an enum type */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.7:  Methods
 *
 * Table 4.5:  Access Flags for methods (New comments,
 * plus new definitions)
 *
 *
 * typedef struct method_info:  See forward references
 * above definition of ClassFile
 *
 * Defined first in section 4.7 (Methods), possibly later also.
 *
 * For other @link #ACC_PUBLIC ACC_xxx@endlink definitions,
 * see also table 4.1, 4.4, 4.7.
 */

/*@{ */ /* Begin grouped definitions */

/*      ACC_PUBLIC           */
/*      ACC_PRIVATE          */
/*      ACC_PROTECTED        */
/*      ACC_STATIC           */
/*      ACC_FINAL            **Declared @c @b final ; may not be
                                  overridden */
#define ACC_SYNCHRONIZED 0x0010 /**< Declared @c @b synchronized ;
                                   invokation is wrapped in a
                                    monitor lock. */

#define ACC_BRIDGE    0x0040    /**< A bridge method, generated by the
                                  compiler. */

#define ACC_VARARGS   0x0080    /**< Declared with variable number of
                                  arguments. */

#define ACC_NATIVE    0x0100    /**< Declared @c @b native ; implemented
                                     in a language other than Java. */

/*      ACC_ABSTRACT            ** Declared @c @b abstract ; no
                                  implementation is provided. */

#define ACC_STRICT    0x0800    /**< Declared @c @b strictfp ;
                                     floating-point mode is FP-strict */

/*      ACC_SYNTHETIC           ** Declared @c @b synthetic ; not
                                   present in the source code. */


/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.1:  Defining and Naming new Attributes
 *
 * typedef struct attribute_info: See forward references
 * above definition of ClassFile
 *
 */

/*@{ */ /* Begin grouped definitions */

#define LOCAL_CONSTANT_UTF8_UNKNOWN_ATTRIBUTE "Unknown" /**<
                                                          Not in spec*/

#define               LOCAL_UNKNOWN_ATTRIBUTE_ENUM   0 /**<
                                                          Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.2:  The ConstantValue Attribute
 *
 * See also table 4.6:  Valid ConstantValue Field Types
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 constantvalue_index;

} ConstantValue_attribute;

#define CONSTANT_UTF8_CONSTANTVALUE_ATTRIBUTE "ConstantValue"

#define       CONSTANTVALUE_ATTRIBUTE_LENGTH 2
#define LOCAL_CONSTANTVALUE_ATTRIBUTE_ENUM   1 /**< Not in spec */


/* Table 4.6:  Valid ConstantValue Field Types */

/*           CONSTANT_Long      */
/*           CONSTANT_Float     */
/*           CONSTANT_Double    */
/*           CONSTANT_Integer   */
/*           CONSTANT_String    */

/*@} */ /* End of grouped definitions */

/*!
 * @name Section 4.8.3:  The Code Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b exception_table structure, here named
 * as @b exception_table_entry (to avoid naming conflicts in code
 * versus spec requirements).
 *
 * See section 4.8.3.
 *
 */
typedef struct
{
    u2 start_pc;
    u2 end_pc;
    u2 handler_pc;
    u2 catch_type;

} exception_table_entry;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 max_stack;
    u2 max_locals;

    u4 code_length;
    u1 *code;      /**< Spec pseudo-code: @c @b code[code_length]; */

    u2 exception_table_length;
    exception_table_entry *exception_table; /**< Spec pseudo-code:
                       @c @b exception_table[exception_table_length]; */

    u2 attributes_count;
    attribute_info_dup **attributes; /**< Spec pseudo-code:
      <b><code>attribute_info attributes[attributes_count]</code></b> */

} Code_attribute;

#define CONSTANT_UTF8_CODE_ATTRIBUTE                  "Code"

#define         LOCAL_CODE_ATTRIBUTE_ENUM             2 /**< Not
                                                               in spec*/

#define CODE_ATTRIBUTE_MAX_LOCALS_BOUNDARY_LONGDOUBLE 2

#define CODE_ATTRIBUTE_MAX_LOCALS_BOUNDARY_OTHERS     1

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.4:  The Exceptions Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 number_of_exceptions;

    u2 exception_index_table[1]; /**< Mark space only for one, but
                                      @b attribute_length will reserve
                                      the correct amount of space.
                                      Spec pseudo-code:
                   @c @b exception_index_table[number_of_exceptions]; */
} Exceptions_attribute;

#define CONSTANT_UTF8_EXCEPTIONS_ATTRIBUTE      "Exceptions"

#define         LOCAL_EXCEPTIONS_ATTRIBUTE_ENUM 3 /**< Not in spec */

#define CODE_DEFAULT_CATCH_TYPE                 0 /**<  This handler is
                                                   for all exceptions */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.5:  The InnerClasses Attribute
 *
 * For other @link #ACC_PUBLIC ACC_xxx@endlink definitions,
 * see also table 4.1, 4.4, 4.5.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b inner_class_table_entry structure
 * (anonymous in spec).
 *
 * See section 4.8.5.
 *
 */
typedef struct
{
    u2 inner_class_info_index;
    u2 outer_class_info_index;
    u2 inner_name_index;
    u2 inner_class_access_flags;

} inner_class_table_entry;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 number_of_classes;
    inner_class_table_entry classes[1]; /**< Mark space for one, but
                         @b number_of_classes will reserve the
                         correct amount of space.  See spec pseudo-code:
                         @c @b classes[number_of_classes]; */
} InnerClasses_attribute;

#define CONSTANT_UTF8_INNERCLASSES_ATTRIBUTE      "InnerClasses"

#define         LOCAL_INNERCLASSES_ATTRIBUTE_ENUM 4 /**< Not in spec */

/* Defined first in section 4.8.5 (The @b InnerClasses attribute),
                                                  possibly later also */

/* Table 4.7: Access Flags for Attributes (New cmts, all defn's above)*/

/*      ACC_PUBLIC         ** Marked or implicitly @c @b public
                                in source. */

/*      ACC_PRIVATE        ** Marked @c @b private in source. */

/*      ACC_PROTECTED      ** Marked @c @b protected in source. */

/*      ACC_STATIC         ** Marked or implicitly @c @b static
                                in source. */

/*      ACC_FINAL          ** Marked @c @b final in source. */

/*      ACC_INTERFACE      ** Was an @c @b interface in source. */

/*      ACC_ABSTRACT       ** Marked or implicitly @@c b abstract
                                in source. */

/*      ACC_SYNTHETIC      ** Declared @c @b synthetic ; not present
                                in source. */

/*      ACC_ANNOTATION     ** Declared as an annotation type */

/*      ACC_ENUM           ** Declared as an enum type */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.6:  The EnclosingMethod Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 class_index;
    u2 method_index;

} EnclosingMethod_attribute;

#define CONSTANT_UTF8_ENCLOSINGMETHOD_ATTRIBUTE        "EnclosingMethod"

#define               ENCLOSINGMETHOD_ATTRIBUTE_LENGTH     4

#define               LOCAL_ENCLOSINGMETHOD_ATTRIBUTE_ENUM 5 /**< Not
                                                              in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.7:  The Synthetic Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;

} Synthetic_attribute;

#define CONSTANT_UTF8_SYNTHETIC_ATTRIBUTE        "Synthetic"

#define               SYNTHETIC_ATTRIBUTE_LENGTH 0

#define         LOCAL_SYNTHETIC_ATTRIBUTE_ENUM   7 /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.8:  The Signature Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 signature_index;

} Signature_attribute;

#define CONSTANT_UTF8_SIGNATURE_ATTRIBUTE        "Signature"

#define               SIGNATURE_ATTRIBUTE_LENGTH 2

#define         LOCAL_SIGNATURE_ATTRIBUTE_ENUM   6 /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.9:  The SourceFile Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 sourcefile_index;

} SourceFile_attribute;


#define CONSTANT_UTF8_SOURCEFILE_ATTRIBUTE        "SourceFile"

#define               SOURCEFILE_ATTRIBUTE_LENGTH 2

#define         LOCAL_SOURCEFILE_ATTRIBUTE_ENUM   8 /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.10:  The LineNumberTable Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b line_number_table_entry structure
 * (anonymous in spec).
 *
 * See section 4.8.10.
 *
 */
typedef struct
{
    u2 start_pc;
    u2 line_number;

} line_number_table_entry;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 line_number_table_length;
    line_number_table_entry line_number_table[1]; /**< Mark space for
                   one, but @b attribute_length will reserve the
                   correct amount of space.  See spec pseudo-code:
                   @c @b line_number_table[line_number_table_length]; */

} LineNumberTable_attribute;

#define CONSTANT_UTF8_LINENUMBERTABLE_ATTRIBUTE      "LineNumberTable"

#define         LOCAL_LINENUMBERTABLE_ATTRIBUTE_ENUM 9 /**< Not in
                                                                 spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.11:  The LocalVariableTable Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b local_variable_table_entry structure
 * (anonymous in spec).
 *
 * See section 4.8.11.
 *
 */
typedef struct
{
    u2 start_pc;
    u2 length;
    u2 name_index;
    u2 descriptor_index;
    u2 index;

} local_variable_table_entry;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 local_variable_table_length;
    local_variable_table_entry local_variable_table[1]; /**< Mark space
             for one, but @b attribute_length will reserve the correct
             amount of space.  See spec pseudo-code:
             @c @b local_variable_table[local_variable_table_length]; */


} LocalVariableTable_attribute;

#define CONSTANT_UTF8_LOCALVARIABLETABLE_ATTRIBUTE  "LocalVariableTable"

#define         LOCAL_LOCALVARIABLETABLE_ATTRIBUTE_ENUM 10 /**< Not in
                                                                 spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.12:  The LocalVariableTypeTable Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b local_variable_type_table_entry structure
 * (anonymous in spec).
 *
 * See section 4.8.12.
 *
 */
typedef struct
{
    u2 start_pc;
    u2 length;
    u2 name_index;
    u2 signature_index;
    u2 index;

} local_variable_type_table_entry;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 local_variable_type_table_length;
    local_variable_type_table_entry
        local_variable_type_table[1]; /**< Mark space for one,
                             but @b local_variable_type_table_length
                             will reserve the correct amount of space.
                             See spec pseudo-code:
        @c @b local_variable_table[local_variable_type_table_length]; */

} LocalVariableTypeTable_attribute;

#define CONSTANT_UTF8_LOCALVARIABLETYPETABLE_ATTRIBUTE \
                                                "LocalVariableTypeTable"

#define         LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE_ENUM 11 /**< Not
                                                              in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.13:  The Deprecated Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;

} Deprecated_attribute;

#define CONSTANT_UTF8_DEPRECATED_ATTRIBUTE        "Deprecated"

#define               DEPRECATED_ATTRIBUTE_LENGTH 0

#define         LOCAL_DEPRECATED_ATTRIBUTE_ENUM   12 /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.8.14:  The RuntimeVisibleAnnotations Attribute
 *
 * Xtodo When implementing @b RuntimeXxxAnnotations attributes,
 * MAKE SURE to understand the implications of
 * @link #ARCH_ODD2_ADDRESS_SIGSEGV ARCH_ODD2_ADDRESS_SIGSEGV@endlink
 * and
 * @link #ARCH_ODD4_ADDRESS_SIGSEGV ARCH_ODD4_ADDRESS_SIGSEGV@endlink
 * upon 2- and 4-byte integer storage accesses, respectively.
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief The class file @b enum_const_value structure
 *
 * See section 4.8.14.1.
 *
 */
typedef struct
{
    jvm_constant_pool_index type_name_index;
    jvm_constant_pool_index const_name_index;

} enum_const_value;


/*! @internal Must use @b @c struct form for forward referencing */
struct element_value_pair_mem_align_struct;
struct element_value_mem_align_struct;

/*!
 * @brief The class file @b annotation structure definition (As
 * represented in memory)
 *
 * @see annotation
 *
 * The annotation_mem_align structure is annotation
 * as represented in memory after being read in from the class file.
 * This is slightly different from annotation so as to overcome
 * 2- and 4-byte memory alignment issues.
 *
 */
typedef struct
{
    u2 type_index;
    u2 num_element_value_pairs;

    struct element_value_pair_mem_align_struct
        *element_value_pairs; /**< Mark space for one,
                 but @b num_element_value_pairs will reserve
                 the correct amount of space.  See spec pseudo-code:
                 @c @b element_value_pairs[num_element_value_pairs] */

} annotation_mem_align;


#define AV_NUM_EMPTIES 2 /**< Size of padding for array_values
                  * structures in size of array_values_mem_align
                  * structure */

/*!
 * @brief The class file @b array_value structure definition.
 *
 * See spec section 4.8.14.1.
 *
 */
typedef struct
{
    rbyte empty[AV_NUM_EMPTIES]; /**< Align @p @b num_values u2 field
                                      against END of 4-byte  word */

    u2     num_values;

    struct element_value_mem_align_struct **pvalues; /**< Spec
                                                          pseudo-code:
                              @c @b element_value values[num_values]; */

} array_values_mem_align;


/*!
 * @brief The class file @b element_values_union structure,
 * anonymous in spec (per spec and class file).
 *
 * See section 4.8.14.1.  The element_values_union union as represented
 * in the specification and in the class file.  This is modified
 * somewhat in memory with element_values_union_mem_align to overcome
 * 2- and 4-byte memory alignment issues.
 *
 * @see element_values_union_mem_align
 *
 * @todo HARMONY-6-jvm-classfile.h-6 on 64-bit architectures, this
 *       union will produce 8-byte pointers.  <b>MAKE SURE</b> that
 *       this size of pointer is @e not larger than the largest of
 *       the other structures so that intensive use of annotations
 *       ends up running off the end of the allocated destination
 *       block when copying the attribute in from the class file
 *       inside of cfattrib_load_annotation() .
 *
 */

typedef union
{
    jvm_constant_pool_index _const_value_index;
    enum_const_value        _enum_const_value;
    jvm_constant_pool_index _class_info_index;
#if 1
    u1                      _annotation_value;
    u1                      _array_value;
    /*!
     * @internal Notice that the source form, when reading the
     *           class file, would use (annotation) and (array_values)
     *           here, but since only a (jbyte *) is reading the buffer,
     *           there is no need of this expression formally set
     *           forth.  This also just so happens to avoid irresolvable
     *           forward/backward reference/definition errors that
     *           resolve for pointer types, but not for the structures
     *           themselves.  (This is why the
     *           element_values_union_mem_align version compiles.)
     */
#else
    annotation              _annotation_value;
    array_values            _array_value;
#endif

} element_values_union;


/*!
 * @brief The class file @b element_values_union structure,
 * anonymous in spec (as represented in memory).
 *
 * @see element_values_union
 *
 * The element_values_union structure is element_value as represented
 * in memory after being read in from the class file.  This is
 * slightly different from element_values_union so as to overcome
 * 2- and 4-byte memory alignment issues.
 */

typedef union
{
    jvm_constant_pool_index  _const_value_index;
    enum_const_value         _enum_const_value;
    jvm_constant_pool_index  _class_info_index;
    annotation_mem_align    *_pannotation_value;
    array_values_mem_align   _array_value;

} element_values_union_mem_align;


/*!
 * @brief The class file @b element_value structure (per spec
 * and class file)
 *
 * Section 4.8.14.1:  The element_value structure as represented
 * in the specification and in the class file.  This is modified
 * somewhat in memory with element_value_mem_align to overcome
 * 2- and 4-byte memory alignment issues.
 *
 * @see element_value_mem_align
 *
 */
typedef struct
{
    u1 tag;

    element_values_union _value;

} element_value;


#define EV_NUM_EMPTIES 3 /**< Size of padding for element_value
                  * structures in size of element_value_mem_align
                  * structure */
/*!
 * @brief The class file @b element_value structure (as represented
 * in memory)
 *
 * @see element_value
 *
 * The element_value_mem_align structure is element_value as represented
 * in memory after being read in from the class file.  This is
 * slightly different from element_value so as to overcome
 * 2- and 4-byte memory alignment issues.
 *
 */
typedef struct element_value_mem_align_struct
{
    rbyte empty[EV_NUM_EMPTIES]; /**< Align @p @b tag u1 byte against
                                      END of 4-byte  word */

    u1 tag;

    element_values_union_mem_align _value;

} element_value_mem_align;


/*!
 * @brief The class file @b element_value_pair structure (per spec
 * and class file)
 *
 * See section 4.8.14.  The @b element_value_pair structure as
 * represented in the specification and in the class file.  This is
 * modified somewhat in memory with element_value_pair_mem_align to
 * overcome 2- and 4-byte memory alignment issues.
 *
 * @see element_value_pair_mem_align
 *
 */
typedef struct
{
    u2            element_value_index;
    element_value value;

} element_value_pair;


/*!
 * @brief The class file @b element_value_pair structure (as represented
 * in memory)
 *
 * @see element_value_pair
 *
 * The element_value_pair_mem_align structure is element_value_pair
 * as represented in memory after being read in from the class file.
 * This is slightly different from element_value_pair so as to overcome
 * 2- and 4-byte memory alignment issues.
 *
 */
typedef struct element_value_pair_mem_align_struct
{
    u2                      element_value_index;
    element_value_mem_align value;

} element_value_pair_mem_align;


/*!
 * @brief The class file @b annotation structure definition (per spec
 * and class file)
 *
 * See spec section 4.8.14.  The @b annotation structure as
 * represented in the specification and in the class file.  This is
 * modified somewhat in memory with annotation_mem_align to overcome
 * 2- and 4-byte memory alignment issues.
 *
 * @see annotation_mem_align
 *
 */
typedef struct annotation_struct
{
    u2 type_index;
    u2 num_element_value_pairs;

    /* @todo HARMONY-6-jvm-classfile.h-7 Make sure there are not any
     * @link ARCH_ODD2_ADDRESS_SIGSEGV ARCH_ODD2_ADDRESS_SIGSEGV@endlink
     * or any
     * @link ARCH_ODD4_ADDRESS_SIGSEGV ARCH_ODD4_ADDRESS_SIGSEGV@endlink
     * boundary problems with an array like this.  It is very likely
     * that element_value_pair does @e not have an odd number of bytes,
     * but for these other issues, it remains to be seen if there are
     * runtime problems with a given CPU architecture.
     */
    element_value_pair element_value_pairs[1]; /**< Mark space for one,
                 but @b num_element_value_pairs will reserve the correct
                 amount of space.  See spec pseudo-code:
                 @c @b element_value_pairs[num_element_value_pairs] */

} annotation;


/*!
 * @brief The class file @b array_value structure definition (per
 * spec and class file)
 *
 * See spec section 4.8.14.1.
 *
 * @see array_values_mem_align
 */
typedef struct array_values_struct
{
    u2            num_values;

    element_value values[1]; /**< Mark space for one, but
                 @b num_values will reserve the correct
                 amount of space.  See spec pseudo-code:
                 @c @b element_value values[num_values]; */

} array_values;


typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 num_annotations;
    annotation_mem_align **annotations; /**< Spec pseudo-code:
                       @c @b annotation annotations[num_annotations];
                       Notice that the source form, when reading the
                       class file, would use (annotation **) instead
                       here, but only a (jbyte *) is reading the buffer,
                       so there is no need of this expression formally
                       set forth. */

} RuntimeVisibleAnnotations_attribute;

#define CONSTANT_UTF8_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE \
                                             "RuntimeVisibleAnnotations"

#define         LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_ENUM 13 /**<
                                                          Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @brief The @b RuntimeInvisibleAnnotations Attribute
 *
 * See spec section 4.8.15: The @b RuntimeInvisibleAnnotations Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 num_annotations;
    annotation_mem_align **annotations; /**< Spec pseudo-code:
                       @c @b annotations[num_annotations];
                       Notice that the source form, when reading the
                       class file, would use (annotation **) instead
                       here, but only a (jbyte *) is reading the buffer,
                       so there is no need of this expression formally
                       set forth. */

} RuntimeInvisibleAnnotations_attribute;

#define CONSTANT_UTF8_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE \
                                         "RuntimeInvisibleAnnotations"

#define         LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_ENUM 14 /**<
                                                          Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @brief The @b RuntimeVisibleParameterAnnotations Attribute
 *
 * See spec section 4.8.16:  The @b RuntimeVisibleParameterAnnotations
 * Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 num_annotations;
    annotation_mem_align **annotations; /**< Spec pseudo-code:
                       @c @b annotations[num_annotations];
                       Notice that the source form, when reading the
                       class file, would use (annotation **) instead
                       here, but only a (jbyte *) is reading the buffer,
                       so there is no need of this expression formally
                       set forth. */

} parameter_annotation;

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 num_parameters;
    parameter_annotation **parameter_annotations; /**< Spec pseudo-code:
                         @c @b parameter_annotations[num_parameters]; */

} RuntimeVisibleParameterAnnotations_attribute;

#define CONSTANT_UTF8_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE \
                                    "RuntimeVisibleParameterAnnotations"

#define       LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM 15
                                                     /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @brief The @b RuntimeInvisibleParameterAnnotations Attribute
 *
 * See spec section 4.8.17: The @b RuntimeInvisibleParameterAnnotations
 * Attribute
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2 attribute_name_index;
    u4 attribute_length;
    u2 num_parameters;
    parameter_annotation **parameter_annotations; /**< Spec pseudo-code:
                         @c @b parameter_annotations[num_parameters]; */

} RuntimeInvisibleParameterAnnotations_attribute;

#define CONSTANT_UTF8_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE \
                                "RuntimeInvisibleParameterAnnotations"

#define     LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM 16
                                                     /**< Not in spec */

/*@} */ /* End of grouped definitions */


/*!
 * @brief The class file @b AnnotationDefault Attribute structure
 * definition (per spec and class file)
 *
 * See spec section 4.8.18: The @b AnnotationDefault Attribute.
 * This structure as represented in the specification and in
 * the class file.  This is modified somewhat in memory with
 * AnnotationDefault_attribute_mem_align to overcome
 * 2- and 4-byte memory alignment issues.
 *
 * @see AnnotationDefault_attribute_mem_align
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef struct
{
    u2            attribute_name_index;
    u4            attribute_length;
    u2            num_parameters;
    element_value default_value;

} AnnotationDefault_attribute;

#define CONSTANT_UTF8_ANNOTATIONDEFAULT_ATTRIBUTE    "AnnotationDefault"

#define         LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE_ENUM 17 /**< Not in
                                                                 spec */

/*@} */ /* End of grouped definitions */


/*!
 * @brief The class file @b AnnotationDefault Attribute structure
 * definition (as represented in memory)
 *
 * @see AnnotationDefault_attribute
 *
 * The AnnotationDefault_attribute_mem_align structure is a default
 * annotation attribute as represented in memory after being read
 * in from the class file.  This is slightly different from 
 * AnnotationDefault so as to overcome 2- and 4-byte memory
 * alignment issues.
 *
 */

typedef struct
{
    u2                      attribute_name_index;
    u4                      attribute_length;
    u2                      num_parameters;
    element_value_mem_align default_value;

} AnnotationDefault_attribute_mem_align;



/*!
 * @name Table 4.8:  Additional tag values for annotation attributes
 *
 */

/*@{ */ /* Begin grouped definitions */

#define BASETYPE_CHAR_s    's'   /**< String */
#define BASETYPE_CHAR_e    'e'   /**< enum constant */
#define BASETYPE_CHAR_c    'c'   /**< Class */
#define BASETYPE_CHAR_AT   '@'   /**< Annotation type */
/*      BASETYPE_CHAR_ARRAY         Array (see also table 4.2) */

#define BASETYPE_STRING_CHAR_ARRAY "[C" /**< @c @b java.lang.String
                                          (jchar)[] @p @b value field */

/*@} */ /* End of grouped definitions */


/*!
 * @name Section 4.9:  Format Checking
 *
 */

/*!
 * @name Section 4.10:  Constraints on Java Virtual Machine Code
 *
 */

/*!
 * @name Section 4.10.1:  Static Constraints
 *
 */

/*@{ */ /* Begin grouped definitions */

#define CODE_CONSTRAINT_CODE_LENGTH_MIN 1 /**< Array size bounds,bytes*/
#define CODE_CONSTRAINT_CODE_LENGTH_MAX 0xffff /**< Maximum PC value */

#define CODE_CONSTRAINT_START_PC        0   /**< Start PC location */

#define LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR "<clinit>"
                                          /**< Not in spec,but implied*/
#define LOCAL_CONSTANT_UTF8_CLASS_CONSTRUCTOR_PARMS "()V"

#define CODE_CONSTRAINT_OP_INVOKEINTERFACE_PARM4 0
#define CODE_CONSTRAINT_OP_ANEWARRAY_MAX_DIMS  CONSTANT_MAX_ARRAY_DIMS

#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_BOOLEAN 4
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_CHAR    5
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_FLOAT   6
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_DOUBLE  7
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_BYTE    8
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_SHORT   9
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_INT     10
#define CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_LONG    11

/*@} */ /* End of grouped definitions */


/*!
 * @brief Section 4.10.2:  Structural Constraints
 */


/*!
 * @brief Section 4.11:  Verification of @b class Files
 */

/*!
 * @brief Section 4.11.1:  Verification by Type Inference
 */


/*!
 * @brief Section 4.11.1.1:  The Process of Verification by Type
 * Inference
 *
 */

/*!
 * @brief Section 4.11.1.2:  The Bytecode Verifier
 *
 */

/*!
 * @brief Section 4.11.1.3:  Values of Types @c @b long and @c @b double
 *
 */

/*!
 * @brief Section 4.11.1.4:  Instance Initialization Methods and Newly
 *                           Created Objects
 *
 */

/*!
 * @brief Section 4.11.1.5:  Exception Handlers
 *
 */

/*!
 * @brief Section 4.9.6:  Exceptions and @c @b finally -- Removed from
 *                        this JVM 1.5 edition, possibly from 1.4.
 *
 */

/*!
 * @brief Section 4.12:  Limitations of the Java Virtual Machine
 *
 */
 

/****************************************************************/

/*!
 * @brief Attribute enumeration
 *
 * Enumeration of all ClassFile attribute types.  Gather the
 * @link #LOCAL_CONSTANTVALUE_ATTRIBUTE_ENUM
   LOCAL_xxx_ATTRIBUTE_ENUM@endlink values from all over the
 * file into one place for use in compiling a single enumeration
 * type.  These @link #LOCAL_CONSTANTVALUE_ATTRIBUTE_ENUM
   LOCAL_xxx_ATTRIBUTE_ENUM@endlink values were defined in the
 * immediate proximity to the attribute they help describe, but
 * this is difficult for creating an enumeration type.  Here near
 * the end of the file, each one is defined, so the enumeration
 * type can be created.  Once created, the @b xxx_ENUM version is
 * undefined.  Thus the @b xxx_ENUM is known only locally in this
 * file, while the @b xxx version is known as part of the enumeration
 * type.
 *
 */
typedef enum
{
    LOCAL_UNKNOWN_ATTRIBUTE =
        LOCAL_UNKNOWN_ATTRIBUTE_ENUM,

    LOCAL_CONSTANTVALUE_ATTRIBUTE =
        LOCAL_CONSTANTVALUE_ATTRIBUTE_ENUM,

    LOCAL_CODE_ATTRIBUTE =
        LOCAL_CODE_ATTRIBUTE_ENUM,

    LOCAL_EXCEPTIONS_ATTRIBUTE =
        LOCAL_EXCEPTIONS_ATTRIBUTE_ENUM,

    LOCAL_INNERCLASSES_ATTRIBUTE =
        LOCAL_INNERCLASSES_ATTRIBUTE_ENUM,

    LOCAL_ENCLOSINGMETHOD_ATTRIBUTE =
        LOCAL_ENCLOSINGMETHOD_ATTRIBUTE_ENUM,

    LOCAL_SYNTHETIC_ATTRIBUTE =
        LOCAL_SYNTHETIC_ATTRIBUTE_ENUM,

    LOCAL_SIGNATURE_ATTRIBUTE =
        LOCAL_SIGNATURE_ATTRIBUTE_ENUM,

    LOCAL_SOURCEFILE_ATTRIBUTE =
        LOCAL_SOURCEFILE_ATTRIBUTE_ENUM,

    LOCAL_LINENUMBERTABLE_ATTRIBUTE =
        LOCAL_LINENUMBERTABLE_ATTRIBUTE_ENUM,

    LOCAL_LOCALVARIABLETABLE_ATTRIBUTE =
        LOCAL_LOCALVARIABLETABLE_ATTRIBUTE_ENUM,

    LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE =
        LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE_ENUM,

    LOCAL_DEPRECATED_ATTRIBUTE =
        LOCAL_DEPRECATED_ATTRIBUTE_ENUM,

    LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE =
        LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_ENUM,

    LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE =
        LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_ENUM,

    LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE =
        LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM,

    LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE =
        LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM,

    LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE =
        LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE_ENUM

} classfile_attribute_enum;

/* Finally remove temporary definitions */
#undef LOCAL_UNKNOWN_ATTRIBUTE_ENUM
#undef LOCAL_CONSTANTVALUE_ATTRIBUTE_ENUM
#undef LOCAL_CODE_ATTRIBUTE_ENUM
#undef LOCAL_EXCEPTIONS_ATTRIBUTE_ENUM
#undef LOCAL_INNERCLASSES_ATTRIBUTE_ENUM
#undef LOCAL_ENCLOSINGMETHOD_ATTRIBUTE_ENUM
#undef LOCAL_SYNTHETIC_ATTRIBUTE_ENUM
#undef LOCAL_SIGNATURE_ATTRIBUTE_ENUM
#undef LOCAL_SOURCEFILE_ATTRIBUTE_ENUM
#undef LOCAL_LINENUMBERTABLE_ATTRIBUTE_ENUM
#undef LOCAL_LOCALVARIABLETABLE_ATTRIBUTE_ENUM
#undef LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE_ENUM
#undef LOCAL_DEPRECATED_ATTRIBUTE_ENUM
#undef LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_ENUM
#undef LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_ENUM
#undef LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM
#undef LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_ENUM
#undef LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE_ENUM

 

/* Prototypes for functions in 'classfile.c' */

extern ClassFile *classfile_allocate_primative(jvm_basetype basetype);
extern ClassFile *classfile_load_classdata(u1     *classfile_image);
extern rvoid classfile_unload_classdata(ClassFile *pcfs);
extern u1 *classfile_read_classfile(rchar *filename);
extern u1 *classfile_read_jarfile(rchar *filename);

/* Prototypes for functions in 'cfattrib.c' */
extern u1 *cfattrib_load_elementvalue(ClassFile              *pcfs,
                                      element_value_mem_align *dst,
                                      element_value           *src);
extern void cfattrib_unload_elementvalue(ClassFile               *pcfs,
                                         element_value_mem_align *dst);

extern u1 *cfattrib_load_annotation(ClassFile            *pcfs,
                                    annotation_mem_align *dst,
                                    annotation           *src);
extern void cfattrib_unload_annotation(ClassFile            *pcfs,
                                       annotation_mem_align *dst);

extern u1 *cfattrib_load_attribute(ClassFile           *pcfs,
                                   attribute_info_dup **dst,
                                   attribute_info      *src);
extern rvoid cfattrib_unload_attribute(ClassFile          *pcfs,
                                       attribute_info_dup *dst);

extern classfile_attribute_enum cfattrib_atr2enum(ClassFile *pcfs,
                              u2 attribute_attribute_name_index);

extern rboolean cfattrib_iscodeattribute(ClassFile *pcfs,
                                     u2 attribute_attribute_name_index);

/* Prototypes for functions in 'cfmsgs.c' */
extern rvoid cfmsgs_typemsg(rchar *fn, ClassFile *pcfs, u2 idx);
extern rvoid cfmsgs_show_constant_pool(ClassFile *pcfs);
extern rvoid cfmsgs_atrmsg(rchar *fn,
                           ClassFile *pcfs,
                           attribute_info_dup *atr);

/*!
 * @internal Remove effects of packing pragma on other code.
 *
 */
#pragma pack()

#endif /* _classfile_h_included_ */

/* EOF */
