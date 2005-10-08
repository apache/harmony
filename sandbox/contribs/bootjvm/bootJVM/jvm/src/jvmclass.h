#ifndef _classlist_h_included_
#define _classlist_h_included_

/*!
 * @file jvmclass.h
 *
 * @brief Definition of <b><code>java.lang</code></b> classes used by
 * the Java Virtual Machine for its normal operation.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/jvmclass.h $ \$Id: jvmclass.h 0 09/28/2005 dlydick $
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

ARCH_COPYRIGHT_APACHE(jvmclass, h, "$URL: https://svn.apache.org/path/name/jvmclass.h $ $Id: jvmclass.h 0 09/28/2005 dlydick $");


/*! Package name for internal Java language classes */
#define JVMCLASS_JAVA_LANG "java/lang"


/*!
 * @name Java class names.
 *
 * @brief Java classes used internally by the JVM.

 * Each of these classes is required internall in some way by the
 * Java Virtual Machine.
 *
 * Make sure whether or not these should be included
 * in @link ./config.sh config.sh@endlink in the @b bootclasspath lists
 * of classes in the JAVA_LANG_CLASS_LIST shell variable
 * (as JAVA_LANG_CLASS_LIST="Object Void String...").
 *
 * Although
 * <b><code>java.nio.channels.ClosedByInterruptException</code></b>
 * is not part of <b><code>java.lang</code></b>, it use also used
 * internally by the JVM and so is listed here.
 */

/*@{ */ /* Begin grouped definitions */

#define JVMCLASS_JAVA_LANG_OBJECT       CONSTANT_UTF8_JAVA_LANG_OBJECT
#define JVMCLASS_JAVA_LANG_STRING       "java/lang/String"
#define JVMCLASS_JAVA_LANG_CLASS        "java/lang/Class"
#define JVMCLASS_JAVA_LANG_THREADGROUP  "java/lang/ThreadGroup"
#define JVMCLASS_JAVA_LANG_RUNTIME      "java/lang/Runtime"
#define JVMCLASS_JAVA_LANG_SYSTEM       "java/lang/System"
#define JVMCLASS_JAVA_LANG_THREAD       "java/lang/Thread"

#define JVMCLASS_JAVA_LANG_THROWABLE    "java/lang/Throwable"

#define JVMCLASS_JAVA_LANG_STACKTRACEELEMENT \
                      "java/lang/StackTraceElement"

#define JVMCLASS_JAVA_LANG_ERROR        "java/lang/Error"
#define JVMCLASS_JAVA_LANG_UNSUPPORTEDCLASSVERSIONERROR \
                      "java/lang/UnsupportedClassVersionError"

/* java.lang.LinkageError and it subclasses */
#define JVMCLASS_JAVA_LANG_LINKAGEERROR "java/lang/LinkageError"
#define JVMCLASS_JAVA_LANG_CLASSCIRCULARITYERROR \
                      "java/lang/ClassCircularityError"
#define JVMCLASS_JAVA_LANG_CLASSFORMATERROR \
                      "java/lang/ClassFormatError"
#define JVMCLASS_JAVA_LANG_EXCEPTIONININITIALIZERERROR \
                      "java/lang/ExceptionInInitializerError"
#define JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR \
                      "java/lang/NoClassDefFoundError"
#define JVMCLASS_JAVA_LANG_UNSATISFIEDLINKERROR \
                      "java/lang/UnsatisfiedLinkError"
#define JVMCLASS_JAVA_LANG_VERIFYERROR \
                      "java/lang/VerifyError"

/* java.lang.IncompatibleClasSchangeError and its subclasses */
#define JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR \
                      "java/lang/IncompatibleClassChangeError"
#define JVMCLASS_JAVA_LANG_NOSUCHFIELDERROR \
                      "NosuchFieldError"
#define JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR \
                      "NoSuchMethodError"
#define JVMCLASS_JAVA_LANG_INSTANTIATIONERROR \
                      "InstantiationError"
#define JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR \
                      "IllegalAccessError"

/* java.lang.VirtualMachineError and its subclasses */
#define JVMCLASS_JAVA_LANG_VIRTUALMACHINEERROR \
                      "java/lang/VirtualMachineError"
#define JVMCLASS_JAVA_LANG_INTERNALERROR \
                      "java/lang/InternalError"
#define JVMCLASS_JAVA_LANG_STACKOVERFLOWERROR \
                      "java/lang/StackOverflowError"
#define JVMCLASS_JAVA_LANG_OUTOFMEMORYERROR \
                      "java/lang/OutOfMemoryError"
#define JVMCLASS_JAVA_LANG_UNKNOWNERROR \
                      "java/lang/UnknownError"

#define JVMCLASS_JAVA_LANG_EXCEPTION    "java/lang/Exception"
#define JVMCLASS_JAVA_LANG_ARRAYINDEXOUTOFBOUNDSEXCEPTION \
                      "java/lang/ArrayIndexOutOfBoundsException"
#define JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION \
                      "java/lang/NegativeArraySizeException"
#define JVMCLASS_JAVA_LANG_ARRAYSTOREEXCEPTION \
                      "java/lang/ArrayStoreException"
#define JVMCLASS_JAVA_LANG_CLASSNOTFOUNDEXCEPTION \
                      "java/lang/ClassNotFoundException"
#define JVMCLASS_JAVA_LANG_CLONENOTSUPPORTEDEXCEPTION \
                      "java/lang/CloneNotSupportedException"
#define JVMCLASS_JAVA_LANG_RUNTIMEEXCEPTION \
                      "java/lang/RuntimeException"
#define JVMCLASS_JAVA_LANG_ARITHMETICEXCEPTION \
                      "java/lang/ArithmeticException"
#define JVMCLASS_JAVA_LANG_ILLEGALARGUMENTEXCEPTION \
                      "java/lang/IllegalArgumentException"
#define JVMCLASS_JAVA_LANG_ILLEGALMONITORSTATEEXCEPTION \
                      "java/lang/IllegalMonitorStateException"
#define JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION \
                      "java/lang/IllegalThreadStateException"
#define JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION \
                      "java/lang/InterruptedException"
#define JVMCLASS_JAVA_LANG_INDEXOUTOFBOUNDSEXCEPTION \
                      "java/lang/IndexOutOfBoundsException"
#define JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION \
                      "java/lang/NullPointerException"
#define JVMCLASS_JAVA_LANG_SECURITYEXCEPTION \
                      "java/lang/SecurityException"

#define JVMCLASS_JAVA_NIO_CHANNELS_CLOSEDBYINTERRUPTEXCEPTION \
                      "java/nio/channels/ClosedByInterruptException"

/*@} */ /* End of grouped definitions */


#endif /* _jvmclass_h_included_ */

/* EOF */
