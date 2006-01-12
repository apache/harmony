## $Id$

SUBDIRS=		native arch

lib_LIBRARIES=		libjc.a

# Standard source files
libjc_a_SOURCES= \
			array.c \
			bootstrap.c \
			cf_parse.c \
			cl_alloc.c \
			class_bytes.c \
			class_loader.c \
			derive.c \
			derive2.c \
			exception.c \
			fatal.c \
			gc_final.c \
			gc_root.c \
			gc_scan.c \
			heap.c \
			init.c \
			initialize.c \
			instance.c \
			interp.c \
			invoke.c \
			jc_invoke.c \
			jni_invoke.c \
			jni_native.c \
			load.c \
			lock.c \
			misc.c \
			mutex.c \
			native_lib.c \
			native_ref.c \
			new.c \
			os_functions.c \
			prepare.c \
			printf.c \
			properties.c \
			reflect.c \
			resolve.c \
			resolve2.c \
			signals.c \
			splay.c \
			stack.c \
			string.c \
			tables.c \
			thread.c \
			utf.c \
			verify.c \
			version.c \
			vm.c \
			vm_alloc.c \
			zip.c

interp.s:	interp.c
	$(COMPILE) -o $@ -S -c `test -f '$<' || echo '$(srcdir)/'`$<

# Java native method source files
libjc_a_SOURCES+=	native/gnu_classpath_VMStackWalker.c \
			native/gnu_classpath_VMSystemProperties.c \
			native/java_lang_VMClass.c \
			native/java_lang_VMClassLoader.c \
			native/java_lang_VMCompiler.c \
			native/java_lang_VMObject.c \
			native/java_lang_VMRuntime.c \
			native/java_lang_VMSystem.c \
			native/java_lang_VMThread.c \
			native/java_lang_VMThrowable.c \
			native/java_lang_reflect_Constructor.c \
			native/java_lang_reflect_Field.c \
			native/java_lang_reflect_Method.c \
			native/org_dellroad_jc_vm_DebugThread.c \
			native/org_dellroad_jc_vm_FinalizerThread.c

# Architecture specific source files
libjc_a_SOURCES+= arch/@JC_ARCH@/arch_functions.c

# Header files
libjc_a_SOURCES+=	cf_parse.h \
			definitions.h \
			libjc.h \
			inline.h \
			structures.h \
			zip.h

EXTRA_DIST=		version.c

version.c:
	echo '#include "libjc.h"' > $@
	echo 'const unsigned long _jc_svn_revision = '`svn info $(top_srcdir) \
	  | grep '^Last Changed Rev:' | awk '{ print $$NF }'`';' >> $@

AM_CFLAGS=		$(CFLAGS) @JC_CFLAGS@
AM_CFLAGS+=		-D_AC_INCLUDEDIR=\"$(includedir)\"
AM_CFLAGS+=		-D_AC_DATADIR=\"$(datadir)\"
AM_CFLAGS+=		-D_AC_LIBDIR=\"$(libdir)\"
AM_CFLAGS+=		-D_AC_SYSCONFDIR=\"$(sysconfdir)\"
AM_CFLAGS+=		-D_JC_CLASSPATH_HOME=\"@CLASSPATH_HOME@\"

AM_CPPFLAGS=		-I$(top_srcdir)/include
AM_CPPFLAGS+=		-I$(srcdir)/arch
AM_CPPFLAGS+=		-Inative

