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
/**
* @author Alexey V. Varlamov, Marina V. Goldburt
*/

#include "port_sysinfo.h"

#include <windows.h>
#include <apr_strings.h>

typedef struct {
	const char* javaId;
	const char* winId;
} WindowsJavaMap;

/*
 * Below is the mapping between Windows-style time zones and Java-style timezones.
 * Windows timezones can be fetched using WinAPI, see
 * http://msdn.microsoft.com/library/default.asp?url=/library/en-us/sysinfo/base/gettimezoneinformation.asp,
 * Java-style timezones are taken from the Olson time database,
 * which can be found here:  ftp://elsie.nci.nih.gov/pub/
 */
static const WindowsJavaMap ZONE_MAP[] = { 
		"Asia/Kabul","Afghanistan Standard Time", 
		"America/Anchorage","Alaskan Standard Time",
		"America/New_York","Eastern Standard Time", 
		"Africa/Cairo","Egypt Standard Time",
		"Asia/Yekaterinburg","Ekaterinburg Standard Time",
		"Pacific/Fiji","Fiji Standard Time",
		"Europe/Helsinki","FLE Standard Time",
		"Europe/London","GMT Standard Time",
		"Asia/Riyadh","Arab Standard Time", 
		"Asia/Muscat","Arabian Standard Time", 
		"Asia/Baghdad","Arabic Standard Time", 
		"America/Halifax","Atlantic Standard Time",
		"Australia/Darwin","AUS Central Standard Time", 
		"Australia/Sydney","AUS Eastern Standard Time", 
		"Atlantic/Azores","Azores Standard Time", 
		"America/Regina","Canada Central Standard Time",
		"Atlantic/Cape_Verde","Cape Verde Standard Time",
		"Asia/Tbilisi","Caucasus Standard Time",
		"Australia/Adelaide","Cen. Australia Standard Time", 
		"America/Sao_Paulo","E. South America Standard Time",
		"America/Godthab","Greenland Standard Time", 
		"GMT","Greenwich Standard Time",
		"Europe/Athens","GTB Standard Time", 
		"Pacific/Honolulu","Hawaiian Standard Time",
		"Asia/Calcutta","India Standard Time",
		"Asia/Tehran","Iran Standard Time",
		"America/Regina","Central America Standard Time",
		"Asia/Dhaka","Central Asia Standard Time",
		"Europe/Prague","Central Europe Standard Time", 
		"Europe/Belgrade","Central European Standard Time", 
		"Pacific/Guadalcanal","Central Pacific Standard Time", 
		"America/Chicago","Central Standard Time",
		"Asia/Shanghai","China Standard Time", 
		"Pacific/Majuro","Dateline Standard Time",
		"Africa/Nairobi","E. Africa Standard Time",
		"Australia/Brisbane","E. Australia Standard Time",
		"Europe/Bucharest","E. Europe Standard Time", 
		"Asia/Jerusalem","Israel Standard Time",
		"America/Denver","Mountain Standard Time",
		"Asia/Rangoon","Myanmar Standard Time",
		"Asia/Novosibirsk","N. Central Asia Standard Time", 
		"Asia/Katmandu","Nepal Standard Time",
		"Pacific/Auckland","New Zealand Standard Time",
		"America/St_Johns","Newfoundland Standard Time",
		"Asia/Ulaanbaatar","North Asia East Standard Time",
		"Asia/Krasnoyarsk","North Asia Standard Time",
		"America/Caracas","Pacific SA Standard Time",
		"Asia/Seoul","Korea Standard Time",
		"America/Mexico_City","Mexico Standard Time",
		"America/Chihuahua","Mexico Standard Time 2",
		"Atlantic/South_Georgia","Mid-Atlantic Standard Time",
		"America/Los_Angeles","Pacific Standard Time",
		"Europe/Paris","Romance Standard Time",
		"Australia/Hobart","Tasmania Standard Time",
		"Asia/Tokyo","Tokyo Standard Time",
		"Pacific/Tongatapu","Tonga Standard Time",
		"America/Indianapolis","US Eastern Standard Time",
		"America/Phoenix","US Mountain Standard Time",
		"Asia/Vladivostok","Vladivostok Standard Time",
		"Australia/Perth","W. Australia Standard Time",
		"Africa/Luanda","W. Central Africa Standard Time",
		"Europe/Berlin","W. Europe Standard Time",
		"Europe/Moscow","Russian Standard Time",
		"America/Buenos_Aires","SA Eastern Standard Time",
		"America/Bogota","SA Pacific Standard Time",
		"America/Caracas","SA Western Standard Time",
		"Pacific/Apia","Samoa Standard Time",
		"Asia/Bangkok","SE Asia Standard Time",
		"Asia/Singapore","Singapore Standard Time",
		"Africa/Harare","South Africa Standard Time",
		"Asia/Colombo","Sri Lanka Standard Time",
		"Asia/Taipei","Taipei Standard Time",
		"Asia/Karachi","West Asia Standard Time",
		"Pacific/Guam","West Pacific Standard Time",
		"Asia/Yakutsk","Yakutsk Standard Time",
		0, 0
};

static const char* REG_KEY = 
	"System\\CurrentControlSet\\Control\\TimeZoneInformation";

APR_DECLARE(apr_status_t) port_user_timezone(char** tzname,
										 apr_pool_t* pool){

	HKEY hkey;
	apr_status_t rv = RegOpenKeyEx(HKEY_LOCAL_MACHINE, 		
		REG_KEY, 0, KEY_QUERY_VALUE, &hkey);

	if (ERROR_SUCCESS == rv) {
		char buffer[100];
		DWORD size = sizeof(buffer);
		int found = 0;
		memset(buffer, 0, size);
		rv = RegQueryValueEx(hkey, "StandardName", NULL, NULL, 
			(LPBYTE)buffer,	&size);
		if (ERROR_SUCCESS == rv) {
			int i = 0;
			for (; ZONE_MAP[i].javaId; i++) {
				if (strcmp(buffer, ZONE_MAP[i].winId) == 0) {
					*tzname = apr_pstrdup(pool, ZONE_MAP[i].javaId);
					++found;
					break;
				}
			}
		}
		if (!found) {
			/* At least try to obtain GMT offset for local time */
			I_32 bias = 0;
			DWORD size = sizeof(bias);
			rv = RegQueryValueEx(hkey, "ActiveTimeBias", NULL, NULL, 
				(LPBYTE)&bias, &size);
			if (ERROR_SUCCESS == rv) {
				*tzname = apr_psprintf(pool, "GMT%+d:%02d", (-bias)/60, abs(bias)%60);
			}
		}

		RegCloseKey(hkey);
	} 
	return (ERROR_SUCCESS == rv) ? APR_SUCCESS : APR_FROM_OS_ERROR(rv);
}
