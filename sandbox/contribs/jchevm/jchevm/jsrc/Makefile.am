## $Id: Makefile.am,v 1.26 2005/05/16 14:23:05 archiecobbs Exp $

jcbindir=	$(bindir)
jcincdir=	$(includedir)/jc
jcsrcdir=	$(datadir)/jc/src
jcobjdir=	$(libdir)/jc/obj
jcdir=		$(datadir)/jc
cpdir=		$(CLASSPATH_HOME)/share/classpath

CLASSPATH=	$(top_srcdir)/java/jc.zip:$(cpdir)/glibj.zip:$(top_srcdir)/soot/sootclasses-$(SOOT_VERSION).jar:$(top_srcdir)/soot/jasminclasses-sable-$(JASMIN_VERSION).jar:$(top_srcdir)/soot/polyglot-$(POLYGLOT_VERSION).jar

# Same as CLASSPATH but without classpath's core Java libraries
VM_CPATH=	$(top_srcdir)/java/jc.zip:$(top_srcdir)/soot/sootclasses-$(SOOT_VERSION).jar:$(top_srcdir)/soot/jasminclasses-sable-$(JASMIN_VERSION).jar:$(top_srcdir)/soot/polyglot-$(POLYGLOT_VERSION).jar

COMPILEFLAGS=   -c -O2 -pipe -g -fno-common -w -Wall -Waggregate-return \
		-Wcast-align -Wcast-qual -Wchar-subscripts -Wcomment \
		-Wformat -Wimplicit -Wmissing-declarations \
		-Wmissing-prototypes -Wnested-externs -Wno-long-long \
		-Wparentheses -Wpointer-arith -Wreturn-type -Wshadow \
		-Wstrict-prototypes -Wswitch -Wtrigraphs -Wuninitialized \
		-Wunused -Wwrite-strings -Wa,-W -I$(jcincdir) -I$(jcsrcdir)

EXTRA_DIST=	jsrc.tgz patterns

##
## Install pre-generated source files and compile them
##
install-exec-local:	jsrc.tgz patterns
	$(mkinstalldirs) $(DESTDIR)$(jcsrcdir)
	$(mkinstalldirs) $(DESTDIR)$(jcobjdir)
	@if [ "$(JC_PREGENERATED_SOURCES)" = "yes" ]; then \
	    if [ ! -d $(DESTDIR)$(jcsrcdir)/java ]; then \
		echo '***' Installing C source files in $(DESTDIR)$(jcsrcdir); \
		echo $(TAR) zxf $(srcdir)/jsrc.tgz -C $(DESTDIR)$(jcsrcdir); \
		$(TAR) zxf $(srcdir)/jsrc.tgz -C $(DESTDIR)$(jcsrcdir); \
	    fi; \
	    echo '***' Generating ELF objects in $(DESTDIR)$(jcobjdir); \
	    if [ -x "$(JAVA)" ]; then \
		$(DESTDIR)$(jcbindir)/jcgen -vmopt -mx400m \
		    -vmclasspath $(VM_CPATH) \
		    -jcclasspath $(CLASSPATH) \
		    -newsrcpath $(DESTDIR)$(jcsrcdir) \
		    -objdir $(DESTDIR)$(jcobjdir) \
		    -incdir $(DESTDIR)$(jcincdir) \
		    `cat $(srcdir)/patterns`; \
	    else \
		find $(DESTDIR)$(jcsrcdir) -name '*.c' | while read CFILE; do \
		    OFILE=`echo $$CFILE | sed -e 's/\.c$$/.o/g'`; \
		    ODIR=`dirname $$OFILE`; \
		    [ -d "$$ODIR" ] || $(mkinstalldirs) $$ODIR; \
		    echo $(HOSTGCC) $(COMPILEFLAGS) -o $$OFILE $$CFILE; \
		    $(HOSTGCC) $(COMPILEFLAGS) -o $$OFILE $$CFILE; \
		done; \
	    fi; \
	fi

uninstall-local:
	rm -rf $(DESTDIR)$(jcsrcdir)
	rm -rf $(DESTDIR)$(jcobjdir)

##
## Create and tar up pre-generated sources
##
jsrc.tgz:
	@if [ ! -d patterns ]; then \
	    echo ${MAKE} patterns; \
	    ${MAKE} patterns; \
	fi
	@if [ ! -d sources ]; then \
	    echo mkdir sources; \
	    mkdir sources; \
	fi
	$(DESTDIR)$(jcbindir)/jcgen -N -vmopt -mx400m \
	    -newsrcpath sources `cat patterns`
	( cd sources && tar cf - * | gzip --best ) > $@

##
## Generate list of all packages containing installed classes
##
patterns:
	@echo '***' Generating package list
	@rm -f patterns patterns.1
	@ZIPS=`echo $(CLASSPATH) | sed 's/:/ /g'`; \
	for ZIP in $${ZIPS}; do \
	    $(UNZIP) -l $${ZIP} | grep '/.*\.class$$' \
	      | sed -e 's/^.* //g' -e 's,/.*$$,/%%,g' >> patterns.1; \
	done; \
	sort -u < patterns.1 > patterns; \
	rm -f patterns.1

CLEANFILES=	sources objects patterns

