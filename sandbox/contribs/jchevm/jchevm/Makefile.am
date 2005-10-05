## $Id: Makefile.am,v 1.6 2005/04/01 00:00:30 archiecobbs Exp $

EXTRA_DIST=	AUTHORS CHANGES README LGPL-2.1 LICENSE INSTALL \
		install-sh libtool missing

SUBDIRS=	tools libjc jc include doc etc java soot jsrc

##
## This replaces the string "@COPYRIGHT@" in each source file
## with the copyright text from the top level COPYRIGHT file
## On Linux, replace "sed -E" with "sed -r".
##
COPYRIGHT_TAG=	@COPYRIGHT@
COPYRIGHT_FILE=	$(srcdir)/COPYRIGHT

dist-hook:
	FILES=`find $(distdir) -type f -print`; \
	if [ `uname -s` = 'Linux' ]; then \
	    SEDEXT="-r"; \
	else \
	    SEDEXT="-E"; \
	fi; \
	for f in $${FILES}; do \
	    if [ "$${f}" != "$(distdir)/Makefile.am" ] \
	      && [ "$${f}" != "$(distdir)/Makefile.in" ] \
	      && [ "$${f}" != "$(distdir)/Makefile" ] \
	      && grep -q '${COPYRIGHT_TAG}' $${f}; then \
		echo substituting '${COPYRIGHT_TAG}' in $${f}; \
		LINE=`grep -n '${COPYRIGHT_TAG}' $${f} \
		    | head -1 | awk -F: '{print $$1}'`; \
		TAIL=`grep -n '${COPYRIGHT_TAG}' $${f} \
		    | head -1 | awk -F: '{print $$2}' \
		    | sed -e 's/\\\\/\\\\\\\\/g' -e 's/"/\\\\"/g'`; \
		PREFX=`echo "$${TAIL}" \
		    | sed $${SEDEXT} 's/^(.*)${COPYRIGHT_TAG}(.*)$$/\\1/g'`; \
		SUFFX=`echo "$${TAIL}" \
		    | sed $${SEDEXT} 's/^(.*)${COPYRIGHT_TAG}(.*)$$/\\2/g'`; \
		if [ "$${LINE}" -gt 1 ]; then \
			head -`expr $${LINE} - 1` < $${f} > $${f}.new; \
		else \
			cat /dev/null > $${f}.new; \
		fi; \
		awk "{ printf \"%s%s%s\\n\", \
		    \"$${PREFX}\", \$$0, \"$${SUFFX}\" }" \
		    < ${COPYRIGHT_FILE} >> $${f}.new; \
		tail +`expr $${LINE} + 1` < $${f} >> $${f}.new; \
		mv $${f}.new $${f}; \
	    fi; \
	done

