#ifndef _jrtypes_h_included_
#define _jrtypes_h_included_

/*!
 * @file jrtypes.h
 *
 * @brief Java architecture types, including those defined
 * by @c @b \<jni.h\>, plus real machine mappings of Java types.
 *
 * Convenient typedefs of both categories are also defined here.
 *
 * These definitions distinguish between Java types and real machine
 * types so as to keep the developer organized as to which is which
 * in processing scenarios.  The Java types begin with @p @b j and the
 * real machine types begin with @p @b r.
 *
 * The main exception to this is the JVM class file definitions from
 * the JVM spec, section 4, as implemented by
 * @link jvm/src/classfile.h classfile.h@endlink.  These
 * are followed without exception.  In fact, a number of common
 * type definitions are based on them.  The
 * @link #jvm_class_index jvm_XXX_index@endlink types
 * are typically either direct class file references (such as
 * @link #jvm_object_hash jvm_object_hash@endlink or
 * @link #jvm_field_index jvm_field_index@endlink) or are real machine
 * definitions that directly support JVM processing structures
 * (such as @link #jvm_class_index jvm_class_index@endlink or
 * @link #jvm_field_lookup_index jvm_field_lookup_index@endlink).
 *
 * The other common usage of a prefix is for variables.  It is not
 * related to this issue at all.  In this situation, the letter @b p
 * will typically be prefixed to pointers to/of any type in either
 * processing domain.
 *
 * The use of raw, native, 'C' language types such as @c @b int should
 * be restricted to system calls and library references such as
 * @c @b open(2) or @c @b strcmp(3), respectively-- also the command
 * line @c @b main() parameters, which get propagated into
 * @link #argv_helpmsg() argv_XXX()@endlink functions.
 * Let the compiler perform any typing and sizing, which is unlikely,
 * but keep @e all other usages to the Java @c @b jTYPEDEF and real
 * machine @c @b rTYPEDEF representations.  This single exception should
 * be obvious when it occurs, and developers should be aware that this
 * convention is used ubiquitously throughout the code.
 *
 * Although definitions used by the JNI interface are found here,
 * JNI is kept as a STRICTLY SEPARATE part of the code, and
 * @c @b \<jni.h\> is ONLY used there, namely in the @b ../include
 * area.
 *
 * @note The @e only place that JNI definitions are used is in the
 *       @c @b JniSomeJavaClassWithNativeMethods.c source file as
 *       found in its @c @b some.java.class.with.native.methods
 *       directory.  For example, the Java class
 *       @c @b java.lang.Object has its Java source file stored
 *    in @link jni/src/harmony/generic/0.0/src/java/lang/Object.java
   jni/src/vendor/product/version/src/java/lang/Object.java@endlink,
 *        with its native support found in the 'C' source file
 *      @link jni/src/harmony/generic/0.0/src/java_lang_Object.c
  jni/src/vendor/product/version/src/java_lang_Object.c@endlink.
 *        The JNI header used to access this native
 *        @c @b java.lang.Object code is found in the related
 *        @b include directory as
 *      @link jni/src/harmony/generic/0.0/include/java_lang_Object.h
  jni/src/vendor/product/version/include/java_lang_Object.h@endlink.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/jrtypes.h $ \$Id: jrtypes.h 0 09/28/2005 dlydick $
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
 * @version \$LastChangedRevision: 0 $
 *
 * @date \$LastChangedDate: 09/28/2005 $
 *
 * @author \$LastChangedBy: dlydick $
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

ARCH_COPYRIGHT_APACHE(jrtypes, h, "$URL: https://svn.apache.org/path/name/jrtypes.h $ $Id: jrtypes.h 0 09/28/2005 dlydick $");


/*!
 * @name Java architecture primative types.
 *
 * @brief Real machine implementation of Java primative types.
 */

/*@{ */ /* Begin grouped definitions */

#define JBITS            8         /**< Number of bits per byte in JVM*/

typedef   signed char    jbyte;    /**< Java @c @b (byte) */

typedef unsigned char    jboolean; /**< Java @c @b (boolean) */

typedef   signed short   jshort;   /**< Java @c @b (short) */

typedef unsigned short   jchar;    /**< Java @c @b (char) */

typedef signed int       jint;     /**< Java @c @b (int) */

#ifdef CONFIG_WORDWIDTH64

typedef signed long      jlong;    /**< Java @c @b (long) */

#else

typedef signed long long jlong;    /**< Java @c @b (long) */

#endif

typedef float            jfloat;   /**< Java @c @b (float) */

typedef double           jdouble;  /**< Java @c @b (double) */

typedef void             jvoid;    /**< Java @c @b (void)
                                    *   is @e not found in
                                    * @c @b \<jni.h\> !!!  It is used
                                    * here to be consistent with
                                    * separation  of Java vs real
                                    * machine data types.  Also defined
                                    * for our JNI purposes in @link
                                      jvm/include/jlObject.h
                                      jlObject.h@endlink
                                    */

/*@} */ /* End of grouped definitions */


/*!
 * @name Java keywords
 *
 * @brief Real machine implementation of selected Java keywords.
 */

/*@{ */ /* Begin grouped definitions */

extern const jvoid    *jnull;      /**< Java constant
                                    * @c @b null */

extern const jboolean jfalse;      /**< Java constant
                                    * @c @b false */

extern const jboolean jtrue;       /**< Java constant
                                    * @c @b true */

/*@} */ /* End of grouped definitions */


/*!
 * @name Java Native Interface definitions.
 *
 * @brief Selected JNI definitions needed by this implementation for
 * JNI interface purposes, but @e never used in the core code.
 */

/*@{ */ /* Begin grouped definitions */

#define JNI_FALSE 0                /**< Defined by \<jni.h\>
                                    * (@e never used in core code) */

#define JNI_TRUE  1                /**< Defined by \<jni.h\>
                                    * (@e never used in core code) */

/*@} */ /* End of grouped definitions */


/*!
 * @name Unsigned equivalents to Java primative types.
 *
 * @brief Convenient workarounds for unsigned typesthat are
 * really @e not in Java.
 *
 * These types are really @e faux, but are needed for internal
 * implementation convenience or for more refined semantic
 * interpretation of JVM spec fields.
 */

/*@{ */ /* Begin grouped definitions */

typedef unsigned char    jubyte;   /**< Unsigned equivalent of
                                    * Java (byte) */

typedef unsigned short   jushort;  /**< Unsigned equivalent of
                                    * Java  (short) */

typedef unsigned int     juint;    /**< Unsigned equivalent of
                                    * Java (int) */

#ifdef CONFIG_WORDWIDTH64

typedef unsigned long    julong;   /**< Unsigned equivalent of
                                    * Java (long) */

#else

typedef unsigned long long julong; /**< Unsigned equivalent of
                                    * Java (long) */

#endif

/*@} */ /* End of grouped definitions */


/*!
 * @name Classfile primative types.
 *
 * @brief Streams of unsigned bytes in the Java class file.
 *
 * The definitions of @link #u1 u1@endlink and @link #u2 u2@endlink
 * and @link #u4 u4@endlink are here so a to decouple these ubiquitous
 * symbols from class file work.
 *
 * Notice that, depending on context, these three definitions
 * may be either signed or unsigned.  For this implementation,
 * there will be no further distinction made other than the
 * @c @b unsigned declarations of these symbols.  In most cases in
 * the spec, usage is unsigned, namely counts, lengths, indices,
 * enumerations, JDK program counter values, etc.  The only
 * significant exception is the CONSTANT_Integer_info.bytes
 * structure, which is still not adjusted for real machine
 * byte ordering, also CONSTANT_Float_info.bytes and their
 * (long) and (double) equivalents, having two u4 items.
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef jubyte u1;                 /**< Single byte */

typedef jushort u2;                /**< Two bytes, like an
                                   <b><code>unsigned short</code></b> */

typedef juint u4;                  /**< Four bytes, like an
                                     <b><code>unsigned int</code></b> */

/*@} */ /* End of grouped definitions */


/*!
 * @name Real machine types.
 *
 * @brief Real machine abstraction of real machine primative types.
 * With the exception of library(3) and system(2) calls, which use
 * the types mandated in their man pages, @e all real machine
 * primative types use these abstractions.  This should significantly
 * ease portability problems.
 */

/*@{ */ /* Begin grouped definitions */

typedef   signed char  rchar;      /**< Normal 8-bit 'C' character */

typedef unsigned char  rbyte;      /**< 8-bit byte for any purpose */

typedef unsigned char  rboolean;   /**< Boolean for any purpose */

typedef   signed short rshort;     /**< Signed 16-bit integer */

typedef unsigned short rushort;    /**< Unsigned 16-bit integer */

typedef   signed int   rint;       /**< Signed 32-bit integer */

typedef unsigned int   ruint;      /**< Unsigned 32-bit integer */


#ifdef CONFIG_WORDWIDTH64

typedef   signed long  rlong;      /**< Signed 64-bit integer */

typedef unsigned long  rulong;     /**< Unsigned 64-bit integer */

#else

typedef   signed long long rlong;  /**< Signed 64-bit integer */

typedef unsigned long long rulong; /**< Unsigned 64-bit integer */

#endif

typedef float            rfloat;   /**< Real machine
                                    * @c @b (float) */

typedef double           rdouble;  /**< Real machine
                                    * @c @b (double) */

typedef void             rvoid;    /**< Real machine
                                    * @c @b (void),
                                    * for pointers,
                                    * @c @b (void *),
                                    * untyped */

/*@} */ /* End of grouped definitions */


/*!
 * @name Selected manifest constants.
 *
 * @brief Common macros used commonly in 'C' code. 
 * Only permit use of @c @b \#define's in constant
 * definition source file, in static initialization,
 * and in selected @c @b switch statements.
 *
 * Most of these constants are found in some @b /usr/include directories
 * on some platforms, but not others, and not regularly defined between
 * platforms.  Also, remove misc. inconsistencies in @c @b \#define
 * usage amongst compilers.
 */

/*@{ */ /* Begin grouped definitions */

#ifndef ERROR0

#define ERROR0 0                   /**< Typically found in \<errno.h\>
                                    * or \<sys/errno.h\>
                                    */

#endif

/*!
 * @internal Destroy any pre-existing version (or even conflicting
 * versions) of several common symbols, then define them explicitly
 * for this compile environment.
 */
#ifdef NULL
#undef NULL
#endif

#ifdef TRUE
#undef TRUE
#endif

#ifdef FALSE
#undef FALSE
#endif

/*!
 * @name Symbols to avoid.
 *
 * In order to keep away from definitions of @c @B TRUE, @c @b FALSE,
 * and @c @b NULL that may be defined all over the place, these
 * symbols have been replaced in the real machine domain with
 * @link #rtrue rtrue@endlink, @link #rfalse rfalse@endlink, and
 * @link #rnull rnull@endlink.  They have been replaced in the
 * Java virtual machine domain by 
 * @link #jtrue jtrue@endlink, @link #jfalse jfalse@endlink, and
 * @link #jnull jnull@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define TRUE  DO_NOT_USE_TRUE  /**< Please use either @link #rtrue
                                    rtrue@endlink for real machine
                                    @c @b TRUE cases or @link #jtrue
                                    jtrue@endlink for Java virtual
                                    machine @c @b TRUE cases */ 

#define FALSE DO_NOT_USE_FALSE /**< Please use either @link #rfalse
                                    rfalse@endlink for real machine
                                    @c @b FALSE cases or @link #jfalse
                                    jfalse@endlink for Java virtual
                                    machine @c @b FALSE cases */ 

#define NULL  DO_NOT_USE_NULL  /**< Please use @link #rnull
                                    rnull@endlink for real machine
                                    @c @b NULL cases or @link #jnull
                                    jnull@endlink for Java virtual
                                    machine @c @b NULL cases */ 

/*@} */ /* End of grouped definitions */

#ifdef I_AM_JRTYPES_C
#undef  NULL
#define NULL ((rvoid *) 0)         /**< Null pointer value */

#undef  TRUE
#define TRUE ((rboolean) 1)        /**< Boolean "true" value */

#undef  FALSE
#define FALSE ((rboolean) 0)       /**< Boolean "false" value */

#define NEITHER_TRUE_NOR_FALSE ((rboolean) 2) /**< Value for
                                    * initializing a boolean to
                                    * "not initialized, that is,
                                    * neither TRUE nor FALSE".
                                    */

#endif

#define CHEAT_AND_USE_FALSE_TO_INITIALIZE ((rboolean) 0) /**<
                                    * Permit boolean "false" manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

#define CHEAT_AND_USE_TRUE_TO_INITIALIZE  ((rboolean) 1) /**<
                                    * Permit boolean "true" manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

#define CHEAT_AND_USE_NULL_TO_INITIALIZE  ((rvoid *) 0) /**<
                                    * Permit null pointer manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

#define CHEAT_AND_ALLOW_NULL_CLASS_INDEX  ((jvm_class_index)  0) /**<
                                    * Permit null class index manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

#define CHEAT_AND_ALLOW_NULL_OBJECT_HASH  ((jvm_object_hash)  0) /**<
                                    * Permit null object hash manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

#define CHEAT_AND_ALLOW_NULL_THREAD_INDEX ((jvm_thread_index) 0) /**<
                                    * Permit null thread index manifest
                                    * constant for initializing static
                                    * and global storage.
                                    */

/*@} */ /* End of grouped definitions */


/*!
 * @name Real machine constants.
 *
 * @brief Real machine implementation of common industry keywords.
 *
 * Instead of permitting unrestrained and potentially misuse and
 * abuse of the common macros @c @b NULL, @c @b TRUE,
 * and @c @b FALSE, including conflicting definitions in
 * various header files, these symbols have been declared explicitly
 * for this program and stored into global constants.  This should
 * also help in development for more accurate typing of expressions,
 * paramters, and return values.  A boolean "not initialized" value
 * is also defined.
 *
 * Use @link #rnull rnull@endlink in all cases except
 * static initalization.  Use @link #rtrue rtrue@endlink and
 * $@link #rfalse rfalse@endlink in all cases except static
 * initialization and @c @b while(TRUE) constructions just
 * before end of function definitions (some compilers complain about
 * missing return statements, see several examples).
 * In this manner, it will @e always be very clear as to whether a
 * @c @b NULL pointer is a Java null pointer or a real
 * machine null pointer, likewise a Java boolean or a real machine
 * boolean.
 */

/*@{ */ /* Begin grouped definitions */

extern const void    *rnull;       /**< Real machine constant
                                    * @c @b NULL
                                    */

extern const rboolean rfalse;      /**< Real machine constant
                                    * @c @b FALSE
                                    */

extern const rboolean rtrue;       /**< Real machine constant
                                    * @c @b TRUE
                                    */

extern const rboolean rneither_true_nor_false; /**<
                                    * Real machine constant @b neither.
                                    *  Typically used during
                                    * initialization to indicate a
                                    * boolean is not ready.
                                    */

/*@} */ /* End of grouped definitions */

#endif /* _jrtypes_h_included_ */


/* EOF */
