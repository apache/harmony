
/* @(#)s_lib_version.c 1.3 95/01/18 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 */

/*
 * MACRO for standards
 */

#include "fdlibm.h"

/*
 * define and initialize _LIB_VERSION
 */

/* Local Change -- insist that only the IEEE version is built. See also fdlibm.h */
#ifndef HY_FIXED_VERSION

#ifdef _POSIX_MODE
_LIB_VERSION_TYPE _LIB_VERSION = _POSIX_;
#else
#ifdef _XOPEN_MODE
_LIB_VERSION_TYPE _LIB_VERSION = _XOPEN_;
#else
#ifdef _SVID3_MODE
_LIB_VERSION_TYPE _LIB_VERSION = _SVID_;
#else					/* default _IEEE_MODE */
_LIB_VERSION_TYPE _LIB_VERSION = _IEEE_;
#endif
#endif
#endif

#endif /* !HY_FIXED_VERSION */
