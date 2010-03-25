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

#define USING_VMI               /* Used in header files */

#include "jni.h"                /* for definitions of JavaVM */
#include "hycomp.h"             /* for portable types (UDATA,etc...) */
#include "hythread.h"           /* for synchronization */
#include "hyport.h"             /* for port library */
#include "hyexelibnls.h"        /* nls strings */
#include "libhlp.h"             /* defaults and environment variables and string buffer functions */
#include "strhelp.h"            /* for properties file parsing */
#ifdef HY_NO_THR
#include "main_hlp.h"           /* plaftorm specific launcher helpers */
#endif /* HY_NO_THR */
#include <string.h>
#include <stdlib.h>
#ifdef ZOS
#include <unistd.h>
#endif


#define PORT_LIB_OPTION "_org.apache.harmony.vmi.portlib"

#define HY_COPYRIGHT_STRING "Apache Harmony Launcher : (c) Copyright 1991, 2010 The Apache Software Foundation or its licensors, as applicable."

/* Tools launchers will invoke HY_TOOLS_PACKAGE+"."+<execname>+"."+HY_TOOLS_MAIN_TYPE */
#define HY_TOOLS_PACKAGE "org.apache.harmony.tools"
#define HY_TOOLS_MAIN_TYPE "Main"
#define HY_TOOLS_PATH "tools.jar"
#define HY_TOOLS_PROP "-Dorg.apache.harmony.tool=true"

#define HARMONY_JARRUNNER_CLASSNAME  "org.apache.harmony.vm.JarRunner"

#if defined(WIN32)
#define PLATFORM_STRNICMP _strnicmp
#endif

#if defined(LINUX) || defined(FREEBSD) || defined(AIX) || defined(MACOSX) || defined(ZOS)
#define PLATFORM_STRNICMP strncasecmp
#endif

static int invocation
PROTOTYPE ((HyPortLibrary * portLibrary, int argc, char **argv, UDATA handle,
            jint version, jboolean ignoreUnrecognized, char *mainClass,
            UDATA classArg, char *propertiesFileName,
            int isStandaloneJar, char *vmdllsubdir, int versionFlag));
static int createVMArgs
PROTOTYPE ((HyPortLibrary * portLibrary, int argc, char **argv,
            jint version, jboolean ignoreUnrecognized,
            JavaVMInitArgs * vm_args,
            UDATA classArg, char *propertiesFileName,
            int isStandaloneJar, char **mainClassJar, char *vmdllsubdir));
char *VMCALL vmdll_parseCmdLine
PROTOTYPE ((HyPortLibrary * portLibrary, UDATA lastLegalArg, char **argv));
char *VMCALL vmdlldir_parseCmdLine
#ifndef HY_NO_THR
PROTOTYPE ((HyPortLibrary * portLibrary, UDATA lastLegalArg, char **argv));
#else /* HY_NO_THR */
PROTOTYPE ((UDATA lastLegalArg, char **argv));
#endif /* HY_NO_THR */
UDATA VMCALL gpProtectedMain PROTOTYPE ((struct haCmdlineOptions * args));
IDATA convertString
PROTOTYPE ((JNIEnv * env, HyPortLibrary * portLibrary, jclass stringClass,
            jmethodID stringMid, char *chars, jstring * str));
int arrangeToolsArgs
PROTOTYPE ((HyPortLibrary * portLibrary, int *pargc, char ***pargv, char *mainClass));
int augmentToolsArgs
PROTOTYPE ((HyPortLibrary * portLibrary, int *argc, char ***argv));
static IDATA addDirsToPath
#ifndef HY_NO_THR
PROTOTYPE ((HyPortLibrary * portLibrary, int count, char *newPathToAdd[], char **argv));
#else /* HY_NO_THR */
PROTOTYPE ((int count, char *newPathToAdd[], char **argv));
#endif /* HY_NO_THR */
int main_runJavaMain
PROTOTYPE ((JNIEnv * env, char *mainClassName, int nameIsUTF, int java_argc,
            char **java_argv, HyPortLibrary * portLibrary));
static I_32 initDefaultDefines
PROTOTYPE ((HyPortLibrary * portLib, void **vmOptionsTable, int argc,
            char **argv, int jarArg, HyStringBuffer ** classPathInd,
            HyStringBuffer ** javaHomeInd,
            HyStringBuffer ** javaLibraryPathInd,
            char *vmdllsubdir, int *vmOptionsCount));

void
printUsageMessage(HyPortLibrary * portLibrary)
{
  PORT_ACCESS_FROM_PORT (portLibrary);
  hyfile_printf (PORTLIB, HYPORT_TTY_OUT, "Harmony Java launcher\n");
  hyfile_printf (PORTLIB, HYPORT_TTY_OUT, HY_COPYRIGHT_STRING "\n");
  hyfile_printf (PORTLIB, HYPORT_TTY_OUT,
                  "java [-vm:vmdll -vmdir:dir -D... [-X...]] [args]\n");
}

/**
 * The actual main function wrapped in the standard GP-handler.
 * 
 * @param[in] args The encapsulated command-line arguments and port library.
 *
 * @return 0 on success, or a non-zero error code on failure. 
 */
UDATA VMCALL
gpProtectedMain (struct haCmdlineOptions *args)
{
  int argc = args->argc;
  char **argv = args->argv;
  char *vmdll;
  char *mainClass = NULL;
  char *mainClassAlloc = NULL;
  int isStandaloneJar = 0;
  UDATA classArg = argc;
  int i;
  char *vmdllsubdir;
  char *vmiPath = NULL;
  char *newPathToAdd = NULL;
  char *propertiesFileName = NULL;
  char *exeName = NULL;
  char *exeBaseName;
  char *endPathPtr;
  UDATA handle;
  char defaultDllName[] = "harmonyvm";
  char defaultDirName[] = "default";
  int rc = -1;
  int versionFlag = 0;
  int vmHelp = 0;
  int genericLauncher = 0;
  char *str;
  char *knownGenericNames[] = { "java", "java.exe", "javaw.exe", NULL };
#ifndef HY_NO_THR
  char *dirs[2];
    
#endif /* ! HY_NO_THR */

  PORT_ACCESS_FROM_PORT (args->portLibrary);

  /* Find out name of the executable we are running as */
  hysysinfo_get_executable_name (argv[0], &exeName);

  /* Pick out the end of the exe path, and start of the basename */
  exeBaseName = strrchr(exeName, DIR_SEPARATOR);
  if (exeBaseName == NULL) {
	  endPathPtr = exeBaseName = exeName;
  } else {
	  exeBaseName += 1;
	  endPathPtr = exeBaseName;
  }

  /* Test whether we are likely the generic java launcher (or a tool) */
  i = 0;
  str = knownGenericNames[i];
  while(str != NULL) {
      genericLauncher = (0 == strcmp (str, exeBaseName));
	  if (genericLauncher) {
		  break;
	  } else {
		  str = knownGenericNames[++i];
	  }
  }
  
  /* If we have a tool name we still may be execv'd so check for the orig
   * command line arg.
   */
  if (!genericLauncher) {
    for (i = 1; i < argc; i++) {
      if (0 == strcmp (argv[i], HY_TOOLS_PROP)) {
        genericLauncher = 1;
        break;
      }
    }
  }

  /* Now we know whether we are running as the original invocation of a tool,
   * or a generic launcher / tool execv
   */

  if (genericLauncher) {
	/* The generic launcher needs at least one argument, otherwise
	 * print out a usage message.
     * 
     *  $$$ GMJ : TODO - rethink this - if a user types "java", they should
     *  get the standard java help dump, not some stuff about a launcher, as
     *  they thought they were running Java...
	 */
	if (argc <= 1) {
      printUsageMessage(PORTLIB);
      goto bail;
    }

	/* We are the generic launcher, figure out if we have a main class
	* to run (the first argument that does not start with a '-', or if we
	* have a '-jar' argument.
	*/
	for (i = 1; i < argc; i++) {
        if ((0 == strcmp ("-help", argv[i])) ||
            (0 == strcmp ("-h", argv[i])) ||
            (0 == strcmp ("-?", argv[i])) ||
            (0 == strcmp ("-X", argv[i]))) {
            vmHelp = 1;
        }

		if ((0 == strcmp ("-cp", argv[i])) ||
		    (0 == strcmp ("-classpath", argv[i]))) {
			/* Skip the classpath argument while looking for main class */
			i++;
			continue;
		}
		if (0 == strcmp ("-jar", argv[i])) {
			/* The arg is a JAR file to run */
			isStandaloneJar = 1;
		}
		if (0 == strncmp ("-version", argv[i], 8)) {
            /* Display version information */
            versionFlag = i;
		}
		if (0 == strncmp ("-showversion", argv[i], 12)) {
			/* We are being asked to print our version and continue */
            if (!versionFlag) versionFlag = i;
		}
		if ('-' != argv[i][0]) {
			/* This is the main class */
			classArg = i;         /* save position */
			mainClass = argv[i];  /* save class to execute */
			break;
		}
	} /* end for-loop */
  } else {
	/* We are a tool launcher: main class deduced from exe name */
    mainClass = hymem_allocate_memory (
      strlen(HY_TOOLS_PACKAGE) + strlen(exeBaseName) + strlen (HY_TOOLS_MAIN_TYPE) + 3);

    if (mainClass == NULL) {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
      PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR, HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
      goto bail;
	} else {
      /* Remember that we malloc'ed this mainClass space so we can free it */
      mainClassAlloc = mainClass;
	}

    strcpy (mainClass, HY_TOOLS_PACKAGE);
    strcat (mainClass, ".");
    if (NULL == (str = strchr (exeBaseName, '.'))) {
      strcat (mainClass, exeBaseName);
      strcat (mainClass, ".");
	} else {
      strncat (mainClass, exeBaseName, (str - exeBaseName + 1));
	}
	strcat (mainClass, HY_TOOLS_MAIN_TYPE);

    /* Useful when debugging */
    /* hytty_printf(PORTLIB, "Before...\n");
     * for (i=0; i<argc; i++) {
     *   hytty_printf(PORTLIB, "i=%d, v=%s\n", i, argv[i]);
     * }
     */ 

	/* Now ensure tools JAR is on classpath */
	augmentToolsArgs(args->portLibrary, &argc, &argv);
	classArg = arrangeToolsArgs(args->portLibrary, &argc, &argv, mainClass);
  }

  if (mainClass == NULL && !isStandaloneJar && !versionFlag && !vmHelp) {
    printUsageMessage(PORTLIB);
    goto bail;
  }

  /* Useful when debugging */
  /* hytty_printf(PORTLIB, "After...\n");
   * for (i=0; i<argc; i++) {
   *  hytty_printf(PORTLIB, "i=%d, v=%s\n", i, argv[i]);
   * }
   */
  
  /* At this point we either have a main class or know that we are running a JAR */

  /* Find the vm dll */
  vmdll = vmdll_parseCmdLine (PORTLIB, argc - 1, argv);
  if (!vmdll) {
     vmdll = defaultDllName;
  }

  /* Find the directory of the dll and set up the path */
#ifndef HY_NO_THR
  vmdllsubdir = vmdlldir_parseCmdLine (PORTLIB, argc - 1, argv);
#else /* HY_NO_THR */
  vmdllsubdir = vmdlldir_parseCmdLine (argc - 1, argv);
#endif /* HY_NO_THR */
  if (!vmdllsubdir) {
      vmdllsubdir = defaultDirName;
   }

  /* jvm dlls are located in a subdirectory off of jre/bin */
  /* setup path to dll named in -vm argument                      */
    endPathPtr[0] = '\0';

    newPathToAdd = hymem_allocate_memory (strlen (exeName) + strlen (vmdllsubdir) + 1);
    
    if (newPathToAdd == NULL) {
        /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
        PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                            HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
        goto bail;
    }
        
    vmiPath =
    hymem_allocate_memory (strlen (exeName) + strlen (vmdllsubdir) +
                            strlen (vmdll) +
                            strlen (DIR_SEPARATOR_STR) + 1);
    if (vmiPath == NULL)
    {
        /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
        PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                            HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
                            
        goto bail;
    }
    
    vmiPath[0] = '\0';
    strcpy (newPathToAdd, exeName);
    strcat (newPathToAdd, vmdllsubdir);
    strcpy (vmiPath, newPathToAdd);
    strcat (vmiPath, DIR_SEPARATOR_STR);
    strcat (vmiPath, vmdll);

#ifndef HY_NO_THR
    dirs[0] = newPathToAdd;
    dirs[1] = exeName;
    
    rc = addDirsToPath(PORTLIB, 2, dirs, argv);
    
    if (rc == -1)
    {
        hytty_printf (PORTLIB, "addDirsToPath Failed\n");
        goto bail;
    }

#endif /* ! HY_NO_THR */
    
  /* set up the properties file */
  propertiesFileName = hymem_allocate_memory (strlen (vmiPath) + 12);
  if (propertiesFileName == NULL)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
      PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                           HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
      goto bail;
    }
  strcpy (propertiesFileName, vmiPath);
  strcat (propertiesFileName, ".properties");

  /* Open the DLL */
  if (hysl_open_shared_library (vmiPath, &handle, TRUE))
    {
      hytty_printf (PORTLIB, "Failed to open JVM DLL: %s (%s)\n", vmiPath,
                    hyerror_last_error_message ());
      goto bail;
    }

  /* main launcher processing in this function */
  rc = invocation
      (PORTLIB, argc, argv, handle, JNI_VERSION_1_4, JNI_TRUE, mainClass,
       classArg, propertiesFileName, isStandaloneJar, vmdllsubdir, versionFlag);
  if (rc)
    {
	  /* Print an error message except in the case where an uncaught Exception 
	     has terminated the VM */
	  if (rc != 100)
	  {
		  hytty_printf (PORTLIB, "FAILED to invoke JVM.\n");
	  }                                                     
      goto bail;
    }

  if (hysl_close_shared_library (handle))
    {
      hytty_printf (PORTLIB, "Failed to close JVM DLL: %s\n", argv[1]);
      goto bail;
    }
bail:
  if (exeName) {
    hymem_free_memory (exeName);
  }

  if (mainClassAlloc) {
    hymem_free_memory (mainClassAlloc);
  }
  if (propertiesFileName) {
    hymem_free_memory (propertiesFileName);
  }
  if (vmiPath) {
    hymem_free_memory (vmiPath);
  }
  if (newPathToAdd) {
    hymem_free_memory (newPathToAdd);
  }
  // error code should be equal to 1 because of compatibility
  return rc == 0 ? 0 : 1;
}


/**
 * Arrange the argument list so that -J options come before the main
 * tools class, and tools options come after.
 */
int
arrangeToolsArgs (HyPortLibrary * portLibrary, int *pargc, char ***pargv, char *mainClass)
{
  int argc = *pargc;
  char **argv = *pargv;
  char **newargv;
  int i, rc;
  int newargvPos;
  PORT_ACCESS_FROM_PORT(portLibrary);

  /* Make room for the main tools class and tools property */
  newargv = hymem_allocate_memory ((argc + 2) * sizeof(pargv));
  if (NULL == newargv) {
    /* HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY=Internal VM error\: VM startup error: Out of memory\n */
    portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                             HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY);

    hytty_err_printf (PORTLIB, "Failed to allocate more memory for tools args\n");
    return 1;
  }

  /* Keep the exe name in position zero */
  newargvPos = 0;
  newargv[newargvPos++] = argv[0];

  /* Copy any -J arguments to the left hand side of the main class */
  for (i = 1; i < argc; i++) {
	  if (0 == strncmp (argv[i], "-J", 2)) {
	    newargv[newargvPos++] = argv[i] + 2; /* Remove the -J */
		/* if it was specifying the classpath, take the next arg across too */
		if ((0 == strncmp(argv[i], "-J-cp", 5)) ||
			(0 == strncmp(argv[i], "-J-classpath", 12))) {
				newargv[newargvPos++] = argv[++i];
		}
	  }
  }

  /* Insert the command line  */
  newargv[newargvPos++] = HY_TOOLS_PROP;

  /* Insert the tools main class */
  rc = newargvPos; /* We will return this position to the caller */
  newargv[newargvPos++] = mainClass;

  /* Now copy remaining arguments to the right hand side of the main class */
  for (i = 1; i < argc; i++) {
	  if (0 != strncmp (argv[i], "-J", 2)) {
	    newargv[newargvPos++] = argv[i];
	  } else {
		  /* Remember to ignore classpath args */
		 if ((0 == strncmp(argv[i], "-J-cp", 5)) ||
			 (0 == strncmp(argv[i], "-J-classpath", 12))) {
				i++;
		 }
	  }
  }

  newargv[newargvPos] = NULL;
  *pargc +=2;
  *pargv = newargv;

  return rc;
}


/**
 * Add the tools.jar to the application classpath.
 */
int
augmentToolsArgs (HyPortLibrary * portLibrary, int *pargc, char ***pargv)
{
  int argc = *pargc;
  char **argv = *pargv;
  char **newargv;
  int i;
  U_16 separator;
  char *classpath;
  char *newClasspath;
  int classpathLen;
  PORT_ACCESS_FROM_PORT(portLibrary);

  /* If there is already a classpath argument, we add our tools to it */
  for (i = 1; i < argc; i++) {
    if ((0 == strncmp (argv[i], "-J-cp", 5)) ||
		(0 == strncmp (argv[i], "-J-classpath", 12))) {
      classpath = argv[++i];
	  if (NULL == classpath) {
		  return 1;
	  }
	  classpathLen = strlen (classpath);
	  separator = hysysinfo_get_classpathSeparator();
	  newClasspath = hymem_allocate_memory (
		  classpathLen + sizeof(separator) + strlen (HY_TOOLS_PATH) + 1);
      if (NULL == newClasspath) {
        /* HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY=Internal VM error\: VM startup error: Out of memory\n */
        portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                 HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY);

        hytty_err_printf (PORTLIB, "Failed to allocate memory for tools path\n");
        return 1;
      }
	  strcpy (newClasspath, classpath);
	  newClasspath[classpathLen++] = (char)(separator & 0xFF);
	  newClasspath[classpathLen] = '\0';
	  strcat(newClasspath, HY_TOOLS_PATH);
	  argv[i] = newClasspath;
	  return 0;
	}
  }

  /* There was no classpath defined, so add one */
  newargv = hymem_allocate_memory ((argc + 2) * sizeof(pargv));
  if (NULL == newargv) {
    /* HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY=Internal VM error\: VM startup error: Out of memory\n */
    portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                             HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY);

    hytty_err_printf (PORTLIB, "Failed to allocate more memory for tools path\n");
    return 1;
  }

  for (i = 0; i < argc; i++) {
	  newargv[i] = argv[i];
  }
  newargv[i++]="-J-cp";
  newargv[i++]=HY_TOOLS_PATH;
  newargv[i]=NULL;

  *pargc +=2;
  *pargv = newargv;

  return 0;
}


/**
 * Scan for the -vm: option and return the associated value, or NULL
 * if the argument cannot be found.
 */
char *VMCALL
vmdll_parseCmdLine (HyPortLibrary * portLibrary, UDATA lastLegalArg,
                    char **argv)
{
  UDATA i;

  /* Parse command line args for -vm: */
  for (i = 1; i <= lastLegalArg; i++)
    {
      if ((argv[i][0] == '-'))
        {
          if ((PLATFORM_STRNICMP (&argv[i][1], "vm:", 3) == 0))
            {
              return &argv[i][4];
            }
        }
    }
  return NULL;
}

/**
 * Scan for the -vmdir: option and return the associated value, or NULL
 * if the argument cannot be found.
 */
char *VMCALL
#ifndef HY_NO_THR
vmdlldir_parseCmdLine (HyPortLibrary * portLibrary, UDATA lastLegalArg,
#else /* HY_NO_THR */
vmdlldir_parseCmdLine (UDATA lastLegalArg,
#endif /* HY_NO_THR */
                       char **argv)
{
  UDATA i;

  /* Parse command line args for -vmdir: */
  for (i = 1; i <= lastLegalArg; i++)
    {
      if ((argv[i][0] == '-'))
        {
          if ((PLATFORM_STRNICMP (&argv[i][1], "vmdir:", 6) == 0))
            {
              return &argv[i][7];
            }
        }
    }
  return NULL;
}

/**
 * Create of a JavaVM using JNI Invocation API and the arguments parsed from argc and argv.
 * Run the java class 
 * 
 * @param[in] portLibrary The port library.
 * @param[in] argc  The number of arguments passed to program on the command line.
 * @param[in] argv  The values of command-line arguments.
 * @param[in] handle The VM dll handle opened via the port library.
 * @param[in] version The invocation API version to test.
 * @param[in] ignoreUnrecognized A hint to the JNI to ignore/fail on unrecognized args.
 * @param[in] mainClass The class to run.
 * @param[in] classArg The index to mainClass in the array of launcher args.  
 * @param[in] propertiesFileName The properties file path and FileName. 
 * 
 * @return 0 on success, or a non-zero error code on failure. 
 */
static int
invocation (HyPortLibrary * portLibrary, int argc, char **argv, UDATA handle,
            jint version, jboolean ignoreUnrecognized, char *mainClass,
            UDATA classArg, char *propertiesFileName,
            int isStandaloneJar, char *vmdllsubdir, int versionFlag)
{
  JavaVMInitArgs vm_args;
  JavaVM *jvm;
  JNIEnv *env;
  char *mainClassJar;
  int isNameUTF = 0;
  int rc;
  jint (JNICALL * CreateJavaVM) (JavaVM **, JNIEnv **, JavaVMInitArgs *);
  PORT_ACCESS_FROM_PORT (portLibrary);

  mainClassJar = NULL;

  if (hysl_lookup_name
      (handle, "JNI_CreateJavaVM", (UDATA *) &CreateJavaVM, "iLLL"))
  {
      hytty_printf (PORTLIB, "Failed to find JNI_CreateJavaVM in DLL\n");
      return 1;
  }

  
  if (createVMArgs(portLibrary, argc, argv, version, ignoreUnrecognized, 
                    &vm_args, classArg, propertiesFileName, 
                    isStandaloneJar, &mainClassJar, vmdllsubdir)) {
     return 1;
   }
       
  if (CreateJavaVM (&jvm, &env, &vm_args))
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JAVA_VM=Internal VM error\: Failed to create Java VM\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JAVA_VM);
      return 1;
    }

  rc = 0;

  if (versionFlag) {
      jclass clazz;
      jmethodID mID;
      jstring jStrObject;

      /* First, print the copyright string to stdout */
      hyfile_printf (PORTLIB, HYPORT_TTY_OUT, HY_COPYRIGHT_STRING "\n");
        
      jStrObject = (*env)->NewStringUTF (env, argv[versionFlag]);
      if (!jStrObject) return 3;
        
      clazz = (*env)->FindClass (env, "org/apache/harmony/luni/util/Version");
      if (!clazz) return 3;
        
      mID = (*env)->GetStaticMethodID (env, clazz, "version",
                         "(Ljava/lang/String;)V");
      if (!mID) return 3;
        
      (*env)->CallStaticVoidMethod(env, clazz, mID, jStrObject);

      /* if -version is specified, exit, otherwise continue */
      if (0 == strncmp ("-version", argv[versionFlag], 8))
          return 0;
  }

  if (mainClass)
    {
      if (isStandaloneJar)
        {
          //jclass jarRunner;
          jclass clazz;
          jmethodID mID;
          jstring jStrObject;

          mainClass = mainClassJar;

          jStrObject = (*env)->NewStringUTF (env, mainClass);
          
          if (!jStrObject)
            {
              rc = 3;
              goto cleanup;
            }

          clazz = (*env)->FindClass (env, "java/lang/Class");
          if (!clazz)
            {
              rc = 3;
              goto cleanup;
            }

          mID =
            (*env)->GetStaticMethodID (env, clazz, "forName",
                                       "(Ljava/lang/String;)Ljava/lang/Class;");
          if (!mID)
            {
              rc = 3;
              goto cleanup;
            }

          /* ensure that the jar is the first arg passed to the jar runner */
          
          classArg--;

            
/*
     $$$ GMJ - removed this as it causes DRLVM to crash.  Need to fix
        DRLVM, but also have no idea why this is important - just seems
        to be a test
    
    jarRunner =
            (*env)->CallStaticObjectMethod (env, clazz, mID, jStrObject);

          if (jarRunner)
            {
              (*env)->DeleteLocalRef (env, jarRunner);
              classArg -= 1;    // make sure that the JAR is the first argument 
            }
          else
            {
              (*env)->ExceptionClear (env);
              (*jvm)->DestroyJavaVM (jvm);
              rc = 3;
              //goto cleanup;
            }
            */
        }

      rc =
        main_runJavaMain (env, mainClass, isNameUTF, (argc - (classArg + 1)),
                          &argv[classArg + 1], portLibrary);

    }
cleanup:
  if (vm_args.options)
    {
      hymem_free_memory (vm_args.options);
    }
  if (mainClassJar)
    {
      hymem_free_memory (mainClassJar);
    }

  /* Updated in the 6.0 spec, we can now detach the main thread before calling DestroyJavaVM, 
     and we must if we wish main thread uncaught exception handlers to be called. */
  (*jvm)->DetachCurrentThread(jvm);
  (*jvm)->DestroyJavaVM (jvm);
  /*if ((*jvm)->DestroyJavaVM(jvm)) {
     hytty_printf (PORTLIB, "Failed to destroy JVM\n");
     return 1;
     } */

  return rc;
}

 /**
 * Converts command-line arguments into a format compatible with JNI invocation API.
 *
 * @param[in] portLibrary The port library.
 * @param[in] argc  The number of arguments passed to program on the command line.
 * @param[in] argv  The values of command-line arguments.
 * @param[in] version The invocation API version to test.
 * @param[in] ignoreUnrecognized A hint to the JNI to ignore/fail on unrecognized args.
 * @param[in/out] vm_args Receives the newly converted JavaVMInitArgs (must be freed by caller).
 * @param[in] classArg The index to mainClass in the array of launcher args. 
 * @param[in] propertiesFileName The properties file path and FileName. 
 * @param[out] mainClassJar The class to run if running Jar file. 
 *  
 * @return 0 on success, or a non-zero error code on failure. 
 */

static int
createVMArgs (HyPortLibrary * portLibrary, int argc, char **argv,
              jint version, jboolean ignoreUnrecognized,
              JavaVMInitArgs * vm_args,
              UDATA classArg, char *propertiesFileName,
              int isStandaloneJar, char **mainClassJar, char *vmdllsubdir)
{
  JavaVMOption *options;
  char *exeName;
  char *endPathPtr;
  UDATA i;
  unsigned int j;
  unsigned int k;
  unsigned int l;
  key_value_pair * props = NULL;
  U_32 propcount = 0;
  unsigned int optcount = 0; /* number of specific options to VM */
  char *classPath;
  int vmJarRunner = -1; /* index of jarMainClass property */
  int ignoreBCP = 0;
  HyStringBuffer *javaHome = NULL, *classPath2 = NULL, *javaLibraryPath =
    NULL;
  char *portLibOptionStr = NULL;

  static char* subst_items[] = {"%LAUNCHER_HOME%", "%VM_DIR%"};
  static size_t subst_item_lens[] = {15, 8};
  static unsigned int subst_num = 2; 

  char* subst_values[2];
  size_t subst_value_lens[2];

  PORT_ACCESS_FROM_PORT (portLibrary);
  /* get the path to the executable */
  hysysinfo_get_executable_name (argv[0], &exeName);
  endPathPtr = strrchr (exeName, DIR_SEPARATOR);
  endPathPtr[0] = '\0';

  subst_values[0] = exeName;
  subst_value_lens[0] = strlen(exeName);
  subst_values[1] = vmdllsubdir ? vmdllsubdir : "";
  subst_value_lens[1] = vmdllsubdir ? strlen(vmdllsubdir) : 0;

   /* read in vm_args from properties file */
   properties_load(portLibrary, propertiesFileName, &props, &propcount);

   if (propcount != 0) {
       /* Check if we need to filter "-Xbootclasspath" options out from properties. */
       for (i = 1; i < classArg; i++)
       {
           if (strncmp (argv[i], "-Xbootclasspath:", 16) == 0) {
               ignoreBCP = 1;
               break;
           }
       }
   }

   for (l = 0; l < propcount; l++) 
   {
       /* only pass arguments starting with '-' to JNI_CreateJavaVM */
       if (props[l].key[0] == '-') 
       {
           /* if running jar file there is special handling for java.class.path later */
           /* Ignore classpath defines for -jar */
           /* XXX -cp is accepted ??? */
           /* if user overrides bootclasspath, skip bootclasspath defines */
           if ( (isStandaloneJar && 0 == strncmp (props[l].key, "-Djava.class.path=", 18))
               || (ignoreBCP && 0 == strncmp (props[l].key, "-Xbootclasspath", 15)) )
           {
               props[l].key[0] = '\0';
               continue;
           }
           ++optcount;
       } 
       else if (isStandaloneJar && 0 == strcmp(props[l].key, "jarMainClass"))
       {
           vmJarRunner = l;
       }
   }

   if (isStandaloneJar)
   {
       char* runner = (vmJarRunner == -1) ? 
            HARMONY_JARRUNNER_CLASSNAME : props[vmJarRunner].value;

       *mainClassJar = hymem_allocate_memory (strlen(runner) + 1);
       if (*mainClassJar == NULL)
       {
           PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
           return 1;
       }
       strcpy (*mainClassJar, runner);
   }

   /* allocate space for entries from command line, properties file, */
   /* 3 defaults plus the port library option */
   options = hymem_allocate_memory((classArg + optcount + 4) * sizeof (*options));

   if (options == NULL)
   {
       portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
           HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY);

       hytty_err_printf (PORTLIB, "Failed to allocate memory for options\n");
       return 1;
   }

   for (l = 0, j = 0; l < propcount; l++)
   {
       HyStringBuffer * lineBuf = NULL;
       char *pos, *start;
       int firstPass = 0;

       if (props[l].key[0] != '-') {
           continue;
       }

       /* format argument string as "key = value" */
       start = pos = props[l].key;
       do {
           /* search & replace %XXX% in key and value, if any. */
           while ( (pos = strchr(pos, '%')) )
           {
               int found = 0;
               for (k = 0; k < subst_num; k++) 
               {
                   if (0 == strncmp(subst_items[k], pos, subst_item_lens[k])) {
                       *pos = '\0';
                       lineBuf = strBufferCat(PORTLIB, lineBuf, start);
                       *pos = '%'; 
                       lineBuf = strBufferCat(PORTLIB, lineBuf, subst_values[k]);
                       start = (pos += subst_item_lens[k]);
                       found = 1;
                       break;
                   }
               }
               if (!found) ++pos;
           }
           lineBuf = strBufferCat(PORTLIB, lineBuf, start);

           /* key is processed in the first pass, check if we need next iteration. */
           if (0 != firstPass++ || *props[l].value == '\0') 
           {
               break;
           }
           lineBuf = strBufferCat(PORTLIB, lineBuf, "=");
           start = pos = props[l].value;
       }
       while (1);

       if (lineBuf == NULL) {
           PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
               HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
           return 1;
       }

       /* FIXME: buffer leak ignored */
       options[j].optionString = (char *)(lineBuf->data); 
       options[j].extraInfo = NULL;
       ++j;
   }

   properties_free(PORTLIB, props);
    
   for (i = 1; i < classArg; i++)
   {
       if ( (strcmp (argv[i], "-jar") != 0) 
           && (strncmp (argv[i], "-vmdir:", 7) != 0)
           && (strncmp (argv[i], "-vm:", 4) != 0) 
           && (strncmp (argv[i], "-version", 8) != 0)
            && (strncmp (argv[i], "-showversion", 12) != 0))
       {
          /* special coding for -classpath and -cp */
          /* they get passed to the vm as -Djava.class.path */
          if ((strcmp (argv[i], "-cp") == 0)
              || (strcmp (argv[i], "-classpath") == 0))
            {
              classPath = hymem_allocate_memory (strlen (argv[i + 1]) + 20);
              if (classPath == NULL)
                {
                  /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
                  PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                                       HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
                  return 1;
                }
              //classPath = hymem_allocate_memory( 2048 );
              strcpy (classPath, "-Djava.class.path=");
              strcat (classPath, argv[i + 1]);
              options[j].optionString = classPath;
              i++;              /*skip next arguement */
            }
          else if (strcmp(argv[i], "-verify")==0)
            {
                options[j].optionString="-Xverify";
            }
          else
            {
              options[j].optionString = argv[i];
            }
          options[j].extraInfo = NULL;
          j++;
        }
    }

  /* Check that the minimum required -D options have been included.  If not, calculate and add the defaults */
  initDefaultDefines (portLibrary, (void **)&options, argc, argv,
                      isStandaloneJar ? classArg : 0, &classPath2, &javaHome,
                      &javaLibraryPath, vmdllsubdir, (int *) &j);

  // Slam in the pointer to the HyPortLibrary
  portLibOptionStr = hymem_allocate_memory (strlen(PORT_LIB_OPTION) + 1);
  if (portLibOptionStr == NULL)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
      PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
        HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
      return 1;
    }

  strcpy (portLibOptionStr, PORT_LIB_OPTION);
  options[j].optionString = portLibOptionStr;
  options[j].extraInfo = portLibrary;
  j++;

  vm_args->version = version;
  vm_args->nOptions = j;
  vm_args->options = options;
  vm_args->ignoreUnrecognized = ignoreUnrecognized;

  // For debugging only
  //printf ("JNI_CreateJavaVM no of args %d\n",vm_args->nOptions);
  //for (j=0 ; j < (vm_args->nOptions) ; j++)
  //  {
  //    printf ("%s\n",vm_args->options[j]);
  //  }

  return 0;
}

static BOOLEAN findDirInPath(char *path, char *dir, char *separator)
{
  char *pos;
  int pathlen;
  int dirlen = strlen(dir);
  while (dirlen > 1 && dir[dirlen-1] == DIR_SEPARATOR) {
    dirlen--;
  }

  while ((pos = strchr(path, *separator)) != NULL) {
    int savedpathlen = pathlen = pos - path;
    while (pathlen > 1 && path[pathlen-1] == DIR_SEPARATOR) {
      pathlen--;
    }
    if (dirlen == pathlen && !strncmp(path, dir, dirlen)) {
      return TRUE;
    }
    path += savedpathlen + 1;
  }

  pathlen = strlen(path);
  while (pathlen > 1 && path[pathlen-1] == DIR_SEPARATOR) {
    pathlen--;
  }
  return dirlen == pathlen && !strncmp(path, dir, dirlen);
}

/**
* Update path to point to directory containing VM's DLLs 
* 
* @param[in] newPathToAdd The directory to add to the PATH environment variable 
* @param[in] argv The commandline argv for linux 
* 
* return 0 on success, -1 on failure
* 
*/
static IDATA
#ifndef HY_NO_THR
addDirsToPath (HyPortLibrary * portLibrary, int count, char *newPathToAdd[], char **argv)
#else /* HY_NO_THR */
addDirsToPath (int count, char *newPathToAdd[], char **argv)
#endif /* HY_NO_THR */
{
  char *oldPath = NULL;
  char *variableName = NULL;
  char *separator = NULL;
  char *newPath;
  int rc = 0;
  char *exeName;
  int found = 0;
  int i = 0;
  int strLen = 0;

#ifndef HY_NO_THR
  PORT_ACCESS_FROM_PORT (portLibrary);

  hysysinfo_get_executable_name (argv[0], &exeName);
#else /* HY_NO_THR */
  main_get_executable_name (argv[0], &exeName);
#endif /* HY_NO_THR */

  variableName = LIBPATH_ENV_VAR;
  separator = PATH_SEPARATOR_STR;

  oldPath = getenv (variableName);
  if (!oldPath) {
    oldPath = "";
  }
  
  /*
   *  see if we can find all paths in the current path
   */
    
  for (i=0; i < count; i++) {
    if (newPathToAdd[i] != NULL) {
      if (findDirInPath(oldPath, newPathToAdd[i], separator) != 0) {
        found++;
      } else {
        strLen += strlen(newPathToAdd[i]) + 1;
      }
    }
  }

  /* 
   *  if we found them all, we're done
   */   
  if (found == count) {
    return 0;
  }

  /*
   *  now add the ones that the oldPath doesn't have.  First figure out the
   *  size overall, and then add what we want on the front (keep the search
   *  short) and then add the old path on the end
   */
   
  strLen += strlen(variableName) + strlen("=") + strlen(oldPath);

#ifndef HY_NO_THR
  newPath = hymem_allocate_memory(strLen + 1);
#else /* HY_NO_THR */
  newPath = main_mem_allocate_memory(strLen + 1);
#endif /* HY_NO_THR */

  strcpy (newPath, variableName);
  strcat (newPath, "=");
  
  for (i=0; i < count; i++) { 
    if (newPathToAdd[i] != NULL
        && findDirInPath(oldPath, newPathToAdd[i], separator) == 0) {
      strcat(newPath, newPathToAdd[i]);
      strcat(newPath, separator);
    }
  }
  
  strcat(newPath, oldPath);

  /* 
   *  now set the new path, and in case of !Windows, execv() to 
   *  restart.  Don't free newPath, as the docs say that the 
   *  string becomes part of the environment, which sounds nutty
   *  but at worst, it's a leak of one string under windows
   */
     
#if defined(WIN32)
  rc = _putenv (newPath);
#else
  rc = putenv (newPath);
  execv (exeName, argv);
#endif

  return rc;
}

int
main_runJavaMain (JNIEnv * env, char *mainClassName, int nameIsUTF,
                  int java_argc, char **java_argv,
                  HyPortLibrary * portLibrary)
{
  int i, rc = 0;
  jclass cls;
  jmethodID mid, stringMid;
  jarray args;
  jclass stringClass;
  char *slashifiedClassName, *dots, *slashes;
  const char *utfClassName;
  jboolean isCopy;
  jstring str;
  jclass globalCls;
  jarray globalArgs;

  slashifiedClassName =
    portLibrary->mem_allocate_memory (portLibrary,
                                      strlen (mainClassName) + 1);
  if (slashifiedClassName == NULL)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
      rc = 2;
      goto done;
    }
  for (slashes = slashifiedClassName, dots = mainClassName; *dots;
       dots++, slashes++)
    {
      *slashes = (*dots == '.' ? '/' : *dots);
    }
  *slashes = '\0';

  /* fetch j.l.String and the constructor that takes a byte array as parm */
  stringClass = (*env)->FindClass (env, "java/lang/String");
  if (!stringClass)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_TO_FIND_JLS=Internal VM error: Failed to find class java/lang/String\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_TO_FIND_JLS);
      rc = 5;
      goto done;
    }
  stringMid = ((*env)->GetMethodID (env, stringClass, "<init>", "([BII)V"));

#ifndef ZOS /* Only do UTF conversion on non-z/OS platforms*/
  if (nameIsUTF)
#endif /* !ZOS */
  {
      cls = (*env)->FindClass (env, slashifiedClassName);
      portLibrary->mem_free_memory (portLibrary, slashifiedClassName);
    }
#ifndef ZOS
  else
    {
      rc =
        convertString (env, portLibrary, stringClass, stringMid,
                       slashifiedClassName, &str);
      portLibrary->mem_free_memory (portLibrary, slashifiedClassName);

      if (rc == 1)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_BA=Internal VM error: Failed to create byte array for class name %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_BA,
                                   mainClassName);
          rc = 10;
          goto done;
        }
      if (rc == 2)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JLS_FOR_CLASSNAME=Internal VM error: Failed to create java/lang/String for class name %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JLS_FOR_CLASSNAME,
                                   mainClassName);
          rc = 11;
          goto done;
        }
      utfClassName = (*env)->GetStringUTFChars (env, str, &isCopy);
      if (utfClassName == NULL)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY_CONVERTING=Internal VM error: Out of memory converting string to UTF Chars for class name %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY_CONVERTING,
                                   mainClassName);
          rc = 12;
          goto done;
        }

      cls = (*env)->FindClass (env, utfClassName);
      (*env)->ReleaseStringUTFChars (env, str, utfClassName);
      (*env)->DeleteLocalRef (env, str);
    }
#endif /* !ZOS */

  if (!cls)
    {
      rc = 3;
      goto done;
    }

  /* Create the String array before getting the methodID to get better performance from HOOK_ABOUT_TO_RUN_MAIN */
  args = (*env)->NewObjectArray (env, java_argc, stringClass, NULL);
  if (!args)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_ARG_ARRAY=Internal VM error: Failed to create argument array\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_ARG_ARRAY);
      rc = 6;
      goto done;
    }
  for (i = 0; i < java_argc; ++i)
    {
#ifndef ZOS /* Only convert the option strings on non-zOS platforms */
      rc =
        convertString (env, portLibrary, stringClass, stringMid, java_argv[i],
                       &str);
      if (rc == 1)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_BYTE_ARRAY=Internal VM error: Failed to create byte array for argument %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_BYTE_ARRAY,
                                   java_argv[i]);
          rc = 7;
          goto done;
        }
      if (rc == 2)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JLS_FOR_ARG=Internal VM error: Failed to create java/lang/String for argument %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JLS_FOR_ARG,
                                   java_argv[i]);
          rc = 8;
          goto done;
        }

      (*env)->SetObjectArrayElement (env, args, i, str);
#else
      (*env)->SetObjectArrayElement (env, args, i, (*env)->NewStringUTF(env, java_argv[i]));
#endif /* !ZOS */
      if ((*env)->ExceptionCheck (env))
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_SET_ARRAY_ELEM=Internal VM error: Failed to set array element for %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_SET_ARRAY_ELEM,
                                   java_argv[i]);
          rc = 9;
          goto done;
        }
#ifndef ZOS
      (*env)->DeleteLocalRef (env, str);
#endif /* !ZOS */
    }

  mid =
    (*env)->GetStaticMethodID (env, cls, "main", "([Ljava/lang/String;)V");
  if (!mid)
    {
      /* Currently, GetStaticMethodID does not throw an exception when the method is not found */
      /* HYNLS_EXELIB_CLASS_DOES_NOT_IMPL_MAIN=Class %s does not implement main()\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_CLASS_DOES_NOT_IMPL_MAIN,
                               mainClassName);
      rc = 4;
      goto done;
    }

  globalCls = (jclass) (*env)->NewGlobalRef (env, cls);
  if (globalCls)
    {
      (*env)->DeleteLocalRef (env, cls);
      cls = globalCls;
    }
  globalArgs = (jarray) (*env)->NewGlobalRef (env, args);
  if (globalArgs)
    {
      (*env)->DeleteLocalRef (env, args);
      args = globalArgs;
    }
  (*env)->DeleteLocalRef (env, stringClass);
  (*env)->CallStaticVoidMethod (env, cls, mid, args);

done:
  if ((*env)->ExceptionCheck (env))
    {
      if (rc == 0)
        rc = 100;
    }

  return rc;
}

void
dumpVersionInfo (HyPortLibrary * portLib)
{
  PORT_ACCESS_FROM_PORT (portLib);
                                                          
  hytty_err_printf (PORTLIB,
        (char *) portLib->nls_lookup_message (PORTLIB,
                HYNLS_DO_NOT_PRINT_MESSAGE_TAG,
                HYNLS_EXELIB_VERSION,
                "\njava version 1.5 (subset)\n"), "1.5 (subset)\n");
}

static I_32
initDefaultDefines (HyPortLibrary * portLib, void **vmOptionsTable, int argc,
                    char **argv, int jarArg, HyStringBuffer ** classPathInd,
                    HyStringBuffer ** javaHomeInd,
                    HyStringBuffer ** javaLibraryPathInd,
                    char *vmdllsubdir, int *vmOptionsCount)
{
  extern char *getDefineArgument (char *, char *);

  int optionCount, i;
  JavaVMOption *options;
  int hasJavaHome = 0;
  int hasJavaLibraryPath = 0;
  int hasClassPath = 0;
  HyStringBuffer *classPath = NULL;
  HyStringBuffer *javaHome = NULL;
  HyStringBuffer *javaLibraryPath = NULL;

  /* Cycle through the list of VM options and check that the minimum required defaults are there.
   * Calculate and insert the missing ones
   */

  optionCount = *vmOptionsCount;
  options = *vmOptionsTable;
  for (i = 0; i < optionCount; i++)
    {
      if (getDefineArgument (options[i].optionString, "java.home"))
        {
          hasJavaHome = 1;
          continue;
        }
      if (getDefineArgument (options[i].optionString, "java.library.path"))
        {
          hasJavaLibraryPath = 1;
          continue;
        }
      if (getDefineArgument (options[i].optionString, "java.class.path"))
        {
          /* Ignore classpath defines for -jar */
          if (!jarArg)
            {
              hasClassPath = 1;
              continue;
            }
        }
    }

  if (!hasJavaHome)
    {

      if (0 != main_initializeJavaHome (portLib, &javaHome, argc, argv))
        {
          /* This might be a memory leak, but main() will fail anyway */
          return -1;
        }

      if (javaHome)
        {
          javaHome = strBufferPrepend (portLib, javaHome, "-Djava.home=");

          if (!javaHome)
            return -1;
          *javaHomeInd = javaHome;

          options[*vmOptionsCount].optionString = (char *) javaHome->data;
          options[*vmOptionsCount].extraInfo = NULL;

          *vmOptionsCount = *vmOptionsCount + 1;
        }
    }

  if (!hasJavaLibraryPath)
    {

      if (0 !=
          main_initializeJavaLibraryPath (portLib, &javaLibraryPath, argv[0]))
        {
          /* This might be a memory leak, but main() will fail anyway */
          return -1;
        }

      if (javaLibraryPath)
        {
          javaLibraryPath =
            strBufferPrepend (portLib, javaLibraryPath,
                              "-Djava.library.path=");
          if (!javaLibraryPath)
            return -1;
          *javaLibraryPathInd = javaLibraryPath;
          options[*vmOptionsCount].optionString =
            (char *) javaLibraryPath->data;
          options[*vmOptionsCount].extraInfo = NULL;
          *vmOptionsCount = *vmOptionsCount + 1;
        }
    }

  if (!hasClassPath)
    {
      /* no free classpath if there is a -jar */
      if (jarArg)
        {
          if (jarArg < argc)
            {
              classPath = strBufferCat (portLib, classPath, argv[jarArg]);
              if (classPath == NULL)
                return -1;
            }
        }
      else
        {
          if (0 != main_initializeClassPath (portLib, &classPath))
            {
              /* This might be a memory leak, but main() will fail anyway */
              return -1;
            }
          if (classPath == NULL || classPath->data[0] == 0)
            {
              classPath = strBufferCat (portLib, classPath, ".");
              if (classPath == NULL)
                return -1;
            }
        }

      classPath = strBufferPrepend (portLib, classPath, "-Djava.class.path=");
      if (classPath == NULL)
        return -1;
      *classPathInd = classPath;
      options[*vmOptionsCount].optionString = (char *) classPath->data;
      options[*vmOptionsCount].extraInfo = NULL;
      *vmOptionsCount = *vmOptionsCount + 1;

    }
  return 0;
}

/* parse arg to determine if it is of the form -Darg=foo, and return foo.
 * Returns an empty string for args of the form -Darg,
 * Returns NULL if the argument is not recognized
 */
char *
getDefineArgument (char *arg, char *key)
{
  if (arg[0] == '-' && arg[1] == 'D')
    {
      int keyLength = strlen (key);
      if (strncmp (&arg[2], key, keyLength) == 0)
        {
          switch (arg[2 + keyLength])
            {
            case '=':
              return &arg[3 + keyLength];
            case '\0':
              return "";
            }
        }
    }
  return NULL;
}

#ifdef HY_NO_THR


int 
main_addVMDirToPath(int argc, char **argv, char **envp) 
{
  char *vmdllsubdir;
  char *newPathToAdd = NULL;
  char *exeName = NULL;
  char *exeBaseName;
  char *endPathPtr;
  char defaultDirName[] = "default";
  int rc = -1;
  char *dirs[2];
	    
  /* Find out name of the executable we are running as */
  main_get_executable_name (argv[0], &exeName);

  /* Pick out the end of the exe path, and start of the basename */
  exeBaseName = strrchr(exeName, DIR_SEPARATOR);
  if (exeBaseName == NULL) {
    endPathPtr = exeBaseName = exeName;
  } else {
    exeBaseName += 1;
    endPathPtr = exeBaseName;
  }

  /* Find the directory of the dll and set up the path */
  vmdllsubdir = vmdlldir_parseCmdLine (argc - 1, argv);
  if (!vmdllsubdir) {
      vmdllsubdir = defaultDirName;
   }

  /* jvm dlls are located in a subdirectory off of jre/bin */
  /* setup path to dll named in -vm argument                      */
  endPathPtr[0] = '\0';

  newPathToAdd = main_mem_allocate_memory (strlen (exeName) + strlen (vmdllsubdir) + 1);
	    
  if (newPathToAdd == NULL) {
     goto bail;
   }
	        
  strcpy (newPathToAdd, exeName);
  strcat (newPathToAdd, vmdllsubdir);

  dirs[0] = newPathToAdd;
  dirs[1] = exeName;
	    
  rc = addDirsToPath(2, dirs, argv);
	    
bail:
  if (exeName) {
    main_mem_free_memory (exeName);
  }

  if (newPathToAdd) {
    main_mem_free_memory (newPathToAdd);
  }
  // error code should be equal to 1 because of compatibility
  return rc == 0 ? 0 : 1;
}
#endif /* HY_NO_THR */
