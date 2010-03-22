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

#undef _WIN32_WINNT
#define _WIN32_WINNT 0x0500

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
#include <windows.h>
#include <sddl.h>

//#include <winbase.h>
#include "authwin32.h"

#include <assert.h>

#include "vmi.h"
#include "jni.h"

#pragma warning(disable:4311)

void error(LPVOID lpJEnv, LPCSTR msg, DWORD dwErr);

jfieldID jf_user = NULL;
jfieldID jf_domainSid = NULL;
    
jfieldID jf_mainGroup = NULL;
jfieldID jf_groups = NULL;
jfieldID jf_token = NULL;

jfieldID jf_debugNative = NULL;


BOOLEAN
getDebugNative( JNIEnv * jenv, jobject thiz )
{
	return (*jenv)->GetBooleanField (jenv, thiz, jf_debugNative) ? TRUE : FALSE;
}


JNIEXPORT void JNICALL 
Java_org_apache_harmony_auth_module_NTSystem_initNatives
(JNIEnv * jenv, jclass klass)
{
	jclass klassErr = NULL;

	if( NULL == (jf_user = (*jenv)->GetFieldID (jenv, klass, "user", "Lorg/apache/harmony/auth/NTSidUserPrincipal;"))) {
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"user\" of type NTSidUserPrincipal");
		return;
	}

	if( NULL == (jf_domainSid = (*jenv)->GetFieldID (jenv, klass, "domainSid", "Ljava/lang/String;")) ) {
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"domainSid\" of type String");
		return;
	}

	if( NULL == (jf_mainGroup = (*jenv)->GetFieldID (jenv, klass, "mainGroup", "Lorg/apache/harmony/auth/NTSidPrimaryGroupPrincipal;")) ) {
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"mainGroup\" of type NTSidPrimaryGroupPrincipal");
		return;
	}

	if( NULL == (jf_groups = (*jenv)->GetFieldID (jenv, klass, "groups", "[Lorg/apache/harmony/auth/NTSidGroupPrincipal;")) ) {
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"groups\" of type NTSidPrimaryGroupPrincipal");
		return;
	}

	if( NULL == (jf_token = (*jenv)->GetFieldID (jenv, klass, "token", "J")) ) {
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"token\" of type NTSidPrimaryGroupPrincipal");
		return;
	}

	if( NULL == (jf_debugNative = (*jenv)->GetFieldID (jenv, klass, "debugNative", "Z")) ) {
		if ( (*jenv)->ExceptionCheck (jenv) ) {
			(*jenv)->ExceptionDescribe (jenv);
			return;
		}
		klassErr = (*jenv)->FindClass (jenv, "java/lang/Error");
		assert(klassErr);
		(*jenv)->ThrowNew (jenv, klassErr, "Could not find field \"debugNative\" of type boolean.");
		return;
	}
}


LPVOID QueryInfo
(JNIEnv * jenv, HANDLE hToken, TOKEN_INFORMATION_CLASS klass) 
{
	PORT_ACCESS_FROM_ENV (jenv);

	DWORD dwSize;
	LPVOID pData = NULL;

	if( !GetTokenInformation(hToken, klass, NULL, 0, &dwSize) ) {
		DWORD dwErr = GetLastError();
		if( ERROR_INSUFFICIENT_BUFFER != dwErr ) {
			return NULL;
		}
	}

	pData = hymem_allocate_memory(dwSize);
	if( !GetTokenInformation(hToken, klass, pData, dwSize, &dwSize) ) {
		DWORD dwErr = GetLastError();
		hymem_free_memory(pData);
		SetLastError(dwErr);
		return NULL;
	}
	return pData;
}


BOOLEAN GetInfo
(JNIEnv * jenv, PSID sid, LPSTR* ppName, LPSTR* ppDomain) 
{
	PORT_ACCESS_FROM_ENV (jenv);

	DWORD dwNameSize = 0;
	DWORD dwDomainNameSize = 0;
	SID_NAME_USE snu;
	if( !LookupAccountSid(NULL, sid, NULL, &dwNameSize, NULL, &dwDomainNameSize, &snu)) {
		if( ERROR_INSUFFICIENT_BUFFER != GetLastError() ) {
			return FALSE;
		}
	}
	*ppName = (LPSTR)hymem_allocate_memory(dwNameSize);
	if( NULL == ppName ) {
		return FALSE;
	}
	*ppDomain = (LPSTR)hymem_allocate_memory(dwDomainNameSize);
	if( NULL == ppName ) {
		DWORD err = GetLastError();
		hymem_free_memory(*ppName);
		SetLastError(err);
		return FALSE;
	}
	if( !LookupAccountSid(NULL, sid, *ppName, &dwNameSize, *ppDomain, &dwDomainNameSize, &snu)) {
		DWORD err = GetLastError();
		hymem_free_memory(*ppName);
		hymem_free_memory(*ppDomain);
		SetLastError(err);
		return FALSE;
	}

	return TRUE;
}


JNIEXPORT void JNICALL
Java_org_apache_harmony_auth_module_NTSystem_load
(JNIEnv * jenv, jobject thiz)
{
	DWORD i; /* tmp */

	DWORD dwError = -1; /* presume unknown error */
	LPCSTR errMsg = NULL;
	DWORD dwSaveError = -1;

	HANDLE hUser = INVALID_HANDLE_VALUE;
	HANDLE iToken= INVALID_HANDLE_VALUE;

	LPVOID lpUserData = NULL, lpGroupData = NULL, lpAllGroupsData = NULL;
	LPSTR lpStr0 = NULL, lpStr1 = NULL, lpStr2 = NULL;
	LPSTR lpUserSid = NULL, lpDomainName = NULL;
	PSID domainSid = NULL;

	SID_IDENTIFIER_AUTHORITY sia = SECURITY_NT_AUTHORITY;

	TOKEN_USER * ptu = NULL;
	PSID userSid = NULL;

	jclass jkl = NULL;
	jmethodID ctor = NULL;

	jstring jstrSid = NULL;
	jstring jstrUser = NULL;
	jstring jstrDomain = NULL;
	jobject obj = NULL;

	jstring jstrDomainSid = NULL;

	PTOKEN_PRIMARY_GROUP ptpg = NULL;
	PSID groupSid = NULL;

	jclass jklassPrimaryGroup = NULL;
	jobject jobj = NULL;

	PTOKEN_GROUPS ptgs = NULL;

	jclass klassGroup = NULL;
	jmethodID groupCtor3 = NULL;
	jmethodID groupCtor1 = NULL;
	jobjectArray jgroups = NULL;

	jobject jobj1 = NULL;

	//
	// Get the token for the user currently running this Thread
	//
	if( !OpenThreadToken(GetCurrentThread(), TOKEN_QUERY|TOKEN_DUPLICATE, TRUE, &hUser) ) {
		// failed to open thread token. well, let's try process' one
		if( !OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY|TOKEN_DUPLICATE, &hUser) ) {
			errMsg = "Unable to obtain user token";
			goto exit;
		}
	}

	//
	// Obtain the User's info
	//
	if( NULL == (lpUserData = (TOKEN_USER*)QueryInfo(jenv, hUser, TokenUser)) ) {
		errMsg = "Unable to obtain user's token info";
		goto exit;
	}

	ptu = (TOKEN_USER*)lpUserData;

	if( !IsValidSid(ptu->User.Sid) ) {
		errMsg = "Got invalid user's SID";
		goto exit;
	}

	userSid = ptu->User.Sid;

	ConvertSidToStringSid(userSid, &lpStr0);
	lpUserSid = lpStr0;
	lpStr0 = NULL;

	//
    // step +n:  Retrieve user name and domain name basing on user's SID.
	//
	if( !GetInfo(jenv, userSid, &lpStr0, &lpStr1) ) {
		errMsg = "Unable to retrieve user's name and domain";
		goto exit;
	};

	jkl = (*jenv)->FindClass (jenv, "org/apache/harmony/auth/NTSidUserPrincipal");
	if( NULL == jkl || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find class NTSidUserPrincipal";
		goto exit;
	}
	ctor = (*jenv)->GetMethodID (jenv, jkl, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if( NULL == ctor || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find ctor at NTSidUserPrincipal class";
		goto exit;
	}

	jstrSid = (*jenv)->NewStringUTF (jenv, lpUserSid);
	jstrUser = (*jenv)->NewStringUTF (jenv, lpStr0);
	jstrDomain = (*jenv)->NewStringUTF (jenv, lpStr1);
	obj = (*jenv)->NewObject (jenv, jkl, ctor, jstrSid, jstrUser, jstrDomain);
	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}
	(*jenv)->SetObjectField (jenv, thiz, jf_user, obj);
	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}
	
	LocalFree(lpStr0); lpStr0 = NULL;
	lpDomainName = lpStr1; 
	lpStr1 = NULL;

	//
	// Step +1: Obtain domain SID
	//
	if( !AllocateAndInitializeSid(
		&sia, 4, 
		*GetSidSubAuthority(userSid, 0), 
		*GetSidSubAuthority(userSid, 1), 
		*GetSidSubAuthority(userSid, 2),
		*GetSidSubAuthority(userSid, 3), 
		0, 0, 0, 0, 
		&domainSid)) {

		errMsg = "Unable to allocate domain SID";
		goto exit;
	}

	if( !IsValidSid(domainSid) ) {
		errMsg = "Got invalid domain SID";
		goto exit;
	}

	ConvertSidToStringSid(domainSid, &lpStr0);

	jstrDomainSid = (*jenv)->NewStringUTF (jenv, lpStr0);
	(*jenv)->SetObjectField (jenv, thiz, jf_domainSid, jstrDomainSid);
	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}
	LocalFree(lpStr0); lpStr0 = NULL;

	//
	// step +1: get primary group sid
	//
	if( NULL == (lpGroupData = QueryInfo(jenv, hUser, TokenPrimaryGroup)) ) {
		errMsg = "Unable to get primary group";
		goto exit;
	};

	ptpg = (PTOKEN_PRIMARY_GROUP)lpGroupData;
	groupSid = ptpg->PrimaryGroup;

	if( !IsValidSid(groupSid) ) {
		errMsg = "Got invalid primary groups' SID";
		goto exit;
	}

	if( !GetInfo(jenv, groupSid, &lpStr0, &lpStr1) ) {
		errMsg = "Unable to get primary group's info";
		goto exit;
	}
	ConvertSidToStringSid(groupSid, &lpStr2);

	jklassPrimaryGroup = (*jenv)->FindClass (jenv, "org/apache/harmony/auth/NTSidPrimaryGroupPrincipal");
	if( NULL == jklassPrimaryGroup || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find class NTSidPrimaryGroupPrincipal";
		goto exit;
	}

	ctor = (*jenv)->GetMethodID (jenv, jklassPrimaryGroup, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if( NULL == ctor ) {
		errMsg = "Could not find appropriate ctor at NTSidPrimaryGroupPrincipal";
		goto exit;
	}

	jobj = (*jenv)->NewObject (jenv, jklassPrimaryGroup, ctor, 
		(*jenv)->NewStringUTF (jenv, lpStr2), (*jenv)->NewStringUTF (jenv, lpStr0), (*jenv)->NewStringUTF (jenv, lpStr1));

	LocalFree(lpStr0); lpStr0 = NULL;
	LocalFree(lpStr1); lpStr1 = NULL;
	LocalFree(lpStr2); lpStr2 = NULL;


	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}
	(*jenv)->SetObjectField (jenv, thiz, jf_mainGroup, jobj);

	//
	// step +1: get groups
	//
	if( NULL== (lpAllGroupsData = QueryInfo(jenv, hUser, TokenGroups)) ) {
		errMsg = "Unable to query user's groups";
		goto exit;
	}

	ptgs = (PTOKEN_GROUPS)lpAllGroupsData;

	klassGroup = (*jenv)->FindClass (jenv, "org/apache/harmony/auth/NTSidGroupPrincipal");
	if( NULL == klassGroup || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find NTSidGroupPrincipal";
		goto exit;
	};

	groupCtor3 = (*jenv)->GetMethodID (jenv, klassGroup, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if( NULL == groupCtor3 || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find appropriate ctor with 3 Strings at NTSidGroupPrincipal";
		goto exit;
	};
	groupCtor1 = (*jenv)->GetMethodID (jenv, klassGroup, "<init>", "(Ljava/lang/String;)V");
	if( NULL == groupCtor1 || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not find appropriate ctor at NTSidGroupPrincipal";
		goto exit;
	};

	// allocate an array 
	jgroups = (*jenv)->NewObjectArray (jenv, ptgs->GroupCount, klassGroup, NULL);

	if( NULL == jgroups || (*jenv)->ExceptionCheck (jenv) ) {
		errMsg = "Could not create array of NTSidGroupPrincipal";
		goto exit;
	};

	for( i=0; i<ptgs->GroupCount; i++ ) {

		ConvertSidToStringSid(ptgs->Groups[i].Sid, &lpStr2);

		if( !GetInfo(jenv, ptgs->Groups[i].Sid, &lpStr0, &lpStr1) ) {
			jobj1 = (*jenv)->NewObject (jenv, klassGroup, groupCtor1, (*jenv)->NewStringUTF (jenv, lpStr2));
			//printf("SET_FIELD: %d] Simple Group: %s\n", i, lpStr2 );
		}
		else {
			jobj1 = (*jenv)->NewObject (jenv, klassGroup, groupCtor3, 
				(*jenv)->NewStringUTF (jenv, lpStr2), (*jenv)->NewStringUTF (jenv, lpStr0), (*jenv)->NewStringUTF (jenv, lpStr1));
//			printf("SET_FIELD: %d] Group: %s@%s \n\t %s\n", i, lpStr0, lpStr1, lpStr2 );
		}
		if( NULL != lpStr0 ) { LocalFree(lpStr0); lpStr0 = NULL; }
		if( NULL != lpStr1 ) { LocalFree(lpStr1); lpStr1 = NULL; }
		if( NULL != lpStr2 ) { LocalFree(lpStr2); lpStr2 = NULL; }
		if( NULL == jobj1 || (*jenv)->ExceptionCheck (jenv) ) {
			goto exit;
		}
		(*jenv)->SetObjectArrayElement (jenv, jgroups, i, jobj1);
		if( (*jenv)->ExceptionCheck (jenv) ) {
			goto exit;
		}
	};
	(*jenv)->SetObjectField (jenv, thiz, jf_groups, jgroups);
	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}

	//
	// step +1: get itoken
	//

	//FIXME: on NT 'SecurityImpersonation'  is not supported. 
	// Check whether we support NT - just to be sure.
	if (!DuplicateToken (hUser, SecurityImpersonation, &iToken)) {
		errMsg = "Unable to duplicate impersonation token";
		goto exit;
	};

	// printf("_SET_FIELD: iToken: %d \n", ((long)iToken) );
	(*jenv)->SetLongField (jenv, thiz, jf_token, ((jlong)iToken));
	if( (*jenv)->ExceptionCheck (jenv) ) {
		goto exit;
	}

	dwError = 0;
exit:
	dwSaveError = GetLastError();

	if( NULL != lpUserData )		LocalFree(lpUserData);
	if( NULL != lpGroupData )		LocalFree(lpGroupData);
	if( NULL != lpAllGroupsData )	LocalFree(lpAllGroupsData);
	if( NULL != lpStr0 )			LocalFree(lpStr0);
	if( NULL != lpStr1 )			LocalFree(lpStr1);
	if( NULL != lpStr2 )			LocalFree(lpStr2);
	if( NULL != lpUserSid )			LocalFree(lpUserSid);
	if( NULL != lpDomainName)		LocalFree(lpDomainName);
	//
	if( NULL != domainSid )			FreeSid(domainSid);

	if( INVALID_HANDLE_VALUE != hUser ) CloseHandle(hUser);

	if( (*jenv)->ExceptionCheck (jenv) ) {
		(*jenv)->ExceptionDescribe (jenv);
	}
	else {
		if( (0 != dwError) || (NULL!=errMsg) ) {
			if( dwError == -1 ) {
				dwError = dwSaveError;
			}
			error((LPVOID)jenv, (LPCSTR)errMsg, dwError);
		}
	}
	return;
}

/*
* Class:     org_apache_harmony_auth_module_NTSystem
* Method:    free
* Signature: ()V
*/
JNIEXPORT void JNICALL
Java_org_apache_harmony_auth_module_NTSystem_free
(JNIEnv * jenv, jobject thiz)
{
	HANDLE hTok = (HANDLE)(*jenv)->GetLongField (jenv, thiz, jf_token);
	if( !(0 == hTok || INVALID_HANDLE_VALUE == hTok) ) {
		if( !CloseHandle(hTok) ) {
			error((LPVOID)jenv, (LPCSTR) "Unable to close handle", GetLastError());
		}
	}
}

void
error
(LPVOID lpJEnv, LPCSTR msg, DWORD dwErr)
{
	JNIEnv * jenv = (JNIEnv*)lpJEnv;
	LPVOID lpMsg = NULL;
	LPVOID lpFullMsg = NULL;
	DWORD args[3];
	jclass excl = NULL;

	if (!FormatMessage(
		FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL, dwErr, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
		(LPTSTR)&lpMsg, 0, NULL )) {
			// error in error(). 
		//assert(false);
	}

	args[0] = (DWORD)msg;
	args[1] = dwErr;
	args[2] = (DWORD)lpMsg;
	
	if (!FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_STRING|FORMAT_MESSAGE_ARGUMENT_ARRAY,
		"%1!s! (#%2!d!): \"%3!s!\"", 0, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
		(LPTSTR)&lpFullMsg, 0, (va_list*)args)) {
			// error in error(). 
	}

	excl = (*jenv)->FindClass (jenv, "java/lang/Error");
	if( NULL == excl ) {
		return;
	}
	(*jenv)->ThrowNew (jenv, excl, (LPCSTR)lpFullMsg);

	// Free the buffer.
	LocalFree( lpMsg );
	LocalFree( lpFullMsg );
}
