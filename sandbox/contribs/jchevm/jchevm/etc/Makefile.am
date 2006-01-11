## $Id$

jcetcdir=		$(sysconfdir)

dist_jcetc_DATA=	jc.conf

EXTRA_DIST=		gdb.userdef makedist.sh jc.xpm \
			classpath.spec.in jcvm.spec.in

rpms:			classpath-rpm jcvm-rpm

classpath-rpm:		rpmdirs
	DIR=`pwd`; \
	rpmbuild -bb --clean --define "_topdir $${DIR}/rpm" classpath.spec

jcvm-rpm:		rpmdirs
	DIR=`pwd`; \
	cp $(srcdir)/jc.xpm $${DIR}/rpm/SOURCES; \
	rpmbuild -bb --clean --define "_topdir $${DIR}/rpm" jcvm.spec

rpmdirs:
	@for DIR in rpm rpm/BUILD rpm/RPMS rpm/SOURCES rpm/SPECS rpm/SRPMS; do \
	    [ -d "$${DIR}" ] || mkdir "$${DIR}"; \
	done
	@RESULT="true"; \
	for TGZ in classpath-$(CLASSPATH_VERSION) jc-$(VERSION); do \
	    if ! [ -f rpm/SOURCES/$$TGZ.tar.gz ]; then \
		echo Please copy $$TGZ.tar.gz into rpm/SOURCES; \
		RESULT="false"; \
	    fi; \
	done; \
	$$RESULT

