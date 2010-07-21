/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * DESCRIPTION:
 * Generic ASCII to EBCDIC character conversion. This file defines
 * a number of "wrapper" functions to transparently convert between
 * ASCII and EBCDIC. Each function calls a platform specific function
 * (defined elsewhere) to perform the actual conversion.
 * ===========================================================================
 */

/*
 * ======================================================================
 * Disable the redefinition of the system IO functions, this
 * prevents two ATOE functions calling themselves.
 * ======================================================================
 */
#undef HY_ATOE

/*
 * ======================================================================
 * Include all system header files.
 * ======================================================================
 */
#define _OPEN_SYS_FILE_EXT  /* For SETCVTOFF */ 
#ifndef __SUSV3  /* For dlfcn.h */
#define __SUSV3
#endif
#include <unistd.h>
#include <fcntl.h>          /* <--SETCVTOFF in here */
#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <utime.h>
#include <pwd.h>
#include <grp.h>
#include <dirent.h>
#include <stdarg.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <locale.h>
#include <netdb.h>
#include <dll.h>
#include <errno.h>
#include <iconv.h>
#include <langinfo.h>
#include <arpa/inet.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <sys/ipc.h>
#include <limits.h>             
#include <_Ccsid.h>
#include <dlfcn.h>

#define check_fcntl_init() (1)
#define set_SETCVTOFF()
#define use_fcntl (0)

int env_mutex_initialized = 0;

/* Whether file tagging enabled by user */
static int fileTaggingEnabled = 0; 
/* CCSID for new files */
static __ccsid_t newFileCCSID = 0;


/*
 * ======================================================================
 * ASCII<=>EBCDIC translate tables built using iconv()
 * ======================================================================
 */
#define BUFLEN 6144
#define CONV_TABLE_SIZE 256
char a2e_tab[CONV_TABLE_SIZE];
char e2a_tab[CONV_TABLE_SIZE];

/*
 * ======================================================================
 * Define ae2,e2a,a2e_string, e2a_string
 * ======================================================================
 */
#include "atoe.h"

int   atoe_vsnprintf(char *str, size_t count, const char *fmt, va_list args);
int   atoe_fprintf(FILE *, const char *, ...);
struct passwd *etoa_passwd(struct passwd *e_passwd);

/*
 * ======================================================================
 * A simple linked list of ascii environment variable entries;
 * each entry is added as a result of a getenv() or putenv() as
 * reflected through the EBCDIC environment maintained by the system.
 * This improves performance by having each environment variable
 * converted only once, and also avoids storage leaks with repeated
 * calls which refer to the same variable.
 * ======================================================================
 */
struct ascii_envvar {
    struct ascii_envvar *next;
    char *name;
    char *value;
};
typedef struct ascii_envvar envvar_t;

/* Anchor entry */
envvar_t env_anchor = {NULL, NULL, NULL};

/* Mutex for single-threading list updates */
pthread_mutex_t env_mutex;


char* sysTranslate(const char *source, int length, char *trtable, char* xlate_buf)
{
    return sysTranslateASM( source, length, trtable, xlate_buf );
}


/**************************************************************************
 * name        - enumEnvvar
 * description -
 *              enumEnvvar() takes an input pointer that is either null for
 *              the first call or contains the result returned from a previous
 *              call. It will also modify two input pointer parameters to point
 *              to the current envar name string and the envar value string. The
 *              strings will contain ascii characters.
 * parameters  -
 * returns     -
 *************************************************************************/
void *
enumEnvvar(void * input_ptr, char ** name, char ** value) {
    if (0 == env_anchor.next ) {   /* empty envar list case */
        *name = 0;
        *value= 0;
        return(void *) 0;
    }
    if (0 == input_ptr) {      /* non-empty list, first call case */
        *name = env_anchor.name;
        *value = env_anchor.value;
        return(void *) env_anchor.next;
    } else {                                   /* walk the list case */
        envvar_t * curr_env= (envvar_t *)input_ptr;
        *name = curr_env->name;
        *value = curr_env->value;
        return(void *) curr_env->next;  /* next = 0 signals end of list */
    }
}

/**************************************************************************
 * name        - getEnvvar
 * description -
 *              getEnvvar() - returns a pointer to the ascii_envvar structure
 *              for the environment variable specified by name, or NULL if there
 *              is no entry for that name. No need to serialise this code.
 * parameters  -
 * returns     -
 *************************************************************************/
envvar_t *
getEnvvar(char *name)
{
    envvar_t *envvar = env_anchor.next;

    while (envvar != NULL) {
        if (strcmp(name, envvar->name) == 0)
            break;
        envvar = envvar->next;
    }

    return envvar;
}

/**************************************************************************
 * name        - putEnvvar
 * description -
 *              putEnvvar() - updates the ascii_envvar structure for the variable
 *              specified by name if it already exists, or creates a new ascii_envvar
 *              structure if it does not, and returns a pointer to the entry.
 *              This function is single_threaded.
 * parameters  -
 * returns     -
 *************************************************************************/
envvar_t *
putEnvvar(char *name, char *value, envvar_t *updvar)
{
    envvar_t *envvar;

    if (!env_mutex_initialized) {
        /* First time in - initialise the mutex */
        pthread_mutexattr_t attr;

        pthread_mutexattr_init(&attr);
        pthread_mutex_init(&env_mutex, &attr);

        env_mutex_initialized = 1;
    }

    /* Grab the lock */
    pthread_mutex_lock(&env_mutex);

    if (updvar != NULL) {
        /* Update the existing entry */
        envvar = updvar;
        free(envvar->value);
        envvar->value = strdup(value);
    } else {
        /* Add a new entry to the list */
        envvar = &env_anchor;
        while (envvar->next != NULL) {
            envvar = envvar->next;
        }
        envvar->next = malloc(sizeof(envvar_t));
        envvar = envvar->next;
        envvar->next = NULL;
        envvar->name = strdup(name);
        envvar->value = strdup(value);
    }

    /* Release the lock */
    pthread_mutex_unlock(&env_mutex);

    return envvar;
}

/**************************************************************************
 * name        - a2e_func 
 * description - Function implementation of a2e macro, for use by DLLs
 *               that don't want to have to link with sysTranslate.o
 * parameters  - str, ASCII string to convert
 *               len, Length of str
 * returns     - Converted EBCDIC string
 *************************************************************************/
char *
a2e_func(char *str, int len)
{
    return a2e(str, len);
}

/**************************************************************************
 * name        - e2a_func 
 * description - Function implementation of e2a macro, for use by DLLs
 *               that don't want to have to link with sysTranslate.o
 * parameters  - str, EBCIDC string to convert
 *               len, Length of str
 * returns     - Converted ASCII string
 *************************************************************************/
char *
e2a_func(char *str, int len)
{
    return e2a(str, len);
}

/**************************************************************************
 * name        - atoe_getenv
 * description -
 *              Returns a pointer to the value of the environment
 *              variable specified by a_name. The ascii environment is searched first,
 *              and the value is returned if found. If not, the EBCDIC environment
 *              is checked, and an ascii entry is created if it is found. If not,
 *              NULL is returned.
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_getenv(const char *name)
{
    char *value, *e_name, *e_value;
    envvar_t *envvar;

    if (name == NULL) {
        return NULL;
    }

    if ((envvar = getEnvvar((char *)name)) != NULL) {
        /* We already have the variable in ascii */
        return envvar->value;
    }

    /* Get the EBCIDC value from the system */
    e_name = a2e_string(name);
    e_value = getenv(e_name);
    free(e_name);

    if (e_value == NULL) {
        /* Variable is not currently defined */
        return NULL;
    }

    /* Create a new ascii environment variable entry */
    value = e2a_string(e_value);
    envvar = putEnvvar((char *)name, value, NULL);
    free(value);

    return envvar->value;
}

/**************************************************************************
 * name        - atoe_putenv
 * description -
 *              Issues putenv() to set the system EBCDIC environment
 *              then updates the ascii environment
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_putenv(const char *envstr)
{
    char *w_envstr, *name, *value;
    envvar_t *envvar;
    int result;

    /* Set the system EBCDIC environment - don't free w_envstr as it's memory is held by the environment */
    w_envstr = a2e_string(envstr);
    result = putenv(w_envstr);

    if (result == 0) {
        /* Update the ascii environment */
        char *asciienv = strdup(envstr);

        /*
         * The name is everything upto the first '='s character and the value
         * is everything after it.
         */
        name = asciienv;
        value = strchr(asciienv, '=');
        *value = '\0';                  /* Add '\0' so name is NULL termintated. */
        value++;                        /* Value is now everything after the '='. */

        envvar = putEnvvar(name, value, getEnvvar(name));
        free(asciienv);
    }

    return result;
}

/**************************************************************************
 * name        - atoe_perror
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
void
atoe_perror( const char *string)
{
    char *e;

    e = a2e_string(string);
    perror(e);

    free(e);

    return;
}


/**************************************************************************
 * name        - atoe_enableFileTagging
 * description - Enables tagging of all new files to the specified ccsid
 * parameters  - ccsid - Required CCSID for file tags
 * returns     - none
 *************************************************************************/
void 
atoe_setFileTaggingCcsid(void *pccsid) { 
   __ccsid_t ccsid = *(__ccsid_t*) pccsid; 
    newFileCCSID = ccsid;
}


/**************************************************************************
 * name        - fileTagRequired
 * description - Determines whether or not a specified file needs tagging
 * parameters  - filename (platform encoded)
 * returns     - 1 if file tagging enabled and file doesn't already exist.
 *               0 otherwise
 *************************************************************************/
static int 
fileTagRequired(const char *filename) {
    struct stat sbuf;
    if (fileTaggingEnabled && newFileCCSID != 0 &&
        (stat(filename,&sbuf) == -1 && errno == ENOENT) ) {
        return 1;
    }
    return 0;
}

/**************************************************************************
 * name        - setFileTag
 * description - Sets the file tag to newFileCCSID for the specified file
 *               descriptor
 * parameters  - fd - open file descriptor of file for tagging
 * returns     - none
 *************************************************************************/
static void 
setFileTag(int fd) {
    struct file_tag tag;
    tag.ft_ccsid = newFileCCSID;
    tag.ft_txtflag = 1;
    tag.ft_deferred = 0;
    tag.ft_rsvflags = 0;
    fcntl(fd, F_SETTAG, &tag);
}


/**************************************************************************
 * name        - atoe_tagged_open
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_open( const char *fname, int options, ...)
{
    int fd;
    int mode;
    int tagFile; 
    va_list args;
    char *f;

    check_fcntl_init();                            

    /* See if the file needs to be tagged */
    tagFile = fileTagRequired(fname);

    f = a2e_string(fname);

    va_start(args,options);

    if ( options & O_CREAT ) {
        mode = va_arg(args, int);
    } else {
        mode = 0;
    }

    va_end(args);

    fd = open(f,options,mode);
                                                   
    if ( (fd != -1) &&                    /* file descriptor ok? */
         (use_fcntl) )                    /* OS level is ok      */
      {
        set_SETCVTOFF();
      }

    if (fd != -1) { /* file descriptor ok? */
        if (tagFile) {
            setFileTag(fd);
        }
    }
                                                   
    free(f);

    return fd;
}

/**************************************************************************
 * name        - atoe_tempnam
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_tempnam( const char *dir, char *pfx)
{
    char *tempfn = 0;
    char *a = 0;
    char *d = a2e_string(dir);
    char *p = a2e_string(pfx);

    if (( tempfn = tempnam(d,p)) == 0 ) {
        atoe_fprintf(stderr,"Creation of temp file %s/%s failed.\n",dir,pfx);
    } else {
        a = e2a_string(tempfn);
        free(tempfn);
    }

    free(d);
    free(p);

    return a;
}


/**************************************************************************
 * name        - atoe_stat
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_stat( const char *pathname, struct stat *sbuf)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = stat(e,sbuf);

    free(e);

    return rc;
}

/**************************************************************************
 * name        - atoe_lstat
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_lstat( const char *pathname, struct stat *sbuf)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = lstat(e,sbuf);

    free(e);

    return rc;
}


/**************************************************************************
 * name        - atoe_fopen
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
FILE *
atoe_fopen( const char *filename, char *mode)
{
    FILE *outfp;
    char *f,*m;
    int  fd = -1;         /* file descriptor */    

    check_fcntl_init();                            

    f = a2e_string(filename);
    m = a2e_string(mode);
    outfp = fopen(f,m);

    if ( (outfp != NULL) &&                        /* fopen() ok?    */
         (use_fcntl)    &&                         /* OS level is ok */
         ( (fd = fileno(outfp)) != -1))            /* have a file descriptor? */
      {
        set_SETCVTOFF();
      }

    free(f);
    free(m);

    return outfp;
}

/**************************************************************************
 * name        - atoe_freopen
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
FILE *
atoe_freopen(const char *filename, char *mode, FILE *stream)
{
    FILE *outfp;
    char *f,*m;
    int  fd = -1;         /* file descriptor */    

    check_fcntl_init();                            

    f = a2e_string(filename);
    m = a2e_string(mode);
    outfp = freopen(f, m, stream);

    if ( (outfp != NULL) &&                        /* fopen() ok?    */
         (use_fcntl)    &&                         /* OS level is ok */
         ( (fd = fileno(outfp)) != -1))            /* have a file descriptor? */
      {
        set_SETCVTOFF();
      }

    free(f);
    free(m);

    return outfp;
}


/**************************************************************************
 * name        - atoe_mkdir
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_mkdir( const char *pathname, mode_t mode)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = mkdir(e, mode);

    free(e);

    return rc;
}

/**************************************************************************
 * name        - atoe_remove
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_remove( const char *pathname)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = remove(e);

    free(e);

    return rc;
}

/**************************************************************************
 * name        - atoe_strerror
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
#define ERRNO_CACHE_SIZE   250
typedef struct ascii_errno_t {
    int   errnum;
    char *ascii_msg;
} ascii_errno_t;
static ascii_errno_t errno_cache[ERRNO_CACHE_SIZE];
static int errno_cache_next = 0;
static pthread_mutex_t strerror_mutex = PTHREAD_MUTEX_INITIALIZER;
static int strerror_initialized = 0;
char *
atoe_strerror(int errnum)
{
    char *a,*e;
    int index;

    pthread_mutex_lock(&strerror_mutex);
    if (0 == strerror_initialized) {
        for (index=0; index<ERRNO_CACHE_SIZE; index++) {
            errno_cache[index].errnum = -1;
            errno_cache[index].ascii_msg = 0;
        }
        strerror_initialized = 1;
    }

    for (index=0;
         index<ERRNO_CACHE_SIZE, errno_cache[index].errnum!=-1;
         index++) {
        if (errnum == errno_cache[index].errnum) {
            pthread_mutex_unlock(&strerror_mutex);
            return errno_cache[index].ascii_msg;
        }
    }

    e = strerror(errnum);
    a = e2a_string(e);

    errno_cache[errno_cache_next].errnum = errnum;
    if (errno_cache[errno_cache_next].ascii_msg)
        free(errno_cache[errno_cache_next].ascii_msg);
    errno_cache[errno_cache_next].ascii_msg = a;
    if (++errno_cache_next == ERRNO_CACHE_SIZE)
          errno_cache_next = 0;
    pthread_mutex_unlock(&strerror_mutex);

    return a;
}

/**************************************************************************
 * name        - atoe_getcwd
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_getcwd(char *buffer, size_t size)
{
    char *abuf;

    if (getcwd(buffer,size) == NULL) {     
        atoe_fprintf(stderr,"atoe_getcwd error: %s\n",
                     errno==ERANGE ? "buffer too small" : atoe_strerror(errno));
        buffer = NULL;
    } else {
        abuf = e2a(buffer,size);
        memcpy(buffer,abuf,size);
        free(abuf);
    }

    return buffer;
}

/**************************************************************************
 * name        - atoe_fgets
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_fgets (char *buffer, int num, FILE *file)
{
    char *abuf;

    if (fgets(buffer, num, file)) {
        int len = strlen(buffer);
        abuf = e2a(buffer,len);
        memcpy(buffer,abuf,len);

        free(abuf);
        return buffer;
    }

    return(char *)0;
}


/**************************************************************************
 * name        - atoe_gets
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_gets(char *buffer)
{
    char *abuf;

    if (gets(buffer)) {
        int len = strlen(buffer);
        abuf = e2a(buffer,len);
        memcpy(buffer,abuf,len);

        free(abuf);
        return buffer;
    }

    return(char *)0;
}

/**************************************************************************
 * name        - atoe_unlink
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_unlink( const char *pathname)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = unlink(e);

    free(e);

    return rc;
}


/**************************************************************************
 * name        - atoe_unlink
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_rmdir( const char *pathname)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = rmdir(e);

    free(e);

    return rc;
}


/**************************************************************************
 * name        - atoe_access
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_access( const char *pathname, int how)
{
    int rc;
    char *e;

    e = a2e_string(pathname);
    rc = access(e, how);

    free(e);

    return rc;
}


/**************************************************************************
 * name        - atoe_opendir
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
DIR *
atoe_opendir( const char *dirname)
{
    DIR *dir;
    char *e;

    e = a2e_string(dirname);
    dir = opendir(e);

    free(e);

    return dir;
}


/**************************************************************************
 * name        - atoe_readdir
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct dirent *
atoe_readdir(DIR *dir) {
    struct dirent *d;
    char * a;

    d = readdir(dir);

    if (d == NULL) return NULL;

    a = e2a_string(d->d_name);

    strcpy(d->d_name, a);
    free(a);

    return d;
}

/**************************************************************************
 * name        - atoe_realpath
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_realpath(const char *file_name, char *resolved_name)
{
    char e_resolved_name[MAXPATHLEN];
    char *e_file_name, *p;

    e_file_name = a2e_string(file_name);                   
    p = realpath(e_file_name, e_resolved_name);                    

    if (p == NULL) return p;

    p = e2a_string(e_resolved_name);
    strcpy(resolved_name, p);
    free(p);

    return resolved_name;
}

/**************************************************************************
 * name        - atoe_rename
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_rename(const char *oldname, const char *newname)
{
    int rc;
    char *o,*n;

    o = a2e_string(oldname);
    n = a2e_string(newname);
    rc = rename(o, n);

    free(o);
    free(n);

    return rc;
}


/**************************************************************************
 * name        - atoe_getpwuid
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct passwd *
atoe_getpwuid(uid_t uid) {
    return etoa_passwd(getpwuid(uid));
}

/**************************************************************************
 * name        - atoe_getpwnam
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct passwd *
atoe_getpwnam(const char *name) {
    struct passwd *e_passwd;
    char *e_name;

    e_name = a2e_string(name);
    e_passwd = getpwnam(e_name);
    free(e_name);

    return etoa_passwd(e_passwd);
}

/**************************************************************************
 * name        - etoa_passwd
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct passwd *
etoa_passwd(struct passwd *e_passwd) {
    struct passwd *a_passwd = NULL;

    if (e_passwd != NULL) {
        a_passwd = (struct passwd *)malloc(sizeof(struct passwd));

        if (a_passwd != NULL) {
            a_passwd->pw_name  = e2a_string(e_passwd->pw_name);
            a_passwd->pw_uid   = e_passwd->pw_uid;
            a_passwd->pw_gid   = e_passwd->pw_gid;
            a_passwd->pw_dir   = e2a_string(e_passwd->pw_dir);
            a_passwd->pw_shell = e2a_string(e_passwd->pw_shell);
        }
    }

    return a_passwd;
}

/**************************************************************************
 * name        - atoe_getlogin
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_getlogin(void)
{
    char *e_login;
    char *a_login = NULL;

    e_login = getlogin();

    if (e_login != NULL) {
        a_login = e2a_string(e_login);
    }

    return a_login;
}

/**************************************************************************
 * name        - etoa_group
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct group *
e2a_group(struct group *e_group) {
    struct group *a_group = NULL;

    if (e_group != NULL) {
        a_group = (struct group *)malloc(sizeof(struct group));

        if (a_group != NULL) {
            char **e_member = NULL;
            char **a_member = NULL;
            int arraySize = 0;
            a_group->gr_name  = e2a_string(e_group->gr_name);
            a_group->gr_gid   = e_group->gr_gid;
            for(e_member = e_group->gr_mem; *e_member != NULL; e_member++) {
                arraySize++;
            }
            a_group->gr_mem = (char **)malloc((arraySize+1)*sizeof(char*));
            if (a_group->gr_mem != NULL) {
		for(e_member = e_group->gr_mem, a_member = a_group->gr_mem;
		    *e_member != NULL; 
		    e_member++, a_member++) {
		    *a_member = e2a_string(*e_member);
		}
		*(++a_member) = NULL;
            }
        }
    }

    return a_group;
}

/**************************************************************************
 * name        - atoe_getgrgid
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct group *
atoe_getgrgid(gid_t gid)
{
    struct group *e_group;
    struct group *a_group = NULL;

    e_group = getgrgid(gid);

    if (e_group != NULL) {
        a_group = e2a_group(e_group);
    }

    return a_group;
}

/**************************************************************************
 * name        - atoe_putchar
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_putchar(int ch)
{
    return putchar((int)a2e_tab[ch]);
}


/**************************************************************************
 * name        - atoe_fprintf
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_fprintf(FILE *file, const char *ascii_chars, ...)
{
    va_list args;
    char buf[BUFLEN];
    char *ebuf;
    int len;

    va_start(args,ascii_chars);

    len = atoe_vsnprintf(buf,BUFLEN,ascii_chars,args);

    /* Abort if failed... */
    if (len == -1) return len;

    ebuf = a2e(buf,len);
#pragma convlit(suspend)
    len = fprintf(file,"%s", ebuf);
#pragma convlit(resume)
    free(ebuf);

    va_end(args);

    return len;
}


/**************************************************************************
 * name        - atoe_printf
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_printf( const char *ascii_chars, ...)
{
    va_list args;
    char buf[BUFLEN];
    char *ebuf;
    int len;

    va_start(args,ascii_chars);

    len = atoe_vsnprintf(buf,BUFLEN,ascii_chars,args);

    /* Abort if failed... */
    if (len==-1) return len;

    ebuf = a2e(buf,len);
#pragma convlit(suspend)
    len = printf("%s", ebuf);
#pragma convlit(resume)
    free(ebuf);

    va_end(args);

    return len;
}

/*************************************************************************
 * name        - std_sprintf
 * description -
 *              This function does not need to convert ascii -> EBCDIC
 *              as there is no IO
 * parameters  -
 * returns     -
 *************************************************************************/
int
std_sprintf( const char *buf, char *ascii_chars, ...)
{
    int len;
    va_list args;
    va_start(args,ascii_chars);

    len = sprintf((char *)buf, ascii_chars,args);

    va_end(args);

    return len;
}

/**************************************************************************
 * name        - atoe_sprintf
 * description -
 *              This function does not need to convert ascii -> EBCDIC
 *              as there is no IO
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_sprintf( const char *buf, char *ascii_chars, ...)
{
    int len;
    char wrkbuf[BUFLEN];

    va_list args;
    va_start(args,ascii_chars);

    len = atoe_vsnprintf(wrkbuf,BUFLEN,ascii_chars,args);

    va_end(args);
    if (-1 == len) return len;                               

    strcpy((char *)buf, wrkbuf);

    return len;
}

/**************************************************************************
 * name        - atoe_vprintf
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_vprintf(const char *ascii_chars, va_list args)
{
    return atoe_vfprintf(stdout, ascii_chars, args); 
}

/**************************************************************************
 * name        - atoe_vfprintf
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_vfprintf(FILE *file, const char *ascii_chars, va_list args)
{
    char buf[BUFLEN];
    char *ebuf;
    int len;

    len = atoe_vsnprintf(buf,BUFLEN,ascii_chars,args);

    if (len == -1) return len;

    ebuf = a2e(buf,len);
#pragma convlit(suspend)
    len = fprintf(file,"%s", ebuf);
#pragma convlit(resume)

    free(ebuf);

    return len;
}

/**************************************************************************
 * name        - atoe_vsprintf                                    
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_vsprintf(char *target, const char *ascii_chars, va_list args)
{
    char buf[BUFLEN];                                     
    int  bsize = 0;                                       

    bsize = atoe_vsnprintf(buf,BUFLEN,ascii_chars,args);  
    if (-1 == bsize) return bsize;                        
    strcpy(target,buf);                                   
    return(bsize);                                        
}

/**************************************************************************
 * name        - atoe_sscanf                              
 * description -
 * parameters  -
 * returns     -
 * RESTRICTIONS - At the time of writing, there is no requirement to support
 *                character or string formatting, so I have opted to avoid
 *                the pain of trawling the format string along with the
 *                argument list in order to convert characters/strings from
 *                EBCDIC back to ASCII after calling the RTL sscanf().
 *************************************************************************/
int
atoe_sscanf(const char *buffer, const char *format, va_list args)
{
    char *e_buffer = a2e_string(buffer);
    char *e_format = a2e_string(format);
    int len = sscanf((const char *)e_buffer, (const char *)e_format, args);
    free(e_buffer);
    free(e_format);
    return len;
}

/**************************************************************************
 * name        - atoe_strftime
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
size_t
atoe_strftime(char *buf, size_t buflen,
              const char *format, const struct tm *timeptr)
{
    size_t num;
    char *e,*a;

    e = a2e_string(format);
    num = strftime(buf, buflen, e, timeptr);
    a = e2a(buf,num);
    memcpy(buf,a,num);

    free(e);
    free(a);

    return num;
}

/**************************************************************************
 * name        - atoe_fwrite
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
size_t
atoe_fwrite(const void *buffer, size_t size, size_t count, FILE *stream)
{
    int numwritten;
    char *e;

    e = a2e((void *)buffer,size*count);      
    numwritten = fwrite(e,size,count,stream);

    free(e);

    return numwritten;
}

/**************************************************************************
 * name        - atoe_fread
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
size_t
atoe_fread(void *buffer, size_t size, size_t count, FILE *stream)
{
    int numread;
    char *a;

    numread = fread(buffer,size,count,stream);
    a = e2a((char *)buffer,numread);
    memcpy(buffer,a,numread);

    free(a);

    return numread;
}

/**************************************************************************
 * name        - atoe_system
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_system( char * cmd)
{
    char *eb;
    int result;

    eb = a2e_string(cmd);
    result = system(eb) ;

    free(eb);

    return result;
}

/**************************************************************************
 * name        - atoe_setlocale
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_setlocale(int category, const char *locale)
{
    char *eb, *result;

    eb = a2e_string(locale);
    result = setlocale(category, eb) ;

    return e2a_string(result);
}

/**************************************************************************
 * name        - atoe_ctime
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
atoe_ctime(const time_t * t_in)
{
    char *eb;

    eb = ctime(t_in);

    return e2a_string(eb);
}

/**************************************************************************
 * name        - atoe_strtod
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
double atoe_strtod( char * s, char ** p)
{
    char * sebcdic;
    char * pebcdic;
    double d;

    sebcdic = a2e_string(s);
    d = strtod(sebcdic,&pebcdic);
    if (p != NULL) {
        *p = s + (pebcdic - sebcdic);
    }
    free(sebcdic);

    return d;
}

/**************************************************************************
 * name        - atoe_strtol                                      
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int atoe_strtol(const char * s, char ** p, int b)
{
    char * sebcdic;
    char * pebcdic;
    int  i;

    sebcdic = a2e_string(s);
    i = strtol((const char *)sebcdic, &pebcdic, b);
    if (p != NULL) {
        *p = (char *)s + (pebcdic - sebcdic);
    }
    free(sebcdic);

    return i;
}

/**************************************************************************
 * name        - atoe_strtoul                                     
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
unsigned long atoe_strtoul(const char * s, char ** p, int b)
{
    char * sebcdic;
    char * pebcdic;
    unsigned long  i;

    sebcdic = a2e_string(s);
    i = strtoul((const char *)sebcdic, &pebcdic, b);
    if (p != NULL) {
        *p = (char *)s + (pebcdic - sebcdic);
    }
    free(sebcdic);

    return i;
}

/**************************************************************************
 * name        - atoe_strtoull
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
unsigned long long atoe_strtoull(const char * s, char ** p, int b)
{
    char * sebcdic;
    char * pebcdic;
    unsigned long long i;

    sebcdic = a2e_string(s);
    i = strtoull((const char *)sebcdic, &pebcdic, b);
    if (p != NULL) {
        *p = (char *)s + (pebcdic - sebcdic);
    }
    free(sebcdic);

    return i;
}

/*************************************************************************
 * name        - atoe_atof
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
double
atoe_atof(char * ascii_string)
{
    double rc;
    char * ebcdic_string;

    ebcdic_string = a2e_string(ascii_string);
    rc = atof(ebcdic_string);
    free(ebcdic_string);

    return rc;
}

/**************************************************************************
 * name        - atoe_atoi
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_atoi(char * ascii_string)
{
    int rc;
    char * ebcdic_string;

    ebcdic_string = a2e_string(ascii_string);
    rc = atoi(ebcdic_string);
    free(ebcdic_string);

    return rc;
}

/*************************************************************************
 * name        - atoe_atol
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
long
atoe_atol(char * ascii_string)
{
    long rc;
    char * ebcdic_string;

    ebcdic_string = a2e_string(ascii_string);
    rc = atol(ebcdic_string);
    free(ebcdic_string);

    return rc;
}

/**************************************************************************
 * name        - atoe_gethostname
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
atoe_gethostname(char *name, int namelen)
{
    int rc = gethostname(name, namelen);

    char *ascii_name = e2a(name, namelen);

    strcpy(name, ascii_name);
    free(ascii_name);

    return rc;
}

/**************************************************************************
 * name        - atoe_gethostbyname                             
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
#define HOSTBYNAME_CACHE_SIZE   50
typedef struct hostbyname_t {
    char *hostname;
    struct hostent h;
} hostbyname_t;
static hostbyname_t hostbyname_cache[HOSTBYNAME_CACHE_SIZE];
static int hostbyname_cache_next = 0;
static pthread_mutex_t hostbyname_mutex = PTHREAD_MUTEX_INITIALIZER;
static int hostbyname_initialized = 0;
struct hostent*
atoe_gethostbyname(const char *hostname) {
    char *h;
    struct hostent *hostptr = NULL, *hostptr_a = NULL;
    int index;

    pthread_mutex_lock(&hostbyname_mutex);
    if (0 == hostbyname_initialized) {
        /* initialize cache in static storage */
        for (index=0; index<HOSTBYNAME_CACHE_SIZE; index++) {
            hostbyname_cache[index].hostname = NULL;
            memset(&hostbyname_cache[index].h, 0, sizeof(struct hostent));
        }
        hostbyname_initialized = 1;
    }

    /* see if we are already using a cache entry for this hostname */
    for (index=0;
         index<HOSTBYNAME_CACHE_SIZE, hostbyname_cache[index].hostname!=NULL;
         index++) {
        if (0 == strcmp(hostname, hostbyname_cache[index].hostname)) {
            hostptr_a = &hostbyname_cache[index].h;
            break;
        }
    }
    if (NULL == hostptr_a) {
        /* use next cache slot for this hostname */
        if (hostbyname_cache[hostbyname_cache_next].hostname) {
            free(hostbyname_cache[hostbyname_cache_next].hostname);
        }
        hostbyname_cache[hostbyname_cache_next].hostname = strdup(hostname);
        if (!hostbyname_cache[hostbyname_cache_next].hostname) {
            pthread_mutex_unlock(&hostbyname_mutex);
            return NULL;
        }
        hostptr_a = &hostbyname_cache[hostbyname_cache_next].h;
        if (++hostbyname_cache_next == HOSTBYNAME_CACHE_SIZE) {
            hostbyname_cache_next = 0;
        }
    }

    /* convert the input hostname to ebcdic */
    h = a2e_string(hostname);
    /* and issue the system call */
    hostptr = gethostbyname(h);
    /* don't need the ebcdic copy of hostname any more */
    free(h);

    /* if the system call worked save the results, otherwise return NULL */
    if (hostptr) {
        /* if we're re-using a cache slot, free any previously saved name */
        if (hostptr_a->h_name) {
            free(hostptr_a->h_name);
        }
        /* copy the system returned hostent into our cache */
        memcpy(hostptr_a, hostptr, sizeof(struct hostent));
        /* convert the system returned name to ascii and save it in our cache */
        hostptr_a->h_name = e2a_string(hostptr->h_name);
        /* if everything worked return the address of the cache entry */
        if (hostptr_a->h_name) {
            hostptr = hostptr_a;
        } else {
            hostptr = NULL;
        }
    }

    pthread_mutex_unlock(&hostbyname_mutex);

    return hostptr;
}

/**************************************************************************
 * name        - atoe_gethostbyname_r                           
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct hostent*
atoe_gethostbyname_r(char *hostname, struct hostent *hentptr,     
                     char *buf, int bufsize, int *h_error) {
    char *h, *h_n;
    struct hostent *hostptr;

    h = a2e_string(hostname);

    if ((hostptr = gethostbyname(h)) == NULL) {                   
    hentptr = NULL;                                            
    } else if ((strlen(hostptr->h_name) + 1) > bufsize) {      
    *h_error = errno = ERANGE;                                 
    hentptr = NULL;                                            
    } else {                                                   
    memcpy(hentptr, hostptr, sizeof(struct hostent));          

        h_n = e2a_string(hostptr->h_name);
    strcpy(buf, h_n);                                          
    free(h_n);                                                 

    hentptr->h_name = buf;                                     
    }

    free(h);

    return hentptr;                                            
}

/**************************************************************************
 * name        - atoe_gethostbyaddr
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct hostent*
atoe_gethostbyaddr(const void *address, int addrlen, int domain,
                   struct hostent *hentptr, char *buf, int bufsize,
                   int *h_error) {
    char *h_n;
    struct hostent *hostptr;

    if ((hostptr = gethostbyaddr(address,addrlen,domain)) == NULL){
    hentptr = NULL;                                            
    } else if ((strlen(hostptr->h_name) + 1) > bufsize) {      
    *h_error = errno = ERANGE;                                 
    hentptr = NULL;                                            
    } else {                                                   
    memcpy(hentptr, hostptr, sizeof(struct hostent));          

        h_n = e2a_string(hostptr->h_name);
    strcpy(buf, h_n);                                          
    free(h_n);                                                 

    hentptr->h_name = buf;                                     
    }

    return hentptr;                                            
}

/**************************************************************************
 * name        - atoe_getprotobyname
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
struct protoent *
atoe_getprotobyname(const char *name) {
    struct protoent *pent;
    char *e_name, *p_n;

    e_name = a2e_string((char *)name);

    if ((pent = getprotobyname(e_name)) != 0) {
        p_n = e2a_string(pent->p_name);
        pent->p_name = p_n;
    }

    free(e_name);

    return pent;
}

/**************************************************************************
 * name        - atoe_inet_addr
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
unsigned long
atoe_inet_addr(char *hostname)
{
    unsigned long iaddr = -1;
    char *h = a2e_string(hostname);

    iaddr = inet_addr(h);

    free(h);

    return iaddr;
}

#define _ISALNUM_ASCII  0x0001
#define _ISALPHA_ASCII  0x0002
#define _ISCNTRL_ASCII  0x0004
#define _ISDIGIT_ASCII  0x0008
#define _ISGRAPH_ASCII  0x0010
#define _ISLOWER_ASCII  0x0020
#define _ISPRINT_ASCII  0x0040
#define _ISPUNCT_ASCII  0x0080
#define _ISSPACE_ASCII  0x0100
#define _ISUPPER_ASCII  0x0200
#define _ISXDIGIT_ASCII 0x0400

#define _XUPPER_ASCII   0xdf                                        
#define _XLOWER_ASCII   0x20                                        

/* Check for iconv_init() already done   */
int iconv_initialised = 0;

/* Table for integer test macros      */
int _ascii_is_tab[CONV_TABLE_SIZE];

#pragma convlit(suspend)
/**************************************************************************
 * name        - iconv_init
 * description -
 *              Allocate code conversion descriptors for iconv()
 *              and build translate tables.
 * parameters  -
 * returns     -
 *************************************************************************/
int
iconv_init(void)
{
    char *asciiSet = "ISO8859-1";
    char *defaultSet = "IBM-1047";
    char *ebcdicSet, *inptr, *outptr;
    size_t inleft, outleft;
    iconv_t atoe_cd = (iconv_t)(-1);  
    iconv_t etoa_cd = (iconv_t)(-1);  
    char init_tab[CONV_TABLE_SIZE];
    int i;

    /* Need to save start locale setting      */   
    char *slc = NULL;
    /* Get the current locale setting    */        
    char *clc = setlocale(LC_ALL, NULL); 

    if (iconv_initialised) {
        return 0;
    }

    /*
     * Test whether we're running with an DBCS codepage
     * If so, use default IBM-1047 EBCDIC SBCS codepage to build translate table.
     */
    {
        char a[] = {97,0}; /* ASCII "a" */

        /* Copy the start locale setting to reset it later          */
        slc = strdup(clc);
        if ( !slc ) return -1;

        /* Set the locale to whatever is set by the env. vars       */
        setlocale(LC_ALL, "") ;

        if (( __atoe(a) < 1) && (errno == EINVAL)) {
            /* The current locale does not describe a single-byte character set.*/
            /* This test does not always give the correct result*/
            ebcdicSet = defaultSet;
        } else {
        char *encoding = NULL;                                    
        char *lc;
        char *p;
        int i;

        lc = getenv("LC_ALL");
        if (lc == NULL) {
        lc = getenv("LC_CTYPE");
        if (lc == NULL) {
            lc = getenv("LANG");
        }
        }

        if (lc != NULL) {

        /* We now have a string with the format
         * language_region.encoding@variant any part of which
         * may be missing. We need to extract the encoding part
         */

        lc = strdup(lc);
        if ((p = strchr(lc, '.')) != NULL) {
            encoding = p+1;                                   
        }
        if ((p = strchr(lc, '@')) != NULL) {
            *p = '\0';
        }
        }

        lc = encoding;

        /* If lc is still NULL or we have "C" encoding
         * use the default encoding.
         */
        if (lc == NULL || !strcmp(lc, "C")) {
        ebcdicSet = defaultSet;
        } else {
        ebcdicSet = lc;
        }
    }
    }

    /* reset the locale to start setting                           */
    clc = setlocale(LC_ALL, slc );
    free( slc );

    if (((etoa_cd = iconv_open(asciiSet, ebcdicSet)) == (iconv_t)(-1)) ||
        ((atoe_cd = iconv_open(ebcdicSet, asciiSet)) == (iconv_t)(-1))) {
        /* Retry with default ebcdicSet                              */
        if (strcmp(ebcdicSet,defaultSet) == 0) {
            return -1;
    }
        /* Close conversion descriptors just in case one of them succeeded */
        if (etoa_cd != (iconv_t)(-1)) {                              
            iconv_close(etoa_cd);                                    
    }                                                             

        if (atoe_cd != (iconv_t)(-1)) {                           
            iconv_close(atoe_cd);                                 
    }                                                             

        if (((etoa_cd = iconv_open(asciiSet, defaultSet)) == (iconv_t)(-1)) ||
            ((atoe_cd = iconv_open(defaultSet, asciiSet)) == (iconv_t)(-1))) {
      return -1;
    }                                                            
    }

    /* Build initial table x'00' - x'ff'   */
    for (i = 0; i < CONV_TABLE_SIZE; i++) {
        init_tab[i] = i;
    }

    /* Create conversion table for ASCII=>EBCDIC */
    inptr = init_tab;
    outptr = a2e_tab;
    inleft = outleft = CONV_TABLE_SIZE;
    if (iconv(atoe_cd, &inptr, &inleft, &outptr, &outleft) == -1) {
        /* atoe conversion failed */
        if (errno == E2BIG) {                                        
        /* The EBCDIC codepage is probably DBCS */
            /* Close conversion descriptors */
            iconv_close(atoe_cd);
            iconv_close(etoa_cd);
            /* Retry with default IBM-1047 */
            if (((etoa_cd = iconv_open(asciiSet, defaultSet)) == (iconv_t)(-1)) ||
                ((atoe_cd = iconv_open(defaultSet, asciiSet)) == (iconv_t)(-1))) {
                return -1;
            }
            inptr = init_tab;
            outptr = a2e_tab;
            inleft = outleft = CONV_TABLE_SIZE;
            if (iconv(atoe_cd, &inptr, &inleft, &outptr, &outleft) == -1) {
               return -1;
        }
    }                                                           
    }

    /* Create conversion table for EBCDIC=>ASCII */
    inptr = init_tab;
    outptr = e2a_tab;
    inleft = outleft = CONV_TABLE_SIZE;

    /* Try to create a complete translate table for this codepage */
    /* if an EILSEQ return is received for a codepoint bypass and */
    /* move on to the next one.                                   */
    while (inleft>0) {
      size_t ret;
      ret = iconv(etoa_cd, &inptr, &inleft, &outptr, &outleft);
      if (ret != (size_t)-1) break;
      if (errno == EILSEQ) {
        inptr  += 1;
        inleft -= 1;
        outptr  += 1;
        outleft -= 1;
      }
      else {
        /* etoa conversion failed */
        return -1;
      }
    }

    /* Close conversion descriptors */
    iconv_close(atoe_cd);
    iconv_close(etoa_cd);

    /* Build integer test macros flag table */
    for (i = 0; i < CONV_TABLE_SIZE; i++) {
        _ascii_is_tab[i] = 0;
        if (isalnum(a2e_tab[i])) _ascii_is_tab[i] |=  _ISALNUM_ASCII;
        if (isalpha(a2e_tab[i])) _ascii_is_tab[i] |=  _ISALPHA_ASCII;
        if (iscntrl(a2e_tab[i])) _ascii_is_tab[i] |=  _ISCNTRL_ASCII;
        if (isdigit(a2e_tab[i])) _ascii_is_tab[i] |=  _ISDIGIT_ASCII;
        if (isgraph(a2e_tab[i])) _ascii_is_tab[i] |=  _ISGRAPH_ASCII;
        if (islower(a2e_tab[i])) _ascii_is_tab[i] |=  _ISLOWER_ASCII;
        if (isprint(a2e_tab[i])) _ascii_is_tab[i] |=  _ISPRINT_ASCII;
        if (ispunct(a2e_tab[i])) _ascii_is_tab[i] |=  _ISPUNCT_ASCII;
        if (isspace(a2e_tab[i])) _ascii_is_tab[i] |=  _ISSPACE_ASCII;
        if (isupper(a2e_tab[i])) _ascii_is_tab[i] |=  _ISUPPER_ASCII;
        if (isxdigit(a2e_tab[i])) _ascii_is_tab[i] |=  _ISXDIGIT_ASCII;
    }

    /* If we get here, then we are initialised.                    */
    iconv_initialised = 1;                                         

    return 0;
}
#pragma convlit(resume)

/**************************************************************************
 * name        - atoe_dlopen
 * description - Load the DLL specified in dllName by converting it to EBCDIC,
 *               then calling the system dlopen
 * parameters  - dllName   the DLL name
 *               mode      describes the mode by which the DLL will be loaded
 * returns     - a handle to the loaded DLL if successful, NULL otherwise
 *************************************************************************/
void*
atoe_dlopen(const char * dllName, int mode) 
{
    void *handle;
    char *d = a2e_string(dllName);

    handle = dlopen(d, mode);
    free(d);
    return handle;
}

void *
atoe_dlsym(void *descriptor, const char *name) 
{
    void *address;
    char *ebcdicName = a2e_string(name);

    address = dlsym(descriptor, ebcdicName);
    free(ebcdicName);
    return address;
}

/**************************************************************************
 * name        - atoe_dllload
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
dllhandle*
atoe_dllload(char * dllName)
{
    dllhandle *handle;
    char *d = a2e_string(dllName);

    handle = dllload(d);
    free(d);
    return handle; 
}

void *atoe_dllqueryvar(dllhandle* dllHandle, char* varName)
{
    char *n = a2e_string(varName);
    void *r = dllqueryvar(dllHandle, n);

    free(n);

    return r;
}


void (*
      atoe_dllqueryfn(dllhandle* dllHandle, char* funcName)) ()
{
    char *n = a2e_string(funcName);
    void (*r)() = dllqueryfn(dllHandle, n);

    free(n);

    return r;
}

/**************************************************************************
 * name        - atoe_execv
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int        
atoe_execv (const char *path, char *const argv[])
{
    char *ebcdicPath = a2e_string(path);
    char **ebcdicArgv;
    int size = 0, i, rc;

    /* Calculate the size of argv. argv always ends with a null pointer. */
    while (argv[size]) {
        size++;
    }

    /* Allocate space for the new array and populate */
    ebcdicArgv = (char**) malloc(sizeof(char*) * (size + 1));
    for (i = 0; i < size; i++) {
        ebcdicArgv[i] = a2e_string(argv[i]);
    }

    /* Null terminate the new array */
    ebcdicArgv[size] = NULL;

    rc = execv(ebcdicPath, ebcdicArgv);

    for (i = 0; i < size; ++i) {
        free(ebcdicArgv[i]);
    }
    free(ebcdicArgv);
    free(ebcdicPath);

    return rc;
}

/**************************************************************************
 * name        - atoe_execvp
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int        
atoe_execvp (const char *file, char *const argv[])
{
    char *ebcdicFile = a2e_string(file);
    char **ebcdicArgv;
    int size = 0, i, rc;

    /* Calculate the size of argv. argv always ends with a null pointer. */
    while (argv[size]) {
        size++;
    }

    /* Allocate space for the new array and populate */
    ebcdicArgv = (char**) malloc(sizeof(char*) * (size + 1));
    for (i = 0; i < size; i++) {
        ebcdicArgv[i] = a2e_string(argv[i]);
    }

    /* Null terminate the new array */
    ebcdicArgv[size] = NULL;

    rc = execvp(ebcdicFile, ebcdicArgv);

    for (i = 0; i < size; ++i) {
        free(ebcdicArgv[i]);
    }
    free(ebcdicArgv);
    free(ebcdicFile);

    return rc;
}

/**************************************************************************
 * name        - etoa_uname                    
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
etoa_uname(struct utsname *a) {
    struct utsname e;
    int rc = 0;

    rc = uname(&e);
    if (rc == 0 && a != NULL) {
        char *temp = NULL;
        temp  = e2a_string(e.sysname);
        strcpy(a->sysname,temp);
    free(temp);
        temp  = e2a_string(e.nodename);
        strcpy(a->nodename,temp);
    free(temp);
        temp  = e2a_string(e.release);
        strcpy(a->release,temp);
    free(temp);
        temp  = e2a_string(e.version);
        strcpy(a->version,temp);
    free(temp);
        temp  = e2a_string(e.machine);
        strcpy(a->machine,temp);
    free(temp);
    }

    return rc;
}

/**************************************************************************
 * name        - etoa_nl_langinfo              
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
char *
etoa_nl_langinfo(int nl_item) {

    char *retVal = NULL;
    retVal = nl_langinfo(nl_item);
    return(retVal != NULL) ? e2a_string(retVal) : NULL;
}

/*********************************************************************/
/* name        - atoe_utimes                                         */
/*                                                                   */
/* description - provide a version of the utimes() system function   */
/*               that converts the specified path from an ascii to   */
/*               an ebcdic string.                                   */
/*                                                                   */
/* parameters  - path:  ascii string representing the path name      */
/*               times: times to be used in setting the modification */
/*                      times of the specified file                  */
/*                                                                   */
/* returns     - int: 0 (success) or -1 (failure)                    */
/*********************************************************************/

int atoe_utimes(const char *path, const struct timeval *times)

{
   char *ep;

   ep = a2e_string(path);

   return utimes(ep, times);
}

/*********************************************************************/
/* name        - atoe_utime                                          */
/*                                                                   */
/* description - provide a version of the utime() system function    */
/*               that converts the specified path from an ascii to   */
/*               an ebcdic string.                                   */
/*                                                                   */
/* parameters  - path:  ascii string representing the path name      */
/*               buf: times to be used in setting the modification   */
/*                      and access times of the specified file       */
/*                                                                   */
/* returns     - int: 0 (success) or -1 (failure)                    */
/*********************************************************************/

int atoe_utime(const char *path, const struct utimebuf *buf)

{
   char *ep;

   ep = a2e_string(path);

   return utime(ep, buf);
}

/*********************************************************************/
/* name        - atoe_chmod                                          */
/*                                                                   */
/* description - provide a version of the chmod() system function    */
/*               that converts the specified path from an ascii to   */
/*               an ebcdic string.                                   */
/*                                                                   */
/* parameters  - path: ascii string representing the path name       */
/*               mode: bit set representing mode to be set on the    */
/*                     specified file                                */
/*                                                                   */
/* returns     - int: 0 (success) or -1 (failure)                    */
/*********************************************************************/

int atoe_chmod(const char *path, mode_t mode)

{
   char *ep;

   ep = a2e_string(path);

   return chmod(ep, mode);
}

/*********************************************************************/
/* name        - atoe_chdir                                          */
/*                                                                   */
/* description - provide a version of the chdir() system function    */
/*               that converts the specified path from an ascii to   */
/*               an ebcdic string.                                   */
/*                                                                   */
/* parameters  - path: ascii string representing the path name       */
/*                                                                   */
/* returns     - int: 0 (success) or -1 (failure)                    */
/*********************************************************************/

int atoe_chdir(const char *path)

{
   int  rc;
   char *ep;

   ep = a2e_string(path);

   rc = chdir(ep);

   free(ep);

   return rc;
}

/*********************************************************************/
/* name        - atoe_chown                                          */
/*                                                                   */
/* description - provide a version of the chown() system function    */
/*               that converts the specified path from an ascii to   */
/*               an ebcdic string.                                   */
/*                                                                   */
/* parameters  - path: ascii string representing the path name       */
/*               uid: new user id (-1 for no change)                 */
/*               gid: new group id (-1 for no change)                */
/*                                                                   */
/* returns     - int: 0 (success) or -1 (failure)                    */
/*********************************************************************/

int atoe_chown(const char *path, uid_t uid, gid_t gid)

{
   int  rc;
   char *ep;

   ep = a2e_string(path);

   rc = chown(ep, uid, gid);

   free(ep);

   return rc;
}

/**************************************************************************
 * name        - atoe_ftok
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/

key_t atoe_ftok(const char *pathname, int id)

{
    key_t key;
    char  *e;

    e   = a2e_string(pathname);
    key = ftok(e, id);

    free(e);

    return key;
}

/**************************************************************************
 * name        - etoa___osname                                 
 * description -
 * parameters  -
 * returns     -
 *************************************************************************/
int
etoa___osname(struct utsname *a) {
    struct utsname e;
    int rc = 0;

    rc = __osname(&e);
    if (rc == 0 && a != NULL) {
        char *temp = NULL;
        temp  = e2a_string(e.sysname);
        strcpy(a->sysname,temp);
    free(temp);
        temp  = e2a_string(e.nodename);
        strcpy(a->nodename,temp);
    free(temp);
        temp  = e2a_string(e.release);
        strcpy(a->release,temp);
    free(temp);
        temp  = e2a_string(e.version);
        strcpy(a->version,temp);
    free(temp);
        temp  = e2a_string(e.machine);
        strcpy(a->machine,temp);
    free(temp);
    }

    return rc;
}


/* END OF FILE */
