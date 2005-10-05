## $Id: Makefile.am,v 1.5 2005/02/27 21:50:04 archiecobbs Exp $

jcbindir=	$(bindir)

jcbin_PROGRAMS=	cfdump

cfdump_SOURCES=	dep.c deps.c main.c support.c cfdump.h

nodist_cfdump_SOURCES=	$(top_srcdir)/libjc/cf_parse.c \
			$(top_srcdir)/libjc/class_bytes.c \
			$(top_srcdir)/libjc/cl_alloc.c \
			$(top_srcdir)/libjc/mutex.c \
			$(top_srcdir)/libjc/splay.c \
			$(top_srcdir)/libjc/tables.c \
			$(top_srcdir)/libjc/misc.c \
			$(top_srcdir)/libjc/utf.c \
			$(top_srcdir)/libjc/vm_alloc.c \
			$(top_srcdir)/libjc/zip.c

AM_CFLAGS=	$(CFLAGS) @JC_CFLAGS@
AM_CFLAGS+=	-D_AC_INCLUDEDIR=\"$(includedir)\"
AM_CFLAGS+=	-D_AC_DATADIR=\"$(datadir)\"
AM_CFLAGS+=	-D_AC_LIBDIR=\"$(libdir)\"
AM_CFLAGS+=	-D_JC_CLASSPATH_HOME=\"@CLASSPATH_HOME@\"
AM_CFLAGS+=	-DSOOT_VERSION=\"@SOOT_VERSION@\"
AM_CFLAGS+=	-DJASMIN_VERSION=\"@JASMIN_VERSION@\"
AM_CFLAGS+=	-DPOLYGLOT_VERSION=\"@POLYGLOT_VERSION@\"

AM_CPPFLAGS=	-I$(top_srcdir)/libjc
AM_CPPFLAGS+=	-I$(top_srcdir)/libjc/arch

