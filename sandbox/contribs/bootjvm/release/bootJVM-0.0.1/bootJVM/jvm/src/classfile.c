/*!
 * @file classfile.c
 *
 * @brief Implementation of <em>The Java Virtual Machine Specification,
 * version 2 Chapter 4, The Class File Format</em>.
 *
 * Manipulation of attributes is performed in
 * @link jvm/src/cfattrib.c cfattrib.c@endlink.
 * All other work is done here.
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
 * @todo  HARMONY-6-jvm-classfile.c-1 Per spec section 5.4.1, need to
 *        verify the contents of the file read in before initializing
 *        the class, else throw @b VerifyError.
 *
 * @todo HARMONY-6-jvm-classfile.c-2 Need to verify which web document
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

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(classfile, c,
"$URL$",
"$Id$");


#include <fcntl.h>
/* #include <stdlib.h> */
/* #include <string.h> */
/* #include <unistd.h> */
/* #include <sys/stat.h> */

#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "classpath.h"
#include "exit.h"
#include "jvm.h"
#include "native.h"
#include "util.h"


/*!
 * @brief Build up empty JVM class file structure for use
 * by Java primative data types.
 *
 * Each primative data type needs a minimal class definition
 * to avoid having special cases for class processing logic.
 * Also, treating primatives like classes can provide convenient
 * information on data size types, like @b (jlong) versus @b (jint).
 * This can be provided by the base type information, which is
 * passed in here when creating these minimal classes.
 *
 * The constant pool for such a minima class looks like this:
 *
 * <ul>
 * <li><b>CP[0]</b>:  Not represented, per spec.  Implies
 *                    @c @b java.lang.Object .
 * </li>
 * <li><b>CP[1]</b>:  CONSTANT_Utf8_info string of one character, the
 *                    @link #BASETYPE_CHAR_B basetype@endlink letter.
 * </li>
 * <li><b>CP[2]</b>:  CONSTANT_Class_info class definition of this
 *                    primative, with @link
                      CONSTANT_Class_info#name_index name_index@endlink
 *                    pointing to UTF8 string at index 1.
 * </li>
 * </ul>
 *
 * @todo  HARMONY-6-jvm-classfile.c-3 Need to take a hard look at the
 *        requirements for @c @b java.lang.Class and see if this is
 *        sufficient or even accurately implemented.
 *
 *
 * @param  basetype   One of the primative base type
 *                    @link #BASETYPE_CHAR_B BASETYPE_CHAR_x@endlink
 *
 *
 * @returns (ClassFile *) to heap-allocated area, used throughtout life
 *          of JVM, then released.
 *
 */
ClassFile *classfile_allocate_primative(jvm_basetype basetype)
{
    ARCH_FUNCTION_NAME(classfile_allocate_primative);

    /*!
     * @internal INITIALIZE TO ZEROES all fields so there are
     *           automatic @link #rnull rnull@endlink pointers in case
     *           of failure along the way.
     */
    ClassFile *pcfs = HEAP_GET_DATA(sizeof(ClassFile), rtrue);


    pcfs->magic = CLASSFILE_MAGIC;      /* "This is a Java class" */
                                        /* Look like -r1.2 source file*/
    pcfs->major_version = VERSION_MAJOR_JDK2;
    pcfs->minor_version = VERSION_MINOR_DEFAULT;

    pcfs->constant_pool_count = 3; /* 1 class, 1 string, + j/l/Object */
    pcfs->constant_pool = HEAP_GET_DATA(pcfs->constant_pool_count *
                                        sizeof(cp_info_dup *), rfalse);


    /*!
     * @internal Since @c @b java.lang.Object is implied,
     *           don't need this slot.
     */
    pcfs->constant_pool[0] = (cp_info_dup *) rnull;


    /* Allocate CONSTANT_Utf8_info member.  Default of u1[1] okay */
    pcfs->constant_pool[1] = HEAP_GET_DATA(sizeof(cp_info_dup) +
                                           sizeof(CONSTANT_Utf8_info) -
                                           sizeof(cp_info), rtrue);
    CONSTANT_Utf8_info *putf =PTR_THIS_CP_Utf8(pcfs->constant_pool[1]);
    putf->tag = CONSTANT_Utf8;
    putf->length = sizeof(u1); /* Primatives have a single char name */
    putf->bytes[0] = basetype; /* single (u1) character */

    /* Allocate CONSTANT_Class_info member */
    pcfs->constant_pool[2] = HEAP_GET_DATA(sizeof(cp_info_dup) +
                                           sizeof(CONSTANT_Class_info) -
                                           sizeof(cp_info), rfalse);
    CONSTANT_Class_info *pci =PTR_THIS_CP_Class(pcfs->constant_pool[2]);
    pci->tag = CONSTANT_Class;
    pci->name_index = 1; /* Index of Utf8 string of primative desc. */


    pcfs->access_flags = ACC_SYNTHETIC; /*!
                                         *  @todo
                                         *  HARMONY-6-jvm-classfile.c-4
                                         *  Needs more thought.
                                         */
    pcfs->this_class = 2;   /* Index to class entry */

                            /*!
                             * @todo  HARMONY-6-jvm-classfile.c-5 Is
                             *         this assumption valid/meaningful?
                             */
    pcfs->super_class = 0;  /* No superclass, imply java.lang.Object */

    pcfs->interfaces_count = 0;
    pcfs->interfaces = (u2 *) rnull;

    pcfs->fields_count = 0;
    pcfs->fields = (field_info **) rnull;

    pcfs->methods_count = 0;
    pcfs->methods = (method_info **) rnull;

    pcfs->attributes_count = 0;
    pcfs->attributes = (attribute_info_dup **) rnull;

    return(pcfs);

} /* END of classfile_allocate_primative() */


/*!
 * @def ALLOC_CP_INFO()
 *
 * @brief Allocate a cp_info_dup structure containing any generic
 * type of @c @b constant_pool entry.
 *
 * Allocate space from heap, populate from class file data,
 * and fill in initial pad bytes.
 *
 *
 * @param spec_typedef   Structure @link #CONSTANT_Class_info
                         CONSTANT_xxx_info@endlink type definition
 *                       of one of the @c @b constant_pool types.
 *
 * @param binding_struct Name of local binding structure @link
                         #CONSTANT_Class_info.LOCAL_Class_binding
                         LOCAL_xxx_binding@endlink associated with
 *                       @b spec_typedef
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

#define ALLOC_CP_INFO(spec_typedef, binding_struct)                    \
    misc_adj = sizeof(u1) * CP_INFO_NUM_EMPTIES;                       \
                                                                       \
    cf_item_size = sizeof(spec_typedef)-sizeof(struct binding_struct); \
    pcpd = HEAP_GET_METHOD(misc_adj + sizeof(spec_typedef), rfalse);   \
    portable_memcpy(((rbyte *) pcpd) + misc_adj,pcpbytes,cf_item_size);\
                                                                       \
    pcpd->empty[0] = FILL_INFO_DUP0;                                   \
    pcpd->empty[1] = FILL_INFO_DUP1;                                   \
    pcpd->empty[2] = FILL_INFO_DUP2; /* Extra ; */


/*!
 * @def ALLOC_CF_ITEM()
 *
 * @brief Allocate either a field_info or method_info structure.
 *
 * Allocate space from heap and  populate from class file data,
 *
 *
 * @param spec_typedef   Structure field_info or method_info type
 *                       definition.
 *
 * @param pbytes         Name <b><code>(jbyte *)</code></b> pointer into
 *                       class file data being parsed pointing to
 *                       where this structure is found.

 * @param pcfsi          <b><code>(spec_typedef *)</code></b> array[]
 *                       pointer to the field_info or method_info table
 *                       slot where the parsed data will be stored.
 *
 * @param binding_struct Structure LOCAL_field_binding or
 *                       LOCAL_method_binding for appropriate
 *                       @b spec_typedef type definition.
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

#define ALLOC_CF_ITEM(spec_typedef, pbytes, pcfsi, binding_struct)     \
    cf_item_size = sizeof(spec_typedef) -                              \
                   sizeof(struct binding_struct) -                     \
                   sizeof(attribute_info_dup **);                      \
    pcfsi = HEAP_GET_METHOD(sizeof(spec_typedef), rfalse);             \
    portable_memcpy(((rbyte *) pcfsi),pbytes,cf_item_size);/*Extra ;*/

/*!
 * @name Range check of @c @b constant_pool indices.
 *
 * @brief Range check @c @b constant_pool @c @b cpidx against max index,
 * with/without typed pointer.
 *
 * (Testing <b><code>cpidx < 0</code></b> is not checked,
 * since @p @b cpidx is an unsigned integer.)
 * 
 *
 * @param type  @link #CONSTANT_Class_info CONSTANT_xxx_info@endlink
 *              pointer of data type to be checked
 *
 * @param pcfs  ClassFile area to be checked
 *
 * @param cpidx @c @b constant_pool index to be checked
 *
 * @param msg   Error message to display at failure
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Range check a typed pointer against the
 * @link #ClassFile.constant_pool_count constant_pool_count@endlink
 * value
 */
#define CPTYPEIDX_RANGE_CHECK(type, pcfs, cpidx, msg)                  \
    LOAD_SYSCALL_FAILURE(( /* ((((type *) &pcpd->cp)->cpidx) < 0) || */\
                          ((((type *) &pcpd->cp)->cpidx) >=            \
                                          pcfs->constant_pool_count)), \
                         msg,                                          \
                         rnull,                                        \
                         rnull); /* Extra ; */   

/*!
 * @brief Range check a simple index against the
 * @link #ClassFile.constant_pool_count constant_pool_count@endlink
 * value
 */
#define CPIDX_RANGE_CHECK(pcfs, cpidx, msg)                         \
    LOAD_SYSCALL_FAILURE((/* (cpidx < 0) || */                      \
                             (cpidx >= pcfs->constant_pool_count)), \
                         msg,                                       \
                         rnull,                                     \
                         rnull); /* Extra ; */

/*@} */ /* End of grouped definitions */

/*!
 * @brief Parse an in-memory JVM class file, create structures to point
 * to various parts of it, per JVM spec pseuco-code structures.
 *
 * Interpret the class file data and load up the data structures
 * which access it, such as the fully qualified class names,
 * the code area, etc.
 *
 * @todo HARMONY-6-jvm-classfile.c-6 Need a @e much better way to free
 *       partially built class structure when an error occurs.  The
 *       current scheme is only piecemeal and @e will leave orphaned
 *       memory blocks lying around when something is freed that has
 *       @e the pointer to it, case in point, the @c @b constant_pool[]
 *       table when freeing the main ClassFile structure block on error.
 *       The partial solutions of adding heap pointer parameters to
 *       LOAD_SYSCALL_FAILURE() and GENERIC_FAILURExxx() macros
 *       is not useful here since so @e many allocations are done
 *       in this function.
 *
 * @param  pclassfile_image  Null-terminated ASCII string,
 *                           pathname of file.
 *
 *
 * @returns Complete class file structure, fully loaded with the
 *          real instantiation of the JVM spec pseudo-code structures.
 *          If error detected, @link #rnull rnull@endlink is returned
 *          and perror("msg") may be called to report the system call
 *          problem that caused the particular failure more.  Status
 *          of heap areas will be undefined since a failure like this
 *          should be fatal to program execution.
 *
 */

ClassFile *classfile_loadclassdata(u1       *pclassfile_image)
{
    ARCH_FUNCTION_NAME(classfile_loadclassdata);

    rint misc_adj; /* For ALLOC_xxx macros */


    u2 tmplenutf;

    /* Needed for CP_ITEM_SWAP_Ux() macros */
#ifdef ARCH_LITTLE_ENDIAN
    u2    *pcpu2;
    u4    *pcpu4;
#endif

    /* Generic data pointers, loaded via MAKE_Ux() macros above */
    u2    *pu2;
    u4    *pu4;

    /*
     * First things first:  Need to sequentially access class file data
     * (Data SOURCE into which *pcfs structure pointers will point)
     */
    u1 *pcfd = (u1 *) pclassfile_image;

    /*
     * Also need to access class file structures
     * (Data INDEX area that holds pointers to class file data in *pcfd)
     */
    ClassFile *pcfs = (ClassFile *) HEAP_GET_METHOD(sizeof(ClassFile),
                                                    rtrue);

    /*
     * Need default value until field is loaded.  Set default
     * to "no class".  Spec also says that this @c @b constant_pool
     * index implies <code>java.lang.Object<code>.
     */
    pcfs->this_class = jvm_class_index_null;

    /*****************************************************************/
    /* Get magic number-- an easy exercise in using generic pointers */
    /*****************************************************************/
    MAKE_PU4(pu4, pcfd);     /* Allocate next class file data pointer */
                             /* Byte swap 2 & 4 byte areas */

    /*
     * Copy into class file structure area.
     *
     * ALWAYS use GETRxx() macros for multi-byte accesses
     * in *pclassfile_image since they are NEVER guaranteed
     * to be 2-byte aligned! (See getrs2() in
     * @link jvm/src/bytegames.c bytegames.c@endlink
     * and ARCH_ODD_ADDRESS_SIGSEGV in
     * @link jvm/src/arch.h arch.h@endlink
     * for details.)
     */
    pcfs->magic = GETRI4(pu4);

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "magic=%08x",
              pcfs->magic);

    /*
     * Perform integrity checking as needed for each data type.
     */
    LOAD_SYSCALL_FAILURE((CLASSFILE_MAGIC != pcfs->magic),
                         "magic",
                         rnull,
                         rnull);

    /* calculate offset to next field in file data */
    pu4++;


    /*****************************************************************/
    /* Get minor class file version                                  */
    /*****************************************************************/
    MAKE_PU2(pu2, pu4);

    pcfs->minor_version = GETRS2(pu2);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-7
     *       LOAD_SYSCALL_FAILURE( what needs checking here?, "minor");
     */

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "minor=%d",
              pcfs->minor_version);

    pu2++;                   /* calc offset to next field in file data*/


    /*****************************************************************/
    /* Get major class file version                                  */
    /*****************************************************************/

    /* Can optimize along the way-- pu2 already points here */
    /* MAKE_PU2(pu2, pu2); */

    pcfs->major_version = GETRS2(pu2++);

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "major=%d",
              pcfs->major_version);

    /* Range check against spec (footnote to field definition in spec)*/
    LOAD_SYSCALL_FAILURE(((pcfs->major_version < VERSION_MAJOR_LOW) ||
                          (pcfs->major_version > VERSION_MAJOR_HIGH)),
                         "major",
                         rnull,
                         rnull);

    LOAD_SYSCALL_FAILURE(((pcfs->major_version == VERSION_MAJOR_LOW)
                          /* &&
                          (pcfs->minor_version <  VERSION_MINOR_LOW) */
                         ),
                         "low minor",
                         rnull,
                         rnull);

    LOAD_SYSCALL_FAILURE(((pcfs->major_version == VERSION_MAJOR_HIGH) &&
                          (pcfs->minor_version >  VERSION_MINOR_HIGH)),
                         "high minor",
                         rnull,
                         rnull);


    /*!
     * @todo  HARMONY-6-jvm-classfile.c-8
     *         Throw @b UnsupportedClassVersionError for bad versions
     */

    /*****************************************************************/
    /* Get constant_pool_count                                       */
    /*****************************************************************/
    pcfs->constant_pool_count = GETRS2(pu2++);

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "cp count=%d",
              pcfs->constant_pool_count);

    /* In reality a NOP, but theoretically possible for empty file */
 /* LOAD_SYSCALL_FAILURE((0 > pcfs->constant_pool_count), "CP index");*/


    /*****************************************************************/
    /* Create constant pool index, then load up constant_pool        */
    /*****************************************************************/

    jvm_constant_pool_index cpidx;
    rint    cf_item_size;
    jbyte  *pcpbytes = (jbyte *) pu2;

    /*
     * Map the indices in the class file to point to actual
     * constant pool enties via a pointer lookup table.
     *
     * Make @c @b constant_pool[] large enough for 0th element as well
     * as defined element (the @c @b java.lang.Object per spec).
     * This is simply a convenience for future access without doing
     * an <b><code>x - 1</code></b> calculation.  Therefore, fill
     * in 0th element with @link #rnull rnull@endlink info as it
     * should never be used.  Any access SHOULD produce @b SIGSEGV
     * because the proper class info should be accessed instead.
     */
     pcfs->constant_pool = HEAP_GET_METHOD(pcfs->constant_pool_count *
                                               sizeof(cp_info_dup *),
                                           rfalse);

    /*
     * Dummy entry for @c @b java.lang.Object constant pool
     * references (0th element)
     */
    pcfs->constant_pool[CONSTANT_CP_DEFAULT_INDEX] =
        (cp_info_dup *) rnull;

    /*
     * Iterate through class file's @c @b constant_pool and fill in
     * the @c @b constant_pool[] pointer array for normal use.
     */
    for (cpidx = CONSTANT_CP_START_INDEX;
         cpidx < pcfs->constant_pool_count + CONSTANT_CP_START_INDEX -1;
         cpidx++)
    {
        cp_info_dup *pcpd;

        pu2 = (u2 *) pcpbytes;

        /*
         * Look up structure size, perform in-place byte swap
         * for little-endian architectures.
         */

        switch (((cp_info *) pcpbytes)->tag)
        {
            case CONSTANT_Class:
                ALLOC_CP_INFO(CONSTANT_Class_info,
                              LOCAL_Class_binding);

                CP_ITEM_SWAP_U2(CONSTANT_Class_info, name_index);

                CPTYPEIDX_RANGE_CHECK(CONSTANT_Class_info,
                                      pcfs,
                                      name_index,
                                      "CP name index");

                /* Initialize late binding extension */
                PTR_THIS_CP_Class(pcpd)->LOCAL_Class_binding.clsidxJVM =
                                                   jvm_class_index_null;

                break;

            case CONSTANT_Fieldref:
                ALLOC_CP_INFO(CONSTANT_Fieldref_info,
                              LOCAL_Fieldref_binding);

                CP_ITEM_SWAP_U2(CONSTANT_Fieldref_info,class_index);

                CP_ITEM_SWAP_U2(CONSTANT_Fieldref_info,
                                name_and_type_index);

                CPTYPEIDX_RANGE_CHECK(CONSTANT_Fieldref_info,
                                      pcfs,
                                      class_index,
                                      "CP class index");

                /* Initialize late binding extension */
                PTR_THIS_CP_Fieldref(pcpd)
                    ->LOCAL_Fieldref_binding.clsidxJVM =
                                                   jvm_class_index_null;

                PTR_THIS_CP_Fieldref(pcpd)
                    ->LOCAL_Fieldref_binding.fluidxJVM =
                                                    jvm_field_index_bad;

                PTR_THIS_CP_Fieldref(pcpd)
                    ->LOCAL_Fieldref_binding.oiflagJVM =
                                                rneither_true_nor_false;

                PTR_THIS_CP_Fieldref(pcpd)
                    ->LOCAL_Fieldref_binding.jvaluetypeJVM =
                                                   LOCAL_BASETYPE_ERROR;

                break;

            case CONSTANT_Methodref:
                ALLOC_CP_INFO(CONSTANT_Methodref_info,
                              LOCAL_Methodref_binding);

                CP_ITEM_SWAP_U2(CONSTANT_Methodref_info, class_index);

                CP_ITEM_SWAP_U2(CONSTANT_Methodref_info,
                                name_and_type_index);

                CPTYPEIDX_RANGE_CHECK(CONSTANT_Methodref_info,
                                      pcfs,
                                      class_index,
                                      "CP method class index");

                CPTYPEIDX_RANGE_CHECK(CONSTANT_Methodref_info,
                                      pcfs,
                                      name_and_type_index,
                                      "CP method name and type index");

                /* Initialize late binding extension */
                PTR_THIS_CP_Methodref(pcpd)
                    ->LOCAL_Methodref_binding.clsidxJVM =
                                                   jvm_class_index_null;

                PTR_THIS_CP_Methodref(pcpd)
                    ->LOCAL_Methodref_binding.mthidxJVM =
                                                   jvm_method_index_bad;

                PTR_THIS_CP_Methodref(pcpd)
                    ->LOCAL_Methodref_binding.codeatridxJVM =
                                                jvm_attribute_index_bad;

                PTR_THIS_CP_Methodref(pcpd)
                    ->LOCAL_Methodref_binding.excpatridxJVM =
                                                jvm_attribute_index_bad;

                PTR_THIS_CP_Methodref(pcpd)
                    ->LOCAL_Methodref_binding.nmordJVM =
                                         jvm_native_method_ordinal_null;

                break;

            case CONSTANT_InterfaceMethodref:
                ALLOC_CP_INFO(CONSTANT_InterfaceMethodref_info,
                              LOCAL_InterfaceMethodref_binding);

                CP_ITEM_SWAP_U2(CONSTANT_InterfaceMethodref_info,
                                class_index);

                CP_ITEM_SWAP_U2(CONSTANT_InterfaceMethodref_info,
                                name_and_type_index);

                CPTYPEIDX_RANGE_CHECK(CONSTANT_InterfaceMethodref_info,
                                      pcfs,
                                      class_index,
                                     "CP interface method class index");

                CPTYPEIDX_RANGE_CHECK(CONSTANT_InterfaceMethodref_info,
                                      pcfs,
                                      name_and_type_index,
                             "CP interface method name and type index");

                /* Initialize late binding extension */
                PTR_THIS_CP_InterfaceMethodref(pcpd)
                    ->LOCAL_InterfaceMethodref_binding.clsidxJVM =
                                                   jvm_class_index_null;

                PTR_THIS_CP_InterfaceMethodref(pcpd)
                    ->LOCAL_InterfaceMethodref_binding.mthidxJVM =
                                                   jvm_method_index_bad;

                PTR_THIS_CP_InterfaceMethodref(pcpd)
                    ->LOCAL_InterfaceMethodref_binding.codeatridxJVM =
                                                jvm_attribute_index_bad;

                PTR_THIS_CP_InterfaceMethodref(pcpd)
                    ->LOCAL_InterfaceMethodref_binding.excpatridxJVM =
                                                jvm_attribute_index_bad;

                PTR_THIS_CP_InterfaceMethodref(pcpd)
                    ->LOCAL_InterfaceMethodref_binding.nmordJVM =
                                         jvm_native_method_ordinal_null;

                break;

            case CONSTANT_String:
                ALLOC_CP_INFO(CONSTANT_String_info,
                              LOCAL_String_binding);

                CP_ITEM_SWAP_U2(CONSTANT_String_info, string_index);

                CPTYPEIDX_RANGE_CHECK(CONSTANT_String_info,
                                      pcfs,
                                      string_index,
                                      "CP string index");
                break;

            case CONSTANT_Integer:
                ALLOC_CP_INFO(CONSTANT_Integer_info,
                              LOCAL_Integer_binding);

                CP_ITEM_SWAP_U4(CONSTANT_Integer_info, bytes);

                break;

            case CONSTANT_Float:
                ALLOC_CP_INFO(CONSTANT_Float_info,
                              LOCAL_Float_binding);

                CP_ITEM_SWAP_U4(CONSTANT_Float_info, bytes);

                break;

            case CONSTANT_Long:
                ALLOC_CP_INFO(CONSTANT_Long_info,
                              LOCAL_Long_binding);

                CP_ITEM_SWAP_U4(CONSTANT_Long_info, high_bytes);

                CP_ITEM_SWAP_U4(CONSTANT_Long_info, low_bytes);

                /*
                 * Make both slots point to the same place-- see
                 * assignment below for value at [cpidx].  This is
                 * a simple way to have something meaningful in
                 * the unused slot.  Ignore this second slot, but
                 * make sure it does not contain garbage.
                 */
                pcfs->constant_pool[cpidx] = pcpd;
                cpidx++;

                break;

            case CONSTANT_Double:
                ALLOC_CP_INFO(CONSTANT_Double_info,
                              LOCAL_Double_binding);

                CP_ITEM_SWAP_U4(CONSTANT_Double_info, high_bytes);

                CP_ITEM_SWAP_U4(CONSTANT_Double_info, low_bytes);

                /*
                 * Make both slots point to the same place-- see
                 * assignment below for value at [cpidx].  This is
                 * a simple way to have something meaningful in
                 * the unused slot.  Ignore this second slot, but
                 * make sure it does not contain garbage.
                 */
                pcfs->constant_pool[cpidx] = pcpd;
                cpidx++;

                break;

            case CONSTANT_NameAndType:
                ALLOC_CP_INFO(CONSTANT_NameAndType_info,
                              LOCAL_NameAndType_binding);
                CP_ITEM_SWAP_U2(CONSTANT_NameAndType_info, name_index);
                CP_ITEM_SWAP_U2(CONSTANT_NameAndType_info,
                                descriptor_index);
                CPTYPEIDX_RANGE_CHECK(CONSTANT_NameAndType_info,
                                      pcfs,
                                      name_index,
                                      "CP name and type name index");
                CPTYPEIDX_RANGE_CHECK(CONSTANT_NameAndType_info,
                                      pcfs,
                                      descriptor_index,
                                   "CP name and type descriptor index");

                /* Initialize late binding extension */

                /* (There is no late binding associated with this tag)*/
                break;

            case CONSTANT_Utf8:

                tmplenutf = ((CONSTANT_Utf8_info *) pcpbytes)->length;
                MACHINE_JSHORT_SWAP(tmplenutf);

                /*
                 * Explicitly unroll the ALLOC_CP_INFO() macro
                 * due to the variable length string area.  Adjust
                 * also for the dummy item
                 * @link CONSTANT_Utf8_info#bytes bytes@endlink,
                 * namely @c @b sizeof(info[1]), or 1 byte.
                 *
                 * The segment below copies each piece of the buffer
                 * from @c @b *pcpbytes to @c @b *pcpd .
                 */

                /* Size of structure to copy */
                cf_item_size = sizeof(CONSTANT_Utf8_info) -
                               sizeof(struct LOCAL_Utf8_binding) -
                               sizeof(u1);

                /* Allocate heap for structure */
                pcpd = HEAP_GET_METHOD(sizeof(u1) * CP_INFO_NUM_EMPTIES+
                                           tmplenutf + cf_item_size,
                                       rfalse);

                /* Copy structure, including @b empty bytes */
                portable_memcpy(((rbyte *)pcpd) +
                                sizeof(u1) * CP_INFO_NUM_EMPTIES,
                                pcpbytes,
                                cf_item_size);
                pcpd->empty[0] = FILL_INFO_DUP0;
                pcpd->empty[1] = FILL_INFO_DUP1;
                pcpd->empty[2] = FILL_INFO_DUP2;

                /*
                 * Byte swap contents of
                 * @link CONSTANT_Utf8_info#length length@endlink
                 * field (only multi-byte field in structure)
                 */
                CP_ITEM_SWAP_U2(CONSTANT_Utf8_info, length);

                /* Copy UTF string itself (No byte reversal needed.) */
                portable_memcpy(PTR_THIS_CP_Utf8(pcpd)->bytes,
                               ((CONSTANT_Utf8_info *) pcpbytes)->bytes,
                                tmplenutf);

                /*
                 * Adjust for variable length UTF8 string in source bfr
                 */
                cf_item_size += tmplenutf;

                /* Initialize late binding extension */

                /* (There is no late binding associated with this tag)*/

                break;

            default:
                /*!
                 * @todo  HARMONY-6-jvm-classfile.c-9 Need better and
                 *        more complete heap free here
                 */

                /* This pointer came from HEAP_GET_METHOD() */
                HEAP_FREE_METHOD(pcfs);

                GENERIC_FAILURE1_PTR(rtrue,
                                     DMLNORM,
                                     arch_function_name, 
                                     "Invalid CP tag %d",
                                     (((cp_info *) pcpbytes)->tag),
                                     ClassFile,
                                     rnull,
                                     rnull);

        } /* switch ... */

        /* Point to storage area for this @c @b constant_pool[] item */
        pcfs->constant_pool[cpidx] = pcpd;

        /* Now point past this CONSTANT_Xxxxxx_info area */
        pcpbytes += cf_item_size;

    } /* for (cpidx) */


    /*****************************************************************/
    /* Get access_flags                                              */
    /*****************************************************************/

    /* Point past the @c @b constant_pool[] area */
    MAKE_PU2(pu2, pcpbytes);

    pcfs->access_flags = GETRS2(pu2++);
    /*
     * Strip out all other @link #ACC_PUBLIC ACC_xxx@endlink bits, per
     * spec
     */
    pcfs->access_flags &= (ACC_PUBLIC | ACC_FINAL | ACC_SUPER |
        ACC_INTERFACE | ACC_ABSTRACT | ACC_SYNTHETIC | ACC_ANNOTATION |
        ACC_ENUM);

    MACHINE_JSHORT_SWAP(pcfs->access_flags);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-10
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "access flags");
     */

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "access %04x",
              pcfs->access_flags);


    /*****************************************************************/
    /* Get this_class                                                */
    /*****************************************************************/
    pcfs->this_class = GETRS2(pu2++);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-11 Need to
     *       free @c @b constant_pool[0..n] also if failure
     */
    CPIDX_RANGE_CHECK(pcfs, pcfs->this_class, "this class");

    cfmsgs_typemsg("this", pcfs, pcfs->this_class);

    cfmsgs_show_constant_pool(pcfs);


    /*****************************************************************/
    /* Get super_class                                               */
    /*****************************************************************/
    pcfs->super_class = GETRS2(pu2++);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-12
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "access flags");
     */

    cfmsgs_typemsg("super", pcfs, pcfs->super_class);

    /*****************************************************************/
    /* Create interfaces index, then load up interfaces array        */
    /*****************************************************************/
    pcfs->interfaces_count = GETRS2(pu2++);

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "intfc count=%d",
              pcfs->interfaces_count);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-13
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "interfaces_count");
     */

    if (0 == pcfs->interfaces_count)
    {
        /* Quite possible, and theoretically okay */
        pcfs->interfaces = (u2 *) rnull;
    }
    else
    {
        /*
         * Map the indices in the class file to point to actual
         * constant pool enties via a pointer lookup table.
         */
        pcfs->interfaces = HEAP_GET_METHOD(pcfs->interfaces_count *
                                               sizeof(u2 *),
                                           rfalse);

        /*
         * Iterate through class file's interfaces[] array and
         * fill in the @c @b constant_pool[] references for normal use.
         */
        jvm_interface_index ifidx;
        for (ifidx = 0; ifidx < pcfs->interfaces_count; ifidx++)
        {
            /* Retrieve next interface index from class file area */
            jvm_interface_index cfifidx = GETRS2(pu2++);

            /* Range check the index value */
            CPIDX_RANGE_CHECK(pcfs,
                              cfifidx,
                              "interface index");

            /*
             * Can't free in GENERIC_FAILURE1_PTR() because
             * these pointer came from HEAP_GET_METHOD(),
             * not from HEAP_GET_DATA()
             */
            if (CONSTANT_Class != CP_TAG(pcfs, cfifidx))
            {
                HEAP_FREE_METHOD(pcfs->interfaces);
                /*!
                 * @todo HARMONY-6-jvm-classfile.c-14
                 *       HEAP_FREE_METHOD(pcfs->constan_pool[0..n]);
                 */
                HEAP_FREE_METHOD(pcfs);
            }

            GENERIC_FAILURE1_PTR((CONSTANT_Class !=
                                                  CP_TAG(pcfs,cfifidx)),
                                 DMLNORM,
                                 arch_function_name,
                                 "Invalid interface tag %d",
                                 CP_TAG(pcfs, cfifidx),
                                 ClassFile,
                                 rnull,
                                 rnull);

            /* Finally store valid constant pool entry index */
            pcfs->interfaces[ifidx] = cfifidx;

            cfmsgs_typemsg("intfc", pcfs, cfifidx);

        } /* for (ifidx) */

        /*
         * Point past the interfaces[] area
         * (done above by autoincrement)
         */

        /* pu2 += pcfs->interfaces_count; */

    }


    /*****************************************************************/
    /* Get field_count                                               */
    /*****************************************************************/
    pcfs->fields_count = GETRS2(pu2++);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-15
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "fields_count");
     */

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "flds count=%d",
              pcfs->fields_count);


    /*****************************************************************/
    /* Create constant pool index, then load up fields[] array       */
    /*****************************************************************/
    jbyte *pfbytes = (jbyte *) pu2;

    if (0 == pcfs->fields_count)
    {
        /* Quite possible, and theoretically okay */
        pcfs->fields = (field_info **) rnull;
    }
    else
    {
        jvm_field_index fldidx;

        /*
         * Map the indices in the class file to point to actual
         * constant pool enties via a pointer lookup table.
         */
        pcfs->fields = HEAP_GET_METHOD(pcfs->fields_count *
                                           sizeof(field_info *),
                                       rtrue);


        /*
         * Iterate through class file's fields and fill in
         * the attributes[] pointer array for normal use.
         */
        for (fldidx = 0; fldidx < pcfs->fields_count; fldidx++)
        {
            /* Allocate a heap area for each fields[] entry */
            ALLOC_CF_ITEM(field_info,
                          pfbytes,
                          pcfs->fields[fldidx],
                          LOCAL_field_binding);

            MACHINE_JSHORT_SWAP(pcfs->fields[fldidx]->access_flags);
            MACHINE_JSHORT_SWAP(pcfs->fields[fldidx]->name_index);
            MACHINE_JSHORT_SWAP(pcfs->fields[fldidx]->descriptor_index);
            MACHINE_JSHORT_SWAP(pcfs->fields[fldidx]->attributes_count);

            /* Strip out unused access flags, per spec */
            pcfs->fields[fldidx]->access_flags &=
                (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED |
                 ACC_STATIC | ACC_FINAL | ACC_VOLATILE | ACC_TRANSIENT |
                 ACC_SYNTHETIC | ACC_ENUM);

            /* Range check the indices */
            CPIDX_RANGE_CHECK(pcfs,
                              pcfs->fields[fldidx]->name_index,
                              "field name index");
            CPIDX_RANGE_CHECK(pcfs,
                              pcfs->fields[fldidx]->descriptor_index,
                              "field descriptor index");

            cfmsgs_typemsg("fld name",
                           pcfs,
                           pcfs->fields[fldidx]->name_index);
            cfmsgs_typemsg("fld desc",
                           pcfs,
                           pcfs->fields[fldidx]->descriptor_index);

            /* Skip past above items to attributes area */
            pfbytes = (jbyte *) &((method_info *) pfbytes)->attributes;


            /*
             * Map the indices in the class file to point to actual
             * constant pool enties via a pointer lookup table.
             */
            if (0 ==  pcfs->fields[fldidx]->attributes_count)
            {
                /* Quite possible for small objects */
                pcfs->fields[fldidx]->attributes =
                                          (attribute_info_dup **) rnull;
            }
            else
            {
                pcfs->fields[fldidx]->attributes =
                    HEAP_GET_METHOD(
                        pcfs->fields[fldidx]->attributes_count *
                            sizeof(attribute_info *),
                        rtrue);

                /*
                 * Load up each attribute in this attribute area
                 */
                jvm_attribute_index atridx;
                for (atridx = 0;
                     atridx < pcfs->fields[fldidx]->attributes_count;
                     atridx++)
                {
                    /*
                     * Load an attribute and verify that it is either
                     * a valid (or an ignored) attribute, then point to
                     * next attribute in class file image.
                     */

                    pfbytes =
                        cfattrib_loadattribute(
                            pcfs,
                            &pcfs->fields[fldidx]->attributes[atridx],
                            (attribute_info *) pfbytes);

                    LOAD_SYSCALL_FAILURE((rnull == pfbytes),
                                         "load field attribute",
                                         rnull,
                                         rnull);

                } /* for (atridx) */

            } /* if attributes_count else */

            /* Finally, Initialize the late binding extension */
            pcfs->fields[fldidx]->LOCAL_field_binding.fluidxJVM =
                                                    jvm_field_index_bad;

        } /* for (fldidx) */

    } /* if pcfs->fields */


    /*****************************************************************/
    /* Get method_count                                              */
    /*****************************************************************/

    /* Point past end of fields area */
    MAKE_PU2(pu2, pfbytes);

    pcfs->methods_count =  GETRS2(pu2++);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-16
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "methods count");
     */

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "meth count=%d",
              pcfs->methods_count);

    /*****************************************************************/
    /* Create constant pool index, then load up methods[] array      */
    /*****************************************************************/
    jbyte *pmbytes = (jbyte *) pu2;

    if (0 == pcfs->methods_count)
    {
        /* Quite possible, and theoretically okay */
        pcfs->methods = (method_info **) rnull;
    }
    else
    {
        jvm_method_index mthidx;

        /*
         * Map the indices in the class file to point to actual
         * constant pool enties via a pointer lookup table.
         */
        pcfs->methods = HEAP_GET_METHOD(pcfs->methods_count *
                                            sizeof(method_info *),
                                        rtrue);

        /*
         * Iterate through class file's methods and fill in
         * the attributes[] pointer array for normal use.
         */
        for (mthidx = 0; mthidx < pcfs->methods_count; mthidx++)
        {
            /* Allocate a heap area for each methods[] entry */
            ALLOC_CF_ITEM(method_info,
                          pmbytes,
                          pcfs->methods[mthidx],
                          LOCAL_method_binding);

            MACHINE_JSHORT_SWAP(pcfs->methods[mthidx]->access_flags);
            MACHINE_JSHORT_SWAP(pcfs->methods[mthidx]->name_index);
           MACHINE_JSHORT_SWAP(pcfs->methods[mthidx]->descriptor_index);
           MACHINE_JSHORT_SWAP(pcfs->methods[mthidx]->attributes_count);

            /* Strip out unused access flags, per spec */
            pcfs->methods[mthidx]->access_flags &=
                (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED |
                 ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | 
                 ACC_BRIDGE | ACC_VARARGS | ACC_NATIVE| ACC_ABSTRACT |
                 ACC_STRICT | ACC_SYNTHETIC);

            /* Range check the indices */
            CPIDX_RANGE_CHECK(pcfs,
                              pcfs->methods[mthidx]->name_index,
                              "method name index");
            CPIDX_RANGE_CHECK(pcfs,
                              pcfs->methods[mthidx]->descriptor_index,
                              "method descriptor index");

            cfmsgs_typemsg("meth name",
                           pcfs,
                           pcfs->methods[mthidx]->name_index);
            cfmsgs_typemsg("meth desc",
                          pcfs,
                          pcfs->methods[mthidx]->descriptor_index);


            /* Skip past above items to attributes area */
            pmbytes = (jbyte *) &((method_info *) pmbytes)->attributes;

            /*
             * Initialize the late binding extension.
             * These values will get overwritten when a
             * code attribute is found and when an optional
             * exception attribute is found.
             */
            pcfs->methods[mthidx]
                    ->LOCAL_method_binding.codeatridxJVM =
                                            jvm_attribute_index_bad;

            pcfs->methods[mthidx]
                    ->LOCAL_method_binding.excpatridxJVM =
                                            jvm_attribute_index_bad;

            pcfs->methods[mthidx]
                    ->LOCAL_method_binding.nmordJVM =
                                     jvm_native_method_ordinal_null;


            /*
             * Map the indices in the class file to point to actual
             * constant pool enties via a pointer lookup table.
             */
            if (0 ==  pcfs->methods[mthidx]->attributes_count)
            {
                /* Quite possible for small objects */
                pcfs->methods[mthidx]->attributes =
                                          (attribute_info_dup **) rnull;
            }
            else
            {
                /*
                 * Allocate the attribute area for this method
                 */
                pcfs->methods[mthidx]->attributes =
                    HEAP_GET_METHOD(
                        pcfs->methods[mthidx]->attributes_count *
                            sizeof(attribute_info_dup *),
                        rtrue);

                /*
                 * Load up each attribute in this attribute area
                 */
                jvm_attribute_index atridx;
                for (atridx = 0;
                     atridx < pcfs->methods[mthidx]->attributes_count;
                     atridx++)
                {
                    /*
                     * Load an attribute and verify that it is either
                     * a valid (or an ignored) attribute, then point to
                     * next attribute in class file image.
                     */

                    pmbytes =
                        cfattrib_loadattribute(
                            pcfs,
                            &pcfs->methods[mthidx]->attributes[atridx],
                            (attribute_info *) pmbytes);

                    LOAD_SYSCALL_FAILURE((rnull == pmbytes),
                                         "verify member attribute",
                                         rnull,
                                         rnull);

                    /*
                     * Load Code_attribute and Exceptions_attribute
                     * atridx
                     */
                    switch (cfattrib_atr2enum(pcfs,
                            pcfs->methods[mthidx]->attributes[atridx]
                                      ->ai.attribute_name_index))
                    {
                        case LOCAL_CODE_ATTRIBUTE:
                            pcfs
                              ->methods[mthidx]
                                ->LOCAL_method_binding.codeatridxJVM =
                                                                 atridx;
                            break;

                        case LOCAL_EXCEPTIONS_ATTRIBUTE:

                            pcfs
                              ->methods[mthidx]
                                ->LOCAL_method_binding.excpatridxJVM =
                                                                 atridx;
                            break;

                        /* Satisfy compiler that all cases are handled*/
                        case LOCAL_UNKNOWN_ATTRIBUTE:
                        case LOCAL_CONSTANTVALUE_ATTRIBUTE:
                        case LOCAL_INNERCLASSES_ATTRIBUTE:
                        case LOCAL_ENCLOSINGMETHOD_ATTRIBUTE:
                        case LOCAL_SYNTHETIC_ATTRIBUTE:
                        case LOCAL_SIGNATURE_ATTRIBUTE:
                        case LOCAL_SOURCEFILE_ATTRIBUTE:
                        case LOCAL_LINENUMBERTABLE_ATTRIBUTE:
                        case LOCAL_LOCALVARIABLETABLE_ATTRIBUTE:
                        case LOCAL_LOCALVARIABLETYPETABLE_ATTRIBUTE:
                        case LOCAL_DEPRECATED_ATTRIBUTE:
                        case LOCAL_RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE:
                        case
                            LOCAL_RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE:
                        case
                     LOCAL_RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
                        case
                   LOCAL_RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE:
                        case LOCAL_ANNOTATIONDEFAULT_ATTRIBUTE:
                            break;
                    } /* switch cfattrib_atr2enum() */

                } /* for (atridx) */

            } /* if attributes_count else */

        } /* for (mthidx) */

    } /* if pcfs->methods */

    /*****************************************************************/
    /* Get attributes_count                                          */
    /*****************************************************************/

    /* Point past end of methods area */
    MAKE_PU2(pu2, pmbytes);

    pcfs->attributes_count = GETRS2(pu2++);

    /*!
     * @todo HARMONY-6-jvm-classfile.c-17
     *       LOAD_SYSCALL_FAILURE(what needs checking here?,
     *                            "methods count");
     */

    sysDbgMsg(DMLNORM,
              arch_function_name,
              "att cnt=%d",
              pcfs->attributes_count);

    /*****************************************************************/
    /* Create constant pool index, then load up attributes[] array   */
    /*****************************************************************/

    if (0 == pcfs->attributes_count)
    {
        /* Not really possible, and theoretically faulty */
        pcfs->attributes = (attribute_info_dup **) rnull;
    }
    else
    {
        /*
         * Map the indices in the class file to point to actual
         * constant pool enties via a pointer lookup table.
         */
        pcfs->attributes = HEAP_GET_METHOD(pcfs->attributes_count *
                                               sizeof(attribute_info *),
                                           rtrue);

        /*
         * Load up each attribute in the class file attribute area
         */
        jvm_attribute_index atridx;
        jbyte *pcbytes = (jbyte *) pu2;

        for (atridx = 0; atridx < pcfs->attributes_count; atridx++)
        {
            /*
             * Load an attribute and verify that it is either
             * a valid (or an ignored) attribute, then point to
             * next attribute in class file image.
             */

            pcbytes =
                cfattrib_loadattribute(
                    pcfs,
                    &pcfs->attributes[atridx],
                    (attribute_info *) pcbytes);

            LOAD_SYSCALL_FAILURE((rnull == pcbytes),
                                 "verify file attribute",
                                 rnull,
                                 rnull);

        } /* for (atridx) */

    } /* if pcfs->methods_count else */


   /*!
    * @todo HARMONY-6-jvm-classfile.c-18
    *       Throw @b VerifyError for classes w/ questionable contents
    */

    /* Class file structures are now fully loaded from class file data*/

    return(pcfs);

} /* END of classfile_loadclassdata() */


/*!
 * @brief Release all heap allocated to a fully loaded
 * ClassFile structure
 *
 * @param  pcfs   Pointer to a ClassFile structure with all its pieces
 *
 *
 * @returns @link #rvoid rvoid@endlink Whether it succeeds or fails,
 *          returning anything does not make much sense.  This is
 *          similar to @c @b free(3) not returning anything even
 *          when a bad pointer was passed in.
 *
 */ 
rvoid classfile_unloadclassdata(ClassFile *pcfs)
{
    ARCH_FUNCTION_NAME(classfile_unloadclassdata);

    if (rnull == pcfs)
    { 
        return; /* Nothing to do if @link #rnull rnull@endlink pointer*/
    }

    jvm_constant_pool_index cpidx;
    jvm_field_index         fldidx;
    jvm_method_index        mthidx;
    jvm_attribute_index     atridx;

    /*
     * Deallocate in the reverse order of allocation to eliminate
     * @e any chance for @link #rnull rnull@endlink pointer.  Do
     * file attributes first (last entry to first), then method
     * attributes, methods, field attributes, fields, and
     * @c @b constant_pool.
     */

    if ((0 < pcfs->attributes_count) && (rnull != pcfs->attributes))
    {
        for (atridx = pcfs->attributes_count - 1;
             atridx != JVMCFG_BAD_ATTRIBUTE; /* wrap-around when done */
             atridx--)
        {
            /*
             * Skip any @link #rnull rnull@endlink entries
             * (should NEVER happen)
             */
            if (rnull == pcfs->attributes[atridx])
            {
                continue;
            }

           cfattrib_unloadattribute(pcfs, pcfs->attributes[atridx]);
        }
    }


    if ((0 < pcfs->methods_count) && (rnull != pcfs->methods))
    {
        for (mthidx = pcfs->methods_count - 1;
             mthidx != JVMCFG_BAD_METHOD; /* wrap-around when done */
             mthidx--)
        {
            /*
             * Skip any @link #rnull rnull@endlink entries
             * (should NEVER happen)
             */
            if (rnull == pcfs->methods[mthidx])
            {
                continue;
            }

            if (0 < pcfs->methods[mthidx]->attributes_count)
            {
                for (atridx = pcfs->methods[mthidx]->attributes_count-1;
                                             /* wrap-around when done */
                     atridx != JVMCFG_BAD_ATTRIBUTE;
                     atridx--)
                {
                    /*
                     * Skip any @link #rnull rnull@endlink entries
                     * (should NEVER happen)
                     */
                    if (rnull ==
                        pcfs->methods[mthidx]->attributes[atridx])
                    {
                        continue;
                    }

                    cfattrib_unloadattribute(pcfs,
                             pcfs->methods[mthidx]->attributes[atridx]);
                }
            }

            HEAP_FREE_METHOD(pcfs->methods[mthidx]->attributes);
            HEAP_FREE_METHOD(pcfs->methods[mthidx]);
        }
    }


    if ((0 < pcfs->fields_count) && (rnull != pcfs->fields))
    {
        for (fldidx = pcfs->fields_count - 1;
             fldidx != JVMCFG_BAD_FIELD; /* wrap-around when done */
             fldidx--)
        {
            /*
             * Skip any @link #rnull rnull@endlink entries
             * (should NEVER happen)
             */
            if (rnull == pcfs->fields[fldidx])
            {
                continue;
            }

            if (0 < pcfs->fields[fldidx]->attributes_count)
            {
                for (atridx = pcfs->fields[fldidx]->attributes_count -1;
                                             /* wrap-around when done */
                     atridx != JVMCFG_BAD_ATTRIBUTE;
                     atridx--)
                {
                    /*
                     * Skip any @link #rnull rnull@endlink entries
                     * (should NEVER happen)
                     */
                    if (rnull ==
                        pcfs->fields[fldidx]->attributes[atridx])
                    {
                        continue;
                    }

                    cfattrib_unloadattribute(pcfs,
                              pcfs->fields[fldidx]->attributes[atridx]);
                }
            }

            HEAP_FREE_METHOD(pcfs->fields[fldidx]->attributes);
            HEAP_FREE_METHOD(pcfs->fields[fldidx]);
        }
    }


    if ((0 < pcfs->constant_pool_count)&&(rnull != pcfs->constant_pool))
    {
        for (cpidx = pcfs->constant_pool_count - 1;
             cpidx > CONSTANT_CP_DEFAULT_INDEX;
             cpidx--)
        {
            /*
             * Skip any @link #rnull rnull@endlink entries
             * (should NEVER happen)
             */
            if (rnull == pcfs->constant_pool[cpidx])
            {
                continue;
            }

            HEAP_FREE_METHOD(pcfs->constant_pool[cpidx]);
        }
    }

    HEAP_FREE_METHOD(pcfs->constant_pool);
    HEAP_FREE_METHOD(pcfs);

    return;

} /* end of classfile_unloadclassdata() */


/*******************************************************************/
/*!
 *
 * @brief Report an error @link #rnull rnull@endlink pointer if
 * a system call fails
 *
 *
 * @param  expr   Any logical expression that returns
 *                @link #rtrue rtrue@endlink or
 *                @link #rfalse rfalse@endlink.
 *
 * @param  msg    Text to display to sysDbgMsg() upon failure
 *
 *
 * @returns If @b expr is @link #rtrue rtrue@endlink, return a
 *          @link #rnull rnull@endlink pointer to the
 *          calling function, cast as <b><code>(rvoid *)</code></b>,
 *          else continue with inline code.
 *
 */
#define READ_SYSCALL_FAILURE(expr, msg)            \
    GENERIC_FAILURE_PTR((expr),                    \
                        DMLMIN,                    \
                        arch_function_name,        \
                        msg,                       \
                        rvoid,                     \
                        rnull,                     \
                        rnull)

/*!
 * @name Read Java object code (class data) from disk files.
 *
 *
 * @param  filename    Null-terminated ASCII string, pathname
 *                     of JAR file or class file.
 *
 *
 * @returns Pointer to memory area containing class file data.
 *          If error detected, @link #rnull rnull@endlink is
 *          returned and perror("msg") may be called to report
 *          the system call problem that caused the
 *          particular failure more.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Read a JVM class file.
 *
 * If a valid class file is read, return pointer to memory area
 * containing its Java class image.
 *
 */
u1 *classfile_readclassfile(rchar *filename)
{
    ARCH_FUNCTION_NAME(classfile_readclassfile);

    off_t filesize = 0;

    rvoid *statbfr; /* Portability library does (struct stat) part */

    rvoid *pclassfile_image;

    int fd;

    /*
     * Chk if file is available and read its stat info, esp file size
     */
    statbfr = portable_stat(filename);

    READ_SYSCALL_FAILURE(rnull == statbfr, "statbfr");

    rlong get_st_size = portable_stat_get_st_size(statbfr);
    HEAP_FREE_DATA(statbfr);

    /*
     * Allocate enough space for entire class file to be
     * Read into memory at once.
     */

    pclassfile_image = (rvoid *) HEAP_GET_DATA(get_st_size, rfalse);

    /* Now go open the file and read it */
    fd = portable_open(filename, O_RDONLY);
    READ_SYSCALL_FAILURE(0 > fd, "file open");

    /* Read the whole file in at once and close it. */
    filesize = portable_read(fd, pclassfile_image, get_st_size);
    READ_SYSCALL_FAILURE(0 > filesize, "file read");

    portable_close(fd);

    /* Make sure stat() and read() have the same size */
    GENERIC_FAILURE_PTR((filesize != get_st_size),
                        DMLNORM,
                        arch_function_name, 
                        "Incomplete file read",
                        rvoid,
                        pclassfile_image,
                        rnull);

    /* Proper completion when entire file is read in */
    return(pclassfile_image);

} /* END of classfile_readclassfile() */


/*!
 * @brief Read an entire JAR file into temporary disk area and
 * load up one class file from it.
 *
 * If a valid JAR file is read, return pointer to memory area containing
 * the Java class image of the startup class that was specified in Jar
 * Manifest file.  In the future, all classes in the JAR file will
 * be available for loading  from the temporary disk area via
 * @b CLASSPATH.
 *
 */
u1 *classfile_readjarfile(rchar *filename)
{
    ARCH_FUNCTION_NAME(classfile_readjarfile);

    rvoid *statbfr; /* Portability library does (struct stat) part */

    rchar *jarparm = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

    rchar *pwd = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

    rchar *jarscript = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

    /*!
     * @todo  HARMONY-6-jvm-classfile.c-19 Need a version of this that
     *        works on MS Windows and on CygWin w/ ALT_ (\\) path spec.
     */

    /*
     * Make sure to have an @e absolute path name to @b filename
     * since it may be relative and the @c @b jar command
     * used @c @b chdir
     */
/* #ifdef CONFIG_CYGWIN
       ** 
        * Need to account for \path\name as well as C:\path\name forms
        * plus CygWin's /path/name form
        **
       if ((JVMCFG_PATHNAME_DELIMITER_CHAR     == filename[0]) ||
           (JVMCFG_PATHNAME_ALT_DELIMITER_CHAR == filename[0]))
   #else
*/
    if (JVMCFG_PATHNAME_DELIMITER_CHAR == filename[0])
/* #endif */
    {
        portable_strcpy(jarparm, filename);
    }
    else
    {
        /*!
         * @todo  HARMONY-6-jvm-classfile.c-20 Check `pwd` overflow
         *        and @link #rnull rnull@endlink returned
         */
        portable_getwd(pwd);

        sprintfLocal(jarparm,
                     "%s%c%s",
                     pwd,
                     JVMCFG_PATHNAME_DELIMITER_CHAR,
                     filename);
    }

    /* Convert input parm to internal form, append suffix */

    /*
     * Build up JAR command using internal class name w/suffix.  Make
     * @e sure all files are writeable for final <code>rm -rf<code>.
     */
    sprintfLocal(jarscript,
                 JVMCFG_JARFILE_MANIFEST_EXTRACT_SCRIPT,
                 tmparea_get(),
                 pjvm->java_home,
                 jarparm);

    int rc = portable_system(jarscript);

    if (0 != rc)
    {
        sysErrMsg(arch_function_name,
                  "Cannot extract data from JAR file %s",
                  jarparm);
        exit_jvm(EXIT_CLASSPATH_JAR);
/*NOTREACHED*/
    }

    /* Verify extraction of manifest file */
    sprintfLocal(jarscript, /* Reuse unneeded buffer */
                 "%s%c%s",
                 tmparea_get(),
                 JVMCFG_PATHNAME_DELIMITER_CHAR,
                 JVMCFG_JARFILE_MANIFEST_FILENAME);


    /* Read manifest file and locate starting class name */
    rchar *mnfstartclass = manifest_get_main(jarscript);

    if (rnull == mnfstartclass)
    {
        sysErrMsg(arch_function_name,
                  "Cannot locate start class in JAR file %s",
                  jarparm);
        exit_jvm(EXIT_CLASSPATH_JAR);
/*NOTREACHED*/
    }

    /* Reuse unneeded bfr w/ descriptive name */
    rchar *start_class_tmpfile = pwd;
    
    /* Verify existence of start class in JAR file */
    sprintfLocal(start_class_tmpfile,
                 "%s%c",
                 tmparea_get(),
                 JVMCFG_PATHNAME_DELIMITER_CHAR);

    int dirlen = portable_strlen(start_class_tmpfile);
    portable_strcat(pwd, mnfstartclass);

    (rvoid) classpath_external2internal_classname_inplace(&pwd[dirlen]);

    int alllen = portable_strlen(start_class_tmpfile);
    start_class_tmpfile[alllen] = JVMCFG_EXTENSION_DELIMITER_CHAR;
    start_class_tmpfile[alllen + 1] = '\0';
    portable_strcat(pwd, CLASSFILE_EXTENSION_DEFAULT);

    statbfr = portable_stat(start_class_tmpfile);

    /* Complain if class in manifest was not found in JAR file */
    if (rnull == statbfr)
    {
        sysErrMsg(arch_function_name,
           "Cannot locate start class '%s' in manifest for JAR file %s",
                  mnfstartclass,
                  jarparm);

        HEAP_FREE_DATA(mnfstartclass);
        HEAP_FREE_DATA(jarparm);
        HEAP_FREE_DATA(start_class_tmpfile);
        HEAP_FREE_DATA(jarscript);

        exit_jvm(EXIT_CLASSPATH_JAR);
    }

    /* Clean up for return */
    HEAP_FREE_DATA(jarparm);
    HEAP_FREE_DATA(mnfstartclass);
    HEAP_FREE_DATA(jarscript);

    /*
     * If starting class file was extracted, report result
     * in heap-allocated bfr
     */

    rvoid *pvrc = classfile_readclassfile(start_class_tmpfile);

    HEAP_FREE_DATA(start_class_tmpfile);

    return(pvrc);

} /* END of classfile_readjarfile() */

/*@} */ /* End of grouped definitions */


/* EOF */
