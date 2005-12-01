/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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
public class Locale_et extends java.util.ListResourceBundle {
protected Object[][] getContents() {
	Object[][] contents = {
		{"First_Day",new java.lang.Integer(2),},
		{"Minimal_Days",new java.lang.Integer(4),},
		{"LocalPatternChars","GanjkHmsSEDFwWxhKzZ",},
		{"eras",new String[]{"e.m.a.","m.a.j.",},
},
		{"months",new String[]{"Jaanuar","Veebruar","M\u00e4rts","Aprill","Mai","Juuni","Juuli","August","September","Oktoober","November","Detsember","",},
},
		{"shortMonths",new String[]{"Jaan","Veebr","M\u00e4rts","Apr","Mai","Juuni","Juuli","Aug","Sept","Okt","Nov","Dets","",},
},
		{"weekdays",new String[]{"","p\u00fchap\u00e4ev","esmasp\u00e4ev","teisip\u00e4ev","kolmap\u00e4ev","neljap\u00e4ev","reede","laup\u00e4ev",},
},
		{"shortWeekdays",new String[]{"","P","E","T","K","N","R","L",},
},
		{"Date_SHORT","d.MM.yy",},
		{"Date_MEDIUM","d.MM.yyyy",},
		{"Date_LONG","EEEE, d. MMMM yyyy. 'a'",},
		{"Date_FULL","EEEE, d. MMMM yyyy",},
		{"Time_SHORT","H:mm",},
		{"Time_MEDIUM","H:mm:ss",},
		{"Time_LONG","H:mm:ss z",},
		{"Time_FULL","H:mm:ss z",},
		{"DecimalPatternChars","0#,\u00a0;%\u2030E,-",},
	};
return contents;
}
}
