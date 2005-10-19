#ifndef _jvmcfg_h_included_
#define _jvmcfg_h_included_

/*!
 * @file jvmcfg.h
 *
 * @brief Sizes of JVM items, max number of classes, objects, threads,
 * etc., and other general program configuration.
 *
 * Notice that the maximum number of items is also dependent
 * on the data type that contains counts of that item.  For
 * example, since the thread index is an
 * <b><code>(unsigned short)</code></b>,
 * there can be no more than 2^16 threads in the system.
 *
 *
 * @par NOTES FOR JAVA NATIVE INTERFACE
 *
 * In order to keep this implementation @e absolutely independent
 * of @e any implementation of \<jni.h\> and subsidiary header files,
 * the following typedefs have been redefined in the JNI portion
 * of these header files:
 *
 * jvm_object_hash  ...found in @link jvm/include/jlObject.h
                       jlObject.h@endlink
 *
 * jvm_thread_index ...found in @link jvm/include/jlThread.h
                       jlThread.h@endlink
 *
 * jvm_class_index  ...found in @link jvm/include/jlClass.h
                       jlClass.h@endlink
 *
 * These types are used for function prototypes for JNI code.
 *
 *
 * @todo  Add proper searching for 'rt.jar' file and '-bootclasspath'.
 *        For the moment, they are defined in
 *        @link config.h config.h@endlink as the
 *       @link #CONFIG_HACKED_RTJARFILE CONFIG_HACKED_RTJARFILE@endlink
 *        and @link #CONFIG_HACKED_BOOTCLASSPATH
                     CONFIG_HACKED_BOOTCLASSPATH@endlink
 *        pre-processor symbols and are implemented in
 *        @link jvm/src/classpath.c classpath.c@endlink
 *        in this way.
 *
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @todo Need to evaluate if and when and how to phase out use of the 
 *       configuration variables @link #CONFIG_HACKED_RTJARFILE
         CONFIG_HACKED_RTJARFILE@endlink and
         @link #CONFIG_HACKED_BOOTCLASSPATH
         CONFIG_HACKED_BOOTCLASSPATH@endlink
 *
 * @internal Decide whether or not to use the
 * @link #CONFIG_HACKED_RTJARFILE CONFIG_HACKED_xxx@endlink definitions
 * from @link config.h config.h@endlink.  Consider two fragments of
 * source code showing with/without comment possibility (immediately
 * follows this note in the source code).
 *
 * @section Reference
 *
 */

ARCH_COPYRIGHT_APACHE(jvmcfg, h, "$URL$ $Id$");


#include "jrtypes.h"


/*
#undef CONFIG_HACKED_BOOTCLASSPATH
*/

/*
#undef CONFIG_HACKED_RTJARFILE
*/


/*!
 * @name OS File system conventions.
 *
 * @brief Conventions for OS file systems (see also
 * @link jvm/src/classfile.h classfile.h@endlink
 * for @link #CLASSFILE_EXTENSION_DEFAULT
   CLASSFILE_EXTENSION_xxx@endlink definitions).
 *
 * @verbatim
  
   Unix style:     /path/name1/name2/filename.extension
  
   Windows style:  c:\path\name1\name2\filename.extension
  
 * @endverbatim
 */

/*@{ */ /* Begin grouped definitions */

#ifdef CONFIG_WINDOWS
#define JVMCFG_PATHNAME_DELIMITER_CHAR   '\\'
#define JVMCFG_PATHNAME_DELIMITER_STRING "\\"

#define JVMCFG_EXTENSION_DELIMITER_CHAR   '.'
#define JVMCFG_EXTENSION_DELIMITER_STRING "."

#else
#define JVMCFG_PATHNAME_DELIMITER_CHAR   '/'
#define JVMCFG_PATHNAME_DELIMITER_STRING "/"

#define JVMCFG_EXTENSION_DELIMITER_CHAR   '.'
#define JVMCFG_EXTENSION_DELIMITER_STRING "."

#endif

/*@} */ /* End of grouped definitions */


/*!
 * @brief Descriptor for strings.
 *
 * Use this descriptor for manually loading startup class
 * @c @b java.lang.String objects.
 *
 * Resolves to "Ljava/lang/String;"
 *
 */
#define JVMCFG_MANUAL_STRING_DESCRIPTOR \
    BASETYPE_STRING_L JVMCLASS_JAVA_LANG_STRING BASETYPE_STRING_L_TERM


/*!
 * @name Individual command line tokens
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Command line declaring that startup class is in a @b JAR file
 * and the name of that file.
 *
 */
#define JVMCFG_JARFILE_STARTCLASS_PARM   "-jar"


/*!
 * @brief Command line option to show command line help.
 *
 */
#define JVMCFG_COMMAND_LINE_HELP_PARM    "-help"


/*!
 * @brief Command line option to show software license.
 *
 */
#define JVMCFG_COMMAND_LINE_LICENSE_PARM "-license"


/*!
 * @brief Command line option to show program version number.
 *
 */
#define JVMCFG_COMMAND_LINE_VERSION_PARM "-version"


/*!
 * @brief Command line option to show program copyright message.
 *
 */
#define JVMCFG_COMMAND_LINE_COPYRIGHT_PARM "-copyright"


/*!
 * @brief Command line option to show program options.
 *
 */
#define JVMCFG_COMMAND_LINE_SHOW_PARM "-show"

/*@} */ /* End of grouped definitions */


/*!
 * @name Environment variable names
 *
 * @brief Environment variables used by Java.
 *
 * Each one of these may be overridden from the command line.
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_ENVIRONMENT_VARIABLE_JAVA_HOME     "JAVA_HOME"
#define JVMCFG_ENVIRONMENT_VARIABLE_CLASSPATH     "CLASSPATH"
#define JVMCFG_ENVIRONMENT_VARIABLE_BOOTCLASSPATH "BOOTCLASSPATH"

/*@} */ /* End of grouped definitions */


/*!
 * @name Default environment variable definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @internal Hard-coded @b CLASSPATH for test purposes.
 * Set to any desired value and uncomment for use.
 */
/* #define JVMCFG_HARDCODED_TEST_CLASSPATH \
           "/tmp:/usr/tmp:/usr/local/tmp"
*/


/*!
 * @brief Default @b JAVA_HOME .
 *
 * Set the default value of the @b JAVA_HOME environment
 * variable to an @link #rnull rnull@endlink pointer.
 */
#define JVMCFG_JAVA_HOME_DEFAULT rnull


/*!
 * @brief Default @b CLASSPATH to current directory only.
 *
 */
#define JVMCFG_CLASSPATH_DEFAULT ((const rchar *) ".")

/*!
 * @brief Default @b BOOTCLASSPATH to our temp area.
 *
 * This area starts out empty, meaning that nothing will be found
 * there to boot from, but a default of some sort is needed.  May
 * be overridden by
 * @link #JVMCFG_BOOTCLASSPATH_FULL_PARM -Xbootclasspath@endlink.
 *
 */
#define JVMCFG_BOOTCLASSPATH_DEFAULT tmparea_get()

/*@} */ /* End of grouped definitions */


/*!
 * @name -Xjava_home token and its aliases
 *
 * @brief Command line options defining @b JAVA_HOME instead.
 * of the environment variable
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_JAVA_HOME_ABBREV_PARM "-Xjh"
#define JVMCFG_JAVA_HOME_MID_PARM    "-Xjavahome"
#define JVMCFG_JAVA_HOME_FULL_PARM   "-Xjava_home"

/*@} */ /* End of grouped definitions */


/*!
 * @name -classpath token and its alises
 *
 * @brief Command line options defining @b CLASSPATH instead of
 * using the environment variable.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_CLASSPATH_ABBREV_PARM "-cp"
#define JVMCFG_CLASSPATH_FULL_PARM   "-classpath"

/*@} */ /* End of grouped definitions */


/*!
 * @name -Xbootclasspath token and its aliases
 *
 * @brief Command line options defining @b BOOTCLASSPATH instead of
 * using the environment variable.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_BOOTCLASSPATH_ABBREV_PARM "-Xbcp"
#define JVMCFG_BOOTCLASSPATH_FULL_PARM   "-Xbootclasspath"

/*@} */ /* End of grouped definitions */


/*!
 * @name -Xdebug_level token and its aliases
 *
 * @brief Command line options defining debug message level.
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_DEBUGMSGLEVEL_ABBREV_PARM "-Xdebug"
#define JVMCFG_DEBUGMSGLEVEL_MID_PARM    "-Xdebuglevel"
#define JVMCFG_DEBUGMSGLEVEL_FULL_PARM   "-Xdebug_level"

/*@} */ /* End of grouped definitions */


/*!
 * @name Native method support
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native method registration method name.
 *
 */
#define JVMCFG_REGISTER_NATIVES_METHOD "registerNatives"

/*!
 * @brief Native method registration method descriptor.
 *
 * Resolves to "()V"
 *
 */
#define JVMCFG_REGISTER_NATIVES_PARMS \
    METHOD_STRING_OPEN_PARM \
    METHOD_STRING_CLOSE_PARM \
    METHOD_STRING_VOID


/*!
 * @brief Native method unregistration method name.
 *
 */
#define JVMCFG_UNREGISTER_NATIVES_METHOD "unregisterNatives"

/*!
 * @brief Native method unregistration method descriptor.
 *
 * Resolves to "()V"
 *
 */
#define JVMCFG_UNREGISTER_NATIVES_PARMS \
    METHOD_STRING_OPEN_PARM \
    METHOD_STRING_CLOSE_PARM \
    METHOD_STRING_VOID

/*!
 * @brief Ignore native method calls.
 *
 * If ignored, then return the same default value as is
 * default for field initializations (namely, zero,
 * @link #jfalse jfalse@endlink, @link #jnull jnull@endlink, etc.)
 * If not ignored, then attempt to run them.  In both
 * cases, produce a return value as appropriate.
 *
 */
#define JVMCFG_IGNORE_NATIVE_METHOD_CALLS rtrue

/*@} */ /* End of grouped definitions */


/*!
 * @name main() method support
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Startup class' @c @b main() method name.
 *
 */
#define JVMCFG_MAIN_METHOD "main"

/*!
 * @brief Startup class' @c @b main() method parameter list.
 *
 * Resolves to "([Ljava/lang/String;)V"
 *
 */
#define JVMCFG_MAIN_PARMS  METHOD_STRING_OPEN_PARM \
                           BASETYPE_STRING_ARRAY \
                           BASETYPE_STRING_L \
                           JVMCLASS_JAVA_LANG_STRING \
                           BASETYPE_STRING_L_TERM \
                           METHOD_STRING_CLOSE_PARM \
                           METHOD_STRING_VOID

/*!
 * @brief Startup class' @c @b main() method local variable index
 * for @c @b args[]
 *
 */
#define JVMCFG_MAIN_PARM_ARGV_INDEX 0

/*@} */ /* End of grouped definitions */


/*!
 * @name Exception handling support.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Constructor to use for basic exception handling.
 *
 */
#define JVMCFG_EXCEPTION_DEFAULT_CONSTRUCTOR_DESCRIPTOR \
    METHOD_STRING_OPEN_PARM \
    METHOD_STRING_CLOSE_PARM \
    METHOD_STRING_VOID


/*!
 * @brief Uncaught exception method name.
 *
 */
#define JVMCFG_UNCAUGHT_EXCEPTION_METHOD "uncaughtException"

/*!
 * @brief Uncaught exception method parameter list.
 *
 * Resolves to "([Ljava/lang/Thread;[Ljava/lang/Throwable;)V"
 *
 */
#define JVMCFG_UNCAUGHT_EXCEPTION_PARMS  METHOD_STRING_OPEN_PARM \
                                         BASETYPE_STRING_ARRAY \
                                         BASETYPE_STRING_L \
                                         JVMCLASS_JAVA_LANG_THREAD \
                                         BASETYPE_STRING_L_TERM \
                                         BASETYPE_STRING_ARRAY \
                                         BASETYPE_STRING_L \
                                         JVMCLASS_JAVA_LANG_THROWABLE \
                                         BASETYPE_STRING_L_TERM \
                                         METHOD_STRING_CLOSE_PARM \
                                         METHOD_STRING_VOID

/*@} */ /* End of grouped definitions */


/*!
 * @name Object finalization support.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * Object finalize method name.
 */
#define JVMCFG_FINALIZE_OBJECT_METHOD "finalize"

/*!
 * Object finalize method descriptor.
 *
 * Resolves to "()V"
 *
 */
#define JVMCFG_FINALIZE_OBJECT_PARMS \
    METHOD_STRING_OPEN_PARM \
    METHOD_STRING_CLOSE_PARM \
    METHOD_STRING_VOID

/*@} */ /* End of grouped definitions */


/*!
 * @name Array dimension support.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Spec-defined number of array dimensions
 */
#define JVMCFG_MAX_ARRAY_DIMS CONSTANT_MAX_ARRAY_DIMS

/*!
 * @brief Array dimension type.
 */
typedef jubyte  jvm_array_dim;

/*@} */ /* End of grouped definitions */


/*!
 * @name Interval timer support.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief JVM interval timer enable.
 *
 * When @link #rtrue rtrue@endlink, the timer runs normally.
 * Set it @link #rfalse rfalse@endlink typically only for debugging
 * the JVM outer loop logic or for debugging the interval timer logic
 * itself.
 *
 * @warning See warning about use of
 * @link #timeslice_tick() timeslice_tick()@endlink
 * and high-speed interval timing.
 *
 * @todo Make sure to enable the time slicer for normal JVM operation.
 *       It may be handy to disable it for debugging, but no threading
 *       will occur in the JVM outer loop until it is enabled!
 *
 */
#define JVMCFG_TIMESLICE_PERIOD_ENABLE      rfalse

/*!
 * @brief JVM interval timer period (seconds)
 *
 * This value is typically zero (0) for normal operation.
 * Set it to any convenient number of seconds for debugging
 * the JVM outer loop.
 *
 * Setting both timer values to zero will disable the interval
 * timer completely.
 *
 * @warning See warning about use of
 * @link #timeslice_tick() timeslice_tick()@endlink
 * and high-speed interval timing.
 *
 */
#define JVMCFG_TIMESLICE_PERIOD_SECONDS              0

/*!
 * @brief JVM interval timer period (microseconds)
 *
 * For correct operation of the JVM interval timer at 1 kHz,
 * set this value to one thousand (1000).  This will guarantee
 * that timer periods such as
 * @link java.lang.Thread.sleep() sleep()@endlink and
 * @link java.lang.Object.wait() wait()@endlink will work
 * according to their definitions.  This value may be set
 * to some other value for debug purposes. 
 *
 * Setting both timer values to zero will disable the interval
 * timer completely.
 *
 * @warning See warning about use of
 * @link #timeslice_tick() timeslice_tick()@endlink
 * and high-speed interval timing.
 *
 */
#define JVMCFG_TIMESLICE_PERIOD_MICROSECONDS      1000 /* 1000 := 1 ms*/


/*!
 * @brief Minimum number of seconds before
 * @link #timeslice_tick() timeslice_tick()@endlink starts
 * printing a short message at every timer tick.
 *
 * Print "timeslice_tick: tick" at every interval timer event.
 * If set to a non-zero value, typically for debug purposes,
 * @link #timeslice_tick() timeslice_tick()@endlink will start
 * reporting timer events through a standard error message
 * via @link #sysDbgMsg() sysDbgMsg()@endlink after the defined
 * number of seconds.  It is effectively a delay value to enable
 * this message, but only after a certain run time has passed.
 * When using this facility, <em>absolutely sure</em> that the
 * value of @link #JVMCFG_TIMESLICE_PERIOD_SECONDS
   JVMCFG_TIMESLICE_PERIOD_SECONDS@endlink is large enough
 * that these reports do not overwhelm the standard error resource
 * and prohibit productive work.
 *
 * A value of zero (0) disables this facility.
 *
 */
#define JVMCFG_TIMESLICE_DEBUG_REPORT_MIN_SECONDS    0

/*@} */ /* End of grouped definitions */


/*!
 * @name Thread table support.
 *
 * @brief JVM Thread model definitions.
 *
 * @note Thread groups are supported by the class library.
 *
 * @note  The @link #JVMCFG_NULL_THREAD JVMCFG_NULL_THREAD@endlink
 *        is @e never used at run time.
 *
 * @note  The @link #JVMCFG_SYSTEM_THREAD JVMCFG_SYSTEM_THREAD@endlink
 *        will @e never be used within the context of the JVM execution
 *        engine.  Its purpose is ONLY for internal administration.
 *        THEREFORE:  there will @e never be a
 *        @c @b java.lang.Thread object created for it!
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_MAX_THREADS 1000 /* Total possible threads */
#ifdef I_AM_JVMCFG_C
#define JVMCFG_NULL_THREAD    0 /* Never allocated */
#endif
#define JVMCFG_SYSTEM_THREAD  1 /* System thread */
#define JVMCFG_GC_THREAD      2 /* Garbage collector thread */
#define JVMCFG_FIRST_THREAD   3 /* First allocatable thread */

                               /*
                                * Thread indices all use this type 
                                *
                                * (See parallel definition for use
                                *  in public interface in
                                *  @link jvm/include/jlThread.h
                                   jlThread.h@endlink)
                                */
typedef rushort jvm_thread_index;

                               /* Real machine NULL index for threads */
extern const jvm_thread_index jvm_thread_index_null;

#define JVMCFG_THREAD_NAME_SYSTEM "system" /* system thread */
#define JVMCFG_THREAD_NAME_GC     "gc"     /* Garbage collector thread*/

/*@} */ /* End of grouped definitions */


/*!
 * @name Class file support
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Constant pool indices all use this type
 */
typedef u2 jvm_constant_pool_index;

/*!
 * @brief Real machine NULL constant_pool index
 */
extern const jvm_constant_pool_index jvm_constant_pool_index_null;


/*!
 * @internal Due to the way the JVM spec defines the
 * @link ClassFile#interfaces interfaces@endlink
 * member as an array of @link #u2 u2@endlink,
 * it is technically a bad thing to redefine
 * such data type.  However, in order to be consistent with all
 * of the other array index definitions, this will be done anyway:
 *
 */

/*!
 * @brief Interface table indices all use this type
 *
 */
typedef u2 jvm_interface_index;

/*!
 * @brief Real machine BAD interface table index
 *
 */
extern const jvm_interface_index jvm_interface_index_bad;

/*!
 * @brief Bad interface slot, usually "not found"
 *
 */
#define JVMCFG_BAD_INTERFACE 65535

/*@} */ /* End of grouped definitions */


/*!
 * @name Class table support
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Max number of class allocations
 *
 */
#define JVMCFG_MAX_CLASSES 200

#ifdef I_AM_JVMCFG_C

/*!
 * @brief Null class slot coincides with hash 0
 *
 */
#define JVMCFG_NULL_CLASS    0
#endif

/*!
 * @brief First object slot to allocate
 *
 */
#define JVMCFG_FIRST_CLASS   1

/*!
 * @brief Class indices all use this type
 *
 */
typedef rushort  jvm_class_index;

/*!
 * @brief Real machine NULL index for classes
 */
extern const jvm_class_index jvm_class_index_null;

/*!
 * @brief Method table indices all use this type 
 *
 */
typedef u2       jvm_method_index;

/*!
 * @brief Real machine BAD index for methods
 *
 */
extern const jvm_method_index jvm_method_index_bad;

/*!
 * @brief Bad method slot, usually "not found"
 *
 */
#define JVMCFG_BAD_METHOD    65535


/*!
 * @brief Field table indices all use this type 
 *
 */
typedef u2       jvm_field_index;

/*!
 * @brief Real machine NULL index for fields
 *
 */
extern const jvm_field_index jvm_field_index_bad;

/*!
 * @brief Bad field slot, usually "not found"
 *
 */
#define JVMCFG_BAD_FIELD  65535

/*!
 * @brief Field table lookups all use this type 
 *
 */
typedef u2       jvm_field_lookup_index;

/*!
 * @brief Real machine NULL index for field lookups
 *
 */
extern const jvm_field_lookup_index jvm_field_lookup_index_bad;

/*!
 * @brief  Bad lookup slot, usually "not found"
 *
 */
#define JVMCFG_BAD_FIELD_LOOKUP 65535

/*!
 * @brief Attribute table indices all use this type
 *
 */
typedef u2       jvm_attribute_index;

/*!
 * @brief Real machine NULL index for attributes
 *
 */
extern const jvm_attribute_index jvm_attribute_index_bad;

/*!
 * @brief Real machine marker for native method
 *
 */
extern const jvm_attribute_index jvm_attribute_index_native;

/*!
 * @brief Ordinal number for local native method
 *
 */
typedef unsigned int jvm_native_method_ordinal;

/*!
 * @brief Real machine NULL ordinal number for local native methods
 *
 */
extern const jvm_native_method_ordinal jvm_native_method_ordinal_null;

/*!
 * @brief Null local method slot, usually "not found".
 *
 * See also parallel definition
 * @link jvm/include/jlObject.h JLOBJECT_NMO_NULL@endlink
 *
 */
#define JVMCFG_JLOBJECT_NMO_NULL 0

/*!
 * @brief Real machine reserved ordinal number for registering
 * local native methods
 *
 */
extern
    const jvm_native_method_ordinal jvm_native_method_ordinal_register;

/*!
 * @brief Reserved local method slot for the
 * registration of @e local native methods.
 *
 * See also parallel definition
 * @link jvm/include/jlObject.h JLOBJECT_NMO_REGISTER@endlink
 *
 */
#define JVMCFG_JLOBJECT_NMO_REGISTER 1

/*!
 * @brief Real machine reserved ordinal number
 * for <em>un</em>-registering local native methods
 *
 */
extern
   const jvm_native_method_ordinal jvm_native_method_ordinal_unregister;

/*!
 * @brief Reserved local method slot for the
 * un-registration of @e local native methods.
 *
 * See also parallel definition
 * @link jvm/include/jlObject.h JLOBJECT_NMO_UNREGISTER@endlink
 *
 */
#define JVMCFG_JLOBJECT_NMO_UNREGISTER 2

/*!
 * @brief Bad attribute slot,usually "not found"
 *
 */
#define JVMCFG_BAD_ATTRIBUTE  65535

/*!
 * @brief Native method slot,no code attribute
 *
 */
#define JVMCFG_NATIVE_METHOD_ATTRIBUTE 65534

/*!
 * @brief UTF8 string table indices all use this type
 *
 */
typedef u2       jvm_utf_string_index;

/*!
 * @brief Unicode string table indices all use this type
 *
 */
typedef u2       jvm_unicode_string_index;

/*!
 * @brief Real machine BAD index for Unicode
 *
 */
extern const jvm_unicode_string_index jvm_unicode_string_index_bad;

/*!
 * @brief Bad unicode string slot, usually "not found"
 *
 */
#define JVMCFG_BAD_UNICODE_STRING  65535

/*@} */ /* End of grouped definitions */


/*!
 * @name Object table support
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Max number of object allocations
 *
 */
#define JVMCFG_MAX_OBJECTS 1000

#ifdef I_AM_JVMCFG_C
/*!
 * @brief Null object slot coincides w/hash 0
 *
 */
#define JVMCFG_NULL_OBJECT   0
#endif

/*!
 * @brief First object slot to allocate
 *
 */
#define JVMCFG_FIRST_OBJECT  1

/*!
 * @brief Object reference, corresponding to the
 * JNI type @c @b jobject
 *
 * See parallel definition for use in public interface in
 * @link jvm/include/jlObject.h jlObject.h@endlink
 *
 */
typedef ruint   jvm_object_hash;

/*!
 * @brief Real machine NULL hash
 *
 */
extern const jvm_object_hash jvm_object_hash_null;

/*!
 * @brief Holds spec @link #ACC_PUBLIC ACC_xxx@endlink bit masks
 *
 */
typedef u2      jvm_access_flags;

/*!
 * @brief Holds object data type, such as @c @b I == Integer
 *
 */
typedef u1      jvm_basetype;

/*@} */ /* End of grouped definitions */


/*!
 * @name Stack size definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Arbitrary max stack size, in bytes
 *
 * This value @e must be in increments of 4 bytes!
 *
 * @warning This value @e must be in increments of 4 bytes!  (Get it?)
 *
 */
#define JVMCFG_STACK_SIZE    (8 * 1024 * sizeof(jint))

/*!
 * @brief Maximum stack pointer value
 *
 */
#define JVMCFG_MAX_SP        (JVMCFG_STACK_SIZE - sizeof(jint))

/*!
 * @brief Empty stack pointer value
 *
 */
#define JVMCFG_NULL_SP       0

/*!
 * @brief Stack pointer type
 *
 */
typedef jushort jvm_sp;

/*@} */ /* End of grouped definitions */


/*!
 * @name Program counter definitions
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Program counter offset into code area of a method
 *
 * This data type is used in jvm_pc.
 *
 */
typedef juint         jvm_pc_offset;

/*!
 * @brief Invalid program counter offset
 *
 */
extern  jvm_pc_offset jvm_pc_offset_bad;

/*@} */ /* End of grouped definitions */


/*!
 * @name Debug levels for sysDbgMsg().
 *
 * @brief Set increasingly verbose debug message levels for sysDbgMsg().
 *
 * Notice that the lowest debug message level setting
 * @link #DML0 DML0@endlink indicates that a message using
 * it is probablt a good candidate to be changed to the
 * unconditional sysErrMsg() function instead.
 *
 * @see jvmutil_set_dml()
 *
 * @see jvmutil_get_dml()
 *
 * @see sysDbgMsg()
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef enum
{
    DML0  = 0,           /**< Only @e VITAL debug messages */
    DML1  = 1,           /**< Minimum amount of debug messages */
    DML2  = 2,
    DML3  = 3,
    DML4  = 4,
    DML5  = 5,           /**< Average amount of debug messages */
    DML6  = 6,
    DML7  = 7,
    DML8  = 8,
    DML9  = 9,
    DML10 = 10           /**< Absolutely ALL debug messages */

} jvm_debug_level_enum;

#define DMLOFF           DML0  /**< Convenient alias for
                                    @link #DML0 DML0@endlink */
#define DMLMIN           DML1  /**< Convenient alias for
                                    @link #DML0 DML1@endlink */
#define DMLNORM          DML5  /**< Convenient alias for
                                    @link #DML0 DML5@endlink */
#define DMLMAX           DML10 /**< Convenient alias for
                                    @link #DML0 DML10@endlink */


#define DMLDEFAULT       DMLNORM /**< Initial debug level, may be
                                  * changed by command line parameter.
                                  */

/*@} */ /* End of grouped definitions */


/*!
 * @name Arbitrary max buffer sizes
 *
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCFG_STDIO_BFR  1024 /**< Convenient size for any stdio msg */
#define JVMCFG_PATH_MAX   1024 /**< Convenient size for any disk path */
#define JVMCFG_SCRIPT_MAX 1024 /**< Convenient size for a
                                    @c @b system(3) call */

/*@} */ /* End of grouped definitions */


/*!
 * @name Standard I/O definitions
 *
 * The definitions of printf, fprint, and sprintf  will co-opt @e all
 * usual standard I/O operations and encourage the use of the local
 * version of these functions.  See
 *@link jvm/src/stdio.c stdio.c@endlink
 * for details on why this is so.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Enable stderr debug messages using sysDbgMsg() format messages
 *
 * When disabled, @e no debug message will display.
 * The debug levels, including the default level
 * @link #DMLDEFAULT DMLDEFAULT@endlink, are defined in
 * @link jvm/src/util.h util.h@endlink.
 *
 */
#define JVMCFG_DEBUG_MESSAGE_ENABLE rtrue

/* Only defined in @link jvm/src/stdio.c stdio.c@endlink */
#ifndef SUPPRESS_STDIO_REDIRECTION

#define printf  _printfLocal
#define fprintf _fprintfLocal
#define sprintf _sprintfLocal

#endif

/*!
 * @brief When desired, persuade Eclipse to flush its stdio buffers
 * better by invoking @c @b sleep(3) for <= 1 second (with arg of '1').
 *
 */
#if 0
#define JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER sleep(1)
#else
#define JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER
#endif

/*!
 * @brief Same thing as @link #JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER
   JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER@endlink, but at end of
 * JVM run before exit, try to persuade Eclipse to flush @e all
 * standard I/O buffers before quitting JVM.
 *
 */
#define JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER_EXIT sleep(1)

/*@} */ /* End of grouped definitions */


/*!
 * @name Initialization roll call globals
 *
 * Each of these symbols tracks a phase of jvm_init() in case there
 * is an error at some point and jvm_shutdown() needs to be called.
 * When that happens, only those initializion phases that were
 * complete are reversed and cleaned up.
 *
 */

/*@{ */ /* Begin grouped definitions */

extern rboolean jvm_timeslice_initialized;
extern rboolean jvm_thread_initialized;
extern rboolean jvm_class_initialized;
extern rboolean jvm_object_initialized;
extern rboolean jvm_argv_initialized;
extern rboolean jvm_classpath_initialized;
extern rboolean jvm_tmparea_initialized;
extern rboolean jvm_heap_initialized;
extern rboolean jvm_model_initialized;

/*@} */ /* End of grouped definitions */

/*!
 * @name Temporary disk area script support
 *
 * @brief Shell scripts to support selected temporary area operations.
 *
 * Notice that @c @b mkdir(2) is used for creating this directory, but
 * that a shell script fragment @link
   #JVMCFG_TMPAREA_REMOVE_SCRIPT JVMCFG_TMPAREA_REMOVE_SCRIPT@endlink
 * is used for removing it.
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Temporary area default when no @b TMPDIR environment variable
 */
#ifdef CONFIG_WINDOWS

#define JVMCFG_TMPAREA_DEFAULT "c:\\temp"

#else

#define JVMCFG_TMPAREA_DEFAULT "/tmp"

#endif

/*!
 * @brief Remove temporary directory
 */
#define JVMCFG_TMPAREA_REMOVE_SCRIPT "rm -rf %s"

/*@} */ /* End of grouped definitions */


/*!
 * @name JAR file structural items
 *
 *
 * @todo  Write and implement DOS/Windows version of these scripts.
 *        Perhaps a two- or three-line .BAT file is the thing to do?
 *
 */

/*@{ */ /* Begin grouped definitions */

#ifdef CONFIG_WINDOWS

#define JVMCFG_JARFILE_DATA_EXTRACT_SCRIPT \
    ".\\jjdes.bat %s %s %s %c %s %s "

/*    "chdir %s; %s/bin/jar -xf %s%c%s %s; chmod -R +w ." */

#define JVMCFG_JARFILE_MANIFEST_EXTRACT_SCRIPT \
    "jjmes %s %s %s"

/*    "chdir %s; %s/bin/jar -xf %s; chmod -R +w ."  */


#else

#define JVMCFG_JARFILE_DATA_EXTRACT_SCRIPT \
    "chdir %s; %s/bin/jar -xf %s%c%s %s; chmod -R +w ."

#define JVMCFG_JARFILE_MANIFEST_EXTRACT_SCRIPT \
    "chdir %s; %s/bin/jar -xf %s; chmod -R +w ."

#endif

#define JVMCFG_JARFILE_MANIFEST_FILENAME "META-INF/MANIFEST.MF"

#define JVMCFG_JARFILE_MANIFEST_MAIN_CLASS "Main-Class:"

#define JVMCFG_JARFILE_MANIFEST_LINE_MAX 72

/*@} */ /* End of grouped definitions */

#endif /* _jvmcfg_h_included_ */


/* EOF */
