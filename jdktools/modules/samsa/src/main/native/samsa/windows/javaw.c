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

#include <windows.h>
#include <stdio.h>
#include <sys/stat.h>

#define JDK_WEXE_POSTFIX        "\\jre\\bin\\javaw.exe\" "
#define JRE_WEXE_POSTFIX        "\\bin\\javaw.exe\" "
#define JRE_TEST_FILE       "\\bin\\harmony.properties"

char *getRoot();
int isJRERoot(const char*);

int WINAPI
WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine,
   int nShowCmd)
{
    PROCESS_INFORMATION procInfo;
    STARTUPINFO startInfo;
    DWORD res = 0;
    char *root = getRoot();
    int isJRE = isJRERoot(root);
    
    char *exePath = (char *)malloc((strlen(root)
                                    +strlen(isJRE 
                                            ? JRE_WEXE_POSTFIX 
                                            : JDK_WEXE_POSTFIX)
                                    +strlen(lpCmdLine)+2)*sizeof(char));
    exePath[0] = '\"';
    strcpy(exePath+1, root);
    strcat(exePath, isJRE ? JRE_WEXE_POSTFIX : JDK_WEXE_POSTFIX);
    strcat(exePath, lpCmdLine);
    
    // create child process
    memset(&procInfo, 0, sizeof(PROCESS_INFORMATION));
    memset(&startInfo, 0, sizeof(STARTUPINFO));
    startInfo.cb = sizeof(STARTUPINFO);
    startInfo.wShowWindow = nShowCmd;

    if (!CreateProcess(NULL, exePath, NULL, NULL, TRUE, 0, NULL, NULL, &startInfo, &procInfo)) { 
        free(exePath);
        return -1;
    }

    free(exePath);
    
    WaitForSingleObject(procInfo.hProcess, INFINITE);
    GetExitCodeProcess(procInfo.hProcess, &res);

    CloseHandle(procInfo.hProcess);
    CloseHandle(procInfo.hThread);

    return (int)res;
}

/*****************************************************************
 * isJRERoot(const char* root)
 * 
 *  returns 1 if root is the jre root
 */
int isJRERoot(const char* root) {

    char *temp = NULL;
#if defined(WIN32)
    DWORD result;
#else
    struct stat statbuf;
    int rc;
#endif

    temp = (char *) malloc(strlen(root) + strlen(JRE_TEST_FILE) + 1);
                
    if (temp == NULL) { 
        return -1;
    }
    
    strcpy(temp, root);
    strcat(temp, JRE_TEST_FILE);
    
#if defined(WIN32)
    result = GetFileAttributes((LPCTSTR) temp);
    free(temp);
    return result == 0xFFFFFFFF ? 0 : 1;
#else
    rc = lstat(temp, &statbuf);
    free(temp);
    return rc == -1 ? 0 : 1;
#endif
}
