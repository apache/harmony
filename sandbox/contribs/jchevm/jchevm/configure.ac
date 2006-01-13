# $Id$

AC_INIT([JC Harmony Edition Virtual Machine], [1.0], [http://incubator.apache.org/harmony/], [jchevm])
AC_CONFIG_AUX_DIR(scripts)
AM_INIT_AUTOMAKE
dnl AM_MAINTAINER_MODE
AC_PREREQ(2.57)
AC_REVISION($Id$)
AC_PREFIX_DEFAULT(/usr/local)
AC_CONFIG_SRCDIR(libjc/libjc.h)
AC_PROG_MAKE_SET

# Definitions
AC_SUBST(LIBRARY_VERSION, [1:1:0])
AC_SUBST(CLASSPATH_VERSION, [0.20])

[JC_CFLAGS='-g -O3 -pipe -Wall -Waggregate-return -Wcast-align -Wchar-subscripts -Wcomment -Wformat -Wimplicit -Wmissing-declarations -Wmissing-prototypes -Wnested-externs -Wno-long-long -Wparentheses -Wpointer-arith -Wredundant-decls -Wreturn-type -Wswitch -Wtrigraphs -Wuninitialized -Wunused -Wwrite-strings']
AC_SUBST(JC_CFLAGS)
# -Wshadow -Wstrict-prototypes -Wcast-qual
# -fstrict-aliasing -foptimize-sibling-calls

# Compile flags for Linux
AC_DEFINE(_GNU_SOURCE, 1, GNU functions)
AC_DEFINE(_BSD_SOURCE, 1, BSD functions)

# Get the install root
[if test "x$prefix" = xNONE; then
    JC_PREFIX="$ac_default_prefix"
else
    JC_PREFIX="$prefix"
fi]

# Get Classpath home, default to /usr/local/classpath
[CLASSPATH_HOME="/usr/local/classpath"]
AC_ARG_WITH([classpath],
    [AC_HELP_STRING([--with-classpath=DIR],
	[specify Classpath installation home])],
    [test x"${withval}" != "xyes" && CLASSPATH_HOME="${withval}"])
AC_SUBST(CLASSPATH_HOME)

# Verify Classpath is really there
AC_CHECK_FILE([${CLASSPATH_HOME}/share/classpath/glibj.zip], ,
    [AC_MSG_ERROR([classpath not found in ${CLASSPATH_HOME}])])

# Optional features
AC_ARG_ENABLE(assertions,
    AC_HELP_STRING([--enable-assertions],
        [enable debugging sanity checks (default NO)]),
    [test x"$enableval" = "xyes" || AC_DEFINE(NDEBUG, 1, [disable assertions])],
    [AC_DEFINE(NDEBUG, 1, [disable assertions])])
AC_ARG_ENABLE(werror,
    AC_HELP_STRING([--enable-werror],
        [include -Werror GCC compiler flag (default NO)]),
    [test x"$enableval" = "xyes" && JC_CFLAGS="${JC_CFLAGS} -Werror"])

# Check for required programs
AC_PROG_INSTALL
AC_PROG_CC
AC_PATH_PROG(JIKES, jikes)
AC_ARG_VAR(JIKES, [path to jikes Java compiler])
AC_PATH_PROG(ZIP, zip)
AC_ARG_VAR(ZIP, [path to zip program])
AC_PATH_PROG(UNZIP, unzip)
AC_ARG_VAR(UNZIP, [path to unzip program])
AC_PATH_PROG(JAVA, java, /usr/bin/java, [$PATH:/usr/local/java/bin])
AC_ARG_VAR(JAVA, [path to java executable])
AC_PATH_PROG(JAVADOC, javadoc, /usr/bin/javadoc, [$PATH:/usr/local/java/bin])
AC_ARG_VAR(JAVADOC, [path to javadoc executable])
AC_PATH_PROG(TAR, tar)
AC_ARG_VAR(TAR, [path to tar executable])

# Check for required libraries
AC_CHECK_LIB(m, fmod,,
	[AC_MSG_ERROR([required library libm missing])])
AC_CHECK_LIB(z, inflateInit_,,
	[AC_MSG_ERROR([required library libz missing])])
AC_CHECK_LIB(popt, poptGetContext,,
	[AC_MSG_ERROR([required library libpopt missing])])
AC_CHECK_LIB(dl, dlopen,,,)

# Check for pthread library, linking either via -pthread or -lpthread
AC_MSG_CHECKING([whether -pthread gets us the pthread library])
[old_LDFLAGS="$LDFLAGS"; LDFLAGS="$LDFLAGS -pthread"]
AC_LINK_IFELSE([AC_LANG_PROGRAM([[#include <pthread.h>
	    void *x=&pthread_create;]])],
	[AC_MSG_RESULT(yes)],
	[AC_MSG_RESULT(no)
	[LDFLAGS="$old_LDFLAGS"]
	AC_CHECK_LIB(pthread, pthread_create,,
	    [AC_MSG_ERROR([required library libpthread missing])])])

# Check for thread local storage support
AC_MSG_CHECKING([for thread local variable support])
AC_COMPILE_IFELSE([AC_LANG_PROGRAM([[__thread int foo;]])],
	[AC_MSG_RESULT(yes)
	    [_JC_THREAD_LOCAL_SUPPORT="1"]],
	[AC_MSG_RESULT(no)
	    [_JC_THREAD_LOCAL_SUPPORT="0"]])
AC_SUBST(_JC_THREAD_LOCAL_SUPPORT)

# Check for required header files
AC_HEADER_STDC
AC_CHECK_HEADERS(sys/mman.h sys/wait.h pthread.h fcntl.h signal.h \
	ctype.h errno.h dlfcn.h sched.h math.h pwd.h zlib.h, [],
	[AC_MSG_ERROR([required header file missing])])
AC_CHECK_HEADERS([ucontext.h], [],
	[AC_MSG_ERROR([required header file missing])],
[#if HAVE_SIGNAL_H
# include <signal.h>
# endif
])

AC_CHECK_HEADERS(inttypes.h)

# Check for required library functions
AC_FUNC_MMAP
AC_CHECK_FUNCS([getcwd gettimeofday memmove memset munmap], , \
    [AC_MSG_ERROR([required function missing])])

# Check for optional library functions
AC_CHECK_FUNCS([getcontext])

# See if pthread_attr_setstack() is available
AC_CHECK_FUNCS([pthread_attr_setstack])

# Check for declarations
AC_CHECK_DECL(environ, [AC_DEFINE([HAVE_DECL_ENVIRON], [1],
    [environ explicitly declared somewhere])])

# Libtool stuff
AC_LIBTOOL_DLOPEN
AC_DISABLE_STATIC
AC_LIBTOOL_DLOPEN
AC_PROG_LIBTOOL

# Get host architecture
[JC_ARCH=`uname -m | sed -e 's/i[456]86/i386/g' | sed -e 's/Power Macintosh/ppc/g'`]
AC_SUBST(JC_ARCH)

# Generated files
AC_CONFIG_FILES(Makefile)
AC_CONFIG_FILES(doc/Makefile)
AC_CONFIG_FILES(etc/Makefile)
AC_CONFIG_FILES(etc/classpath.spec)
AC_CONFIG_FILES(etc/jchevm.spec)
AC_CONFIG_FILES(include/Makefile)
AC_CONFIG_FILES(include/jc_machdep.h)
AC_CONFIG_FILES(java/Makefile)
AC_CONFIG_FILES(java/org/Makefile)
AC_CONFIG_FILES(java/org/dellroad/Makefile)
AC_CONFIG_FILES(java/org/dellroad/jc/Makefile)
AC_CONFIG_FILES(java/org/dellroad/jc/vm/Makefile)
AC_CONFIG_FILES(jc/Makefile)
AC_CONFIG_FILES(libjc/Makefile)
AC_CONFIG_FILES(libjc/arch/Makefile)
AC_CONFIG_FILES(libjc/arch/i386/Makefile)
AC_CONFIG_FILES(libjc/arch/ppc/Makefile)
AC_CONFIG_FILES(libjc/native/Makefile)
AC_CONFIG_FILES(tools/Makefile)
AC_CONFIG_FILES(tools/cfdump/Makefile)
AC_CONFIG_FILES(tools/jcjavah/Makefile)

AM_CONFIG_HEADER(include/config.h)

AC_OUTPUT
