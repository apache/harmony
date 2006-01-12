## $Id$

info_TEXINFOS=	jc.texi

htmldir=	$(datadir)/jc/doc
html_DATA=	jc.html

man_MANS=	jc.1

jc.1:		Makefile $(srcdir)/jc.1.in
	rm -f jc.1
	sed -e 's,@libdir\@,$(libdir),g' \
	    -e 's,@datadir\@,$(datadir),g' \
	    -e 's,@sysconfdir\@,$(sysconfdir),g' \
	    $(srcdir)/jc.1.in > jc.1

EXTRA_DIST=	jc.html jc.1.in

clean-local:
	rm -f jc.1

SUFFIXES=	.info .html

.texi.html:
	makeinfo -I $(srcdir) -o $@ --html --no-split $<

