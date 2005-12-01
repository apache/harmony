/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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



package com.ibm.oti.locale;
public class Locale_no extends java.util.ListResourceBundle {
protected Object[][] getContents() {
	Object[][] contents = {
		{"First_Day",new java.lang.Integer(2),},
		{"Minimal_Days",new java.lang.Integer(4),},
		{"months",new String[]{"januar","februar","mars","april","mai","juni","juli","august","september","oktober","november","desember","",},
},
		{"shortMonths",new String[]{"jan","feb","mar","apr","mai","jun","jul","aug","sep","okt","nov","des","",},
},
		{"weekdays",new String[]{"","s\u00f8ndag","mandag","tirsdag","onsdag","torsdag","fredag","l\u00f8rdag",},
},
		{"shortWeekdays",new String[]{"","s\u00f8","ma","ti","on","to","fr","l\u00f8",},
},
		{"Date_SHORT","dd.MM.yy",},
		{"Date_MEDIUM","dd.MMM.yyyy",},
		{"Date_LONG","d. MMMM yyyy",},
		{"Date_FULL","d. MMMM yyyy",},
		{"Time_SHORT","HH:mm",},
		{"Time_MEDIUM","HH:mm:ss",},
		{"Time_LONG","HH:mm:ss z",},
		{"Time_FULL","'kl 'HH.mm z",},
		{"DecimalPatternChars","0#,\u00a0;%\u2030E,-",},
	};
return contents;
}
}
