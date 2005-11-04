#ifndef _cfmacros_h_included_
#define _cfmacros_h_included_

/*!
 * @file cfmacros.h
 *
 * @brief Macros for navigating class file structures in a ClassFile.
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

ARCH_HEADER_COPYRIGHT_APACHE(cfmacros, h,
"$URL$",
"$Id$");

#include "heap.h"

/*!
 * @name Typed constant_pool pointers
 *
 * @brief Convert generic @c @b constant_pool[] entry into a
 * @link #CONSTANT_Class_info CONSTANT_xxxxx_info@endlink typed pointer,
 * stripping off the generic prefix bytes.
 *
 * Adjust a generic @c @b constant_pool entry (cp_info_dup *)
 * into its corresponding
 * @link #CONSTANT_Class_info CONSTANT_xxxxx_info@endlink typed pointer
 * by changing the pointer to point not to the beginning of the
 * (cp_info_dup) structure, but to its @p @b cp member, which is where
 * the data actually begins.  This is  useful for argument passing
 * when the whole (cp_info_dup *) is available.  The original
 * adjustment was made in the first place to support native member
 * accesses on real machine architectures that are picky about
 * multi-byte accesses that are not aligned to addresses of the
 * same size.
 *
 * @see ARCH_ODD4_ADDRESS_SIGSEGV
 *
 * @see ARCH_ODD2_ADDRESS_SIGSEGV
 *
 *
 * @param pcpinfodup Pointer to a @c @b constant_pool entry, typically
 *                   @c @b &pcfs->constant_pool[n]
 *
 *
 * @returns Pointer to the @c @b ->cp member, typed as
 *          @link #CONSTANT_Class_info CONSTANT_xxxxx_info@endlink
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PTR_THIS_CP_Class(pcpinfodup) \
    ((CONSTANT_Class_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Fieldref(pcpinfodup) \
    ((CONSTANT_Fieldref_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Methodref(pcpinfodup) \
    ((CONSTANT_Methodref_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_InterfaceMethodref(pcpinfodup) \
    ((CONSTANT_InterfaceMethodref_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_String(pcpinfodup) \
    ((CONSTANT_String_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Integer(pcpinfodup) \
    ((CONSTANT_Integer_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Float(pcpinfodup) \
    ((CONSTANT_Float_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Long(pcpinfodup) \
    ((CONSTANT_Long_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Double(pcpinfodup) \
    ((CONSTANT_Double_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_NameAndType(pcpinfodup) \
    ((CONSTANT_NameAndType_info *) &(pcpinfodup)->cp)

#define PTR_THIS_CP_Utf8(pcpinfodup) \
    ((CONSTANT_Utf8_info *) &(pcpinfodup)->cp)

/*@} */ /* End of grouped definitions */


/*!
 * @name General navigation and parsing macros.
 *
 * @param  pcfs   ClassFile pointer to a fully parsed class data area
 *
 * @param  cpidx  Index into its @c @b constant_pool[] array.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Report the (cp_info *) of the address of the
 * class file @p @b pcfs @c @b constant_pool entry at this
 * index @p @b cpidx.
 *
 *
 * @returns(cp_info *) to a @c @b constant_pool[cpidx]
 *
 */
#define PTR_CP_ENTRY(pcfs, cpidx) (&(pcfs->constant_pool[cpidx])->cp)


/*!
 * @brief Report the (u1) tag value of the class file @p @b pcf
 * @c @b constant_pool entry at this index @p @b cpidx.
 *
 *
 * @returns (u1) tag value of entry at @c @b constant_pool[cpidx]
 *
 */
#define CP_TAG(pcfs, cpidx)  ((PTR_CP_ENTRY(pcfs, cpidx))->tag)


/*!
 * @brief Point into a (cp_info) data structure and return
 * start of @link cp_info#info info@endlink field as a (u1 *).
 *
 *
 * @returns address or contents of something in a @c @b constant_pool[]
 *          entry, see above description.
 */
#define PTR_CP_INFO(pcfs, cpidx) \
    ((u1 *) (&PTR_CP_ENTRY(pcfs, cpidx)->info))

/*@} */ /* End of grouped definitions */


/*!
 * @name Pointer casting for very common constructions.
 *
 * @brief Report the (cp_info *) of a PTR_CP_ENTRY(),
 * but cast as a pointer to one of the following
 * data types.
 *
 * The PTR_CP_ENTRY_TYPE() macro may  used to choose any
 * arbitrary type at all.  
 *
 * If @p @b type is a
 * (@link #CONSTANT_Class_info CONSTANT_xxxxx_info@endlink *), the
 * result may be referenced DIRECTLY BY THE VM
 * SPEC.  These types are:
 *
 *
 * <ul>
 * <li>
 *     PTR_CP_ENTRY_TYPE()     (any_desired_type *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_CLASS()    (CONSTANT_Class_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_INTEGER()  (CONSTANT_Integer_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_FLOAT()    (CONSTANT_Float_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_LONG()     (CONSTANT_Long_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_DOUBLE()   (CONSTANT_Double_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_STRING()   (CONSTANT_String_info *)
 * </li>
 *
 * <li>
 *     PTR_CP_ENTRY_UTF8()     (CONSTANT_Utf8_info *)
 * </li>
 * </ul>
 *
 *
 * @param  type   typedef definition for pointer cast of result
 *
 * @param  pcfs   ClassFile pointer to a fully parsed class data area
 *
 * @param  cpidx  Index into its @c @b constant_pool[] array.
 *
 *
 * @returns Pointer to a @c @b constant_pool[cpidx], variously typed as
 *          above.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define PTR_CP_ENTRY_TYPE(type, pcfs, cpidx)  \
   ((type *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_CLASS(pcfs, cpidx)  \
   ((CONSTANT_Class_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_INTEGER(pcfs, cpidx)  \
   ((CONSTANT_Integer_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_FLOAT(pcfs, cpidx)  \
   ((CONSTANT_Float_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_LONG(pcfs, cpidx)  \
   ((CONSTANT_Long_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_DOUBLE(pcfs, cpidx)  \
   ((CONSTANT_Double_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_STRING(pcfs, cpidx)  \
   ((CONSTANT_String_info *) PTR_CP_ENTRY(pcfs, cpidx))

#define PTR_CP_ENTRY_UTF8(pcfs, cpidx)  \
   ((CONSTANT_Utf8_info *) PTR_CP_ENTRY(pcfs, cpidx))

/*@} */ /* End of grouped definitions */


/*******************************************************************/
/*!
 * @name UTF string manipulation macros.
 *
 * @brief Probe CONSTANT_Utf8_info @c @b constant_pool entries for
 * field data and addresses.
 *
 * Return information about (CONSTANT_Utf8_info) entry, namely:
 *
 * <ul>
 *
 * <li>
 * Pointing to @c @b constant_pool entry of THIS @p @b pcfs
 *       and @p @b cpidx, being a (CONSTANT_Utf8_info) string:
 * <ul> 
 * <li>
 *     PTR_CP_THIS()            Pointer to whole (CONSTANT_Utf8_info)
 *                              CP entry cast as (CONSTANT_Utf8_info *),
 *                              namely to the beginning of the
 *                              structure.
 * </li>
 *
 * <li>
 *     CP_THIS_STRLEN()         @p @b length field of
 *                               (CONSTANT_Utf8_info) CP entry, as (u2).
 * </li>
 *
 * <li>
 *     PTR_CP_THIS_STRNAME()    Pointer to @p @b bytes area of
 *                              (CONSTANT_Utf8_info) CP entry, cast
 *                              as (rchar *).
 * </li></ul></li>
 *
 *
 * <li>
 * ONE level of indirection, namely, point to UTF string info
 *               AT THE ENTRY POINTED TO by this @p @b pcfs and
 *               @p @b cpidx AS FOUND IN @p @b strname_idx FIELD:
 * <ul>
 *
 * <li>
 *     PTR_CP1_NAME()          Pointer to whole (CONSTANT_Utf8_info)
 *                             CP entry referenced by index in
 *                             @p @b strname_idx field, cast as
 *                             (CONSTANT_Utf8_info *) and pointing
 *                             to the beginning of that structure.
 * </li>
 *
 * <li>
 *     CP1_NAME_STRLEN()       @p @b length field of
 *                             (CONSTANT_Utf8_info) field as
 *                             indirectly referenced by
 *                             PTR_CP1_NAME() macro, as (u2).
 * </li>
 *
 * <li>
 *     PTR_CP1_NAME_STRNAME()  Pointer to @p @b bytes area of
 *                             (CONSTANT_Utf8_info) entry as
 *                             indirectly referenced by
 *                             PTR_CP1_NAME() macro, as (u1 *).
 * </li></ul></li>
 *
 *
 * <li>
 * ONE level of indirection, specifically using type
 *     (CONSTANT_Class_info) as the @c @b constant_pool entry type.
 *
 * <ul>
 *
 * <li>
 *     PTR_CP1_CLASS_NAME()    Pointer to whole (CONSTANT_Utf8_info)
 *                               CP entry referenced by index that
 *                               represents a (CONSTANT_Class_info)
 *                               slot entry.  Cast as type
 *                               (CONSTANT_Utf8_info *) and pointing
 *                               to the beginning of that structure.
 * </li>
 *
 * <li>
 *     CP1_CLASS_NAME_STRLEN() @p @b length field of string name of this
 *                               (CONSTANT_Class_info) entry's UTF
 *                               name string, as (u2).
 * </li>
 *
 * <li>
 *     PTR_CP1_CLASS_NAME_STRNAME() Pointer to @p @b bytes area of this
 *                               (CONSTANT_Class_info) entry's UTF
 *                               name string, as (u1 *).
 * </li></ul></li>
 *
 * <li>
 * TWO levels of indirection, namely, this @p @b pcfs and @p @b cpidx
 *                point to a @c @b (type) @c @b constant_pool entry
 *                whose @p @b strname_idx field contains a
 *                @c @b constant_pool index to a (CONSTANT_Class_info)
 *                entry. Obtain info about the (CONSTANT_Utf8_info)
 *                entry which is named by that (CONSTANT_Class_info)
 *                entry's @p @b name_index field, that is, the name
 *                of the class:
 * <ul> 
 *
 * <li>
 *     PTR_CP2_CLASS_NAME()          Pointer to whole
 *                                   (CONSTANT_Utf8_info) CP entry
 *                                   referenced by index in
 *                                   @p @b strname_idx field of @b type
 *                                   CP entry (that is,
                                     @link #CONSTANT_Class_info
                                     CONSTANT_xxxxx_info@endlink),
 *                                   which references the class name
 *                                   of that CP entry, as referenced by
 *                                   the @p @b strname_idx CP entry of
 *                                   @p @b cpidx case as
 *                                   (CONSTANT_Utf8_info *) and
 *                                   pointing to the beginning of
 *                                   that structure.
 * </li>
 *
 * <li>
 *     CP2_CLASS_NAME_STRLEN()       @p @b length field of
 *                                   (CONSTANT_Utf8_info) field as
 *                                   indirectly referenced by
 *                                   PTR_CP2_CLASS_NAME() macro,
 *                                   as (u2).
 * </li>
 *
 * <li>
 *     PTR_CP2_CLASS_NAME_STRNAME()  Pointer to @p @b bytes area of
 *                                   (CONSTANT_Utf8_info) entry as
 *                                   indirectly referenced by
 *                                   PTR_CP2_CLASS_NAME() macro,
 *                                   as (u1 *).
 * </li></ul></li></ul>
 *
 *
 *
 * @param  type        typedef definition for pointer cast of result
 *
 * @param  pcfs        ClassFile pointer to a fully parsed class
 *                       data area
 *
 * @param  cpidx       Index into its @c @b constant_pool[] array.
 *
 * @param  strname_idx Field name of indirect @c @b constant_pool
 *                       pointed to by @p @b pcfs and @p @b cpidx.
 *                       The index found here is the index containing
 *                       the UTF string. (@c @b CP1_xxx and
 *                       @c @b CP2_xxx macros only)
 *
 *
 * @returns (cp_info *) to a @c @b constant_pool[cpidx]
 *
 */

/*@{ */ /* Begin grouped definitions */

/*******************************************************************
 *
 * Pointing to @c @b constant_pool entry of THIS @p @b pcfs and
 * @p @b cpidx, being a (CONSTANT_Utf8_info) string.
 *
 */
#define PTR_CP_THIS(pcfs, cpidx)  \
    PTR_CP_ENTRY_TYPE(CONSTANT_Utf8_info, pcfs, cpidx)

#define CP_THIS_STRLEN(pcfs, cpidx)  \
    PTR_CP_ENTRY_TYPE(CONSTANT_Utf8_info, pcfs, cpidx)->length

#define PTR_CP_THIS_STRNAME(pcfs, cpidx)  \
    ((rchar *) &PTR_CP_ENTRY_TYPE(CONSTANT_Utf8_info, pcfs, cpidx) \
                   ->bytes[0])


/*******************************************************************
 *
 * --- ONE level of indirection, namely, point to UTF string info
 *               AT THE ENTRY POINTED TO by this @p @b pcfs and
 *               @p @b cpidx AS FOUND IN FIELD @p @b strname_idx:
 */
#define PTR_CP1_NAME(type, pcfs, cpidx, strname_idx) \
   PTR_CP_THIS(pcfs, (PTR_CP_ENTRY_TYPE(type, pcfs,cpidx)->strname_idx))

#define CP1_NAME_STRLEN(type, pcfs, cpidx, strname_idx) \
 CP_THIS_STRLEN(pcfs, (PTR_CP_ENTRY_TYPE(type,pcfs,cpidx)->strname_idx))

#define PTR_CP1_NAME_STRNAME(type, pcfs, cpidx, strname_idx)       \
   PTR_CP_THIS_STRNAME(pcfs, (PTR_CP_ENTRY_TYPE(type, pcfs, cpidx) \
                                ->strname_idx))

/*******************************************************************
 *
 * --- ONE level of indirection, but specifically for manipulating
 *               (CONSTANT_Class_info) slot in @c @b constant_pool.
 *               @p @b pcfs and @c @b cpidx refer to such a class info
 *               slot.
 */
#define PTR_CP1_CLASS_NAME(pcfs, cpidx) \
    PTR_CP1_NAME(CONSTANT_Class_info, pcfs, cpidx, name_index)


#define CP1_CLASS_NAME_STRLEN(pcfs, cpidx) \
    CP1_NAME_STRLEN(CONSTANT_Class_info, pcfs, cpidx, name_index)

#define PTR_CP1_CLASS_NAME_STRNAME(pcfs, cpidx) \
    PTR_CP1_NAME_STRNAME(CONSTANT_Class_info, pcfs, cpidx, name_index)

/*******************************************************************
 *
 * --- TWO levels of indirection, namely, point to UTF string info
 *                AT THE ENTRY POINTED TO by this @p @b pcfs and
 *                @p @b cpidx AS FOUND IN FIELD @p @b strname_idx:
 */

#define PTR_CP2_CLASS_NAME(type, pcfs, cpidx, strname_idx)       \
   PTR_CP_THIS(pcfs,                                             \
              (PTR_CP_ENTRY_TYPE(CONSTANT_Class_info,            \
                           pcfs,                                 \
                           (PTR_CP_ENTRY_TYPE(type, pcfs, cpidx) \
                                ->strname_idx))                  \
                   ->name_index))

#define CP2_CLASS_NAME_STRLEN(type, pcfs, cpidx, strname_idx)        \
    CP_THIS_STRLEN(pcfs,                                             \
                  (PTR_CP_ENTRY_TYPE(CONSTANT_Class_info,            \
                               pcfs,                                 \
                               (PTR_CP_ENTRY_TYPE(type, pcfs, cpidx) \
                                    ->strname_idx))                  \
                       ->name_index))

#define PTR_CP2_CLASS_NAME_STRNAME(type, pcfs, cpidx, strname_idx)     \
   PTR_CP_THIS_STRNAME(pcfs,                                           \
                      (PTR_CP_ENTRY_TYPE(CONSTANT_Class_info,          \
                                 pcfs,                                 \
                                 (PTR_CP_ENTRY_TYPE(type, pcfs, cpidx) \
                                      ->strname_idx))                  \
                           ->name_index))

/*@} */ /* End of grouped definitions */

/*******************************************************************/

/*!
 * @name Non-aligned multi-byte field access
 *
 * @brief Cast arbitrary pointers to type (u2 *) and (u4 *) for
 * referencing items in class file data image.  These pointers
 * DO NOT need to be 2- or 4-byte aligned.
 *
 *
 * @param  ptr    Name of pointer variable to receive (cast) value
 *
 * @param  loc    Name of pointer variable to be (recast) as either
 *                  (u2 *) or (u4 *).
 *
 *
 * @returns @p @b loc receives new value from (recast *) @p @b loc
 *
 */

/*@{ */ /* Begin grouped definitions */

#define MAKE_PU2(ptr, loc) ptr = (u2 *)    (loc)
#define MAKE_PU4(ptr, loc) ptr = (u4 *)    (loc)

/*@} */ /* End of grouped definitions */


/*******************************************************************/
/*!
 * @name Responses to generic failure types
 *
 * @brief Generic return type for error test, including either return
 * value of a typed @link #rnull rnull@endlink pointer
 * (@b GENERIC_FAILUREx_PTR() only), a generic value
 * (@b GENERIC_FAILUREx_VALUE() only), or throw an error
 * (@b GENERIC_FAILUREx_THROWERROR() only), plus a error message.
 * If @p @b expr is @link #rtrue rtrue@endlink, return the return value
 * or throw the error (depending on the specific macro), else
 * continue with inline code.
 *
 * The versions @b GENERAL_FAILURE1_xxx() and @b GENERAL_FAILURE2_xxx()
 * also may pass a formatting parameter for @p @b msg to format into the
 * output string.
 *
 * The following is a grand union of the parameters for @e all of these
 * macros.  A given macro may or may not use some of them:
 *
 *
 * @param  expr     Any logical expression that returns
 *                  @link #rtrue rtrue@endlink or
 *                  @link #rfalse rfalse@endlink.
 *
 * @param  dml      Debug message level for amount of output verbosity
 *
 * @param  fn       Name of function invoking this macro
 *
 * @param  msg      Any text message to display to stderr if @p @b expr
 *                    is @link #rtrue rtrue@endlink.
 *                    For @b GENERIC_FAILURE{1|2}(), this
 *                    should also contain 1 or 2 formatting items also.
 *
 * @param  parm1    (for @b GENERIC_FAILURE{1|2}_xxx() only)  First
 *                    parameter to format into @p @b msg.
 *
 * @param  parm2    (for @b GENERIC_FAILURE2_xxx() only)  Second
 *                    parameter to format into @p @b msg.
 *
 * @param  rettype  Function return TYPE (not including '*' modifier)
 *                    to be used instead of (rvoid *).  Used by
 *                    @b GENERIC_FAILUREx_PTR() only.
 *
 * @param  retval   Function return VALUE, used by
 *                    @b GENERIC_FAILUREx_VALUE() and
 *                    @b GENERIC_FAILUREx_THROWERROR() .
 *
 * @param  retclass Function error class to throw, used by
 *                   GENERIC_FAILUREx_THROWERROR() only.
 *
 * @param  heap1ptr Data heap block to free, or
 *         @link #rnull rnull@endlink if not needed.
 *
 * @param  heap2ptr Data heap block to free, or
 *         @link #rnull rnull@endlink if not needed.
 *
 *
 * @returns If @p @b expr is @link #rtrue rtrue@endlink,
 *          return a typed @link #rnull rnull@endlink pointer to the
 *          calling function, cast as <b><code>(rettype *)</code></b>,
 *          else continue with inline code.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define GENERIC_FAILURE_PTR(expr, dml, fn, msg, rettype,               \
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg);                                       \
        return((rettype *) rnull);                                     \
    }

#define GENERIC_FAILURE1_PTR(expr, dml, fn, msg, parm1, rettype,       \
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1);                                \
        return((rettype *) rnull);                                     \
    }

#define GENERIC_FAILURE2_PTR(expr, dml, fn, msg, parm1, parm2, rettype,\
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1, parm2);                         \
        return((rettype *) rnull);                                     \
    }

#define GENERIC_FAILURE_VALUE(expr, dml, fn, msg, retval, heap1ptr,    \
                                                             heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg);                                       \
        return(retval);                                                \
    }

#define GENERIC_FAILURE1_VALUE(expr, dml, fn, msg, parm1, retval,      \
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1);                                \
        return(retval);                                                \
    }

#define GENERIC_FAILURE2_VALUE(expr, dml, fn, msg, parm1, parm2,retval,\
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1, parm2);                         \
        return(retval);                                                \
    }

#define GENERIC_FAILURE_THROWERROR(expr, dml, fn, msg, retval,retclass,\
                                                   heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg);                                       \
        exit_throw_exception(retval, retclass);                        \
/*NOTREACHED*/                                                         \
    }

#define GENERIC_FAILURE1_THROWERROR(expr, dml, fn, msg, parm1, retval, \
                                         retclass, heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1);                                \
        exit_throw_exception(retval, retclass);                        \
/*NOTREACHED*/                                                         \
    }

#define GENERIC_FAILURE2_THROWERROR(expr, dml, fn, msg, parm1, parm2,  \
                                 retval, retclass, heap1ptr, heap2ptr) \
    if (expr)                                                          \
    {                                                                  \
        HEAP_FREE_DATA((rvoid *) heap1ptr); /* Ignored if rnull */     \
        HEAP_FREE_DATA((rvoid *) heap2ptr); /* Ignored if rnull */     \
        sysDbgMsg(dml, fn, msg, parm1, parm2);                         \
        exit_throw_exception(retval, retclass);                        \
/*NOTREACHED*/                                                         \
    }

/*@} */ /* End of grouped definitions */

/*******************************************************************/
/*!
 * @name Support for failed system calls
 *
 * @brief Recover from error when loading and parsing class file
 * by returning an @link #rnull rnull@endlink pointer, properly cast.
 *
 * <ul>
 * <li>
 *     LOAD_SYSCALL_FAILURE()        @link #rnull rnull@endlink pointer
 *                                   as (ClassFile *)
 * </li>
 *
 * <li>
 *     LOAD_SYSCALL_FAILURE_ATTRIB() @link #rnull rnull@endlink pointer
 *                                   as (u1 *)
 * </li>
 * </ul>
 *
 *
 * @param  expr     Any logical expression that returns
 *                  @link #rtrue rtrue@endlink or
 *                  @link #rfalse rfalse@endlink.
 *
 * @param  msg      Any text message to display to stderr if @p @b expr
 *                    is @link #rtrue rtrue@endlink.
 *
 * @param  heap1ptr Method heap block to free, or
 *                  @link #rnull rnull@endlink if not needed.
 *
 * @param  heap2ptr Method heap block to free, or
 *                  @link #rnull rnull@endlink if not needed.
 *
 *
 * @returns If @p @b expr is @link #rtrue rtrue@endlink, return
 *          an @link #rnull rnull@endlink pointer to the
 *          calling function, cast as shown above, else continue
 *          with inline code.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define LOAD_SYSCALL_FAILURE(expr, msg, heap1ptr, heap2ptr) \
    if (expr)                                               \
    {                                                       \
        HEAP_FREE_METHOD((rvoid *) heap1ptr);               \
        HEAP_FREE_METHOD((rvoid *) heap2ptr);               \
    }                                                       \
    GENERIC_FAILURE_PTR(expr,                               \
                        DMLMIN,                             \
                        arch_function_name,                 \
                        msg,                                \
                        ClassFile,                          \
                        rnull,                              \
                        rnull); /* Extra ; */

#define LOAD_SYSCALL_FAILURE_ATTRIB(expr, msg, heap1ptr, heap2ptr) \
    if (expr)                                                      \
    {                                                              \
        HEAP_FREE_METHOD((rvoid *) heap1ptr);                      \
        HEAP_FREE_METHOD((rvoid *) heap2ptr);                      \
    }                                                              \
    GENERIC_FAILURE_PTR(expr,                                      \
                        DMLMIN,                                    \
                        arch_function_name,                        \
                        msg,                                       \
                        u1,                                        \
                        rnull,                                     \
                        rnull); /* Extra ; */

/*@} */ /* End of grouped definitions */


/*******************************************************************/
/*!
 *
 * @name Architecture-dependent byte swapping macros.
 *
 * @brief Inline the logic to swap bytes on multi-byte elements.
 *
 * This is only meaningful on
 * @link #ARCH_LITTLE_ENDIAN ARCH_LITTLE_ENDIAN@endlink architectures.
 * (Notice that if @p @b member does not match the pointer's
 * type, there @e will be a compile warning or error.)
 *
 *
 * @param  type   Which @c @b constant_pool type, CONSTANT_xxx_info,
 *                  that will be used as (type *) to reference members.
 * @param  member Member of (CONSTANT_xxx_info *) to be manipulated.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @attention The following variables area must be defined for the
 *            CP_ITEM_SWAP_U2() and CP_ITEM_SWAP_U2 macros to work:
 * @verbatim
      u2 *pcpu2;
      u4 *pcpu4;
   @endverbatim
 *
 *
 * @todo  HARMONY-6-jvm-cfmacros.h-1 There needs to be logic
 *        implemented that can determine whether or not a particular
 *        field has been byte swapped in case a constant pool
 *        reference is made to an entry that was previously byte
 *        swapped.  This has NOT been done for this original
 *        implementation because it is on a Solaris 9 machine,
 *        which is big endian.
 *
 */

/*@{ */ /* Begin grouped definitions */

#ifdef ARCH_LITTLE_ENDIAN
#define CP_ITEM_SWAP_U2(type, member) \
    pcpu2 = &(((type *) &pcpd->cp)->member); \
    MACHINE_JSHORT_SWAP_PTR(pcpu2)

#define CP_ITEM_SWAP_U4(type, member) \
    pcpu4 = &(((type *) &pcpd->cp)->member); \
    MACHINE_JINT_SWAP_PTR(pcpu4)

#else
/* ARCH_BIG_ENDIAN: big endian architectures have nothing to do */

#define CP_ITEM_SWAP_U2(type, member)
#define CP_ITEM_SWAP_U4(type, member)

#endif
/*@} */ /* End of grouped definitions */

/*******************************************************************/
/*!
 * @name Generic prefix bytes.
 *
 * @brief Fill in empty area at start of selected structures for
 * address alignment purposes.
 *
 * Fill pattern bytes are provided for (cp_info) and (attribute_info)
 * structure alignment within (cp_info_dup) and (attribute_info_dup)
 * structures.  This assures that 2- and 4-byte alignments needed
 * for the beginning of those structures is followed (after the first
 * element, the @p @b tag and @p @b attribute_name_index elements,
 * respectively).
 *
 * Real machine architectures that have issues with non-aligned
 * multi-byte accesses do @e not like the fact that ClassFile
 * structures such as cp_info begin with a single byte field followed
 * by multi-byte fields.  In like manner, structures like attribute_info
 * have only 2- and 4-byte fields, yet may contain an odd number of
 * bytes in their @link #attribute_info info@endlink fields.
 * Due to the streaming nature of Java class files, subsequent
 * attributes may thus be misaligned, even though they may only contain
 * fields with an even number of bytes.  Thus the cp_info_dup and
 * attribute_info_dup structures were devised to suppress problems like
 * this.  When the class file is read, all structure accesses are
 * performed on blocks of allocated memory that start on 4- or 8-byte
 * address boundaries and pad the first few bytes so that all 2- and
 * 4-byte accesses are on 2- or 4-byte boundaries.
 *
 * There are three @b FILL_INFO_DUPx fill fields for padding and
 * three @b FILL_INFO_NOTUSED_Ux fill fields for assigning values
 * to @b notusedXX fields.
 *
 * @see cp_info_dup
 * @see attribute_info_dup
 * @see ARCH_ODD4_ADDRESS_SIGSEGV
 * @see ARCH_ODD2_ADDRESS_SIGSEGV
 *
 * @todo HARMONY-6-jvm-cfmacros.h-2 WATCH OUT!  When changing
 *       CP_INFO_NUM_EMPTIES or ATTRIBUTE_INFO_NUM_EMPTIES, beware
 *       of not having he right number of @c @b struct->empty[x]=FILL
 *       lines in the cp_info_dup and attribute_info_dup assignments!
 *       Could eliminate these, but they are useful for
 *       debugging.
 *
 * @todo HARMONY-6-jvm-cfmacros.h-3 4-byte unused fields are meaningful
 *       only for 64-bit implementations.
 *
 * @todo HARMONY-6-jvm-cfmacros.h-4 For 64-bit compilations, these fill
 *       patterns may need to be expanded to support 8-byte alignments
 *       for some architectures.  For 32-bit compilations, nothing needs
 *       to be done. (Verify if this is so.) Remember that the
 *       @b WORDSIZE64 configuration macro is used to
 *       distinguish this mode of compilation.  (See
 *       @link ./config.sh config.sh@endlink for details.)
 *
 */
/*@{*/
#define FILL_INFO_DUP0 0xbe
#define FILL_INFO_DUP1 0xef
#define FILL_INFO_DUP2 0x99

/*
 * Same for 1-, 2- and 4-byte @b notusedX fields.
 */
#define FILL_INFO_NOTUSED_U1 0x9a
#define FILL_INFO_NOTUSED_U2 0x9ace
#define FILL_INFO_NOTUSED_U4 0x9aceface
/*@}*/

#endif /* _cfmacros_h_included_ */

/* EOF */
