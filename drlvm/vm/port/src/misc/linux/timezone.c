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
* @author Alexey V. Varlamov, Artem A. Aliev
*/  

#include <unistd.h>
#include <apr_file_info.h>
#include <apr_strings.h>
#include <apr_file_io.h>
#include "port_sysinfo.h"

static const char* const zone_dir = "/usr/share/zoneinfo";

static int search_zoneinfo(char** tzname, const char* dir_name, 
						   const apr_finfo_t* file_info, void* orig_content,
						   apr_pool_t* pool) {
	int found = 0;
	apr_dir_t* dir;
	apr_finfo_t entry;
	const apr_int32_t wanted = APR_FINFO_NAME | APR_FINFO_TYPE | APR_FINFO_INODE;
	apr_status_t rv = apr_dir_open(&dir, dir_name, pool);
	if (APR_SUCCESS == rv) {
		char path[80];
		while (!found && APR_SUCCESS == apr_dir_read(&entry, wanted, dir) ) {
			switch (entry.filetype) {
				case APR_DIR: 
					if (strcmp(entry.name, ".") && strcmp(entry.name,"..")) {
						strcpy(path, dir_name);
						strcat(path, "/");
						strcat(path, entry.name);
						found = search_zoneinfo(tzname, path, file_info, 
							orig_content, pool);
					}
					break;
				case APR_REG:
					strcpy(path, dir_name);
					strcat(path, "/");
					strcat(path, entry.name);
					if (entry.inode == file_info->inode) {
						*tzname = apr_pstrdup(pool, path + strlen(zone_dir) + 1);
						++found;
					} else {
						apr_finfo_t fi;
						if (APR_SUCCESS == apr_stat(&fi, path, APR_FINFO_SIZE, pool)
							&& fi.size == file_info->size) {
							apr_file_t *ff;
							rv = apr_file_open(&ff, path, APR_FOPEN_READ, 0, pool);
							if (APR_SUCCESS == rv) {
								size_t size = fi.size/sizeof(char) + sizeof(char);
								char buf[size];
								memset(buf, 0, sizeof(buf));
								rv = apr_file_read(ff, buf, &size);
								apr_file_close(ff);
								if (APR_SUCCESS == rv 
									&& !memcmp(orig_content, buf, fi.size)){
									*tzname = apr_pstrdup(pool, buf + strlen(zone_dir) + 1);
									++found;
								}
							}
						}
					}
					break;
				default:
					break;
			}
		}
		apr_dir_close(dir);
	}

	return found;
}

APR_DECLARE(apr_status_t) port_user_timezone(char** tzname,
											 apr_pool_t* pool){

/*
 * Unfortunately there is no straightforward or at least reliable way
 * to detect user time zone name.
 * tzset()&tzname give at most an abbreviation of the time zone, 
 * moreover ambiguous. Though it is possible to compose a (static) table
 * which maps abbreviation + GMT offset to more comprehensive name.
 * 
 * Here we prefer to use an alternative approach, also not very nice 
 * but somewhat more dynamic. We'll try to analyze system configuration files
 * directly, see below for details.
 */
	int found = 0;
	apr_file_t *ff;

	/* if /etc/timezone exists, it just contains timezone name */
	apr_status_t rv = apr_file_open(&ff, "/etc/timezone", APR_FOPEN_READ, 0, pool);
	if (APR_SUCCESS == rv) {
		char buf[50];
		rv = apr_file_gets(buf, 50, ff);
		apr_file_close(ff);
		if (APR_SUCCESS == rv){
			*tzname = apr_pstrdup(pool, buf);
			++found;
		}
	}
	
	if (!found) {
		/* 
		 * if /etc/localtime exists, it should be either a symlink to or 
		 * a copy of one of the files in /usr/share/zoneinfo directory.
		 * All we need is to find out a name of that file.
		 */
		apr_finfo_t fi;
		char* cur_file = "/etc/localtime";
		char buf[80];
		apr_int32_t wanted = APR_FINFO_LINK | APR_FINFO_SIZE | APR_FINFO_INODE;

		while (APR_SUCCESS == (rv = apr_stat(&fi, cur_file, wanted, pool)) 
			&& APR_LNK == fi.filetype) {

			memset(buf, 0, sizeof(buf));
			if (readlink(cur_file, buf, sizeof(buf)-1) == -1) {
				break;
			}
			cur_file = buf;

			if (strncmp(zone_dir, cur_file, strlen(zone_dir)) == 0) {
				*tzname = apr_pstrdup(pool, cur_file + strlen(zone_dir) + 1);
				++found;
				break;
			}
		}
		
		if (!found && rv == APR_SUCCESS) {
			/* 
			* the file exists but is not a symlink, 
			* let's try to find a copy in /usr/share/zoneinfo directory 
			*/
			rv = apr_file_open(&ff, cur_file, APR_FOPEN_READ, 0, pool);
			if (APR_SUCCESS == rv) {
				size_t size = fi.size/sizeof(char) + sizeof(char);
				char buf[size];
				memset(buf, 0, sizeof(buf));
				rv = apr_file_read(ff, buf, &size);
				apr_file_close(ff);
				if (APR_SUCCESS == rv)
				{
					fi.fname = cur_file;
					found = search_zoneinfo(tzname, zone_dir, &fi, buf, pool);
				}
			}
		}
	}

	if (!found){
		/* /etc/sysconfig/clock usually contains smth like TIMEZONE=<tz_name> */
		rv = apr_file_open(&ff, "/etc/sysconfig/clock", APR_FOPEN_READ, 0, pool);
		if (APR_SUCCESS == rv) {
			const char * pref = "ZONE=";
			char buf[60];
			char *pos;
			while(APR_SUCCESS == apr_file_gets(buf, 60, ff)) {
				if ((pos = strstr(buf, pref))) {
					*tzname = apr_pstrdup(pool, pos + strlen(pref));
					++found;
					break;
				}
			}
			apr_file_close(ff);
		}
	}

	return (found) ? APR_SUCCESS : APR_ENOENT;
}
