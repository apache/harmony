## $Id$

jcdir=		$(datadir)/jc
apidir=		$(jcdir)/doc/api

jc_DATA=	jc.zip

CLASSPATH=	$(CLASSPATH_HOME)/share/classpath/glibj.zip

JCOMPILE=	$(JIKES) -nowarn +F -bootclasspath '' -extdirs '' -sourcepath '' -classpath $(srcdir):$(CLASSPATH) -d classes

SUBDIRS=	org

EXTRA_DIST=	jc.zip
#EXTRA_DIST+=	api.tgz

#install-data-local:	api.tgz
#	$(mkinstalldirs) $(DESTDIR)$(apidir)
#	$(TAR) xzf $(srcdir)/api.tgz -C $(DESTDIR)$(apidir)

#uninstall-local:
#	rm -rf $(DESTDIR)$(apidir)

jc.zip:
	rm -rf classes
	mkdir classes
	$(JCOMPILE) `find $(srcdir) -name '*.java'`
	( cd classes && $(ZIP) -qD -r ../jc.zip \
	    `find $(SUBDIRS) -name '*.class'`)
	rm -rf classes

JDOC_TITLE=	"JC Virtual Machine"
JDOC_FLAGS=	-classpath $(CLASSPATH) -sourcepath $(srcdir) -d api -doctitle $(JDOC_TITLE) -windowtitle $(JDOC_TITLE) -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api

api.tgz:
	rm -rf api
	mkdir api
	$(JAVADOC) $(JDOC_FLAGS) org.dellroad.jc.vm
	( cd api && $(TAR) czf ../api.tgz * )
	rm -rf api

