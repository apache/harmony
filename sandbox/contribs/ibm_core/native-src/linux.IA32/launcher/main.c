/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define USING_VMI               /* Used in header files */

#include "jni.h"                /* for definitions of JavaVM */
#include "hycomp.h"             /* for portable types (UDATA,etc...) */
#include "hythread.h"           /* for synchronization */
#include "hyport.h"             /* for port library */
#include "hyexelibnls.h"        /* nls strings */
#include "libhlp.h"             /* defaults and environment variables and string buffer functions */
#include <string.h>
#include <stdlib.h>

#define PORT_LIB_OPTION "_org.apache.harmony.vmi.portlib"

#define HY_COPYRIGHT_STRING "(c) Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable."

#define HY_PATH_SLASH DIR_SEPARATOR

#if defined(WIN32)
#define PLATFORM_STRNICMP strnicmp
#endif

#if (LINUX)
#define PLATFORM_STRNICMP strncasecmp
#endif

static int invocation
PROTOTYPE ((HyPortLibrary * portLibrary, int argc, char **argv, UDATA handle,
            jint version, jboolean ignoreUnrecognized, char *mainClass,
            UDATA classArg, int isJvmSubDir, char *propertiesFileName,
            int isStandaloneJar, char *vmdllsubdir));
static int createVMArgs
PROTOTYPE ((HyPortLibrary * portLibrary, int argc, char **argv, UDATA handle,
            jint version, jboolean ignoreUnrecognized,
            JavaVMInitArgs * vm_args,
            jint (JNICALL ** CreateJavaVM) (JavaVM **, JNIEnv **,
                                            JavaVMInitArgs *),
            int isJvmSubDir, UDATA classArg, char *propertiesFileName,
            int isStandaloneJar, char *mainClassJar, char *vmdllsubdir));
char *VMCALL vmdll_parseCmdLine
PROTOTYPE ((HyPortLibrary * portLibrary, UDATA lastLegalArg, char **argv));
char *VMCALL vmdlldir_parseCmdLine
PROTOTYPE ((HyPortLibrary * portLibrary, UDATA lastLegalArg, char **argv));
UDATA VMCALL gpProtectedMain PROTOTYPE ((struct haCmdlineOptions * args));
IDATA convertString
PROTOTYPE ((JNIEnv * env, HyPortLibrary * portLibrary, jclass stringClass,
            jmethodID stringMid, char *chars, jstring * str));
static IDATA addJreDirToPath
PROTOTYPE ((HyPortLibrary * portLibrary, char *newPathToAdd, char **argv));
int main_runJavaMain
PROTOTYPE ((JNIEnv * env, char *mainClassName, int nameIsUTF, int java_argc,
            char **java_argv, HyPortLibrary * portLibrary));
jint readPropertiesFile
PROTOTYPE ((HyPortLibrary * portLibrary, char *propertiesFileName,
            char **fileContentsPtr));
static I_32 initDefaultDefines
PROTOTYPE ((HyPortLibrary * portLib, void **vmOptionsTable, int argc,
            char **argv, int jarArg, HyStringBuffer ** classPathInd,
            HyStringBuffer ** javaHomeInd,
            HyStringBuffer ** javaLibraryPathInd, int isJvmSubDir,
            char *vmdllsubdir, int *vmOptionsCount));

#if defined(WIN32)
#define	DIR_SEPERATOR '\\'
#define	DIR_SEPERATOR_STRING "\\"
#else
#define	DIR_SEPERATOR '/'
#define	DIR_SEPERATOR_STRING "/"
#endif

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
  int isStandaloneJar = 0;
  int copyrightWritten = 0;
  int versionWritten = 0;
  UDATA classArg = argc;
  int i;
  char *vmdllsubdir;
  int isJvmSubDir = 0;
  char *vmiPath;
  char *newPathToAdd;
  char *propertiesFileName;
  char *exeName;
  char *endPathPtr;
  //jint (JNICALL *JNI_CreateJavaVM)(JavaVM**, JNIEnv**, JavaVMInitArgs*);
  UDATA handle;
  int javaRc = 0;
  char defaultDllName[] = "clearvm";
  char defaultDirName[] = "default";
  int rc = -1;
  int showVersion = 0;

  PORT_ACCESS_FROM_PORT (args->portLibrary);

  if (argc <= 1)
    {
      hyfile_printf (PORTLIB, HYPORT_TTY_OUT,
                     "Harmony java launcher using vmi interface\n");
      hyfile_printf (PORTLIB, HYPORT_TTY_OUT, HY_COPYRIGHT_STRING "\n");
      hyfile_printf (PORTLIB, HYPORT_TTY_OUT,
                     "java [-vm:vmdll -vmdir:dir -D... [-X...]] classname [args]\n");
      return 0;
    }
  /* Find the class to load. First arguement to launcher not starting with - */

  for (i = 1; i < argc; i++)
    {
      if ('-' != argv[i][0])
        {
          classArg = i;         /* save position */
          mainClass = argv[i];  /* save class to execute */
          break;
        }
      else if ((strcmp ("-cp", argv[i]) == 0)
               || (strcmp ("-classpath", argv[i]) == 0))
        {
          i++;                  /* Skip the next arg */
        }

      if (0 == strcmp ("-version", argv[i]))
        {
          if (!versionWritten)
            {
              dumpVersionInfo (PORTLIB);
              hyfile_printf (PORTLIB, HYPORT_TTY_OUT,
                             HY_COPYRIGHT_STRING "\n");
              copyrightWritten = 1;
              versionWritten = 1;
            }
          return 0;
        }

      if (0 == strcmp ("-showversion", argv[i]))
        {
          showVersion = 1;
        }

      if (strcmp ("-jar", argv[i]) == 0)
        {
          isStandaloneJar = 1;
        }
    }
  /* Find the vm dll */
  vmdll = vmdll_parseCmdLine (PORTLIB, argc - 1, argv);
  if (!vmdll)
    {
      vmdll = defaultDllName;
    }

  /* Find the directory of the dll and set up the path */
  vmdllsubdir = vmdlldir_parseCmdLine (PORTLIB, argc - 1, argv);
  if (!vmdllsubdir)
    {
      isJvmSubDir = 1;
      vmdllsubdir = defaultDirName;
    }
  else
    {
      isJvmSubDir = 1;
    }

  /* if jvm dlls are located in a subdirectory off of jre/bin */
  /* setup path to dll named in -vm argument                      */
  if (isJvmSubDir)
    {
      hysysinfo_get_executable_name (argv[0], &exeName);
      endPathPtr = strrchr (exeName, DIR_SEPERATOR);
      endPathPtr[1] = '\0';
      newPathToAdd =
        hymem_allocate_memory (strlen (exeName) + strlen (vmdllsubdir) + 1);
      if (newPathToAdd == NULL)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
          PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
          goto bail;
        }

      vmiPath =
        hymem_allocate_memory (strlen (exeName) + strlen (vmdllsubdir) +
                               strlen (vmdll) +
                               strlen (DIR_SEPERATOR_STRING) + 1);
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
      strcat (vmiPath, DIR_SEPERATOR_STRING);
      strcat (vmiPath, vmdll);
      rc = addJreDirToPath (PORTLIB, newPathToAdd, argv);
      if (rc == -1)
        {
          hytty_printf (PORTLIB, "addJreDirToPath Failed\n");
          goto bail;
        }
    }
  else
    {
      vmiPath = hymem_allocate_memory (strlen (vmdll) + 1);
      if (vmiPath == NULL)
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
          PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
          goto bail;
        }
      strcpy (vmiPath, vmdll);
    }

  if (showVersion == 1)
    {
      if (!versionWritten)
        {
          dumpVersionInfo (PORTLIB);
          hyfile_printf (PORTLIB, HYPORT_TTY_OUT, HY_COPYRIGHT_STRING "\n");
          copyrightWritten = 1;
          versionWritten = 1;
        }
    }
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
  if (invocation
      (PORTLIB, argc, argv, handle, JNI_VERSION_1_4, JNI_TRUE, mainClass,
       classArg, isJvmSubDir, propertiesFileName, isStandaloneJar,
       vmdllsubdir))
    {
      hytty_printf (PORTLIB, "FAILED.\n");
      goto bail;
    }

  if (hysl_close_shared_library (handle))
    {
      hytty_printf (PORTLIB, "Failed to close JVM DLL: %s\n", argv[1]);
      goto bail;
    }
bail:
  if (propertiesFileName)
    {
      hymem_free_memory (propertiesFileName);
    }
  if (vmiPath)
    {
      hymem_free_memory (vmiPath);
    }
  if (newPathToAdd)
    {
      hymem_free_memory (newPathToAdd);
    }
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

#if (LINUX)
#define COMPARE_STRINGS strncasecmp
#else
#define COMPARE_STRINGS strncasecmp
#endif

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
vmdlldir_parseCmdLine (HyPortLibrary * portLibrary, UDATA lastLegalArg,
                       char **argv)
{
  UDATA i;

#if (LINUX)
#define COMPARE_STRINGS strncasecmp
#else
#define COMPARE_STRINGS strncasecmp
#endif

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
            UDATA classArg, int isJvmSubDir, char *propertiesFileName,
            int isStandaloneJar, char *vmdllsubdir)
{
  JavaVMInitArgs vm_args;
  JavaVM *jvm;
  JNIEnv *env;
  char *mainClassJar;
  int javaRc = 0;
  int isNameUTF = 0;
  int rc;
  jint (JNICALL * CreateJavaVM) (JavaVM **, JNIEnv **, JavaVMInitArgs *);
  PORT_ACCESS_FROM_PORT (portLibrary);

  mainClassJar = hymem_allocate_memory (50);
  if (mainClassJar == NULL)
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
      PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                           HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
      return 1;
    }
  if (createVMArgs
      (portLibrary, argc, argv, handle, version, ignoreUnrecognized, &vm_args,
       &CreateJavaVM, isJvmSubDir, classArg, propertiesFileName,
       isStandaloneJar, mainClassJar, vmdllsubdir))
    return 1;

  if (CreateJavaVM (&jvm, &env, &vm_args))
    {
      /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JAVA_VM=Internal VM error\: Failed to create Java VM\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_CREATE_JAVA_VM);
      return 1;
    }

  rc = 0;
  if (mainClass)
    {
      if (isStandaloneJar)
        {
          jclass jarRunner;
          jclass clazz;
          jmethodID mID;
          jstring jStrObject;

          mainClass = mainClassJar;

          jStrObject = (*env)->NewStringUTF (env, mainClass);
          if (!jStrObject)
            {
              (*env)->ExceptionDescribe (env);
              (*jvm)->DestroyJavaVM (jvm);
              rc = 3;
              //goto cleanup;
            }

          clazz = (*env)->FindClass (env, "java/lang/Class");
          if (!clazz)
            {
              (*env)->ExceptionDescribe (env);
              (*jvm)->DestroyJavaVM (jvm);
              rc = 3;
              //goto cleanup;
            }

          mID =
            (*env)->GetStaticMethodID (env, clazz, "forName",
                                       "(Ljava/lang/String;)Ljava/lang/Class;");
          if (!mID)
            {
              (*env)->ExceptionDescribe (env);
              (*jvm)->DestroyJavaVM (jvm);
              rc = 3;
              //goto cleanup;
            }

          /* should not spawn an exception */
          jarRunner =
            (*env)->CallStaticObjectMethod (env, clazz, mID, jStrObject);

          if (jarRunner)
            {
              (*env)->DeleteLocalRef (env, jarRunner);
              classArg -= 1;    /* make sure that the JAR is the first argument */
            }
          else
            {
              (*env)->ExceptionClear (env);
              (*jvm)->DestroyJavaVM (jvm);
              /* "-jar option is not available for this class library\n" */
              //              portLibrary->nls_printf(portLibrary, HYNLS_ERROR, HYNLS_EXE_JAR_OPTION_NOT_AVAIL );
              rc = 3;
              //goto cleanup;
            }
        }

      javaRc =
        main_runJavaMain (env, mainClass, isNameUTF, (argc - (classArg + 1)),
                          &argv[classArg + 1], portLibrary);

    }
  if (vm_args.options)
    {
      hymem_free_memory (vm_args.options);
    }
  if (mainClassJar)
    {
      hymem_free_memory (mainClassJar);
    }

  (*jvm)->DestroyJavaVM (jvm);
  /*if ((*jvm)->DestroyJavaVM(jvm)) {
     hytty_printf (PORTLIB, "Failed to destroy JVM\n");
     return 1;
     } */

  return 0;
}

 /**
 * Converts command-line arguments into a format compatible with JNI invocation API,
 * and looks up the JNI_CreateJavaVM() function in dll/shared library specified by
 * handle.
 *
 * @param[in] portLibrary The port library.
 * @param[in] argc  The number of arguments passed to program on the command line.
 * @param[in] argv  The values of command-line arguments.
 * @param[in] handle The VM dll handle opened via the port library.
 * @param[in] version The invocation API version to test.
 * @param[in] ignoreUnrecognized A hint to the JNI to ignore/fail on unrecognized args.
 * @param[in/out] vm_args Receives the newly converted JavaVMInitArgs (must be freed by caller).
 * @param[in/out] CreateJavaVM Receives the address of the JNI_CreateJavaVM() function. 
 * @param[in] isJvmSubDir The indicator to show we are using a subdirectory for the VM
 * @param[in] classArg The index to mainClass in the array of launcher args. 
 * @param[in] propertiesFileName The properties file path and FileName. 
 * @param[out] mainClassJar The class to run if running Jar file. 
 *  
 * @return 0 on success, or a non-zero error code on failure. 
 */

static int
createVMArgs (HyPortLibrary * portLibrary, int argc, char **argv,
              UDATA handle, jint version, jboolean ignoreUnrecognized,
              JavaVMInitArgs * vm_args,
              jint (JNICALL ** CreateJavaVM) (JavaVM **, JNIEnv **,
                                              JavaVMInitArgs *),
              int isJvmSubDir, UDATA classArg, char *propertiesFileName,
              int isStandaloneJar, char *mainClassJar, char *vmdllsubdir)
{
  JavaVMOption *options;
  char *exeName;
  char *endPathPtr;
  int lengthExeName;
  UDATA i = 0;
  int j = 0;
  int k = 0;
  int l = 0;
  char *fileContents = NULL;
  char **fileContentsPtr = &fileContents;
  int rc = 0;
  char *lineDelimiter = NULL;
  char *equalsDelimiter = NULL;
  char *startOfLine = NULL;
  char *lineStr[20];
  char *expandedLineStr[20];
  int linecount = 0;
  int noOfLauncherHomes;
  int offset;
  char *classPath;
  int useDefaultJarRunner = 0;
  HyStringBuffer *javaHome = NULL, *classPath2 = NULL, *javaLibraryPath =
    NULL;
  char *portLibOptionStr = NULL;

  PORT_ACCESS_FROM_PORT (portLibrary);
  /* get the path to the executable */
  hysysinfo_get_executable_name (argv[0], &exeName);
  endPathPtr = strrchr (exeName, DIR_SEPERATOR);
  endPathPtr[0] = '\0';
  lengthExeName = strlen (exeName);

  /* read in vm_args from properties file */
  rc = readPropertiesFile (portLibrary, propertiesFileName, fileContentsPtr);
  if (rc == 0)
    {

      //printf ("filecontents =%s\n", fileContents);
      lineDelimiter = strstr (fileContents, PLATFORM_LINE_DELIMITER);
      startOfLine = fileContents;
      /* note this logic means you need a line feed at the end of every line */
      /* you cannot have a line ending with the end of file character       */
      while (lineDelimiter)
        {
          /* Hammer the line delimiter to be a null */
          *lineDelimiter = '\0';
          if (*startOfLine == '-')
            {
              lineStr[linecount] = startOfLine;
              //printf ("linecount %d  = %s\n",linecount, lineStr[linecount]); 
              linecount++;
            }
          else if (isStandaloneJar)
            {
              if (strncmp (startOfLine, "jarMainClass", 12) == 0)
                {
                  strcpy (mainClassJar, startOfLine + 13);
                  useDefaultJarRunner = 1;
                }
            }
          startOfLine = lineDelimiter + strlen (PLATFORM_LINE_DELIMITER);

          lineDelimiter = lineDelimiter + strlen (PLATFORM_LINE_DELIMITER);
          lineDelimiter = strstr (lineDelimiter, PLATFORM_LINE_DELIMITER);
        }
      //printf ("line count = %d\n", linecount);
    }
  /* now expand strings with %LAUNCHER_HOME% */
  for (l = 0; l < linecount; l++)
    {
      offset = 0x00;
      noOfLauncherHomes = 0;

      /* count number of instances of %LAUNCHER_HOME% in String */
      while (equalsDelimiter =
             strstr (lineStr[l] + offset, "%LAUNCHER_HOME%"))
        {
          noOfLauncherHomes++;
          offset = equalsDelimiter - lineStr[l] + 15;
          //printf("counter = %d\n",noOfLauncherHomes);
        }
      /* Allocate memory for expanding string */
      if (equalsDelimiter = strstr (lineStr[l], "%LAUNCHER_HOME%"))
        {
          expandedLineStr[l] =
            hymem_allocate_memory (strlen (lineStr[l]) +
                                   (lengthExeName * noOfLauncherHomes));
          if (expandedLineStr[l] == NULL)
            {
              /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
              PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
              return 1;
            }
        }
      /* fill in expanded string */
      offset = 0;
      *expandedLineStr[l] = '\0';
      while (equalsDelimiter = strstr (lineStr[l], "%LAUNCHER_HOME%"))
        {
          /* Hammer the line delimiter to be a null */
          *equalsDelimiter = '\0';
          strcat (expandedLineStr[l], lineStr[l]);
          strcat (expandedLineStr[l], exeName);
          //printf ("expandedLineStr[l] = %s\n", expandedLineStr[l]);
          lineStr[l] = equalsDelimiter + 15;
        }

      if (noOfLauncherHomes == 0)
        {
          expandedLineStr[l] = lineStr[l];
        }
      else
        {
          strcat (expandedLineStr[l], lineStr[l]);
          //printf ("expandedLineStr[l] = %s\n", expandedLineStr[l]);
        }
    }
  if (isStandaloneJar)
    {
      if (useDefaultJarRunner == 0)
        {
          mainClassJar = hymem_allocate_memory (50);
          if (mainClassJar == NULL)
            {
              /* HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY=Internal VM error: Out of memory\n */
              PORTLIB->nls_printf (PORTLIB, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_OUT_OF_MEMORY);
              return 1;
            }
          strcpy (mainClassJar, "com.ibm.oti.vm.JarRunner");
        }
    }
  /* entries from command line, properties file, 3 defaults plus the port library option */
  options =
    hymem_allocate_memory ((argc + linecount + 4) * sizeof (*options));

  if (options == NULL)
    {
      /* HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY=Internal VM error\: VM startup error: Out of memory\n */
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                               HYNLS_EXELIB_VM_STARTUP_ERR_OUT_OF_MEMORY);

      hytty_err_printf (PORTLIB, "Failed to allocate memory for options\n");
      return 1;
    }

  if (rc == 0)                  /* there is a properties file */
    {
      for (k = 0, j = 0; k < linecount; k++)
        {
          /* if jar file there is special handling for java.class.path later */
          /* Ignore classpath defines for -jar */
          if (isStandaloneJar)
            {
              if (strncmp (lineStr[k], "-Djava.class.path=", 18) != 0)
                {
                  options[j].optionString = expandedLineStr[k];
                  options[j].extraInfo = NULL;
                  j++;
                }
            }
          else
            {
              options[j].optionString = expandedLineStr[k];
              options[j].extraInfo = NULL;
              j++;
            }
        }
    }

  /* only pass arguments starting with '-' to JNI_CreateJavaVM */
  /* but note parameters to program might have '-'             */
  for (i = 1; i < classArg; i++)
    {
      if (('-' == argv[i][0]) && (strncmp (argv[i], "-jar", 4) != 0) &&
          (strncmp (argv[i], "-vmdir:", 7) != 0)
          && (strncmp (argv[i], "-vm:", 4) != 0)
          && (strncmp (argv[i], "-showversion", 12) != 0))
        {
          /* special coding for -classpath and -cp */
          /* they get passed to the vm as -Djava.class.path */
          if ((strncmp (argv[i], "-cp", 3) == 0)
              || (strncmp (argv[i], "-classpath", 9) == 0))
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
                      &javaLibraryPath, isJvmSubDir, vmdllsubdir, &j);

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

  if (hysl_lookup_name
      (handle, "JNI_CreateJavaVM", (UDATA *) CreateJavaVM, "iLLL"))
    {
      hytty_printf (PORTLIB, "Failed to find JNI_CreateJavaVM in DLL\n");
      return 1;
    }

  return 0;
}

/**
* Update path to point to directory containing VM's DLLs 
* 
* @param[in] newPathToAdd The directory to add to the PATH environment variable 
* @param[in] argv The commandline argv for linux 
* 
*/
static IDATA
addJreDirToPath (HyPortLibrary * portLibrary, char *newPathToAdd, char **argv)
{
  char *tempchar = NULL;
  char *variableName = NULL;
  char *separator = NULL;
  UDATA newPathLength;
  char *newPath;
  int rc = 0;
  char *exeName;

  PORT_ACCESS_FROM_PORT (portLibrary);

  hysysinfo_get_executable_name (argv[0], &exeName);

#if defined(WIN32)
  variableName = "PATH";
  separator = ";";
#else
  variableName = "LD_LIBRARY_PATH";
  separator = ":";
#endif

  tempchar = getenv (variableName);
  if (!tempchar)
    tempchar = "";
  newPathLength = strlen (tempchar) + strlen (newPathToAdd) + strlen (variableName) + 3;        /* 3 = separator + equals + EOL */
  newPath = hymem_allocate_memory (newPathLength);

  if (!newPath)
    return -1;
  strcpy (newPath, variableName);
  strcat (newPath, "=");
  strcat (newPath, newPathToAdd);
  strcat (newPath, separator);
  strcat (newPath, tempchar);

#if defined(WIN32)
  rc = _putenv (newPath);
#else
  if (strstr (tempchar, newPathToAdd) == NULL)
    {
      rc = putenv (newPath);
      execv (exeName, argv);
    }
#endif

  /* putenv still uses the memory */
  //hymem_free_memory(newPath);

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

  PORT_ACCESS_FROM_PORT (portLibrary);
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

  if (nameIsUTF)
    {
      cls = (*env)->FindClass (env, slashifiedClassName);
      portLibrary->mem_free_memory (portLibrary, slashifiedClassName);
    }
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
      if ((*env)->ExceptionCheck (env))
        {
          /* HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_SET_ARRAY_ELEM=Internal VM error: Failed to set array element for %s\n */
          portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
                                   HYNLS_EXELIB_INTERNAL_VM_ERR_FAILED_SET_ARRAY_ELEM,
                                   java_argv[i]);
          rc = 9;
          goto done;
        }
      (*env)->DeleteLocalRef (env, str);
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

  (*env)->ExceptionDescribe (env);
  return rc;
}

/**
  * Read the properties file specified by <tt>propertiesFileName</tt> into  <tt>file contents</tt> pointer.
  *
  * @param portLibrary - The port library used to interact with the platform.
  * @param propertiesFileName - The file from which to read data using hyfile* functions.
  * @param fileContents- A pointer to memory containing the property file entries.
  *
  * @return JNI_OK on success, or a JNI error code on failure.
  */
jint
readPropertiesFile (HyPortLibrary * portLibrary, char *propertiesFileName,
                    char **fileContentsPtr)
{
  PORT_ACCESS_FROM_PORT (portLibrary);
  IDATA propsFD = -1;
  I_64 seekResult;
  char *fileContents = NULL;
  IDATA fileSize;
  IDATA bytesRemaining;
  jint returnCode = JNI_OK;
  char *writeCursor;

  /* Determine the file size, fail if > 2G */
  seekResult = hyfile_length (propertiesFileName);
  if ((seekResult <= 0) || (seekResult > 0x7FFFFFFF))
    {
      return JNI_ERR;
    }
  fileSize = (IDATA) seekResult;

  /* Open the properties file */
  propsFD = hyfile_open (propertiesFileName, (I_32) HyOpenRead, (I_32) 0);
  if (propsFD == -1)
    {
      /* Could not open the file */
      return JNI_ERR;
    }

  /* Allocate temporary storage */
  fileContents = hymem_allocate_memory (fileSize);
  if (!fileContents)
    {
      return JNI_ENOMEM;
    }

  /* Initialize the read state */
  bytesRemaining = fileSize;
  writeCursor = fileContents;

  /* Suck the file into memory */
  while (bytesRemaining > 0)
    {
      IDATA bytesRead = hyfile_read (propsFD, writeCursor, bytesRemaining);
      if (bytesRead == -1)
        {
          /* Read failed */
          returnCode = JNI_ERR;
          goto bail;
        }

      /* Advance the read state */
      bytesRemaining -= bytesRead;
      writeCursor += bytesRead;
    }
  *fileContentsPtr = fileContents;

bail:
  if (propsFD != -1)
    {
      hyfile_close (propsFD);
    }

  return returnCode;
}

void
dumpVersionInfo (HyPortLibrary * portLib)
{
  PORT_ACCESS_FROM_PORT (portLib);
                                                          
  hytty_err_printf (PORTLIB,
        (char *) portLib->nls_lookup_message (PORTLIB,
                HYNLS_DO_NOT_PRINT_MESSAGE_TAG,
                HYNLS_EXELIB_VERSION,
                "\njava version 1.4.2 (subset)\n"), "1.4.2 (subset)\n");
}

static I_32
initDefaultDefines (HyPortLibrary * portLib, void **vmOptionsTable, int argc,
                    char **argv, int jarArg, HyStringBuffer ** classPathInd,
                    HyStringBuffer ** javaHomeInd,
                    HyStringBuffer ** javaLibraryPathInd, int isJvmSubDir,
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

