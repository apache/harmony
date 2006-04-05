/*!
 * @file object.c
 *
 * @brief Create and manage real machine Java object data structures.
 *
 * An object reference is a simple (@b jobject ), an integer index
 * into the object table.
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
ARCH_SOURCE_COPYRIGHT_APACHE(object, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "gc.h" 
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h" 
#include "method.h"
#include "native.h"
#include "opcode.h"
#include "opmacros.h"
#include "utf.h"
#include "unicode.h"
#include "util.h"


/*!
 * @brief Initialize the object area of the JVM model
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid object_init()
{
    ARCH_FUNCTION_NAME(object_init);

    object_new_setup(jvm_object_hash_null);

    pjvm->object_allocate_last = jvm_object_hash_null;

    /* Declare this module initialized */
    jvm_object_initialized = rtrue;

    return;

} /* END of object_init() */


/*!
 * @brief Invoke a method on a thread for any class, either
 * static or instance method, either native or virtual.
 *
 * Similar logic may be found in opcode_run() in its uncaught
 * exception handler concerning initiating execution of a JVM method.
 *
 *
 * @param  clsidx   Class table index of method to invoke.
 *
 * @param  mthname  Null-terminated name of method to invoke.
 *
 * @param  mthdesc  Null-terminated description of method parameters
 *                    and return types.
 *
 * @param  thridx   Thread table index of thread to load and run it on.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         @link #JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR
 *         if the requested method is not found in the class
 *         or has no code attribute.@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERNALERROR
 *         @link #JVMCLASS_JAVA_LANG_INTERNALERROR
 *         if null thread index@endlink.
 *
 */
rvoid object_run_method(jvm_class_index   clsidx,
                        rchar            *mthname,
                        rchar            *mthdesc,
                        jvm_thread_index  thridx)
{
    ARCH_FUNCTION_NAME(object_run_method);

    /* Must specify a valid thread */
    if (jvm_thread_index_null == thridx)
    {
        exit_throw_exception(EXIT_JVM_THREAD,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }

    /* No error will thrown here, it is handled below */
    jvm_method_index mthidx =
        method_find_by_prchar(clsidx, mthname, mthdesc);

    if (jvm_method_index_bad == mthidx)
    {
        exit_throw_exception(EXIT_JVM_METHOD,
                             JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
    }

    /*
     * Set FP lower boundary so Java @c @b return
     * instruction does not keep going after handler,
     * chk if @c @b \<init\> is there, and run it.
     */
    jvm_sp fp_save_end_program = THREAD(thridx).fp_end_program;

    /* Make JVM execution stop once method has finished running */
    THREAD(thridx).fp_end_program = THREAD(thridx).fp;

    /* Load up entry point for method */
    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    jvm_attribute_index codeatridx =
        pcfs->methods[mthidx]->LOCAL_method_binding.codeatridxJVM;

    /*
     * In reality, should have both or neither mthidx/codeatridx
     * in a valid and properly linked class file.
     */
    if (jvm_attribute_index_bad == codeatridx)
    {
        exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                             JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
    }

    if (jvm_attribute_index_native == codeatridx)
    {
        /* Pass parameters for both local method and JNI call */
        method_info *pmthidx = pcfs->methods[mthidx];
        native_run_method(thridx,
                          clsidx,
                          pmthidx->LOCAL_method_binding.nmordJVM,
                          pmthidx->name_index,
                          pmthidx->descriptor_index,
                          pmthidx->access_flags,
                          method_implied_opcode_from_cp_entry_pcfs(
                              pcfs,
                              pmthidx->name_index,
                              pmthidx->access_flags),
                              IS_INIT_METHOD(pcfs,
                                             pmthidx->name_index));
    }
    else
    {

        Code_attribute *pca =
            (Code_attribute *)
            &pcfs->methods[mthidx]->attributes[codeatridx]->ai;

        PUSH_FRAME(thridx, pca->max_locals);
        PUT_PC_IMMEDIATE(thridx,
                         clsidx,
                         mthidx,
                         pcfs->methods[mthidx]
                               ->LOCAL_method_binding.codeatridxJVM,
                         pcfs->methods[mthidx]
                               ->LOCAL_method_binding.excpatridxJVM,
                         CODE_CONSTRAINT_START_PC);

        /*
         * WARNING:  RECURSIVE CALL!!! But should @e only go one
         *           level deep per throwable condition, which
         *           may or may not have iterative behavior, but
         *           will not likely recurse infinitely.
         */
        if (rfalse == opcode_run(thridx, rfalse))
        {
            /* Problem running error class, so quit */
            sysErrMsg(arch_function_name,
                      "Cannot run method %s%s",
                      mthname,
                      mthdesc);

            jvmutil_print_stack(thridx, (rchar *) rnull);

            exit_jvm(EXIT_JVM_THREAD);
/*NOTREACHED*/
        }

        /*
         * Permit java.lang.Exception * and java.lang.Throwable
         * to continue by restoring lower FP boundary.
         */
        THREAD(thridx).fp_end_program = fp_save_end_program;
    }

    return;

} /* END of object_run_method() */


/*!
 * @brief Look up java.lang.String of this value.
 *
 * If a java.lang.String with this value already exists,
 * return its object hash, otherwise 
 * @link #jvm_object_hash_null jvm_object_hash_null@endlink.
 *
 * @param  utf8string  UTF8 representation of Unicode string to
 *                     locate in object table.
 *
 *
 * @returns @link #jvm_object_hash jvm_object_hash@endlink of a
 *          java.lang.String that contains this string value, otherwise
 *          @link #jvm_object_hash_null jvm_object_hash_null@endlink.
 *
 *
 * @todo  HARMONY-6-jvm-object.c-5 Needs better unit testing.
 *
 */
jvm_object_hash
    object_utf8_string_lookup(CONSTANT_Utf8_info *utf8string)
{
    ARCH_FUNCTION_NAME(object_utf8_string_lookup);

    if (rnull == utf8string)
    {
        return(jvm_object_hash_null);
    }

    jvm_class_index clsidx =
        class_find_by_prchar(JVMCLASS_JAVA_LANG_STRING);

    /* Should @e never be the case after JVM initialization */
    if (jvm_object_hash_null == clsidx)
    {
        return(jvm_object_hash_null);
    }

    /* Unicode representation will @e always be same or less than UTF */
    jchar *punicode = HEAP_GET_DATA(utf8string->length * sizeof(jchar),
                                    rfalse);

    jshort len_unicode = utf_utf2unicode(utf8string, punicode);

    jvm_object_hash objhash;

    for (objhash = JVMCFG_FIRST_OBJECT;
         objhash < JVMCFG_MAX_OBJECTS;
         objhash++)
    {
        if ((OBJECT(objhash).status & OBJECT_STATUS_INUSE) &&
            (OBJECT(objhash).status & OBJECT_STATUS_STRING))
        {
             jvm_object_hash array_objhash = 
                OBJECT(objhash)
                  .object_instance_field_data
                    [native_jlString_critical_field_value]._jobjhash;

             if (0 == unicode_strcmp(punicode,
                                     len_unicode,
                                     OBJECT(array_objhash).arraydata,
                                  OBJECT(array_objhash).arraylength))
             {
                return(objhash);
             }
        }
    }/* for objhash */

    return(jvm_object_hash_null);

} /* END of object_utf8_string_lookup() */


/*!
 * @brief Set up an empty object in a given object table slot.
 *
 * The @b objhash of JVMCFG_NULL_OBJECT has special
 * properties in that it can ALWAYS be allocated and
 * is NEVER garbage collected!  Part of the purpose
 * for this is the JVMCFG_NULL_OBJECT is of value zero,
 * which is widely used throughout the code as a special
 * value.  This this slot is not available for @e anything
 * else.
 * 
 *
 * @param  objhash   Object table hash of slot to set up.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid object_new_setup(jvm_object_hash objhash)
{
    ARCH_FUNCTION_NAME(object_new_setup);

    /*
     * Declare slot in use, but not initialized.
     * (Redundant for most situations where
     * object_allocate_slot() was called, but needed
     * for initializing classes like JVMCFG_NULL_OBJECT
     * with an absolute slot number that was not
     * searched for by the allocator.)
     */
    OBJECT(objhash).status = OBJECT_STATUS_INUSE | OBJECT_STATUS_NULL;

    /*
     * Start out with no array allocation and no array dimensions
     */
    OBJECT(objhash).arraybasetype = LOCAL_BASETYPE_ERROR;
    OBJECT(objhash).arraydims     = LOCAL_CONSTANT_NO_ARRAY_DIMS;
    OBJECT(objhash).arraylength   = 0;
    OBJECT(objhash).arraydata     = (rvoid *) rnull;

    /* No object monitor is locked */
    OBJECT(objhash).mlock_count   = 0;
    OBJECT(objhash).mlock_thridx  = jvm_thread_index_null;

    /* No superclass */
    OBJECT(objhash).objhash_superclass = jvm_object_hash_null;

    /* No access flag context */
    OBJECT(objhash).access_flags = LOCAL_ACC_EMPTY;

    /* No class or class file or thread linkage */
    OBJECT_OBJECT_LINKAGE(objhash)->pcfs   = (ClassFile *) rnull;
    OBJECT_OBJECT_LINKAGE(objhash)->clsidx = jvm_class_index_null;
    OBJECT_OBJECT_LINKAGE(objhash)->thridx = jvm_thread_index_null;

    /* No object instance fields */
    OBJECT(objhash).object_instance_field_data = (jvalue *) rnull;

    /*
     * Garbage collection @e initialization is performed by
     * @link #GC_OBJECT_NEW GC_OBJECT_NEW()@endlink.
     *
     * Garbage collection @e finalization is performed by
     * @link #GC_OBJECT_DELETE GC_OBJECT_DELETE()@endlink.
     */
    OBJECT(objhash).pgarbage = (rvoid *) rnull;

    /*
     * Do not set up GC_OBJECT_NEW() unless there is
     * a real object with the possibility of real fields.
     */

    return;

} /* END of object_new_setup() */


/*!
 * @brief Locate and reserve  an unused object table slot for
 * a new object instance.
 *
 *
 * @param  tryagain   If @link #rtrue rtrue@endlink, run garbage
 *                    collection @e once if no empty slots are
 *                    available so as to try and free up something.
 *                    Typically, invoke as @link #rtrue rtrue@endlink,
 *                    and let recursion call it with
 *                    @link #rfalse rfalse@endlink.
 *
 *
 * @returns Object hash of an empty slot.  Throw error if no slots.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no object slots are available.@endlink.
 *
 */
static jvm_object_hash object_allocate_slot(rboolean tryagain)
{
    ARCH_FUNCTION_NAME(object_allocate_slot);

    /* Search for a free object table slot */
    jvm_object_hash objhash =
        (JVMCFG_MAX_OBJECTS == pjvm->object_allocate_last)
        ? JVMCFG_FIRST_OBJECT
        : 1 + pjvm->object_allocate_last;

    /* Count allocated slots in all slots are full */
    jvm_object_hash count = 0;

    while(rtrue)
    {
        if (OBJECT(objhash).status & OBJECT_STATUS_INUSE)
        {
            /* Point to next slot, wrap around at end */
            objhash++;

            if (objhash == JVMCFG_MAX_OBJECTS - 1)
            {
                objhash = JVMCFG_FIRST_OBJECT;
            }

            /* Limit high value to end of table */
            if (pjvm->object_allocate_last == JVMCFG_MAX_OBJECTS - 1)
            {
                pjvm->object_allocate_last = JVMCFG_FIRST_OBJECT - 1;
            }


            /* Count this attempt and keep looking */
            count++;

            if (count ==  (JVMCFG_MAX_OBJECTS - JVMCFG_FIRST_OBJECT))
            {
                /* Try again (with rfalse) if requested */
                if (rtrue == tryagain)
                {
                    GC_RUN(rtrue);  /* Try to free up some space */

                    /* Now try to locate a free slot */

                    /* WARNING!!! Recursive call-- but only 1 deep */
                    return(object_allocate_slot(rfalse));
                }

                /* No more slots, cannot continue */
                exit_throw_exception(EXIT_JVM_OBJECT,
                                   JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR);
/*NOTREACHED*/
            }
        }


        /* Declare slot in use, but not initialized. */
        OBJECT(objhash).status =
                               OBJECT_STATUS_INUSE | OBJECT_STATUS_NULL;

        /* Report where this allocation was performed */
        pjvm->object_allocate_last = objhash;

        return(objhash);
    }
/*NOTREACHED*/
    return(jvm_object_hash_null); /* Satisfy compiler */

} /* END of object_allocate_slot() */


/*!
 * @brief Create a Java @c @b new object instance of a class and
 * optionally run its @c @b \<init\> method with default
 * parameters (none).
 *
 * The following five mutually exclusive variations are available using
 * @b special_obj modifier:
 *
 * <ul>
 * <li><b>OBJECT_STATUS_EMPTY</b>: Normal object, no special treatment.
 * </li>
 *
 * <li><b>OBJECT_STATUS_THREAD</b>: Treat the object instance creation
 *                                  as a normal object, but mark it as a
 *                                  @c @b java.lang.Thread .
 * </li>
 *
 * <li><b>OBJECT_STATUS_STRING</b>: Treat the object instance creation
 *                                  as a normal object, but mark it as a
 *                                  @c @b java.lang.String .
 * </li>
 *
 * <li><b>OBJECT_STATUS_CLASS</b>:  Treat the object instance creation
 *                                  as loading a class instead of
 *                                  instantiating an object and use
 *                                  @b clsidx as the class table slot
 *                                  value that defines the class.
 * </li>
 *
 * <li><b>OBJECT_STATUS_ARRAY</b>:  Treat the object instance creation
 *                                  as a dimension of an array class
 *                                  instead of instantiating an object
 *                                  of the requested class.
 * </li>
 *
 * Plus a fifth for internal use in this function only:
 *
 * <li><b>OBJECT_STATUS_SUBARRAY</b>: used internally to flag that this
 *                                    object is a subset of a larger
 *                                    array.  <em>DO NOT USE this
 *                                    modifier!</em>
 * </li>
 * </ul>
 *
 * No verification of @b special_obj is performed, only these values
 * are assumed.
 *
 * All fields (or array components) are set to zeroes per JVM spec.
 *
 * Notice that standard practice is to do
 * <b><code>new ArrayType[][][]</code></b> and initialize one
 * dimension at a time.  This function can certainly operate
 * in this way, simply set @link robject#arraydims arraydims@endlink
 * to 1.  But it @e can initialize a multi-dimensional array all at
 * once.
 *
 * Use a simple circular slot allocation mechanism to report where
 * most recent object was allocated.  The search for the next slot
 * will begin from here and go all the way around the array to this
 * same slot.  If not successful, return a
 * @link #jvm_object_hash_null jvm_object_hash_null@endlink, else
 * the object hash value of the slot.
 *
 * Even though this function makes types of two recursive calls to
 * itself, there is very little local storage allocated on the stack,
 * and that is mainly fragmented into the if() blocks where such
 * storage is used.  Therefore, even though this function may recurse
 * for either (a) number of array dimensions, or (b) number of
 * superclasses, in depth, there should never be any real machine
 * stack overflow problems unless the stack is either, (a) unnaturally
 * restricted to a very small size or, (b) a class file is not checked
 * for CONSTANT_MAX_ARRAY_DIMS or, (c) test for superclass circularity
 * is not working properly or, (d) a class has zillions of legitimate
 * superclasses, such as could be created by an automated JVM
 * regression tester.
 *
 *
 * @param   special_obj Bit-mapped request for various special
 *                      considerations for object construction. If not
 *                      needed, use @link #OBJECT_STATUS_EMPTY
                        OBJECT_STATUS_EMPTY@endlink.
 *                      If used, the values are:
 *
 * <ul>
 * <li>         @link #OBJECT_STATUS_THREAD OBJECT_STATUS_THREAD@endlink
 *                                              This is a normal object,
 *                                              and it is an instance of
 *                                              @c @b java.lang.Thread .
 * </li>
 *
 * <li>         @link #OBJECT_STATUS_STRING OBJECT_STATUS_STRING@endlink
 *                                              This is a normal object,
 *                                              and it is an instance of
 *                                              @c @b java.lang.String .
 * </li>
 *
 * <li>         @link #OBJECT_STATUS_CLASS OBJECT_STATUS_CLASS@endlink
 *                                             create new class object
 *                                             instead of class instance
 * </li>
 *
 * <li>         @link #OBJECT_STATUS_ARRAY OBJECT_STATUS_ARRAY@endlink
 *                                              create new array object
 *                                              reference.
 * </li>
 *
 * <li>     @link #OBJECT_STATUS_SUBARRAY OBJECT_STATUS_SUBARRAY@endlink
 *                                              this array is a subset
 *                                              of a larger array
 *                                          (<em>internal use only</em>)
 * </li>
 * </ul>
 *
 * @param    pcfs       Pointer to ClassFile area.
 *
 * @param    clsidx     Class table index for class object linkage.
 *
 * @param    arraydims  If this is an instance of an array object,
 *                      specifies the number of array dimensions.
 *                      @e Always use LOCAL_CONSTANT_NO_ARRAY_DIMS for
 *                      non-arrays, that is, when OBJECT_STATUS_ARRAY
 *                      is not set.
 *
 * @param   arraylength Array of number of elements in @e each array
 *                      dimension. Only meaningful when
 *                      OBJECT_STATUS_ARRAY is set.  Okay to be zero
 *                      in the first dimension, meaning no data in the
 *                    object's @link robject#arraydata arraydata@endlink
 *                       member (@link #rnull rnull@endlink instead).
 *                       This pointer @e must have been created by
 *                       HEAP_GET_DATA() and it will be freed by
 *                       HEAP_FREE_DATA() when the object is deleted.
 *
 * @param   run_init_   When @link #rtrue rtrue@endlink, run the
 *                      object's @c @b \<init\> method with default
 *                      parameters, otherwise
 *                      @link #rfalse rfalse@endlink.  Not meaningful
 *                      for OBJECT_STATUS_STRING.
 *
 * @param   thridx      Thread table index associated with this
 *                      @c @b java.lang.Thread object.
 *                      Meaningful only @b when OBJECT_STATUS_THREAD is
 *                      set or when @b run_init_ is
 *                      @link #rtrue rtrue@endlink.
 *
 * @param   utf8string  UTF8 string data associated with this
 *                      @c @b java.lang.String object.
 *                      Meaningful only when OBJECT_STATUS_STRING is
 *                      set.  @c @b run_init_ is meaningless when
 *                      this parameter is used.  When this parameter
 *                      is not used, it should be set to
 *                      @link #rnull rnull@endlink.
 *
 *
 * @returns Object hash value of allocation.  Throw error if no slots.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         @link #JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR
 *         if no object slots are available@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_UNSUPPORTEDCLASSVERSIONERROR
 *         @link #JVMCLASS_JAVA_LANG_UNSUPPORTEDCLASSVERSIONERROR
 *         if there is an unrecognized array base type@endlink.
 *
 */

jvm_object_hash object_instance_new(rushort          special_obj,
                                    ClassFile       *pcfs,
                                    jvm_class_index  clsidx,
                                    jvm_array_dim    arraydims,
                                    jint            *arraylength,
                                    rboolean         run_init_,
                                    jvm_thread_index thridx,
                                    CONSTANT_Utf8_info *utf8string)
{
    ARCH_FUNCTION_NAME(object_instance_new);

    if (OBJECT_STATUS_STRING & special_obj)
    {
        /* Check existing string of these identical contents */
        jvm_object_hash existing_string =
            object_utf8_string_lookup(utf8string);

        /* Done if this string already exists */
        if (jvm_object_hash_null != existing_string)
        {
            return(existing_string);
        }
    }

    /* Locate an empty slot */
    jvm_object_hash objhash = object_allocate_slot(rtrue);

    /* Initialize object structures */
    object_new_setup(objhash);

    /*
     * Store table linkages early for immediate use
     * (esp loading field data)
     */
    OBJECT(objhash).table_linkage.pcfs   = pcfs;
    OBJECT(objhash).table_linkage.clsidx = clsidx;
    OBJECT(objhash).table_linkage.thridx = jvm_thread_index_null;

    /*
     * Read values from *pcfs where possible
     * (need for loading field data soon)
     */
    OBJECT(objhash).access_flags = pcfs->access_flags;

    /* Check if class load instead of object instantiate */
    if (OBJECT_STATUS_CLASS & special_obj)
    {
        /*
         * Mark slot as being a class, not an object.
         * (See comments on recursion below as to
         * disposition of superclass definition.)
         */
        OBJECT(objhash).status |= OBJECT_STATUS_CLASS;
    }
    else
    if (OBJECT_STATUS_ARRAY & special_obj)
    {
        /*
         * Since pcfs points to non-array version of class, use it
         * to extract the formatted class string, including the
         * base type.  Everything will be BASETYPE_CHAR_L except
         * for the java.lang.Class primative pseudo-classes, which
         * will contain other types.  The base type of the array
         * ultimately governs the structure of the last array
         * dimension, where the actual data is stored.
         */
        CONSTANT_Utf8_info *pclsname = PTR_CP1_CLASS_NAME(pcfs,
                                                      pcfs->this_class);

        if (CLASS(clsidx).status & CLASS_STATUS_PRIMATIVE)
        {
            OBJECT(objhash).arraybasetype = (jvm_basetype)
                       pclsname->bytes[utf_get_utf_arraydims(pclsname)];
        }
        else
        {
            OBJECT(objhash).arraybasetype = BASETYPE_CHAR_L;
        }

        /* Set ARRAY status if applicable, also SUBARRAY */
        OBJECT(objhash).status |=
           special_obj & (OBJECT_STATUS_ARRAY | OBJECT_STATUS_SUBARRAY);


        OBJECT(objhash).arraydims = arraydims;

        OBJECT(objhash).arraylength   = arraylength[0];

        /* Recursively build array object with 1 less array dimension */

        /* Allocate the current array dimension--arraylength[0] */
        if (1 < arraydims)
        {
            /*
             * @c @b arraylength[0] may be zero,
             * meaning no data
             */

            OBJECT(objhash).arraydata =
                ((0 == arraylength[0])

                    /*!
                     * @todo  HARMONY-6-jvm-object.c-1 case needs
                     *         testing: 0 == arraylength[0]
                     */
                 ? (rvoid *) rnull

                 : HEAP_GET_DATA(
                      arraylength[0] * sizeof(jvm_object_hash),
                      rtrue)
                );

            jint dimlength;

            /* Notice if 0 == arraylength[0], this loop is skipped */
            for (dimlength = 0; dimlength < arraylength[0]; dimlength++)
            {
                /*
                 * Go allocate each element of this dimension of array,
                 * namely, the sub-array of arraylength[0]...
                 * ONLY set SUBARRAY status from this recursive call.
                 * Its purpose is for deallocation and cleanup.
                 */

                /* WARNING!  Recursive call, @b arraydims levels deep */
                jvm_object_hash objhasharray =
                    object_instance_new(
                        OBJECT_STATUS_ARRAY | OBJECT_STATUS_SUBARRAY,
                        pcfs,
                        clsidx,
                        arraydims - 1,
                        &arraylength[1],
                        run_init_,
                        thridx,
                        (CONSTANT_Utf8_info *) rnull);

                /*
                 * Add this object to this dimension's array and
                 * mark it as having one reference to it.
                 */
                ((jvm_object_hash *)
                 OBJECT(objhasharray)
                   .arraydata)[dimlength] = objhasharray;

                (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(objhash,
                                                    objhasharray);
            }
        }
        else
        if (1 == arraydims)
        {
            /*
             * Notice OBJECT(objhash).status does NOT have
             * OBJECT_STATUS_ARRAY set once the final array component
             * is to be constructed.  It is @e only for the data
             * superstructures of these components.
             *
             * Now look up base type, less BASETYPE_CHAR_ARRAY,
             * which is already accounted for.
             */
            int unit;
            switch(OBJECT(objhash).arraybasetype)
            {
                case BASETYPE_CHAR_B:  unit = sizeof(jbyte);    break;
                case BASETYPE_CHAR_C:  unit = sizeof(jchar);    break;
                case BASETYPE_CHAR_D:  unit = sizeof(jdouble);  break;
                case BASETYPE_CHAR_F:  unit = sizeof(jfloat);   break;
                case BASETYPE_CHAR_I:  unit = sizeof(jint);     break;
                case BASETYPE_CHAR_J:  unit = sizeof(jlong);    break;
                case BASETYPE_CHAR_L:  unit = sizeof(jvm_object_hash);
                                                                break;
                case BASETYPE_CHAR_S:  unit = sizeof(jshort);   break;
                case BASETYPE_CHAR_Z:  unit = sizeof(jboolean); break;

                default:
                    exit_throw_exception(EXIT_JVM_OBJECT,
                       JVMCLASS_JAVA_LANG_UNSUPPORTEDCLASSVERSIONERROR);
            }

            /* Allocate and zero out each component of 1-dim array */
            if (0 < arraylength[0])
            {
                /*!
                 * @internal By passing in @link #rtrue rtrue@endlink,
                 *           all array members are cleared to zero,
                 *           which corresponds to their correct initial
                 *           values for all data types, including @link
                             #jvm_object_hash jvm_object_hash@endlink
                 *
                 */
                OBJECT(objhash).arraydata =
                    HEAP_GET_DATA(unit * arraylength[0], rtrue);
            }
            else
            {
                OBJECT(objhash).arraydata = (rvoid *) rnull;
            }
        }
        else
        if (0 == arraydims)
        {
            /* Either bad dimensions or should not be marked as array */
            exit_throw_exception(EXIT_JVM_OBJECT,
                                 JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
        }

    } /* if OBJECT_STATUS_ARRAY */
    else
    {
        if (OBJECT_STATUS_THREAD & special_obj)
        {
            /*
             * Mark slot as being a @c @b java.lang.Thread,
             * which is simply a status bit set on a normal object.
             */
            OBJECT(objhash).status |= OBJECT_STATUS_THREAD;
            OBJECT(objhash).table_linkage.thridx = thridx;
        }
        else
        if (OBJECT_STATUS_STRING & special_obj)
        {
            /*
             * Mark slot as being a @c @b java.lang.String,
             * which is simply a status bit set on a normal object.
             */
            OBJECT(objhash).status |= OBJECT_STATUS_STRING;
            OBJECT(objhash).table_linkage.thridx = thridx;
        }

        /* Initialize object instance fields, read initialized fields */
        OBJECT(objhash).object_instance_field_data =
            class_get_object_instance_field_data(clsidx,
                                                 objhash,
                                                 pcfs);
    }

    /*
     * After allocating slot, instantiate superclass first,
     * then load up remainder of this object.
     */
    /*!
     * @todo  HARMONY-6-jvm-object.c-7 Does a multi-dimensional
     *        array have a superclass object?  Or even a ONE-dimensional
     *        array have a superclass object?  Should such be reserved
     *        for the array components _only_, where applicable?  Or
     *        should some or all array objects themselves have
     *        superclass objects so they eventually inherit the various
     *        methods from java.lang.Object (except .clone() method )
     *        and java.lang.Cloneable and java.io.Serializable (see
     *        spec section 2.15 on these implementations).
     */
    if (CONSTANT_CP_DEFAULT_INDEX != pcfs->super_class)
    {
        /*
         * WARNING!  RECURSIVE CALL!  This will recurse until
         * the superclass of this object is @c @b java/Lang/Object.
         * The recursion for class definitions is performed
         * by the calling method, so class loading does not
         * need recursion.
         */
        jvm_class_index clsidxsuper =
            class_find_by_cp_entry(PTR_CP_SLOT(pcfs, 
                                               PTR_CP_ENTRY_CLASS(pcfs,
                                                      pcfs->super_class)
                                                 ->name_index));

        if (jvm_object_hash_null == clsidxsuper)
        {
            /*!
             * @todo  HARMONY-6-jvm-object.c-6 Which is better, throw
             *        an error or clean up and return null.  If the
             *        latter, then add cleanup logic before returning.
             */
#if 0
            return(jvm_object_hash_null);
#else
        exit_throw_exception(EXIT_JVM_THREAD,
                             JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
#endif
        }

        jvm_table_linkage *ptl = CLASS_OBJECT_LINKAGE(clsidxsuper);

        /* Construct a superclass object instance for this object */
        /*
         * @warning  Recursive call, <b># of superclasses</b>
         *           levels deep.
         */
        OBJECT(objhash).objhash_superclass =
            object_instance_new(
             /*!
              * @todo  HARMONY-6-jvm-object.c-9 Do I need this logic, or
              *        can I get away with @e only OBJECT_STATUS_EMPTY?
              */
             /* (0 == utf_prchar_classname_strcmp(
                          JVMCLASS_JAVA_LANG_THREAD,
                          ptl->pcfs,
                          ptl->pcfs->this_class))
                    ? OBJECT_STATUS_THREAD
                    : (0 == utf_prchar_classname_strcmp(
                          JVMCLASS_JAVA_LANG_STRING,
                          ptl->pcfs,
                          ptl->pcfs->this_class))
                        ? OBJECT_STATUS_STRING
                        : OBJECT_STATUS_EMPTY
              */
                OBJECT_STATUS_EMPTY,

                ptl->pcfs,
                clsidxsuper,
                LOCAL_CONSTANT_NO_ARRAY_DIMS,
                (jint *) rnull,
                run_init_,
                thridx,
                utf8string);

        (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(
                    objhash,
                    OBJECT(objhash).objhash_superclass);
    }

    (rvoid) GC_CLASS_MKREF_FROM_OBJECT(
                objhash,
                OBJECT(objhash).table_linkage.clsidx);

    /* Start GC tracking for object */
    (rvoid) GC_OBJECT_NEW(objhash);

    /* Declare that object is initialized */
    OBJECT(objhash).status &= ~OBJECT_STATUS_NULL;

    if (OBJECT_STATUS_STRING & special_obj)
    {
        /*!
         * @todo  HARMONY-6-jvm-object.c-4 Need to write the code
         *        that loads the string data from @c @b stlen and 
         *        @c @b utf8string into the string object itself.
         *        This should probably be accomplished with
         *        the byte array constructor:
         *        String(byte[] bytes, int offset,int length)
         */
        jint numchars = utf8string->length;

        jvm_object_hash array_objhash =
            object_instance_new(OBJECT_STATUS_EMPTY,
                                CLASS_OBJECT_LINKAGE(
                                    pjvm->class_primative_char)->pcfs,
                                pjvm->class_primative_char,
                                1,
                                &numchars,
                                rfalse,
                                thridx,
                                (CONSTANT_Utf8_info *) rnull);

        /* Store UTF8 string into Unicode array buffer */
        utf_utf2unicode(utf8string, OBJECT(array_objhash).arraydata);

        /*
         * Manually stuff java.lang.String hooks into hard-coded field
         * positions.
         */
        OBJECT(objhash)
          .object_instance_field_data
            [native_jlString_critical_field_value]._jobjhash =
                                     /* Could say '_jarray' here */
                                                          array_objhash;

        OBJECT(objhash)
          .object_instance_field_data
            [native_jlString_critical_field_length]._jint =
                                                     utf8string->length;

        /* Initiate first reference */
        (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(objhash, array_objhash);
    }
    else
    {
        /*!
         * @todo  HARMONY-6-jvm-object.c-8 Furthermore (per discussion
         *        of superclass objects for arrays), should there be
         *        any constructor run for arrays?  Since all components
         *        are initialized to zero during heap allocation, is
         *        there anything to do?  Could it simply skip this step?
         */

        /* Done if not running @c @b \<init\> method or if an array */
        if ((rtrue == run_init_) &&
            (!(OBJECT_STATUS_ARRAY & special_obj)))
        {
            object_run_method(clsidx,
                CONSTANT_UTF8_INSTANCE_CONSTRUCTOR,
                CONSTANT_UTF8_INSTANCE_CONSTRUCTOR_DESCRIPTOR_DEFAULT,
                thridx);
        }
    }

    /*
     * Declare this object instance as being referenced,
     * but not here.  The calling function must perform
     * this task.
     */
    /* (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(..., objhash); */


    /* Done running @c @b \<init\> method, so quit */
    return(objhash);

} /* END of object_instance_new() */


/*!
 * @brief Finalize an object instance before deletion.
 *
 * This invokes an object's finalize() method, if declared,
 * on the specified thread, or on the system thread if
 * @link #jvm_thread_index_null jvm_thread_index_null@endlink
 * thread given.
 *
 *
 * @param  objhash   Object hash of object to finalize.
 *
 * @param  thridx    Thread table index of thread to run finalize() on.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @bug HARMONY-6-object.c-1001 During the invocation of
 *      @link #object_run_method() object_run_method()@endlink,
 *      a @b SIGSEGV occurs, seemingly on a valid Code_attribute.
 *      Has that somehow been freed due to @link #GC_RUN()
        GC_RUN@endlink during the @link #class_shutdown_1()
        class_shutdown_1()@endlink just recently?  Or is there
 *      a wild pointer somewhere?
 *
 */
rvoid object_instance_finalize(jvm_object_hash objhash,
                               jvm_thread_index thridx)
{
    ARCH_FUNCTION_NAME(object_instance_finalize);

    jvm_class_index clsidx = OBJECT_OBJECT_LINKAGE(objhash)->clsidx;

    jvm_method_index mthidx =
        method_find_by_prchar(clsidx,
                              JVMCFG_FINALIZE_OBJECT_METHOD,
                              JVMCFG_FINALIZE_OBJECT_PARMS);

    /* Done if no finalize() method to invoke */
    if (jvm_method_index_bad == mthidx)
    {
        return;
    }

    /*If null thread index,run on GC thread-- This is @e very unlikely*/
    if (jvm_thread_index_null == thridx)
    {
        thridx = JVMCFG_GC_THREAD;
    }

    object_run_method(clsidx,
                      JVMCFG_FINALIZE_OBJECT_METHOD,
                      JVMCFG_FINALIZE_OBJECT_PARMS,
                      thridx);

} /* END of object_instance_finalize() */


/*!
 * @brief Check whether or not a class file data area is used
 * by at least one other object besides this one.
 *
 * If a @link #jvm_table_linkage.pcfs jvm_table_linkage.pcfs@endlink
 *  pointer is @link #rnull rnull@endlink, that means that that object
 * has probably already run this function from object_instance_delete(),
 * having found that at least one other object was using this class
 * data.  After all other objects have cleared out that pointer,
 * there will only be one reference, and that is the one from this
 * object.  Upon return to object_instance_delete(), call
 * classfile_unload_classdata() and clear the pointer to
 * @link #rnull rnull@endlink, thus completing the unload process
 * for the class file data area and all its object slot pointers.
 *
 *
 * @param  objhash   Object hash of object to unload its class data
 *
 *
 * @returns @link #rtrue rtrue@endlink if another object is using
 *          this class file, otherwise @link #rfalse rfalse@endlink.
 *
 */ 
rboolean object_locate_pcfs(jvm_object_hash objhash)
{
    ARCH_FUNCTION_NAME(object_locate_pcfs);

    jvm_object_hash objhashSCAN;

    ClassFile *pcfs = OBJECT_OBJECT_LINKAGE(objhash)->pcfs;

    /*
     * With @link #rnull rnull@endlink class file pointer, not only
     * is this class not using a class file (such as the
     * @link #jvm_class_index_null jvm_class_index_null@endlink class),
     * but it is not known if another class actually @e is using one
     * because there is nothing to compare against.  Therefore, cannot
     * unload a non-existent class and cannot free an
     * @link #rnull rnull@endlink pointer.
     */
    if (rnull == pcfs)
    {
        return(rtrue);
    }

    /* Scan object table for other classes using this class file */
    for (objhashSCAN = jvm_object_hash_null;
         objhashSCAN < JVMCFG_MAX_OBJECTS;
         objhashSCAN++)
    {
        /* Already know this one is in use due to calling sequence */
        if (objhash == objhashSCAN)
        {
            continue;
        }

        /* Skip empty slots */
        if (!(OBJECT(objhash).status & OBJECT_STATUS_INUSE))
        {
            continue;
        }

        /* Check if this object has already cleared it or not */
        ClassFile *pcfsSCAN = OBJECT_OBJECT_LINKAGE(objhashSCAN)->pcfs;

        if (rnull == pcfsSCAN)
        {
            continue;
        }

        /*
         * If not already cleared, check if this object uses the
         * same class file data area as requested object.
         */
        if (pcfs == pcfsSCAN)
        {
            return(rtrue);
        }
    }

    return(rfalse);

} /* END of object_locate_pcfs() */

/*!
 * @brief Un-reserve a slot from the object area.
 *
 * This is the reverse of the process of object_instance_new() above.
 * Only tear down the heap allocations and mark the slot as empty.
 * Leave the rest of the data in place for post-mortem.  When the slot
 * gets allocated again, any zeroing out of values will just get
 * overwritten again, so don't bother.
 *
 * The object_instance_finalize() function MUST be run before
 * calling this function!
 *
 *
 * @param    objhash  Object hash value of allocation.
 *
 * @param    rmref    @link #rtrue rtrue@endlink if @e objhash
 *                    references should be removed,
 *                    which is NOT SO during JVM shutdown.
 *                    Regardless of this value, @e class references
 *                    are always removed.
 *
 *
 * @returns Same as input if slot was freed, else
 *          @link #jvm_object_hash_null jvm_object_hash_null@endlink.
 *
 */

jvm_object_hash object_instance_delete(jvm_object_hash objhash,
                                       rboolean        rmref)
{
    ARCH_FUNCTION_NAME(object_instance_delete);

    /*!
     * @todo  HARMONY-6-jvm-object.c-2 Should the
     *        @link #jvm_object_hash_null jvm_object_hash_null@endlink
     *        object be undeleteable?
     */
#if 0
    if (jvm_object_hash_null == objhash)
    { 
        return(jvm_object_hash_null);
    }
#endif

    if (OBJECT(objhash).status & OBJECT_STATUS_INUSE)
    {
        /*!
         * @todo  HARMONY-6-jvm-object.c-3 Is the
         *        java.lang.Object.finalize() method (or
         *        its subclass) invoked by the actual Java code
         *        when an object reference is removed?  Is this a
         *        GC function?  Where should this method be called
         *        when tearing down this object?
         */

        /* Deallocate slot, report to caller */
        if ((OBJECT_STATUS_ARRAY    & OBJECT(objhash).status) ||
            (OBJECT_STATUS_SUBARRAY & OBJECT(objhash).status))
        {
            if ((rtrue == rmref) &&
                (rnull != OBJECT(objhash).arraydata))
            {
                rint i;

                if (1 < OBJECT(objhash).arraydims)
                {
                    for (i = 0; i < OBJECT(objhash).arraylength; i++)
                    {
                        (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(
                                    objhash,
                                    ((jvm_object_hash *)
                                     OBJECT(objhash).arraydata)[i]);
                    }
                }
                else
                if (1 == OBJECT(objhash).arraydims)
                {
                    if (BASETYPE_CHAR_L ==
                        OBJECT(objhash).arraybasetype)
                    {
                        for (i = 0;
                             i < OBJECT(objhash).arraylength;
                             i++)
                        {
                            (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(
                                        objhash,
                                        ((jvm_object_hash *)
                                     OBJECT(objhash).arraydata)[i]);
                        }
                    }
                    else
                    {
                        /*
                         * 1-dimensional primative arrays store data
                         * directly in *arraydata, so just do
                         * HEAP_FREE_DATA() for cleanup.
                         */
                    }
                }
                else
                if (0 == OBJECT(objhash).arraydims)
                {
                    /*
                     * Either bad dimensions or should not be marked
                     * as array
                     */
                    exit_throw_exception(EXIT_JVM_OBJECT,
                                 JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
                }
            }
        }

        /* OBJECT(objhash).arraylength = 0; */

        if (rnull != OBJECT(objhash).arraydata)
        {
            HEAP_FREE_DATA(OBJECT(objhash).arraydata);
        }
        /* OBJECT(objhash).arraydata     =  (rvoid *) rnull; */

        /* OBJECT(objhash).mlock_count   = 0; */
        /* OBJECT(objhash).mlock_thridx  = jvm_thread_index_null; */
        if (jvm_object_hash_null != OBJECT(objhash).objhash_superclass)
        {
            if (rtrue == rmref)
            {
                (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(
                            objhash,
                            OBJECT(objhash).objhash_superclass);
            }
        }
        /* OBJECT(objhash).objhash_superclass = jvm_object_hash_null; */
        /* OBJECT(objhash).access_flags  = LOCAL_ACC_EMPTY; */

        (rvoid) GC_CLASS_RMREF_FROM_OBJECT(
                    objhash,
                    OBJECT(objhash).table_linkage.clsidx);

        /* Unload class file if no other references to it */
        if (rfalse == object_locate_pcfs(objhash))
        {
            classfile_unloadclassdata(OBJECT(objhash)
                                        .table_linkage.pcfs);
        }

        /* Disables @link jvm/src/linkage.h linkage.h@endlink
         * macros, specifically:
         *
         * CLASS_OBJECT_LINKAGE(),
         * OBJECT_CLASS_LINKAGE(),
         * THREAD_OBJECT_LINKAGE(),
         * OBJECT_THREAD_LINKAGE(),
         * OBJECT_OBJECT_LINKAGE()
         */
        /* OBJECT(objhash).table_linkage.pcfs   = (ClassFile *) rnull;*/
        /* OBJECT(objhash).table_linkage.clsidx =jvm_class_index_null;*/
        /* OBJECT(objhash).table_linkage.thridx=jvm_thread_index_null;*/

        if (rnull != OBJECT(objhash).object_instance_field_data)
        {
            /* Remove object instance field reference markings */
            u2 noifl = CLASS(OBJECT_OBJECT_LINKAGE(objhash)->clsidx)
                         .num_object_instance_field_lookups;
            jvm_field_lookup_index oiflidx;
            for (oiflidx = 0; oiflidx < noifl; oiflidx++)
            {
                (rvoid) GC_OBJECT_FIELD_RMREF(objhash, oiflidx);
            }

            HEAP_FREE_DATA(OBJECT(objhash).object_instance_field_data);
        }
        /* OBJECT(objhash).object_instance_field_data =
                                                    (jvalue *) rnull; */

        /* Finalize garbage collection status of this object instance */
        (rvoid) GC_OBJECT_DELETE(objhash);

        /* OBJECT(objhash).status = OBJECT_STATUS_EMPTY; */
        OBJECT(objhash).status &= ~OBJECT_STATUS_INUSE;

        return(objhash);
    }
    else
    {
        /* Error-- slot was already free */
        return(jvm_object_hash_null);
    }

} /* END of object_instance_delete() */


/*!
 * @brief Shut down the object area of the JVM model after JVM execution
 *
 * See also comments on why there are two stages of class shutdown,
 * class_shutdown_1() and class_shutdown_2().
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid object_shutdown()
{
    ARCH_FUNCTION_NAME(object_shutdown);

    jvm_object_hash objhash;

    for (objhash = jvm_object_hash_null;
         objhash < JVMCFG_MAX_OBJECTS;
         objhash++)
    {
        if (OBJECT(objhash).status & OBJECT_STATUS_INUSE)
        {
            (rvoid) GC_CLASS_RMREF_FROM_OBJECT(
                        OBJECT_OBJECT_LINKAGE(objhash)->clsidx,
                        objhash);

            (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(
                        objhash,
                        OBJECT(objhash).objhash_superclass);

            (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(jvm_object_hash_null,
                                                objhash);
        }
    }

    /*! @note This may result in a @e large garbage collection */
    GC_RUN(rfalse);

    /* Go clear out any remaining objects */
    for (objhash = jvm_object_hash_null;
         objhash < JVMCFG_MAX_OBJECTS;
         objhash++)
    {
        if (OBJECT(objhash).status & OBJECT_STATUS_INUSE)
        {
            if (!(OBJECT(objhash).status & OBJECT_STATUS_NULL))
            {
                object_instance_finalize(objhash, JVMCFG_SYSTEM_THREAD);
            }
            (rvoid) object_instance_delete(objhash, rfalse);
        }
    }

    /* Declare this module uninitialized */
    jvm_object_initialized = rfalse;

    return;

} /* END of object_shutdown() */


/* EOF */
