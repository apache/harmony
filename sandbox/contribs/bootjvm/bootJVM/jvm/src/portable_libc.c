/*!
 * @file portable_libc.c
 *
 * @brief Portable version of @b system(2) calls and @b library(3)
 * utility functions.
 *
 * Isolate all section 2 system calls and section 3 library functions
 * from the normal compilation environment so as to eliminate all
 * questions about @b libc compilation modes, especially use of
 * structure packing.  For GCC, this means how the compile option
 * <b>-fpack-struct</b> was or was not used.
 *
 * Operands are passed in using the type definitions as applicable to
 * the application code.  They are then cast into the types required
 * for library calls.  For example, a @link #jfloat jfloat@endlink
 * will be cast into a @c @b float .  This is so that the system calls
 * and library functions will see the @e exact type that they require
 * in their definition.  All casting is done explicitly.
 *
 * In order to reduce visibility of data structures that might have
 * packing issues, all structure references have been abstracted
 * to passing (@link #rvoid rvoid@endlink *) pointers in and out.
 * This is applicable @e only to functions that do not already
 * pass back a pointer.
 *
 * These abstracted pointers will point to blocks allocated with
 * @link #HEAP_GET_DATA() HEAP_GET_DATA@endlink and must be
 * freed with @link #HEAP_FREE_DATA() HEAP_FREE_DATA()@endlink
 * when they are no longer needed.  For this reason, this portability
 * library is @e not an exact, drop-in replacement for the library
 * functions that it impliments.
 *
 * As example of this abstraction, @c @b stat(2) is passed a structure
 * pointer for an area to fill.  In order to avoid packing errors, the
 * portable version @link #portable_stat() portable_stat()@endlink
 * implements this abstraction.  On the other hand, @c @b fopen(3)
 * passes back a pointer in its current definition and does not need
 * the abstraction beyond changing the pointer type to
 * (@link #rvoid rvoid@endlink *).  Simply pass the pointer to
 * all related function.
 *
 *
 * @todo HARMONY-6-jvm-portable_libc.c-1 In order to present a
 *       systematic and regular implementation across all external
 *       library references, the standard I/O entry points
 *       @c @b portable_fprintf() and so forth should
 *       probably be written to point over to the existing
 *       standard I/O function replacements in
 *       @link jvm/src/stdio.c stdio.c@endlink.
 *
 * @see @link jvm/src/portable_libm.c portable_libm.c@endlink
 *
 * @see @link jvm/src/portable_jmp_buf.c portable_jmp_buf.c@endlink
 *
 * @see @link jvm/src/portable.h portable.h@endlink
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <unistd.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(portable_libc, c,
"$URL$",
"$Id$");

 
#define I_AM_PORTABLE_C /* Suppress function name remapping */
#include "jvmcfg.h"
#include "exit.h"
#include "heap.h"

/*!
 * @name Level 1 file access
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b mkdir(2) system call
 *
 *
 * @param path   Path name of directory to create, relative or absolute.
 *
 * @param mode   Flag for mkdir creation mode.
 *
 *
 * @returns 0 if successful or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_mkdir(const rchar *path, rint mode)
{
    ARCH_FUNCTION_NAME(portable_mkdir);

    char *pathlocal = (char *) path;
    int   modelocal = (int) mode;

    int rc = mkdir(pathlocal, modelocal);

    return((rint) rc);

} /* END of portable_mkdir() */


/*!
 * @brief Portable replacement for @c @b rmdir(2) system call
 *
 *
 * @param path   Path name of directory to remove, relative or absolute.
 *
 *
 * @returns 0 if successful or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_rmdir(const rchar *path)
{
    ARCH_FUNCTION_NAME(portable_rmdir);

    char *pathlocal = (char *) path;

    int rc = rmdir(pathlocal);

    return((rint) rc);

} /* END of portable_rmdir() */


/*!
 * @brief Portable replacement for @c @b stat(2) system call
 *
 * @attention This function @b requires freeing of a result pointer
 *            with @link #HEAP_FREE_DATA() HEAP_FREE_DATA()@endlink
 *            when it is no longer needed.
 *
 *
 * Notice that this function returns an (@link #rvoid rvoid@endlink *)
 * with its result.
 *
 * @param path   Path name of file, relative or absolute.
 *
 *
 * @returns (@link #rvoid rvoid@endlink *) to status buffer
 *          if successful or @link #rnull rnull@endlink with
 *          @c @b errno set to report the error that occurred.
 *
 */
rvoid *portable_stat(const rchar *path)
{
    ARCH_FUNCTION_NAME(portable_stat);

    char        *pathlocal = (char *) path;

    struct stat *buflocal  = HEAP_GET_DATA(sizeof(struct stat), rfalse);

    int rc = stat(pathlocal, buflocal);

    if (0 == rc)
    {
        return((rvoid *) buflocal);
    }
    else
    { 
        HEAP_FREE_DATA(buflocal);
        return((rvoid *) rnull);
    }

} /* END of portable_stat() */


/*!
 * @brief Portable replacement for reading
 * <b><code>(struct stat).st_size</code></b>
 *
 *
 * @param statbfr   Pointer to structure previously reported
 *                  by @link #portable_stat() portable_stat()@endlink
 *
 *
 * @returns @c @b st_size value from @c @b stat structure.
 *
 */
rlong portable_stat_get_st_size(rvoid *statbfr)
{
    ARCH_FUNCTION_NAME(portable_stat_get_st_size);

    struct stat *statbfrlocal = (struct stat *) statbfr;

    long rc = (long) statbfrlocal->st_size;

    return((rlong) rc);

} /* END of portable_stat_get_st_size() */


/*!
 * @brief Portable replacement for @c @b open(2) system call
 *
 * The optional third parameter is not supported only because it
 * is not needed.  It may be added if desired.
 *
 *
 * @param path   Path name of file, relative or absolute.
 *
 * @param oflag  Flag for file open modes.
 *
 *
 * @returns File handle of opened file or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_open(const rchar *path, rint oflag)
{
    ARCH_FUNCTION_NAME(portable_open);

    char *pathlocal = (char *) path;
    int   oflaglocal = (int) oflag;

    int rc = open(pathlocal, oflaglocal);

    return((rint) rc);

} /* END of portable_open() */


/*!
 * @brief Portable replacement for @c @b lseek(2) system call
 *
 *
 * @param[in]  fildes  Integer file handle of open file.
 *
 * @param[in]  offset  Offset in bytes of distance into file.
 *
 * @param[in]  whence  Seek mode-- SEEK_SET, SEEK_CUR, SEEK_END.
 *
 *
 * @returns Requested offset into file or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rlong portable_lseek(rint fildes, rlong offset, rint whence)
{
    ARCH_FUNCTION_NAME(portable_lseek);

    int   fildeslocal = (int)   fildes;
    off_t offsetlocal = (off_t) offset;
    int   whencelocal = (int)   whence;

    long rc = lseek(fildeslocal, offsetlocal, whencelocal);

    return((rlong) rc);

} /* END of portable_lseek() */


/*!
 * @brief Portable replacement for @c @b read(2) system call
 *
 *
 * @param[in]  fildes  Integer file handle of open file.
 *
 * @param[out] buf     Location to store data read in.
 *
 * @param[in]  nbyte   Number of bytes to read.
 *
 *
 * @returns Number of bytes read or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rlong portable_read(rint fildes, rvoid *buf, rlong nbyte)
{
    ARCH_FUNCTION_NAME(portable_read);

    int   fildeslocal = (int) fildes;
    char *buflocal    = (char *) buf;
    long  nbytelocal  = (long) nbyte;

    long rc = read(fildeslocal, buflocal, nbytelocal);

    return((rlong) rc);

} /* END of portable_read() */


/*!
 * @brief Portable replacement for @c @b close(2) system call
 *
 *
 * @param fildes      Integer file handle of open file.
 *
 *
 * @returns 0 if successful or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_close(rint fildes)
{
    ARCH_FUNCTION_NAME(portable_close);

    int   fildeslocal = (int) fildes;

    int rc = close(fildeslocal);

    return((rint) rc);

} /* END of portable_close() */


/*@} */ /* End of grouped definitions */


/*!
 * @name Level 2 file access
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b fopen(3) library function
 *
 *
 * @param filename   Path name of file, relative or absolute.
 *
 * @param mode       String for file open modes.
 *
 *
 * @returns (@link #rvoid rvoid@endlink *) to standard I/O stream
 *          structure if successful or @link #rnull rnull@endlink with
 *          @c @b errno set to report the error that occurred.
 *
 */
rvoid *portable_fopen(const rchar *filename, const rchar *mode)
{
    ARCH_FUNCTION_NAME(portable_fopen);

    char *filenamelocal = (char *) filename;
    char *modelocal     = (char *) mode;

    FILE *rc = fopen(filenamelocal, modelocal);

    return((rvoid *) rc);

} /* END of portable_fopen() */


/*!
 * @brief Portable replacement for @c @b fgets(3) library function
 *
 *
 * @param[out] s      Output buffer to receive data read in.
 *
 * @param[in]  n      Number of bytes to read.
 *
 * @param[in]  stream FILE stream pointer returned by
 *                    @link #portable_fopen() portable_fopen()@endlink
 *
 *
 * @returns @b s if successful or @link #rnull rnull@endlink
 *          if EOF or error.  If error, @c @b errno is also set
 *          to report the error that occurred.
 *
 */
rchar *portable_fgets(rchar *s, rint n, rvoid *stream)
{
    ARCH_FUNCTION_NAME(portable_fgets);

    char *slocal      = (char *) s;
    int   nlocal      = (int) n;
    FILE *streamlocal = (FILE *) stream;

    char *rc = (char *) fgets(slocal, nlocal, streamlocal);

    return((rchar *) rc);

} /* END of portable_fgets() */


/*!
 * @brief Portable replacement for @c @b fclose(3) library function
 *
 *
 * @param stream  FILE stream pointer returned by
 *                @link #portable_fopen() portable_fopen()@endlink
 *
 *
 * @returns @b s if successful or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_fclose(rvoid *stream)
{
    ARCH_FUNCTION_NAME(portable_fclose);

    FILE *streamlocal = (FILE *) stream;

    int rc = (int) fclose(streamlocal);

    return((rint) rc);

} /* END of portable_fclose() */


/*@} */ /* End of grouped definitions */


/*!
 * @name Shell and process control
 *
 */

/*@{ */ /* Begin grouped definitions */


/*!
 * @brief Portable replacement for @c @b getwd(3) library function
 *
 *
 * @param[out] path_name  Buffer to hold name of current working
 *                        directory.
 *
 *
 * @returns @c @b path_name if successful or @link #rnull rnull@endlink
 *          if an error occurred.  @c @b errno is not set.
 *
 */
rchar *portable_getwd(rchar *path_name)
{
    ARCH_FUNCTION_NAME(portable_getwd);

    char *path_name_local = (char *) path_name;

    char *rc = getwd(path_name_local);

    return((rchar *) rc);

} /* END of portable_getwd() */


/*!
 * @brief Portable replacement for @c @b getenv(3) library function
 *
 *
 * @param name   Null-terminated string containing the name of an
 *               environment variable to read.
 *
 *
 * @returns @c @b name if successful or @link #rnull rnull@endlink
 *          if an error occurred.  @c @b errno is not set.
 *
 */
rchar *portable_getenv(const rchar *name)
{
    ARCH_FUNCTION_NAME(portable_getenv);

    char *namelocal = (char *) name;

    char *rc = getenv(namelocal);

    return((rchar *) rc);

} /* END of portable_getenv() */


/*!
 * @brief Portable replacement for @c @b getpid(2) system call
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns Process ID number of current process or
 *          <b><code>((ruint) -1)</code></b> with @c @b errno set
 *          to report the error that occurred.
 *
 * @warning The return type of @c @b getpid(2) is typically
 *          platform-specific, even if only marginally so.
 *          Verify the proper size when porting.
 *
 */
rlong  portable_getpid(rvoid)
{
    ARCH_FUNCTION_NAME(portable_getpis);

    pid_t rc = getpid();

    return((rlong) rc);

} /* END of portable_getpid() */


/*!
 * @brief Portable replacement for @c @b system(3) library function
 *
 * @param string  Shell command string to execute.
 *
 *
 * @returns File handle of opened file or -1 with @c @b errno set
 *          to report the error that occurred.
 *
 */
rint portable_system(const rchar *string)
{
    ARCH_FUNCTION_NAME(portable_system);

    char *stringlocal = (char *) string;

    int rc = system(stringlocal);

    return((rint) rc);

} /* END of portable_system() */


/*!
 * @brief Portable replacement for @c @b sleep(3) library function
 *
 *
 * @param seconds  Number of seconds to sleep.
 *
 *
 * @returns Number of seconds from @c @b seconds that did not
 *          expire before return, typically due to a signal or
 *          other event.
 *
 */
ruint  portable_sleep(ruint seconds)
{
    ARCH_FUNCTION_NAME(portable_sleep);

    unsigned int   secondslocal = (unsigned int) seconds;

    unsigned int rc = sleep(secondslocal);

    return((ruint) rc);

} /* END of portable_sleep() */


/*@} */ /* End of grouped definitions */


/*!
 * @name String manipulation
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b strchr(3) library function
 *
 *
 * @param s  Null-terminated string to evaluate
 *
 * @param c  Character to locate in @c @b s buffer
 *
 *
 * @returns the first occurrence in @c @b s of @c @b c or
 *          @link #rnull rnull@endlink if not found.
 *
 */
rchar *portable_strchr(const rchar *s, rint c)
{
    ARCH_FUNCTION_NAME(portable_strchr);

    char *slocal = (char *) s;
    int   clocal = (int)    c;

    char *rc = strchr(slocal, clocal);

    return((rchar *) rc);

} /* END of portable_strchr() */


/*!
 * @brief Portable replacement for @c @b strrchr(3) library function
 *
 *
 * @param s  Null-terminated string to evaluate
 *
 * @param c  Character to locate in @c @b s buffer
 *
 *
 * @returns the last occurrence in @c @b s of @c @b c or
 *          @link #rnull rnull@endlink if not found.
 *
 */
rchar *portable_strrchr(const rchar *s, rint c)
{
    ARCH_FUNCTION_NAME(portable_strrchr);

    char *slocal = (char *) s;
    int   clocal = (int)    c;

    char *rc = strrchr(slocal, clocal);

    return((rchar *) rc);

} /* END of portable_strrchr() */


/*!
 * @brief Portable replacement for @c @b strcmp(3) library function
 *
 *
 * @param s1  First null-terminated string to evaluate
 *
 * @param s2  Second null-terminated string to evaluate
 *
 *
 * @returns lexicographical difference between first pair of
 *          differing bytes between the strings, or 0 if identical.
 *
 */
rint portable_strcmp(const rchar *s1, const rchar *s2)
{
    ARCH_FUNCTION_NAME(portable_strcmp);

    char   *s1local = (char *) s1;
    char   *s2local = (char *) s2;

    int rc = strcmp(s1local, s2local);

    return((rint) rc);

} /* END of portable_strcmp() */


/*!
 * @brief Portable replacement for @c @b strncmp(3) library function
 *
 *
 * @param s1  First null-terminated string to evaluate
 *
 * @param s2  Second null-terminated string to evaluate
 *
 * @param n   Number of bytes in the two strings to compare
 *
 *
 * @returns lexicographical difference between first pair of
 *          differing bytes between the strings, or 0 if identical.
 *
 */
rint portable_strncmp(const rchar *s1, const rchar *s2, rlong n)
{
    ARCH_FUNCTION_NAME(portable_strncmp);

    char   *s1local = (char *) s1;
    char   *s2local = (char *) s2;
    size_t  nlocal  = (size_t) n;

    int rc = strncmp(s1local, s2local, nlocal);

    return((rint) rc);

} /* END of portable_strncmp() */


/*!
 * @brief Portable replacement for @c @b strlen(3) library function
 *
 *
 * @param s  Null-terminated string to evaluate
 *
 *
 * @returns the number of characters found in @c @b s buffer
 *
 */
rlong portable_strlen(const rchar *s)
{
    ARCH_FUNCTION_NAME(portable_strlen);

    char *slocal = (char *) s;

    size_t rc = strlen(slocal);

    return((rlong) rc);

} /* END of portable_strlen() */


/*!
 * @brief Portable replacement for @c @b strcat(3) library function
 *
 *
 * @param[in,out] s1  Destination null-terminated string whence @c @b s2
 *                    contents are appended
 *
 * @param[in] s2     Source null-terminated string to append to @c @b s1
 *                    buffer
 *
 *
 * @returns @c @b s1
 *
 */
rchar *portable_strcat(rchar *s1, const rchar *s2)
{
    ARCH_FUNCTION_NAME(portable_strcat);

    char   *s1local = (char *) s1;
    char   *s2local = (char *) s2;

    char *rc = strcat(s1local, s2local);

    return((rchar *) rc);

} /* END of portable_strcat() */


/*!
 * @brief Portable replacement for @c @b strcpy(3) library function
 *
 *
 * @param[out] s1  Destination null-terminated string buffer
 *                 into which @c @b s2 contents are copied
 *
 * @param[in] s2   Source null-terminated string to copy into @c @b s1
 *                 buffer
 *
 *
 * @returns @c @b s1
 *
 */
rchar *portable_strcpy(rchar *s1, const rchar *s2)
{
    ARCH_FUNCTION_NAME(portable_strcpy);

    char   *s1local = (char *) s1;
    char   *s2local = (char *) s2;

    char *rc = strcpy(s1local, s2local);

    return((rchar *) rc);

} /* END of portable_strcpy() */


/*@} */ /* End of grouped definitions */

/*!
 * @name Memory manipulation
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b memcpy(3) library function
 *
 *
 * @param[out] s1  Destination buffer to receive contents of @c @b s2
 *                 buffer
 *
 * @param[in] s2   Source buffer to copy to @c @b s1 buffer
 *
 * @param[in] n   Number of bytes to copy from one buffer to the other
 *
 *
 * @returns @c @b s1
 *
 */
rvoid *portable_memcpy(rvoid *s1, const rvoid *s2, rlong n)
{
    ARCH_FUNCTION_NAME(portable_memcpy);

    void   *s1local = (void *) s1;
    void   *s2local = (void *) s2;
    size_t  nlocal  = (size_t) n;

    void *rc = memcpy(s1local, s2local, nlocal);

    return((rvoid *) rc);

} /* END of portable_memcpy() */


/*@} */ /* End of grouped definitions */


/*!
 * @name C type library
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b atol(3) library function
 *
 *
 * @param str  Null-terminated string to convert
 *
 *
 * @returns integer equivalent of @c @b s2 or 0 if error and @c @b errno
 *          will be set to indicate error.
 *
 */
rlong portable_atol(const rchar *str)
{
    ARCH_FUNCTION_NAME(portable_atol);

    char   *strlocal = (char *) str;

    long rc = atol(strlocal);

    return((rlong) rc);

} /* END of portable_atol() */


/*!
 * @brief Portable replacement for @c @b isspace(3) library function
 *
 *
 * @param c  Character to evaluate
 *
 *
 * @returns non-zero if @c @b c is a white space character,
 *          otherwise 0.  If @c @b c is not in the domain of
 *          comparison, the result is undefined.
 *
 */
rint portable_isspace(rint c)
{
    ARCH_FUNCTION_NAME(portable_isspace);

    int clocal = (int) c;

    int rc = isspace(clocal);

    return((rint) rc);

} /* END of portable_isspace() */


/*@} */ /* End of grouped definitions */


/*!
 * @name Memory allocation
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b malloc(3) library function
 *
 *
 * @param size  Number of bytes to allocate
 *
 *
 * @returns pointer to allocated memory area if successful,
 *          otherwise @link #rnull rnull@endlink.
 *
 */
rvoid *portable_malloc(rlong size)
{
    ARCH_FUNCTION_NAME(portable_malloc);

    size_t sizelocal = (size_t) size;

    void *rc = malloc(sizelocal);

    return((rvoid *) rc);

} /* END of portable_malloc() */


/*!
 * @brief Portable replacement for @c @b free(3) library function
 *
 *
 * @param ptr  Pointer to buffer to free
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid portable_free(rvoid *ptr)
{
    ARCH_FUNCTION_NAME(portable_free);

    void   *ptrlocal = (void *) ptr;

    free(ptrlocal);

    return;

} /* END of portable_free() */


/*@} */ /* End of grouped definitions */


/* EOF */
