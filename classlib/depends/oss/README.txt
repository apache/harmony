README
======

This directory contains third party open source software which has been placed
into three separate source archives. During the build of the class library 
native source, the contents of these three archives will be copied to the
appropriate part of the source tree for inclusion in the build process.

Keeping these OSS source files distinct from the rest of the native source tree
helps distinguish third party content from the source of the project. The OSS files
may be licensed under terms other than the Apache License V2.  Some small
modifications to the included OSS source files were required. These changes are
discussed below.

For information about the licenses of the three OSS libraries discussed in this
document please refer to the file THIRD_PARTY_NOTICES.txt.



FDLIBM version 5.3
------------------
FDLIBM (Freely Distributable LIBM) is a C math library for supporting IEEE 754
floating-point arithmetic. File fdlibm53.tar.gz contains the original source 
as downloaded from the FDLIBM home page.

The official FDLIBM page is located at http://www.netlib.org/fdlibm/



ZLIB version 1.2.3
------------------
zlib is a freely available compression library. The file zlib_1.2.3.zip
contains almost the original source for version 1.2.3 as downloaded from the
zlib home page. So as not to cause confusion with the makefile developed for
this original code contribution the original Makefile from zlib 1.2.3 has been
renamed Makefile.orig.  The original /contrib directory has been removed.

The official zlib home page is located at http://www.gzip.org/zlib
