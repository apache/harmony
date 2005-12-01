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
public class Locale_nl extends java.util.ListResourceBundle {
protected Object[][] getContents() {
	Object[][] contents = {
		{"First_Day",new java.lang.Integer(2),},
		{"Minimal_Days",new java.lang.Integer(4),},
		{"months",new String[]{"januari","februari","maart","april","mei","juni","juli","augustus","september","oktober","november","december","",},
},
		{"shortMonths",new String[]{"jan","feb","mrt","apr","mei","jun","jul","aug","sep","okt","nov","dec","",},
},
		{"weekdays",new String[]{"","zondag","maandag","dinsdag","woensdag","donderdag","vrijdag","zaterdag",},
},
		{"shortWeekdays",new String[]{"","zo","ma","di","wo","do","vr","za",},
},
		{"Date_SHORT","d-M-yy",},
		{"Date_MEDIUM","d-MMM-yyyy",},
		{"Date_LONG","d MMMM yyyy",},
		{"Date_FULL","EEEE d MMMM yyyy",},
		{"Time_SHORT","H:mm",},
		{"Time_MEDIUM","H:mm:ss",},
		{"Time_LONG","H:mm:ss z",},
		{"Time_FULL","H:mm:ss' uur' z",},
		{"DecimalPatternChars","0#,.;%\u2030E,-",},
	};
return contents;
}
}
