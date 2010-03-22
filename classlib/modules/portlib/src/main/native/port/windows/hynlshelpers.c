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

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @internal @file
 * @ingroup Port
 * @brief Native language support helpers
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include <winbase.h>
#include <stdlib.h>
#include <LMCONS.H>
#include <direct.h>

#include "hyport.h"
#include "portpriv.h"

#define CDEV_CURRENT_FUNCTION _prototypes_public
void nls_determine_locale (struct HyPortLibrary *portLibrary);
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION _prototypes_private
/* none *
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION tableMap

/* tableMap */

static const char *const CountryMap[] = {
  "LBR", "LR",
  "MAC", "MO",
  "LBY", "LY",
  "BHS", "BS",
  "SYC", "SC",
  "NIU", "NU",
  "AUT", "AT",
  "ESH", "EH",
  "PCN", "PN",
  "UKR", "UA",
  "GUY", "GY",
  "CAF", "CF",
  "GNQ", "GQ",
  "EST", "EE",
  "MDG", "MG",
  "SWZ", "SZ",
  "JAM", "JM",
  "SUR", "SR",
  "MNP", "MP",
  "MYT", "YT",
  "PNG", "PG",
  "TUV", "TV",
  "CHN", "CN",
  "TKM", "TM",
  "AGO", "AO",
  "BDI", "BI",
  "MDV", "MV",
  "BGD", "BD",
  "GUF", "GF",
  "PYF", "PF",
  "CYM", "KY",
  "SGS", "GS",
  "TUN", "TN",
  "IRL", "IE",
  "ISR", "IL",
  "BLR", "BY",
  "FSM", "FM",
  "ARM", "AM",
  "SEN", "SN",
  "BRB", "BB",
  "WLF", "WF",
  "MOZ", "MZ",
  "BEN", "BJ",
  "PRK", "KP",
  "IRQ", "IQ",
  "BLZ", "BZ",
  "COG", "CG",
  "MLT", "MT",
  "COK", "CK",
  "TMP", "TP",
  "BRN", "BN",
  "COM", "KM",
  "MEX", "MX",
  "ATG", "AG",
  "PRT", "PT",
  "KOR", "KR",
  "SLB", "SB",
  "POL", "PL",
  "ARE", "AE",
  "URY", "UY",
  "PLW", "PW",
  "FRO", "FO",
  "GIN", "GN",
  "SLV", "SV",
  "ATF", "TF",
  "MTQ", "MQ",
  "GRL", "GL",
  "ATA", "AQ",
  "ZAR", "ZR",
  "BIH", "BA",
  "AND", "AD",
  "CPV", "CV",
  "SPM", "PM",
  "GNB", "GW",
  "PRY", "PY",
  "GRD", "GD",
  "CHL", "CL",
  "GLP", "GP",
  "KAZ", "KZ",
  "PAK", "PK",
  "SWE", "SE",
  "SVK", "SK",
  "SVN", "SI",
  "FLK", "FK",
  "TUR", "TR",
  "DNK", "DK",
  "ABW", "AW",
  "TCD", "TD"
};

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION nls_determine_locale
/**
 * @internal
 * Set set locale.
 *
 * @param[in] portLibrary The port library
 */
void
nls_determine_locale (struct HyPortLibrary *portLibrary)
{
  OSVERSIONINFO versionInfo;
  LCID threadLocale;
  LCTYPE infoType;
  int length, i;
  char lang[8];
  char country[8];
  HyNLSDataCache *nls = &portLibrary->portGlobals->nls_data;

  PORT_ACCESS_FROM_PORT (portLibrary);

  versionInfo.dwOSVersionInfoSize = sizeof (OSVERSIONINFO);
  GetVersionEx (&versionInfo);

  /* Get the language */
  infoType = LOCALE_SABBREVLANGNAME;
  if (versionInfo.dwPlatformId == VER_PLATFORM_WIN32_NT)
    infoType = LOCALE_SISO639LANGNAME;

  threadLocale = GetThreadLocale ();

  length = GetLocaleInfo (threadLocale, infoType, &lang[0], sizeof (lang));
  if (length < 2)
    {
      strncpy (nls->language, "en", 2);
    }
  else
    {

#if defined(UNICODE)
      /* convert double byte to single byte */
      for (i = 0; i < length; i++)
	{
	  lang[i] = (char) ((short *) lang)[i];
	}
#endif

      if (infoType == LOCALE_SABBREVLANGNAME)
	lang[2] = 0;

      _strlwr (lang);

      // Not required for NT, Win32 gets it wrong
      if (!strcmp (lang, "jp"))
	strncpy (nls->language, "ja", 2);
      /* One platform gives ch instead of zh for Chinese */
      else if (!strcmp (lang, "ch"))
	strncpy (nls->language, "zh", 2);
      else
	strncpy (nls->language, lang, 3);
    }

  /* Get the region */
  infoType = LOCALE_SABBREVCTRYNAME;
  if (versionInfo.dwPlatformId == VER_PLATFORM_WIN32_NT)
    infoType = LOCALE_SISO3166CTRYNAME;

  length =
    GetLocaleInfo (threadLocale, infoType, &country[0], sizeof (country));
  if (length < 2)
    {
      strncpy (nls->region, "US", 2);
    }
  else
    {
#if defined(UNICODE)
      // convert double byte to single byte
      for (i = 0; i < length; i++)
	{
	  country[i] = (char) ((short *) country)[i];
	}
      country[3] = 0;
#endif

      if (versionInfo.dwPlatformId != VER_PLATFORM_WIN32_NT)
	{
	  // If not NT, lookup in the table to find the code
	  for (i = 0; i < sizeof (CountryMap) >> 2; i += 2)
	    {
	      if (strcmp (country, CountryMap[i]) == 0)
		{
		  strcpy (&country[0], CountryMap[i + 1]);
		  break;
		}
	    }
	}
      /* Countries which are not in the table use the first two letters. */
      country[2] = 0;
      strncpy (nls->region, country, 2);
    }
}

#undef CDEV_CURRENT_FUNCTION
